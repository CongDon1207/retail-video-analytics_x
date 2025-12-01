package org.rva.gold;

import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * Gold Job 1: gold_minute_by_cam
 * Aggregates detections by store, camera, and minute.
 */
public class GoldMinuteByCamJob extends GoldJobBase {

    public static void main(String[] args) throws Exception {
        GoldMinuteByCamJob job = new GoldMinuteByCamJob();
        job.run();
    }

    public void run() throws Exception {
        StreamTableEnvironment tEnv = setupEnvironment();

        // Create table if not exists
        tEnv.executeSql(
            "CREATE TABLE IF NOT EXISTS gold_minute_by_cam (" +
            "  store_id STRING, " +
            "  camera_id STRING, " +
            "  ts_minute TIMESTAMP(3), " +
            "  detection_count BIGINT, " +
            "  unique_tracks BIGINT, " +
            "  avg_conf DOUBLE, " +
            "  bbox_area_ratio_avg DOUBLE, " +
            "  PRIMARY KEY (store_id, camera_id, ts_minute) NOT ENFORCED" +
            ") WITH (" +
            "  'format-version'='2', " +
            "  'write.upsert.enabled'='true', " +
            "  'write.format.default'='parquet'" +
            ")"
        );

        // Execute streaming insert
        tEnv.executeSql(
            "INSERT INTO gold_minute_by_cam " +
            "SELECT store_id, camera_id, FLOOR(capture_ts TO MINUTE), " +
            "  COUNT(*), COUNT(DISTINCT track_id), AVG(conf), " +
            "  AVG((CAST(bbox_x2 AS DOUBLE) - CAST(bbox_x1 AS DOUBLE)) * " +
            "      (CAST(bbox_y2 AS DOUBLE) - CAST(bbox_y1 AS DOUBLE)) / " +
            "      NULLIF(CAST(img_w AS DOUBLE) * CAST(img_h AS DOUBLE), 0.0)) " +
            "FROM silver_detections " + STREAMING_HINT + " " +
            "GROUP BY store_id, camera_id, FLOOR(capture_ts TO MINUTE)"
        );
    }
}
