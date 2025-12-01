package org.rva.gold;

import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base class cho các Gold jobs riêng biệt.
 * Mỗi job xử lý 1 Gold table để tránh concurrent commit issues.
 */
public abstract class GoldJobBase {

    protected static final String STREAMING_HINT = "/*+ OPTIONS('streaming'='true', 'monitor-interval'='5s') */";

    protected StreamTableEnvironment setupEnvironment() {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(30_000L, CheckpointingMode.EXACTLY_ONCE);
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env);

        // Cấu hình streaming
        tEnv.getConfig().set("execution.runtime-mode", "streaming");
        tEnv.getConfig().set("table.dynamic-table-options.enabled", "true");

        // Catalog config
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

        return tEnv;
    }

    protected static String getenv(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isEmpty()) ? def : v;
    }

    protected static String firstNotBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }
}
