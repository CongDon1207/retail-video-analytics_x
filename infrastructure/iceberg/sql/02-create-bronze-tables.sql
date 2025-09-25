-- Bảng Bronze: 1 dòng / 1 detection
CREATE TABLE IF NOT EXISTS iceberg.rva.bronze_detections (
  schema_version        VARCHAR,
  pipeline_run_id       VARCHAR,
  store_id              VARCHAR,
  camera_id             VARCHAR,
  stream_id             VARCHAR,
  frame_index           INTEGER,
  capture_ts            TIMESTAMP(3),
  img_w                 INTEGER,
  img_h                 INTEGER,
  det_id                VARCHAR,
  class                 VARCHAR,
  class_id              INTEGER,
  conf                  DOUBLE,
  bbox_x1               INTEGER,
  bbox_y1               INTEGER,
  bbox_x2               INTEGER,
  bbox_y2               INTEGER,
  track_id              BIGINT
)
WITH (
  format = 'PARQUET',
  partitioning = ARRAY[
    'days(capture_ts)',
    'bucket(16, camera_id)'
  ]
);
