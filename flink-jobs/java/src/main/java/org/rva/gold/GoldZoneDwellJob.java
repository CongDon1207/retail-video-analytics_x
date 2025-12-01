package org.rva.gold;

import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * Gold Job 5: gold_zone_dwell
 * Calculates dwell time per zone.
 */
public class GoldZoneDwellJob extends GoldJobBase {

    public static void main(String[] args) throws Exception {
        GoldZoneDwellJob job = new GoldZoneDwellJob();
        job.run();
    }

    public void run() throws Exception {
        StreamTableEnvironment tEnv = setupEnvironment();

        tEnv.executeSql(
            "CREATE TABLE IF NOT EXISTS gold_zone_dwell (" +
            "  store_id STRING, " +
            "  camera_id STRING, " +
            "  zone_x INT, " +
            "  zone_y INT, " +
            "  visits BIGINT, " +
            "  total_dwell_seconds DOUBLE, " +
            "  avg_dwell_seconds DOUBLE, " +
            "  PRIMARY KEY (store_id, camera_id, zone_x, zone_y) NOT ENFORCED" +
            ") WITH (" +
            "  'format-version'='2', " +
            "  'write.upsert.enabled'='true', " +
            "  'write.format.default'='parquet'" +
            ")"
        );

        tEnv.executeSql(
            "INSERT INTO gold_zone_dwell " +
            "WITH per_track_zone AS (" +
            "  SELECT store_id, camera_id, track_id, " +
            "    CAST(FLOOR(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0) / 128.0) AS INT) AS zone_x, " +
            "    CAST(FLOOR(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0) / 72.0) AS INT) AS zone_y, " +
            "    MIN(capture_ts) AS start_time, " +
            "    MAX(capture_ts) AS end_time, " +
            "    CAST(TIMESTAMPDIFF(SECOND, MIN(capture_ts), MAX(capture_ts)) AS DOUBLE) AS dwell_seconds " +
            "  FROM silver_detections " + STREAMING_HINT + " " +
            "  GROUP BY store_id, camera_id, track_id, " +
            "    CAST(FLOOR(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0) / 128.0) AS INT), " +
            "    CAST(FLOOR(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0) / 72.0) AS INT)" +
            ") " +
            "SELECT store_id, camera_id, zone_x, zone_y, " +
            "  COUNT(*), SUM(dwell_seconds), AVG(dwell_seconds) " +
            "FROM per_track_zone " +
            "GROUP BY store_id, camera_id, zone_x, zone_y"
        );
    }
}
