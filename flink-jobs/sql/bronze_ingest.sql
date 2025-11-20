-- 0. Đảm bảo database mặc định của built-in catalog tồn tại
CREATE DATABASE IF NOT EXISTS default_catalog.`default`;

-- 1. Define Source Table (Read from Pulsar) - Use fully qualified name in default_catalog
CREATE TABLE IF NOT EXISTS default_catalog.`default`.pulsar_source_raw (
    `value` STRING,
    `event_time` TIMESTAMP(3) METADATA FROM 'publish_time',
    `properties` MAP<STRING, STRING> METADATA FROM 'properties'
) WITH (
    'connector' = 'pulsar',
    'topics' = 'persistent://retail/metadata/events',
    'service-url' = 'pulsar://pulsar-broker:6650',
    'source.start.message-id' = 'earliest',
    'pulsar.consumer.subscription-name' = 'flink-bronze-ingest-v3',
    'format' = 'raw'
);

-- 2. Define Sink Table (Write to Iceberg Bronze)
CREATE TABLE IF NOT EXISTS iceberg.retail.bronze_detections (
    ingest_ts TIMESTAMP(3),
    publish_ts TIMESTAMP(3),
    raw_payload STRING,
    source_properties MAP<STRING, STRING>
);

-- 3. Insert Job (Streaming ETL)
INSERT INTO iceberg.retail.bronze_detections
SELECT 
    CURRENT_TIMESTAMP,
    event_time,
    `value`,
    properties
FROM default_catalog.`default`.pulsar_source_raw;
