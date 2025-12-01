package org.rva.gold;

import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * Gold Job 3: gold_people_per_minute
 * Counts unique people per minute.
 */
public class GoldPeoplePerMinuteJob extends GoldJobBase {

    public static void main(String[] args) throws Exception {
        GoldPeoplePerMinuteJob job = new GoldPeoplePerMinuteJob();
        job.run();
    }

    public void run() throws Exception {
        StreamTableEnvironment tEnv = setupEnvironment();

        tEnv.executeSql(
            "CREATE TABLE IF NOT EXISTS gold_people_per_minute (" +
            "  store_id STRING, " +
            "  camera_id STRING, " +
            "  ts_minute TIMESTAMP(3), " +
            "  detections BIGINT, " +
            "  unique_people BIGINT, " +
            "  PRIMARY KEY (store_id, camera_id, ts_minute) NOT ENFORCED" +
            ") WITH (" +
            "  'format-version'='2', " +
            "  'write.upsert.enabled'='true', " +
            "  'write.format.default'='parquet'" +
            ")"
        );

        tEnv.executeSql(
            "INSERT INTO gold_people_per_minute " +
            "SELECT store_id, camera_id, FLOOR(capture_ts TO MINUTE), " +
            "  COUNT(*), COUNT(DISTINCT track_id) " +
            "FROM silver_detections " + STREAMING_HINT + " " +
            "GROUP BY store_id, camera_id, FLOOR(capture_ts TO MINUTE)"
        );
    }
}
