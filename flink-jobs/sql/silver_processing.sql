-- =============================================================================
-- GIAI ĐOẠN 1: ĐỊNH NGHĨA CATALOG
-- SỬA LỖI: Dùng CHÍNH XÁC cấu hình catalog (bao gồm cả hardcode)
-- giống hệt file bronze_ingest.sql để đảm bảo Flink
-- kết nối đúng vào warehouse (s3://warehouse/iceberg)
-- =============================================================================
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
USE rva; -- Database 'rva' này đã được job Bronze tạo ra

-- =============================================================================
-- GIAI ĐOẠN 2: TẠO BẢNG SILVER (ĐÍCH)
-- Tạo bảng đích (sink) cho Tầng Silver.
-- Schema này được "làm phẳng" (flattened) - không còn cột JSON 'payload'.
-- =============================================================================
CREATE TABLE IF NOT EXISTS rva.silver_detections (
  -- Metadata được kế thừa
  schema_version        VARCHAR,
  pipeline_run_id       VARCHAR,
  store_id              VARCHAR,
  camera_id             VARCHAR,
  frame_index           BIGINT,
  capture_ts            TIMESTAMP(3), -- Thời gian gốc của frame (trích xuất từ payload)
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
  
  -- Cột thời gian xử lý (được Flink tự động điền)
  processing_ts         TIMESTAMP_LTZ(3) METADATA FROM 'timestamp'
)
-- Định nghĩa phân vùng (partitioning)
-- Dùng tên cột trần (không có hàm, không có dấu nháy)
PARTITIONED BY (
    store_id,
    camera_id,
    capture_ts 
)
WITH (
  'format-version' = '2',
  'write.format.default' = 'parquet',
  
  -- Định nghĩa các hàm biến đổi (transforms) cho phân vùng
  -- Dưới dạng MỘT CHUỖI (STRING) duy nhất, dùng dấu nháy đơn (')
  'partitioning' = "['store_id', 'bucket(16, camera_id)', 'days(capture_ts)']"
);

-- =============================================================================
-- GIAI ĐOẠN 3: LOGIC CHUYỂN ĐỔI (BRONZE -> SILVER)
-- Đọc từ bảng Bronze, "bóc tách" (flatten) mảng 'detections',
-- "làm sạch" (clean) dữ liệu, và ghi vào bảng Silver.
-- =============================================================================
INSERT INTO rva.silver_detections (
    -- Liệt kê rõ các cột, vì 'processing_ts' được Flink tự điền
    schema_version, pipeline_run_id, store_id, camera_id, frame_index,
    capture_ts, img_w, img_h,
    det_id, class_name, class_id, conf,
    bbox_x1, bbox_y1, bbox_x2, bbox_y2,
    track_id
)
SELECT
    -- 1. Kế thừa các trường từ bảng Bronze (B)
    B.schema_version,
    B.pipeline_run_id,
    B.store_id,
    B.camera_id,
    B.frame_index,
    
    -- 2. Trích xuất (Extract) và Chuyển đổi (Cast) từ gốc của 'payload'
    CAST(JSON_VALUE(B.payload, '$.capture_ts') AS TIMESTAMP(3)) AS capture_ts,
    CAST(JSON_VALUE(B.payload, '$.image_size.width') AS INTEGER) AS img_w,
    CAST(JSON_VALUE(B.payload, '$.image_size.height') AS INTEGER) AS img_h,

    -- 3. Các trường từ mảng 'detections' (đã được bóc tách bởi T)
    T.det_id,
    COALESCE(T.class_name, 'person') AS class_name, -- Làm sạch 'class: null'
    T.class_id,
    T.conf,
    T.bbox_x1,
    T.bbox_y1,
    T.bbox_x2,
    T.bbox_y2,
    COALESCE(T.track_id, -1) AS track_id -- Làm sạch 'track_id: null'

FROM
    rva.bronze_raw AS B,
    
    -- 4. "Bóc tách" (Flatten) mảng JSON
    -- Dùng LATERAL TABLE và JSON_TABLE để biến mảng 'detections'
    -- thành các dòng riêng biệt, đặt tên là T
    LATERAL TABLE(
        JSON_TABLE(
            B.payload,
            '$.detections[*]' -- Đường dẫn đến mảng 'detections'
            COLUMNS (
                -- Ánh xạ các trường con trong JSON sang cột SQL
                det_id        VARCHAR PATH '$.det_id',
                class_name    VARCHAR PATH '$.class',
                class_id      INTEGER PATH '$.class_id',
                conf          DOUBLE  PATH '$.conf',
                bbox_x1       INTEGER PATH '$.bbox.x1',
                bbox_y1       INTEGER PATH '$.bbox.y1',
                bbox_x2       INTEGER PATH '$.bbox.x2',
                bbox_y2       INTEGER PATH '$.bbox.y2',
                track_id      BIGINT  PATH '$.track_id'
            )
        )
    ) AS T;