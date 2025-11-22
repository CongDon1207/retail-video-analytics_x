-- ============================================
-- Bronze Layer Ingestion: Pulsar -> Iceberg (JSON)
-- ============================================

-- 1. Iceberg REST catalog trên MinIO (S3FileIO)
CREATE CATALOG lakehouse WITH (
  'type' = 'iceberg',
  'catalog-impl' = 'org.apache.iceberg.rest.RESTCatalog',
  'uri' = 'http://iceberg-rest:8181',
  'warehouse' = 's3://warehouse',
  'io-impl' = 'org.apache.iceberg.aws.s3.S3FileIO',
  's3.endpoint' = 'http://minio:9000',
  's3.path-style-access' = 'true',
  's3.access-key-id' = 'minioadmin',
  's3.secret-access-key' = 'minioadmin123',
  'client.region' = 'us-east-1',
  's3.region' = 'us-east-1'
);

-- 2. Dùng catalog + database riêng cho Bronze
USE CATALOG lakehouse;
CREATE DATABASE IF NOT EXISTS rva;
USE rva;

-- 3. Bronze table: lưu raw payload + thông tin camera/store
CREATE TABLE IF NOT EXISTS bronze_raw (
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

-- 4. Pulsar source: đọc JSON từ topic retail/metadata/events với nested structure
CREATE TEMPORARY TABLE pulsar_source (
  schema_version STRING,
  pipeline_run_id STRING,
  source ROW<store_id STRING, camera_id STRING, stream_id STRING>,
  frame_index BIGINT,
  capture_ts TIMESTAMP(3),
  image_size ROW<width INT, height INT>,
  detections ARRAY<ROW<
    det_id STRING,
    `class` STRING,
    class_id INT,
    conf DOUBLE,
    bbox ROW<x1 DOUBLE, y1 DOUBLE, x2 DOUBLE, y2 DOUBLE>,
    track_id BIGINT
  >>,
  runtime ROW<model_name STRING, tracker_type STRING, conf_thres DOUBLE, class_filter ARRAY<INT>>,
  source_uri STRING
) WITH (
  'connector' = 'pulsar',
  'topics' = 'persistent://retail/metadata/events',
  'service-url' = 'pulsar://pulsar-broker:6650',
  'source.start.message-id' = 'earliest',
  'source.subscription-name' = 'flink-bronze-ingest',
  'format' = 'json'
);

-- 5. Streaming INSERT: Pulsar → Iceberg Bronze (flatten và explode detections)
INSERT INTO bronze_raw
SELECT
  schema_version,
  pipeline_run_id,
  frame_index,
  CAST(NULL AS STRING) AS payload,  -- Để null vì không cần lưu raw payload nữa
  source.camera_id AS camera_id,
  source.store_id AS store_id,
  CURRENT_TIMESTAMP AS ingest_ts
FROM pulsar_source;
