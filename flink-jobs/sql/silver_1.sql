-- =============================================================================
-- SILVER_1.SQL
-- Mục tiêu: Tách dữ liệu từ Bronze (rva.bronze_raw) → Silver (rva.silver_detections)
-- Chạy độc lập: script tự tạo CATALOG, DB, TABLE và thực thi INSERT streaming.
-- Lưu ý: yêu cầu Flink 1.18, Iceberg trên MinIO, Pulsar đã đẩy dữ liệu vào Bronze.
-- =============================================================================

-- 1) KHAI BÁO CATALOG (giống các file setup hiện có)
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
USE rva;

-- 2) TẠO BẢNG SILVER (nếu chưa có)
CREATE TABLE IF NOT EXISTS rva.silver_detections (
  schema_version  STRING,
  pipeline_run_id STRING,
  store_id        STRING,
  camera_id       STRING,
  frame_index     BIGINT,
  capture_ts      TIMESTAMP(3),
  img_w           INT,
  img_h           INT,
  det_id          STRING,
  class_name      STRING,
  class_id        INT,
  conf            DOUBLE,
  bbox_x1         INT,
  bbox_y1         INT,
  bbox_x2         INT,
  bbox_y2         INT,
  track_id        BIGINT,
  processing_ts   TIMESTAMP_LTZ(3)
)
WITH (
  'format-version' = '2',
  'write.format.default' = 'parquet',
  -- Khai báo partition transform qua thuộc tính Iceberg để tránh lỗi parser Flink
  'partitioning' = 'store_id,bucket(16, camera_id),days(capture_ts)'
);

-- Sử dụng FROM_JSON + UNNEST (ổn định hơn JSON_TABLE trên Flink 1.18)
INSERT INTO rva.silver_detections (
  schema_version, pipeline_run_id, store_id, camera_id, frame_index,
  capture_ts, img_w, img_h,
  det_id, class_name, class_id, conf,
  bbox_x1, bbox_y1, bbox_x2, bbox_y2,
  track_id, processing_ts
)
SELECT
  src.r.schema_version,
  src.r.pipeline_run_id,
  COALESCE(src.r.source.store_id,  src.br_store_id)  AS store_id,
  COALESCE(src.r.source.camera_id, src.br_camera_id) AS camera_id,
  src.r.frame_index,
  CAST(src.r.capture_ts AS TIMESTAMP(3))             AS capture_ts,
  src.r.image_size.width                              AS img_w,
  src.r.image_size.height                             AS img_h,
  det.det_id,
  COALESCE(det.`class`, 'person')                    AS class_name,
  det.class_id,
  det.conf,
  CAST(det.bbox.x1 AS INT)                           AS bbox_x1,
  CAST(det.bbox.y1 AS INT)                           AS bbox_y1,
  CAST(det.bbox.x2 AS INT)                           AS bbox_x2,
  CAST(det.bbox.y2 AS INT)                           AS bbox_y2,
  COALESCE(det.track_id, -1)                         AS track_id,
  CURRENT_TIMESTAMP                                  AS processing_ts
FROM (
  SELECT
    br.store_id  AS br_store_id,
    br.camera_id AS br_camera_id,
    FROM_JSON(
      br.payload,
      'ROW(
        schema_version STRING,
        pipeline_run_id STRING,
        source ROW(store_id STRING, camera_id STRING, stream_id STRING),
        frame_index BIGINT,
        capture_ts STRING,
        image_size ROW(width INT, height INT),
        detections ARRAY<ROW(
          det_id STRING,
          `class` STRING,
          class_id INT,
          conf DOUBLE,
          bbox ROW(x1 DOUBLE, y1 DOUBLE, x2 DOUBLE, y2 DOUBLE),
          track_id BIGINT
        )>
      )'
    ) AS r
  FROM rva.bronze_raw AS br
) AS src
CROSS JOIN UNNEST(src.r.detections) AS det;
