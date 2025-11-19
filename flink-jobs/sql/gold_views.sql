-- Trino SQL - Tạo các VIEW cho lớp GOLD dựa trên Silver
-- Cách chạy:
--   docker cp flink-jobs/sql/gold_views.sql trino:/tmp/gold_views.sql
--   docker exec -it trino trino --file /tmp/gold_views.sql

USE lakehouse.rva;

-- 1) Chỉ số theo phút cho từng camera
CREATE OR REPLACE VIEW v_gold_minute_by_cam AS
SELECT
  store_id,
  camera_id,
  date_trunc('minute', capture_ts) AS ts_minute,
  count(*) AS detection_count,
  count(DISTINCT track_id) AS unique_tracks,
  avg(conf) AS avg_conf,
  approx_percentile(conf, 0.95) AS p95_conf,
  -- tỉ lệ diện tích bbox so với frame (giới hạn trong [0,1])
  avg(
    greatest(
      0.0,
      least(bbox_x2, img_w) - bbox_x1
    ) * greatest(
      0.0,
      least(bbox_y2, img_h) - bbox_y1
    ) / nullif(img_w * img_h, 0)
  ) AS bbox_area_ratio_avg
FROM silver_detections
GROUP BY 1,2,3;

-- 2) Chỉ số theo giờ cho từng camera
CREATE OR REPLACE VIEW v_gold_hour_by_cam AS
SELECT
  store_id,
  camera_id,
  date_trunc('hour', capture_ts) AS ts_hour,
  count(*) AS detection_count,
  count(DISTINCT track_id) AS unique_tracks,
  avg(conf) AS avg_conf,
  approx_percentile(conf, 0.95) AS p95_conf
FROM silver_detections
GROUP BY 1,2,3;

-- 3) Thời lượng hiện diện theo track (dwell)
CREATE OR REPLACE VIEW v_gold_track_dwell AS
SELECT
  store_id,
  camera_id,
  track_id,
  min(capture_ts) AS first_seen,
  max(capture_ts) AS last_seen,
  date_diff('second', min(capture_ts), max(capture_ts)) AS dwell_seconds,
  count(*) AS frames_in_track,
  avg(conf) AS avg_conf
FROM silver_detections
WHERE track_id IS NOT NULL AND track_id <> -1
GROUP BY 1,2,3;

