-- =============================================================================
-- SILVER PROCESSING (Bronze -> Silver) - Flink 1.18 + Iceberg REST Catalog
-- Mục tiêu: bản SQL tối thiểu, tránh computed/metadata columns, dùng partition đơn giản.
-- =============================================================================

-- 1) Khai báo Iceberg REST Catalog (giữ tối thiểu, không hardcode secrets)
CREATE CATALOG lakehouse WITH (
  'type' = 'iceberg',
  'catalog-impl' = 'org.apache.iceberg.rest.RESTCatalog',
  'uri' = 'http://iceberg-rest:8181',
  'warehouse' = 's3a://warehouse',
  'io-impl' = 'org.apache.iceberg.aws.s3.S3FileIO',
  's3.endpoint' = 'http://minio:9000',
  's3.path-style-access' = 'true',
  'client.region' = 'us-east-1',
  's3.region' = 'us-east-1'
);

USE CATALOG lakehouse;
USE rva;

-- 2) Tạo bảng Silver tối thiểu (KHÔNG dùng computed/metadata columns)
CREATE TABLE IF NOT EXISTS rva.silver_detections (
  -- Metadata kế thừa
  schema_version        VARCHAR,
  pipeline_run_id       VARCHAR,
  store_id              VARCHAR,
  camera_id             VARCHAR,
  frame_index           BIGINT,
  capture_ts            TIMESTAMP(3),
  img_w                 INTEGER,
  img_h                 INTEGER,

  -- Dữ liệu detection (đã bóc tách)
  det_id                VARCHAR,
  class_name            VARCHAR,
  class_id              INTEGER,
  conf                  DOUBLE,

  -- Bounding box (pixel)
  bbox_x1               INTEGER,
  bbox_y1               INTEGER,
  bbox_x2               INTEGER,
  bbox_y2               INTEGER,

  -- Tracking
  track_id              BIGINT,

  -- Các cột hệ thống do job ghi
  processing_ts         TIMESTAMP_LTZ(3),
  capture_date          DATE
)
PARTITIONED BY (
  store_id,
  camera_id,
  capture_date
)
WITH (
  'format-version' = '2',
  'write.format.default' = 'parquet'
);

-- Dùng INSERT đơn giản (không WITH) để tránh lỗi parser
INSERT INTO rva.silver_detections
SELECT
  B.schema_version,
  B.pipeline_run_id,
  B.store_id,
  B.camera_id,
  B.frame_index,
  CAST(JSON_VALUE(B.payload, '$.capture_ts') AS TIMESTAMP(3)) AS capture_ts,
  CAST(JSON_VALUE(B.payload, '$.image_size.width') AS INTEGER) AS img_w,
  CAST(JSON_VALUE(B.payload, '$.image_size.height') AS INTEGER) AS img_h,
  d.det_id,
  COALESCE(d.`class`, 'person') AS class_name,
  d.class_id,
  d.conf,
  d.bbox.x1 AS bbox_x1,
  d.bbox.y1 AS bbox_y1,
  d.bbox.x2 AS bbox_x2,
  d.bbox.y2 AS bbox_y2,
  COALESCE(d.track_id, -1) AS track_id,
  CURRENT_TIMESTAMP AS processing_ts,
  CAST(JSON_VALUE(B.payload, '$.capture_ts') AS DATE) AS capture_date
FROM (
  SELECT
    Br.*,
    FROM_JSON(
      JSON_QUERY(Br.payload, '$.detections'),
      ARRAY<ROW(
        det_id STRING,
        `class` STRING,
        class_id INT,
        conf DOUBLE,
        bbox ROW<x1 INT, y1 INT, x2 INT, y2 INT>,
        track_id BIGINT
      )>
    ) AS dets
  FROM rva.bronze_raw AS Br
) B
CROSS JOIN UNNEST(B.dets) AS T(d);
