-- Switch to in-memory catalog for Pulsar source definition
-- (Iceberg catalog cannot store Pulsar connector tables)
USE CATALOG default_catalog;

-- 1. Define Source Table (Read from Pulsar)
CREATE TABLE IF NOT EXISTS pulsar_source_raw (
    `value` STRING,
    `event_time` TIMESTAMP(3) METADATA FROM 'publish_time',
    `properties` MAP<STRING, STRING> METADATA FROM 'properties'
) WITH (
    'connector' = 'pulsar',
    'topics' = 'persistent://retail/metadata/events',
    'service-url' = 'pulsar://pulsar-broker:6650',
    'source.start.message-id' = 'earliest',
    'format' = 'raw'
);

-- 2. Define Sink Table (Write to Iceberg Bronze)
-- We reference the iceberg catalog explicitly
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
FROM pulsar_source_raw;
