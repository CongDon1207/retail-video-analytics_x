package org.rva;

import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import static org.apache.flink.table.api.Expressions.$;
import static org.apache.flink.table.api.Expressions.currentTimestamp;

/**
 * Bronze ingest job: Pulsar -> Iceberg.
 * Đọc raw JSON từ topic retail/metadata/events, ghi vào bảng Iceberg lakehouse.rva.bronze_raw.
 */
public class BronzeIngestJob {

    public static void main(String[] args) throws Exception {
        // 1. Setup Environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // Cấu hình Checkpoint (BẮT BUỘC cho Iceberg Sink)
        env.enableCheckpointing(60_000L, CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(30_000L);
        
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env);

        // 2. Tạo Iceberg Catalog (Lakehouse)
        tEnv.executeSql(
            "CREATE CATALOG lakehouse WITH (" +
            "  'type' = 'iceberg'," +
            "  'catalog-impl' = 'org.apache.iceberg.rest.RESTCatalog'," +
            "  'uri' = 'http://iceberg-rest:8181'," +
            "  'warehouse' = 's3://warehouse'," +
            "  'io-impl' = 'org.apache.iceberg.aws.s3.S3FileIO'," +
            "  's3.endpoint' = 'http://minio:9000'," +
            "  's3.path-style-access' = 'true'," +
            "  's3.access-key-id' = 'minioadmin'," +
            "  's3.secret-access-key' = 'minioadmin123'," +
            "  'client.region' = 'us-east-1'," +
            "  's3.region' = 'us-east-1'" +
            ")"
        );

        tEnv.executeSql("CREATE DATABASE IF NOT EXISTS lakehouse.rva");
        tEnv.useCatalog("lakehouse");
        tEnv.useDatabase("rva");

        // 3. Tạo bảng Bronze (nếu chưa có) - Match với schema đã tồn tại
        tEnv.executeSql(
            "CREATE TABLE IF NOT EXISTS bronze_raw (" +
            "  schema_version STRING," +
            "  pipeline_run_id STRING," +
            "  frame_index BIGINT," +
            "  payload STRING," +
            "  camera_id STRING," +
            "  store_id STRING," +
            "  ingest_ts TIMESTAMP(6)" +
            ") PARTITIONED BY (store_id) " +
            "WITH ('format-version'='2', 'write.format.default'='parquet')"
        );

        // 4. Tạo Pulsar Source Table
        tEnv.executeSql(
            "CREATE TEMPORARY TABLE pulsar_source (" +
            "  raw_payload STRING," +
            "  event_time AS PROCTIME()" +
            ") WITH (" +
            "  'connector' = 'pulsar'," +
            "  'service-url' = 'pulsar://pulsar-broker:6650'," +
            "  'topics' = 'persistent://retail/metadata/events'," +
            "  'format' = 'raw'," +
            "  'source.subscription-name' = 'flink-bronze-java-sub'," +
            "  'source.start.message-id' = 'earliest'" +
            ")"
        );

        // 5. Transform & Insert
        tEnv.executeSql(
            "INSERT INTO bronze_raw " +
            "SELECT " +
            "  'v1' AS schema_version, " +
            "  CAST(NULL AS STRING) AS pipeline_run_id, " +
            "  CAST(NULL AS BIGINT) AS frame_index, " +
            "  raw_payload AS payload, " +
            "  JSON_VALUE(raw_payload, '$.source.camera_id') AS camera_id, " +
            "  JSON_VALUE(raw_payload, '$.source.store_id') AS store_id, " +
            "  CURRENT_TIMESTAMP AS ingest_ts " +
            "FROM pulsar_source"
        );
    }
}

