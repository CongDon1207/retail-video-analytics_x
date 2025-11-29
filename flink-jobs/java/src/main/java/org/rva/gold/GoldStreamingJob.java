package org.rva.gold;

import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.StatementSet; // <-- Import mới
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GoldStreamingJob: Phiên bản Real-time (Optimized).
 * - Sử dụng StatementSet để gom tất cả logic vào 1 JOB DUY NHẤT.
 * - Tiết kiệm tài nguyên, tránh lỗi Restart.
 */
public class GoldStreamingJob {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(30_000L, CheckpointingMode.EXACTLY_ONCE);
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env);

        // --- 1. SETUP CATALOG (Giữ nguyên) ---
        Map<String, String> cfg = new LinkedHashMap<>();
        cfg.put("type", "iceberg");
        cfg.put("catalog-impl", "org.apache.iceberg.rest.RESTCatalog");
        cfg.put("uri", getenv("ICEBERG_REST_URI", "http://iceberg-rest:8181"));
        cfg.put("warehouse", getenv("ICEBERG_WAREHOUSE", "s3://warehouse/iceberg"));
        cfg.put("io-impl", "org.apache.iceberg.aws.s3.S3FileIO");
        cfg.put("s3.endpoint", getenv("S3_ENDPOINT", "http://minio:9000"));
        cfg.put("s3.path-style-access", getenv("S3_PATH_STYLE", "true"));
        cfg.put("s3.region", getenv("S3_REGION", "us-east-1"));

        String accessKey = firstNotBlank(System.getenv("MINIO_ROOT_USER"), System.getenv("AWS_ACCESS_KEY_ID"));
        String secretKey = firstNotBlank(System.getenv("MINIO_ROOT_PASSWORD"), System.getenv("AWS_SECRET_ACCESS_KEY"));
        if (accessKey != null) cfg.put("s3.access-key-id", accessKey);
        if (secretKey != null) cfg.put("s3.secret-access-key", secretKey);

        String catalogSql = "CREATE CATALOG lakehouse WITH (" + cfg.entrySet().stream()
            .map(e -> "'" + e.getKey() + "' = '" + e.getValue() + "'")
            .collect(Collectors.joining(", ")) + ")";

        tEnv.executeSql(catalogSql);
        tEnv.executeSql("USE CATALOG lakehouse");
        tEnv.executeSql("USE rva");

        // --- 2. TẠO BẢNG (Giữ nguyên phần DDL) ---
        
        // Bảng 1
        tEnv.executeSql("DROP TABLE IF EXISTS gold_minute_by_cam");
        tEnv.executeSql("CREATE TABLE gold_minute_by_cam (store_id STRING, camera_id STRING, ts_minute TIMESTAMP(3), detection_count BIGINT, unique_tracks BIGINT, avg_conf DOUBLE, bbox_area_ratio_avg DOUBLE, PRIMARY KEY (store_id, camera_id, ts_minute) NOT ENFORCED) WITH ('format-version'='2', 'write.upsert.enabled'='true', 'write.format.default'='parquet')");

        // Bảng 2
        tEnv.executeSql("DROP TABLE IF EXISTS gold_hour_by_cam");
        tEnv.executeSql("CREATE TABLE gold_hour_by_cam (store_id STRING, camera_id STRING, ts_hour TIMESTAMP(3), detection_count BIGINT, unique_tracks BIGINT, avg_conf DOUBLE, PRIMARY KEY (store_id, camera_id, ts_hour) NOT ENFORCED) WITH ('format-version'='2', 'write.upsert.enabled'='true', 'write.format.default'='parquet')");

        // Bảng 3
        tEnv.executeSql("DROP TABLE IF EXISTS gold_people_per_minute");
        tEnv.executeSql("CREATE TABLE gold_people_per_minute (store_id STRING, camera_id STRING, ts_minute TIMESTAMP(3), detections BIGINT, unique_people BIGINT, PRIMARY KEY (store_id, camera_id, ts_minute) NOT ENFORCED) WITH ('format-version'='2', 'write.upsert.enabled'='true', 'write.format.default'='parquet')");

        // Bảng 4
        tEnv.executeSql("DROP TABLE IF EXISTS gold_zone_heatmap");
        tEnv.executeSql("CREATE TABLE gold_zone_heatmap (store_id STRING, camera_id STRING, zone_x INT, zone_y INT, hits BIGINT, unique_tracks BIGINT, PRIMARY KEY (store_id, camera_id, zone_x, zone_y) NOT ENFORCED) WITH ('format-version'='2', 'write.upsert.enabled'='true', 'write.format.default'='parquet')");

        // Bảng 5
        tEnv.executeSql("DROP TABLE IF EXISTS gold_zone_dwell");
        tEnv.executeSql("CREATE TABLE gold_zone_dwell (store_id STRING, camera_id STRING, zone_x INT, zone_y INT, visits BIGINT, total_dwell_seconds DOUBLE, avg_dwell_seconds DOUBLE, PRIMARY KEY (store_id, camera_id, zone_x, zone_y) NOT ENFORCED) WITH ('format-version'='2', 'write.upsert.enabled'='true', 'write.format.default'='parquet')");

        // Bảng 6
        tEnv.executeSql("DROP TABLE IF EXISTS gold_track_summary");
        tEnv.executeSql("CREATE TABLE gold_track_summary (store_id STRING, camera_id STRING, track_id BIGINT, frames BIGINT, start_time TIMESTAMP(3), end_time TIMESTAMP(3), duration_seconds DOUBLE, min_x DOUBLE, max_x DOUBLE, delta_x DOUBLE, min_y DOUBLE, max_y DOUBLE, delta_y DOUBLE, avg_x DOUBLE, avg_y DOUBLE, avg_conf DOUBLE, PRIMARY KEY (store_id, camera_id, track_id) NOT ENFORCED) WITH ('format-version'='2', 'write.upsert.enabled'='true', 'write.format.default'='parquet')");

        // --- 3. OPTIMIZED EXECUTION: GOM NHIỀU INSERT VÀO 1 JOB ---
        StatementSet statementSet = tEnv.createStatementSet(); // <--- CHÌA KHÓA Ở ĐÂY

        // Add Job 1
        statementSet.addInsertSql(
            "INSERT INTO gold_minute_by_cam " +
            "SELECT store_id, camera_id, FLOOR(capture_ts TO MINUTE), COUNT(*), COUNT(DISTINCT track_id), AVG(conf), " +
            "AVG( (CAST(bbox_x2 AS DOUBLE) - CAST(bbox_x1 AS DOUBLE)) * (CAST(bbox_y2 AS DOUBLE) - CAST(bbox_y1 AS DOUBLE)) / NULLIF(CAST(img_w AS DOUBLE) * CAST(img_h AS DOUBLE), 0.0) ) " +
            "FROM rva.silver_detections /*+ OPTIONS('streaming'='true', 'monitor-interval'='1s') */ " +
            "GROUP BY store_id, camera_id, FLOOR(capture_ts TO MINUTE)"
        );

        // Add Job 2
        statementSet.addInsertSql(
            "INSERT INTO gold_hour_by_cam " +
            "SELECT store_id, camera_id, FLOOR(capture_ts TO HOUR), COUNT(*), COUNT(DISTINCT track_id), AVG(conf) " +
            "FROM rva.silver_detections /*+ OPTIONS('streaming'='true', 'monitor-interval'='1s') */ " +
            "GROUP BY store_id, camera_id, FLOOR(capture_ts TO HOUR)"
        );

        // Add Job 3
        statementSet.addInsertSql(
            "INSERT INTO gold_people_per_minute " +
            "SELECT store_id, camera_id, FLOOR(capture_ts TO MINUTE), COUNT(*), COUNT(DISTINCT track_id) " +
            "FROM rva.silver_detections /*+ OPTIONS('streaming'='true', 'monitor-interval'='1s') */ " +
            "GROUP BY store_id, camera_id, FLOOR(capture_ts TO MINUTE)"
        );

        // Add Job 4
        statementSet.addInsertSql(
            "INSERT INTO gold_zone_heatmap " +
            "SELECT store_id, camera_id, CAST(FLOOR(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0) / 128.0) AS INT), CAST(FLOOR(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0) / 72.0) AS INT), COUNT(*), COUNT(DISTINCT track_id) " +
            "FROM rva.silver_detections /*+ OPTIONS('streaming'='true', 'monitor-interval'='1s') */ " +
            "GROUP BY store_id, camera_id, CAST(FLOOR(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0) / 128.0) AS INT), CAST(FLOOR(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0) / 72.0) AS INT)"
        );

        // Add Job 5
        statementSet.addInsertSql(
            "INSERT INTO gold_zone_dwell " +
            "WITH per_track_zone AS (SELECT store_id, camera_id, track_id, CAST(FLOOR(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0) / 128.0) AS INT) AS zone_x, CAST(FLOOR(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0) / 72.0) AS INT) AS zone_y, MIN(capture_ts) AS start_time, MAX(capture_ts) AS end_time, CAST(TIMESTAMPDIFF(SECOND, MIN(capture_ts), MAX(capture_ts)) AS DOUBLE) AS dwell_seconds FROM rva.silver_detections /*+ OPTIONS('streaming'='true', 'monitor-interval'='1s') */ GROUP BY store_id, camera_id, track_id, CAST(FLOOR(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0) / 128.0) AS INT), CAST(FLOOR(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0) / 72.0) AS INT)) " +
            "SELECT store_id, camera_id, zone_x, zone_y, COUNT(*), SUM(dwell_seconds), AVG(dwell_seconds) FROM per_track_zone GROUP BY store_id, camera_id, zone_x, zone_y"
        );

        // Add Job 6
        statementSet.addInsertSql(
            "INSERT INTO gold_track_summary " +
            "SELECT store_id, camera_id, track_id, COUNT(*), MIN(capture_ts), MAX(capture_ts), CAST(TIMESTAMPDIFF(SECOND, MIN(capture_ts), MAX(capture_ts)) AS DOUBLE), MIN(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)), MAX(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)), MAX(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)) - MIN(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)), MIN(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)), MAX(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)), MAX(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)) - MIN(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)), AVG(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)), AVG(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)), AVG(conf) " +
            "FROM rva.silver_detections /*+ OPTIONS('streaming'='true', 'monitor-interval'='1s') */ " +
            "GROUP BY store_id, camera_id, track_id"
        );

        // Kích hoạt tất cả trong 1 Job
        statementSet.execute();
    }

    private static String getenv(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isEmpty()) ? def : v;
    }

    private static String firstNotBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }
}