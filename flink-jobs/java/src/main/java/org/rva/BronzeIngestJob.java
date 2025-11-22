package org.rva;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.pulsar.source.PulsarSource;
import org.apache.flink.connector.pulsar.source.enumerator.startup.StartupMode;
import org.apache.flink.connector.pulsar.source.reader.deserializer.PulsarDeserializationSchema;
import org.apache.flink.connector.pulsar.table.PulsarCatalogSupport;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableDescriptor;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.apache.flink.table.api.Expressions.$;

/**
 * Bronze ingest job: Pulsar -> Iceberg.
 * Đọc raw JSON từ topic retail/metadata/events, ghi vào bảng Iceberg retail.bronze_detections.
 */
public class BronzeIngestJob {

    public static void main(String[] args) throws Exception {
        ParameterTool params = ParameterTool.fromArgs(args);

        // Cho phép override URL Pulsar qua args nếu cần
        String pulsarServiceUrl = params.get("pulsar.service-url", "pulsar://pulsar-broker:6650");
        String topic = params.get("pulsar.topic", "persistent://retail/metadata/events");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(60_000L, CheckpointingMode.EXACTLY_ONCE);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(30_000L);
        env.setStateBackend(new HashMapStateBackend());

        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env);

        // Pulsar source: đọc raw payload dưới dạng String
        PulsarSource<String> pulsarSource = PulsarSource
            .builder()
            .setServiceUrl(pulsarServiceUrl)
            .setAdminUrl("http://pulsar-broker:8082")
            .setStartCursor(StartupMode.EARLIEST)
            .setTopics(topic)
            .setDeserializationSchema(
                PulsarDeserializationSchema.flinkSchema(new SimpleStringSchema()))
            .build();

        DataStream<String> rawStream = env.fromSource(
            pulsarSource,
            WatermarkStrategy.forBoundedOutOfOrderness(Duration.ZERO),
            "pulsar_bronze_source");

        // Đưa DataStream vào Table, thêm ingest_ts
        Table bronzed = tEnv.fromDataStream(
            rawStream,
            Schema
                .newBuilder()
                .column("raw_payload", "STRING")
                .columnByExpression("ingest_ts", "CURRENT_TIMESTAMP")
                .build()
        );

        // Đăng ký Iceberg catalog giống SQL job
        Map<String, String> catalogOptions = new HashMap<>();
        catalogOptions.put("type", "iceberg");
        catalogOptions.put("catalog-impl", "org.apache.iceberg.rest.RESTCatalog");
        catalogOptions.put("uri", "http://iceberg-rest:8181");
        catalogOptions.put("warehouse", "s3a://warehouse");
        catalogOptions.put("io-impl", "org.apache.iceberg.aws.s3.S3FileIO");
        catalogOptions.put("s3.endpoint", "http://minio:9000");
        catalogOptions.put("s3.path-style-access", "true");
        catalogOptions.put("s3.access-key-id", "minioadmin");
        catalogOptions.put("s3.secret-access-key", "minioadmin123");
        catalogOptions.put("s3.region", "us-east-1");

        tEnv.createCatalog("iceberg", PulsarCatalogSupport.newIcebergCatalog("iceberg", catalogOptions));
        tEnv.useCatalog("iceberg");
        tEnv.executeSql("CREATE DATABASE IF NOT EXISTS retail");
        tEnv.useDatabase("retail");

        // Tạo bảng Bronze nếu chưa có
        tEnv.createTable(
            "bronze_detections",
            TableDescriptor.forConnector("iceberg")
                .schema(Schema.newBuilder()
                    .column("ingest_ts", "TIMESTAMP(3)")
                    .column("publish_ts", "TIMESTAMP(3)")
                    .column("raw_payload", "STRING")
                    .column("source_properties", "MAP<STRING, STRING>")
                    .build())
                .option("write.format.default", "parquet")
                .build()
        );

        // Ghi vào Bronze: publish_ts/source_properties hiện để null, chỉ để un-block flow
        tEnv.from(bronzed)
            .select(
                $("ingest_ts"),
                $("ingest_ts").as("publish_ts"),
                $("raw_payload"),
                $("source_properties")  // sẽ là NULL, vì upstream chưa parse properties
            )
            .executeInsert("bronze_detections");

        env.execute("BronzeIngestJob");
    }
}

