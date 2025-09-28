-- Iceberg REST catalog trên MinIO (S3FileIO)
CREATE CATALOG lakehouse WITH (
  'type' = 'iceberg',
  'catalog-impl' = 'org.apache.iceberg.rest.RESTCatalog',
  'uri' = 'http://iceberg-rest:8181',
  'warehouse' = 's3://warehouse/iceberg',
  'io-impl' = 'org.apache.iceberg.aws.s3.S3FileIO',
  's3.endpoint' = 'http://minio:9000',
  's3.path-style-access' = 'true',
  's3.access-key-id' = 'minioadmin',
  's3.secret-access-key' = 'minioadmin123',
  'client.region' = 'us-east-1',
  's3.region' = 'us-east-1'
);

USE CATALOG lakehouse;
CREATE DATABASE IF NOT EXISTS rva;

CREATE TABLE IF NOT EXISTS rva.bronze_raw (
  schema_version STRING,
  pipeline_run_id STRING,
  frame_index BIGINT,
  payload STRING,
  camera_id STRING,
  store_id STRING,
  ingest_ts TIMESTAMP_LTZ(3)
)
PARTITIONED BY (store_id)
WITH (
  'format-version' = '2',
  'write.format.default' = 'parquet'
);

CREATE TEMPORARY TABLE pulsar_source (
  schema_version STRING,
  pipeline_run_id STRING,
  frame_index BIGINT,
  payload STRING,
  camera_id STRING METADATA FROM 'properties.camera_id',
  store_id STRING METADATA FROM 'properties.store_id',
  event_time TIMESTAMP_LTZ(3) METADATA FROM 'event-time' VIRTUAL,
  publish_ts TIMESTAMP_LTZ(3) METADATA FROM 'publish-time' VIRTUAL
) WITH (
  'connector' = 'pulsar',
  'topics' = 'persistent://retail/metadata/events',
  'service-url' = 'pulsar://pulsar-broker:6650',

  'source.start.message-id' = 'earliest',
  'format' = 'avro'
);

INSERT INTO rva.bronze_raw
SELECT schema_version,
       pipeline_run_id,
       frame_index,
       payload,
       COALESCE(camera_id, 'unknown') AS camera_id,
       COALESCE(store_id, 'unknown') AS store_id,
       COALESCE(event_time, publish_ts, CURRENT_TIMESTAMP) AS ingest_ts
FROM pulsar_source;
