package org.rva.gold;

import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.StatementSet;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GoldStreamingJob: Phiên bản Real-time với SQL Hints inline trong mỗi INSERT.
 * - Set execution.runtime-mode = streaming và table.dynamic-table-options.enabled = true
 * - Dùng SQL Hints inline trong mỗi SELECT FROM clause
 */
public class GoldStreamingJob {

    private static final String STREAMING_HINT = "/*+ OPTIONS('streaming'='true', 'monitor-interval'='5s') */";

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(30_000L, CheckpointingMode.EXACTLY_ONCE);
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env);

        // --- 1. CẤU HÌNH QUAN TRỌNG CHO STREAMING ---
        tEnv.getConfig().set("execution.runtime-mode", "streaming");
        tEnv.getConfig().set("table.dynamic-table-options.enabled", "true");

        // --- 2. CATALOG CONFIG ---
        String icebergRestUri = getenv("ICEBERG_REST_URI", "http://iceberg-rest:8181");
        String warehouse = getenv("ICEBERG_WAREHOUSE", "s3://warehouse/iceberg");
        String s3Endpoint = getenv("S3_ENDPOINT", "http://minio:9000");
        String accessKey = firstNotBlank(System.getenv("MINIO_ROOT_USER"), System.getenv("AWS_ACCESS_KEY_ID"));
        String secretKey = firstNotBlank(System.getenv("MINIO_ROOT_PASSWORD"), System.getenv("AWS_SECRET_ACCESS_KEY"));

        Map<String, String> cfg = new LinkedHashMap<>();
        cfg.put("type", "iceberg");
        cfg.put("catalog-impl", "org.apache.iceberg.rest.RESTCatalog");
        cfg.put("uri", icebergRestUri);
        cfg.put("warehouse", warehouse);
        cfg.put("io-impl", "org.apache.iceberg.aws.s3.S3FileIO");
        cfg.put("s3.endpoint", s3Endpoint);
        cfg.put("s3.path-style-access", getenv("S3_PATH_STYLE", "true"));
        cfg.put("s3.region", getenv("S3_REGION", "us-east-1"));
        if (accessKey != null) cfg.put("s3.access-key-id", accessKey);
        if (secretKey != null) cfg.put("s3.secret-access-key", secretKey);

        String catalogSql = "CREATE CATALOG lakehouse WITH (" + cfg.entrySet().stream()
            .map(e -> "'" + e.getKey() + "' = '" + e.getValue() + "'")
            .collect(Collectors.joining(", ")) + ")";

        tEnv.executeSql(catalogSql);
        tEnv.executeSql("USE CATALOG lakehouse");
        tEnv.executeSql("USE rva");

        // --- 3. TẠO GOLD TABLES (DDL) - Dùng CREATE TABLE IF NOT EXISTS ---
        tEnv.executeSql("CREATE TABLE IF NOT EXISTS gold_minute_by_cam (store_id STRING, camera_id STRING, ts_minute TIMESTAMP(3), detection_count BIGINT, unique_tracks BIGINT, avg_conf DOUBLE, bbox_area_ratio_avg DOUBLE, PRIMARY KEY (store_id, camera_id, ts_minute) NOT ENFORCED) WITH ('format-version'='2', 'write.upsert.enabled'='true', 'write.format.default'='parquet')");

        tEnv.executeSql("CREATE TABLE IF NOT EXISTS gold_hour_by_cam (store_id STRING, camera_id STRING, ts_hour TIMESTAMP(3), detection_count BIGINT, unique_tracks BIGINT, avg_conf DOUBLE, PRIMARY KEY (store_id, camera_id, ts_hour) NOT ENFORCED) WITH ('format-version'='2', 'write.upsert.enabled'='true', 'write.format.default'='parquet')");

        tEnv.executeSql("CREATE TABLE IF NOT EXISTS gold_people_per_minute (store_id STRING, camera_id STRING, ts_minute TIMESTAMP(3), detections BIGINT, unique_people BIGINT, PRIMARY KEY (store_id, camera_id, ts_minute) NOT ENFORCED) WITH ('format-version'='2', 'write.upsert.enabled'='true', 'write.format.default'='parquet')");

        tEnv.executeSql("CREATE TABLE IF NOT EXISTS gold_zone_heatmap (store_id STRING, camera_id STRING, zone_x INT, zone_y INT, hits BIGINT, unique_tracks BIGINT, PRIMARY KEY (store_id, camera_id, zone_x, zone_y) NOT ENFORCED) WITH ('format-version'='2', 'write.upsert.enabled'='true', 'write.format.default'='parquet')");

        tEnv.executeSql("CREATE TABLE IF NOT EXISTS gold_zone_dwell (store_id STRING, camera_id STRING, zone_x INT, zone_y INT, visits BIGINT, total_dwell_seconds DOUBLE, avg_dwell_seconds DOUBLE, PRIMARY KEY (store_id, camera_id, zone_x, zone_y) NOT ENFORCED) WITH ('format-version'='2', 'write.upsert.enabled'='true', 'write.format.default'='parquet')");

        tEnv.executeSql("CREATE TABLE IF NOT EXISTS gold_track_summary (store_id STRING, camera_id STRING, track_id BIGINT, frames BIGINT, start_time TIMESTAMP(3), end_time TIMESTAMP(3), duration_seconds DOUBLE, min_x DOUBLE, max_x DOUBLE, delta_x DOUBLE, min_y DOUBLE, max_y DOUBLE, delta_y DOUBLE, avg_x DOUBLE, avg_y DOUBLE, avg_conf DOUBLE, PRIMARY KEY (store_id, camera_id, track_id) NOT ENFORCED) WITH ('format-version'='2', 'write.upsert.enabled'='true', 'write.format.default'='parquet')");

        // --- 4. STATEMENTSET: GOM TẤT CẢ INSERT VÀO 1 JOB ---
        // Mỗi INSERT sử dụng streaming hint trực tiếp trong FROM clause
        StatementSet statementSet = tEnv.createStatementSet();

        statementSet.addInsertSql(
            "INSERT INTO gold_minute_by_cam " +
            "SELECT store_id, camera_id, FLOOR(capture_ts TO MINUTE), COUNT(*), COUNT(DISTINCT track_id), AVG(conf), " +
            "AVG( (CAST(bbox_x2 AS DOUBLE) - CAST(bbox_x1 AS DOUBLE)) * (CAST(bbox_y2 AS DOUBLE) - CAST(bbox_y1 AS DOUBLE)) / NULLIF(CAST(img_w AS DOUBLE) * CAST(img_h AS DOUBLE), 0.0) ) " +
            "FROM silver_detections " + STREAMING_HINT + " " +
            "GROUP BY store_id, camera_id, FLOOR(capture_ts TO MINUTE)"
        );

        statementSet.addInsertSql(
            "INSERT INTO gold_hour_by_cam " +
            "SELECT store_id, camera_id, FLOOR(capture_ts TO HOUR), COUNT(*), COUNT(DISTINCT track_id), AVG(conf) " +
            "FROM silver_detections " + STREAMING_HINT + " " +
            "GROUP BY store_id, camera_id, FLOOR(capture_ts TO HOUR)"
        );

        statementSet.addInsertSql(
            "INSERT INTO gold_people_per_minute " +
            "SELECT store_id, camera_id, FLOOR(capture_ts TO MINUTE), COUNT(*), COUNT(DISTINCT track_id) " +
            "FROM silver_detections " + STREAMING_HINT + " " +
            "GROUP BY store_id, camera_id, FLOOR(capture_ts TO MINUTE)"
        );

        statementSet.addInsertSql(
            "INSERT INTO gold_zone_heatmap " +
            "SELECT store_id, camera_id, " +
            "CAST(FLOOR(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0) / 128.0) AS INT), " +
            "CAST(FLOOR(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0) / 72.0) AS INT), " +
            "COUNT(*), COUNT(DISTINCT track_id) " +
            "FROM silver_detections " + STREAMING_HINT + " " +
            "GROUP BY store_id, camera_id, " +
            "CAST(FLOOR(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0) / 128.0) AS INT), " +
            "CAST(FLOOR(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0) / 72.0) AS INT)"
        );

        statementSet.addInsertSql(
            "INSERT INTO gold_zone_dwell " +
            "WITH per_track_zone AS (" +
            "  SELECT store_id, camera_id, track_id, " +
            "    CAST(FLOOR(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0) / 128.0) AS INT) AS zone_x, " +
            "    CAST(FLOOR(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0) / 72.0) AS INT) AS zone_y, " +
            "    MIN(capture_ts) AS start_time, " +
            "    MAX(capture_ts) AS end_time, " +
            "    CAST(TIMESTAMPDIFF(SECOND, MIN(capture_ts), MAX(capture_ts)) AS DOUBLE) AS dwell_seconds " +
            "  FROM silver_detections " + STREAMING_HINT + " " +
            "  GROUP BY store_id, camera_id, track_id, " +
            "    CAST(FLOOR(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0) / 128.0) AS INT), " +
            "    CAST(FLOOR(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0) / 72.0) AS INT)" +
            ") " +
            "SELECT store_id, camera_id, zone_x, zone_y, COUNT(*), SUM(dwell_seconds), AVG(dwell_seconds) " +
            "FROM per_track_zone " +
            "GROUP BY store_id, camera_id, zone_x, zone_y"
        );

        statementSet.addInsertSql(
            "INSERT INTO gold_track_summary " +
            "SELECT store_id, camera_id, track_id, COUNT(*), MIN(capture_ts), MAX(capture_ts), " +
            "CAST(TIMESTAMPDIFF(SECOND, MIN(capture_ts), MAX(capture_ts)) AS DOUBLE), " +
            "MIN(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)), " +
            "MAX(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)), " +
            "MAX(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)) - MIN(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)), " +
            "MIN(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)), " +
            "MAX(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)), " +
            "MAX(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)) - MIN(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)), " +
            "AVG(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)), " +
            "AVG(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)), " +
            "AVG(conf) " +
            "FROM silver_detections " + STREAMING_HINT + " " +
            "GROUP BY store_id, camera_id, track_id"
        );

        // Execute tất cả trong 1 job
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
