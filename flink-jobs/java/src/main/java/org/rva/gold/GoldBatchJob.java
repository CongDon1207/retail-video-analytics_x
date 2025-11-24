package org.rva.gold;

import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GoldBatchJob: Tính các bảng GOLD (minute/hour) từ Silver bằng chế độ BATCH.
 * - Đảm bảo idempotent bằng INSERT OVERWRITE.
 * - Đọc cấu hình Iceberg/S3 từ biến môi trường (không hardcode).
 *
 * Tham số chạy (tùy chọn):
 *   --mode=both|minute|hour (mặc định both)
 *   --from=2025-11-13T06:00:00Z  --to=2025-11-13T07:00:00Z  (lọc khoảng thời gian; nếu bỏ trống sẽ tính toàn bộ)
 */
public class GoldBatchJob {

    public static void main(String[] args) {
        // Batch mode để xử lý dữ liệu hữu hạn và hỗ trợ overwrite an toàn
        EnvironmentSettings settings = EnvironmentSettings.newInstance()
            .inBatchMode()
            .build();
        TableEnvironment tEnv = TableEnvironment.create(settings);

        // Parse tham số đơn giản dạng --key=value
        Map<String, String> params = parseArgs(args);
        String mode = params.getOrDefault("mode", "both").toLowerCase(Locale.ROOT);
        String fromIso = params.get("from");
        String toIso = params.get("to");

        // CATALOG Iceberg (REST) – lấy từ ENV
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
        tEnv.executeSql("CREATE DATABASE IF NOT EXISTS rva");
        tEnv.executeSql("USE rva");

        // Tạo bảng GOLD (nếu chưa có) – gồm cả bảng cũ (minute/hour)
        // và các bảng Gold giống notebook (people_per_minute, zone_heatmap, zone_dwell, track_summary)
        tEnv.executeSql(String.join("\n",
            "CREATE TABLE IF NOT EXISTS rva.gold_minute_by_cam (",
            "  store_id STRING,",
            "  camera_id STRING,",
            "  ts_minute TIMESTAMP(3),",
            "  detection_count BIGINT,",
            "  unique_tracks BIGINT,",
            "  avg_conf DOUBLE,",
            "  bbox_area_ratio_avg DOUBLE",
            ") WITH (",
            "  'format-version' = '2',",
            "  'write.format.default' = 'parquet',",
            "  'partitioning' = 'store_id,days(ts_minute)'",
            ")"
        ));

        tEnv.executeSql(String.join("\n",
            "CREATE TABLE IF NOT EXISTS rva.gold_hour_by_cam (",
            "  store_id STRING,",
            "  camera_id STRING,",
            "  ts_hour TIMESTAMP(3),",
            "  detection_count BIGINT,",
            "  unique_tracks BIGINT,",
            "  avg_conf DOUBLE",
            ") WITH (",
            "  'format-version' = '2',",
            "  'write.format.default' = 'parquet',",
            "  'partitioning' = 'store_id,days(ts_hour)'",
            ")"
        ));

        tEnv.executeSql(String.join("\n",
            "CREATE TABLE IF NOT EXISTS rva.gold_people_per_minute (",
            "  store_id STRING,",
            "  camera_id STRING,",
            "  ts_minute TIMESTAMP(3),",
            "  detections BIGINT,",
            "  unique_people BIGINT",
            ") WITH (",
            "  'format-version' = '2',",
            "  'write.format.default' = 'parquet',",
            "  'partitioning' = 'store_id,days(ts_minute)'",
            ")"
        ));

        tEnv.executeSql(String.join("\n",
            "CREATE TABLE IF NOT EXISTS rva.gold_zone_heatmap (",
            "  store_id STRING,",
            "  camera_id STRING,",
            "  zone_x INT,",
            "  zone_y INT,",
            "  hits BIGINT,",
            "  unique_tracks BIGINT",
            ") WITH (",
            "  'format-version' = '2',",
            "  'write.format.default' = 'parquet'",
            ")"
        ));

        tEnv.executeSql(String.join("\n",
            "CREATE TABLE IF NOT EXISTS rva.gold_zone_dwell (",
            "  store_id STRING,",
            "  camera_id STRING,",
            "  zone_x INT,",
            "  zone_y INT,",
            "  visits BIGINT,",
            "  total_dwell_seconds DOUBLE,",
            "  avg_dwell_seconds DOUBLE",
            ") WITH (",
            "  'format-version' = '2',",
            "  'write.format.default' = 'parquet'",
            ")"
        ));

        tEnv.executeSql(String.join("\n",
            "CREATE TABLE IF NOT EXISTS rva.gold_track_summary (",
            "  store_id STRING,",
            "  camera_id STRING,",
            "  track_id BIGINT,",
            "  frames BIGINT,",
            "  start_time TIMESTAMP(3),",
            "  end_time TIMESTAMP(3),",
            "  duration_seconds DOUBLE,",
            "  min_x DOUBLE,",
            "  max_x DOUBLE,",
            "  delta_x DOUBLE,",
            "  min_y DOUBLE,",
            "  max_y DOUBLE,",
            "  delta_y DOUBLE,",
            "  avg_x DOUBLE,",
            "  avg_y DOUBLE,",
            "  avg_conf DOUBLE",
            ") WITH (",
            "  'format-version' = '2',",
            "  'write.format.default' = 'parquet'",
            ")"
        ));

        // WHERE clause theo khoảng thời gian (tùy chọn)
        String where = timeFilter(fromIso, toIso);

        if (mode.equals("minute") || mode.equals("both")) {
            String sql = String.join("\n",
                "INSERT OVERWRITE rva.gold_minute_by_cam",
                "SELECT",
                "  store_id,",
                "  camera_id,",
                "  FLOOR(capture_ts TO MINUTE) AS ts_minute,",
                "  COUNT(*) AS detection_count,",
                "  COUNT(DISTINCT track_id) AS unique_tracks,",
                "  AVG(conf) AS avg_conf,",
                // đơn giản, đã chuẩn hóa bbox ở UDTF nên hiệu lực >= 0
                "  AVG( (CAST(bbox_x2 AS DOUBLE) - CAST(bbox_x1 AS DOUBLE)) *",
                "       (CAST(bbox_y2 AS DOUBLE) - CAST(bbox_y1 AS DOUBLE)) /",
                "       NULLIF(CAST(img_w AS DOUBLE) * CAST(img_h AS DOUBLE), 0.0) ) AS bbox_area_ratio_avg",
                "FROM rva.silver_detections",
                where,
                "GROUP BY store_id, camera_id, FLOOR(capture_ts TO MINUTE)"
            );
            tEnv.executeSql(sql);
        }

        if (mode.equals("hour") || mode.equals("both")) {
            String sql = String.join("\n",
                "INSERT OVERWRITE rva.gold_hour_by_cam",
                "SELECT",
                "  store_id,",
                "  camera_id,",
                "  FLOOR(capture_ts TO HOUR) AS ts_hour,",
                "  COUNT(*) AS detection_count,",
                "  COUNT(DISTINCT track_id) AS unique_tracks,",
                "  AVG(conf) AS avg_conf",
                "FROM rva.silver_detections",
                where,
                "GROUP BY store_id, camera_id, FLOOR(capture_ts TO HOUR)"
            );
            tEnv.executeSql(sql);
        }

        // Bảng Gold giống notebook: 1) gold_people_per_minute
        String peoplePerMinuteSql = String.join("\n",
            "INSERT OVERWRITE rva.gold_people_per_minute",
            "SELECT",
            "  store_id,",
            "  camera_id,",
            "  FLOOR(capture_ts TO MINUTE) AS ts_minute,",
            "  COUNT(*) AS detections,",
            "  COUNT(DISTINCT track_id) AS unique_people",
            "FROM rva.silver_detections",
            "GROUP BY store_id, camera_id, FLOOR(capture_ts TO MINUTE)"
        );
        tEnv.executeSql(peoplePerMinuteSql);

        // 2) gold_zone_heatmap – chia lưới 10x10 trên khung 1280x720, dùng centroid từ bbox
        String heatmapSql = String.join("\n",
            "INSERT OVERWRITE rva.gold_zone_heatmap",
            "SELECT",
            "  store_id,",
            "  camera_id,",
            "  CAST(FLOOR(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0) / 128.0) AS INT) AS zone_x,",
            "  CAST(FLOOR(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0) / 72.0) AS INT) AS zone_y,",
            "  COUNT(*) AS hits,",
            "  COUNT(DISTINCT track_id) AS unique_tracks",
            "FROM rva.silver_detections",
            "GROUP BY",
            "  store_id,",
            "  camera_id,",
            "  CAST(FLOOR(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0) / 128.0) AS INT),",
            "  CAST(FLOOR(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0) / 72.0) AS INT)"
        );
        tEnv.executeSql(heatmapSql);

        // 3) gold_zone_dwell – dwell theo track_id + zone
        String dwellSql = String.join("\n",
            "INSERT OVERWRITE rva.gold_zone_dwell",
            "WITH per_track_zone AS (",
            "  SELECT",
            "    store_id,",
            "    camera_id,",
            "    track_id,",
            "    CAST(FLOOR(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0) / 128.0) AS INT) AS zone_x,",
            "    CAST(FLOOR(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0) / 72.0) AS INT) AS zone_y,",
            "    MIN(capture_ts) AS start_time,",
            "    MAX(capture_ts) AS end_time,",
            "    CAST(TIMESTAMPDIFF(SECOND, MIN(capture_ts), MAX(capture_ts)) AS DOUBLE) AS dwell_seconds",
            "  FROM rva.silver_detections",
            "  GROUP BY",
            "    store_id,",
            "    camera_id,",
            "    track_id,",
            "    CAST(FLOOR(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0) / 128.0) AS INT),",
            "    CAST(FLOOR(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0) / 72.0) AS INT)",
            ")",
            "SELECT",
            "  store_id,",
            "  camera_id,",
            "  zone_x,",
            "  zone_y,",
            "  COUNT(*) AS visits,",
            "  SUM(dwell_seconds) AS total_dwell_seconds,",
            "  AVG(dwell_seconds) AS avg_dwell_seconds",
            "FROM per_track_zone",
            "GROUP BY store_id, camera_id, zone_x, zone_y"
        );
        tEnv.executeSql(dwellSql);

        // 4) gold_track_summary – thống kê theo track
        String trackSummarySql = String.join("\n",
            "INSERT OVERWRITE rva.gold_track_summary",
            "SELECT",
            "  store_id,",
            "  camera_id,",
            "  track_id,",
            "  COUNT(*) AS frames,",
            "  MIN(capture_ts) AS start_time,",
            "  MAX(capture_ts) AS end_time,",
            "  CAST(TIMESTAMPDIFF(SECOND, MIN(capture_ts), MAX(capture_ts)) AS DOUBLE) AS duration_seconds,",
            "  MIN(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)) AS min_x,",
            "  MAX(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)) AS max_x,",
            "  MAX(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)) -",
            "    MIN(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)) AS delta_x,",
            "  MIN(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)) AS min_y,",
            "  MAX(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)) AS max_y,",
            "  MAX(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)) -",
            "    MIN(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)) AS delta_y,",
            "  AVG(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)) AS avg_x,",
            "  AVG(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)) AS avg_y,",
            "  AVG(conf) AS avg_conf",
            "FROM rva.silver_detections",
            "GROUP BY store_id, camera_id, track_id"
        );
        tEnv.executeSql(trackSummarySql);
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new LinkedHashMap<>();
        if (args == null) return m;
        for (String a : args) {
            if (a == null) continue;
            String s = a.trim();
            if (s.startsWith("--") && s.contains("=")) {
                int i = s.indexOf('=');
                String k = s.substring(2, i);
                String v = s.substring(i + 1);
                m.put(k, v);
            }
        }
        return m;
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

    // Tạo WHERE clause nếu có tham số from/to (ISO-8601), tránh lỗi parser
    private static String timeFilter(String fromIso, String toIso) {
        if ((fromIso == null || fromIso.isBlank()) && (toIso == null || toIso.isBlank())) {
            return ""; // không filter
        }
        DateTimeFormatter f = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);
        StringBuilder sb = new StringBuilder("WHERE 1=1");
        try {
            if (fromIso != null && !fromIso.isBlank()) {
                Instant from = Instant.from(f.parse(fromIso));
                sb.append(" AND capture_ts >= TIMESTAMP '" + toLocalTs(from) + "'");
            }
        } catch (Exception ignore) { /* bỏ filter nếu parse lỗi */ }
        try {
            if (toIso != null && !toIso.isBlank()) {
                Instant to = Instant.from(f.parse(toIso));
                sb.append(" AND capture_ts < TIMESTAMP '" + toLocalTs(to) + "'");
            }
        } catch (Exception ignore) { /* bỏ filter nếu parse lỗi */ }
        return sb.toString();
    }

    private static String toLocalTs(Instant instantUtc) {
        // Flink TIMESTAMP literal không hỗ trợ 'Z', cần định dạng 'yyyy-MM-dd HH:mm:ss.SSS'
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC)
            .format(instantUtc);
    }
}
