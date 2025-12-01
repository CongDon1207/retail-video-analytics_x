package org.rva.gold;

import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * Gold Job 6: gold_track_summary
 * Summarizes each track's movement and statistics.
 */
public class GoldTrackSummaryJob extends GoldJobBase {

    public static void main(String[] args) throws Exception {
        GoldTrackSummaryJob job = new GoldTrackSummaryJob();
        job.run();
    }

    public void run() throws Exception {
        StreamTableEnvironment tEnv = setupEnvironment();

        tEnv.executeSql(
            "CREATE TABLE IF NOT EXISTS gold_track_summary (" +
            "  store_id STRING, " +
            "  camera_id STRING, " +
            "  track_id BIGINT, " +
            "  frames BIGINT, " +
            "  start_time TIMESTAMP(3), " +
            "  end_time TIMESTAMP(3), " +
            "  duration_seconds DOUBLE, " +
            "  min_x DOUBLE, " +
            "  max_x DOUBLE, " +
            "  delta_x DOUBLE, " +
            "  min_y DOUBLE, " +
            "  max_y DOUBLE, " +
            "  delta_y DOUBLE, " +
            "  avg_x DOUBLE, " +
            "  avg_y DOUBLE, " +
            "  avg_conf DOUBLE, " +
            "  PRIMARY KEY (store_id, camera_id, track_id) NOT ENFORCED" +
            ") WITH (" +
            "  'format-version'='2', " +
            "  'write.upsert.enabled'='true', " +
            "  'write.format.default'='parquet'" +
            ")"
        );

        tEnv.executeSql(
            "INSERT INTO gold_track_summary " +
            "SELECT store_id, camera_id, track_id, " +
            "  COUNT(*), MIN(capture_ts), MAX(capture_ts), " +
            "  CAST(TIMESTAMPDIFF(SECOND, MIN(capture_ts), MAX(capture_ts)) AS DOUBLE), " +
            "  MIN(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)), " +
            "  MAX(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)), " +
            "  MAX(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)) - " +
            "    MIN(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)), " +
            "  MIN(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)), " +
            "  MAX(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)), " +
            "  MAX(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)) - " +
            "    MIN(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)), " +
            "  AVG(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0)), " +
            "  AVG(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0)), " +
            "  AVG(conf) " +
            "FROM silver_detections " + STREAMING_HINT + " " +
            "GROUP BY store_id, camera_id, track_id"
        );
    }
}
