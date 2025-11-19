package org.rva.silver;

import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;
import org.rva.silver.udf.ParseDetections;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SilverJob {
    public static void main(String[] args) throws Exception {
        EnvironmentSettings settings = EnvironmentSettings.newInstance()
            .inStreamingMode()
            .build();
        TableEnvironment tEnv = TableEnvironment.create(settings);

        // Đăng ký UDTF parse JSON
        tEnv.createTemporarySystemFunction("parse_detections", ParseDetections.class);

        // Lấy cấu hình từ ENV (không hardcode secrets)
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

        // Tạo bảng Silver (Iceberg)
        String createSilver = String.join("\n",
            "CREATE TABLE IF NOT EXISTS rva.silver_detections (",
            "  schema_version  STRING,",
            "  pipeline_run_id STRING,",
            "  store_id        STRING,",
            "  camera_id       STRING,",
            "  frame_index     BIGINT,",
            "  capture_ts      TIMESTAMP(3),",
            "  img_w           INT,",
            "  img_h           INT,",
            "  det_id          STRING,",
            "  class_name      STRING,",
            "  class_id        INT,",
            "  conf            DOUBLE,",
            "  bbox_x1         INT,",
            "  bbox_y1         INT,",
            "  bbox_x2         INT,",
            "  bbox_y2         INT,",
            "  track_id        BIGINT,",
            "  processing_ts   TIMESTAMP_LTZ(3)",
            ") WITH (",
            "  'format-version' = '2',",
            "  'write.format.default' = 'parquet',",
            "  'partitioning' = 'store_id,bucket(16, camera_id),days(capture_ts)'",
            ")"
        );
        tEnv.executeSql(createSilver);

        // Chạy INSERT streaming: dùng UDTF để tránh phụ thuộc FROM_JSON/JSON_TABLE
        String insertSql = String.join("\n",
            "INSERT INTO rva.silver_detections",
            "SELECT",
            "  b.schema_version,",
            "  b.pipeline_run_id,",
            "  b.store_id,",
            "  b.camera_id,",
            "  b.frame_index,",
            "  TO_TIMESTAMP_LTZ(t.capture_ts_ms, 3) AS capture_ts,",
            "  t.img_w,",
            "  t.img_h,",
            "  t.det_id,",
            "  t.class_name,",
            "  t.class_id,",
            "  t.conf,",
            "  t.bbox_x1,",
            "  t.bbox_y1,",
            "  t.bbox_x2,",
            "  t.bbox_y2,",
            "  t.track_id,",
            "  CURRENT_TIMESTAMP AS processing_ts",
            "FROM rva.bronze_raw AS b,",
            "LATERAL TABLE(parse_detections(b.payload)) AS t"
        );

        // Gửi job (streaming) – để Detached mode khi submit qua 'flink run -d'
        tEnv.executeSql(insertSql);

        // Không block; khi submit bằng -d job sẽ chạy nền.
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

