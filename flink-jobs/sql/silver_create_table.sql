-- Dùng đúng Iceberg catalog đã tạo ở bronze_ingest.sql
USE CATALOG lakehouse;

-- Tạo DB nếu chưa có
CREATE DATABASE IF NOT EXISTS rva;

-- Bảng Silver chuẩn hoá từ bronze_raw.payload
CREATE TABLE IF NOT EXISTS rva.silver_detections (
  schema_version  STRING,
  pipeline_run_id STRING,
  store_id        STRING,
  camera_id       STRING,
  frame_index     BIGINT,
  capture_ts      TIMESTAMP_LTZ(3),
  img_w           INT,
  img_h           INT,
  det_id          STRING,
  class_name      STRING,
  class_id        INT,
  conf            DOUBLE,
  bbox_x1         DOUBLE,
  bbox_y1         DOUBLE,
  bbox_x2         DOUBLE,
  bbox_y2         DOUBLE,
  centroid_x      INT,
  centroid_y      INT,
  track_id        BIGINT,
  processing_ts   TIMESTAMP_LTZ(3)
)
WITH (
  'format-version' = '2',
  'write.format.default' = 'parquet',
  -- Khai báo partition spec theo Iceberg (KHÔNG dùng PARTITIONED BY)
  'partitioning' = 'identity(store_id), bucket(16, camera_id), days(capture_ts)'
);
