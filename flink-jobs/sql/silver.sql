-- Dùng đúng Iceberg REST catalog đã tạo ở bronze_ingest.sql
USE CATALOG lakehouse;
CREATE DATABASE IF NOT EXISTS rva;

-- =========================
-- TẠO BẢNG SILVER (Iceberg)
-- =========================
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
  -- Khai báo partition spec theo Iceberg qua property 'partitioning'
  'partitioning' = 'identity(store_id), bucket(16, camera_id), days(capture_ts)'
);

-- =========================================================
-- INSERT TỪ BRONZE (parse JSON + UNNEST detections -> rows)
-- =========================================================
INSERT INTO rva.silver_detections
WITH parsed AS (
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
          bbox_norm ROW(x DOUBLE, y DOUBLE, w DOUBLE, h DOUBLE),
          centroid ROW(x INT, y INT),
          centroid_norm ROW(x DOUBLE, y DOUBLE),
          track_id BIGINT
        )>
      )'
    ) AS r
  FROM rva.bronze_raw AS br
)
SELECT
  r.schema_version,
  r.pipeline_run_id,
  COALESCE(r.source.store_id,  br_store_id)  AS store_id,
  COALESCE(r.source.camera_id, br_camera_id) AS camera_id,
  r.frame_index,
  CAST(r.capture_ts AS TIMESTAMP_LTZ(3))     AS capture_ts,
  r.image_size.width                         AS img_w,
  r.image_size.height                        AS img_h,
  det.det_id,
  det.`class`                                AS class_name,
  det.class_id,
  det.conf,
  det.bbox.x1                                AS bbox_x1,
  det.bbox.y1                                AS bbox_y1,
  det.bbox.x2                                AS bbox_x2,
  det.bbox.y2                                AS bbox_y2,
  det.centroid.x                             AS centroid_x,
  det.centroid.y                             AS centroid_y,
  det.track_id,
  CURRENT_TIMESTAMP                          AS processing_ts
FROM parsed
CROSS JOIN UNNEST(r.detections) AS det;
