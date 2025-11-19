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

        // Tạo bảng GOLD (nếu chưa có)
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
