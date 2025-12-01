package org.rva.gold;

import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * Gold Job 2: gold_hour_by_cam
 * Aggregates detections by store, camera, and hour.
 */
public class GoldHourByCamJob extends GoldJobBase {

    public static void main(String[] args) throws Exception {
        GoldHourByCamJob job = new GoldHourByCamJob();
        job.run();
    }

    public void run() throws Exception {
        StreamTableEnvironment tEnv = setupEnvironment();

        tEnv.executeSql(
            "CREATE TABLE IF NOT EXISTS gold_hour_by_cam (" +
            "  store_id STRING, " +
            "  camera_id STRING, " +
            "  ts_hour TIMESTAMP(3), " +
            "  detection_count BIGINT, " +
            "  unique_tracks BIGINT, " +
            "  avg_conf DOUBLE, " +
            "  PRIMARY KEY (store_id, camera_id, ts_hour) NOT ENFORCED" +
            ") WITH (" +
            "  'format-version'='2', " +
            "  'write.upsert.enabled'='true', " +
            "  'write.format.default'='parquet'" +
            ")"
        );

        tEnv.executeSql(
            "INSERT INTO gold_hour_by_cam " +
            "SELECT store_id, camera_id, FLOOR(capture_ts TO HOUR), " +
            "  COUNT(*), COUNT(DISTINCT track_id), AVG(conf) " +
            "FROM silver_detections " + STREAMING_HINT + " " +
            "GROUP BY store_id, camera_id, FLOOR(capture_ts TO HOUR)"
        );
    }
}
