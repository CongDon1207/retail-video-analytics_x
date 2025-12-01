package org.rva.gold;

import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * Gold Job 4: gold_zone_heatmap
 * Creates a heatmap of detections by zone (grid cell).
 */
public class GoldZoneHeatmapJob extends GoldJobBase {

    public static void main(String[] args) throws Exception {
        GoldZoneHeatmapJob job = new GoldZoneHeatmapJob();
        job.run();
    }

    public void run() throws Exception {
        StreamTableEnvironment tEnv = setupEnvironment();

        tEnv.executeSql(
            "CREATE TABLE IF NOT EXISTS gold_zone_heatmap (" +
            "  store_id STRING, " +
            "  camera_id STRING, " +
            "  zone_x INT, " +
            "  zone_y INT, " +
            "  hits BIGINT, " +
            "  unique_tracks BIGINT, " +
            "  PRIMARY KEY (store_id, camera_id, zone_x, zone_y) NOT ENFORCED" +
            ") WITH (" +
            "  'format-version'='2', " +
            "  'write.upsert.enabled'='true', " +
            "  'write.format.default'='parquet'" +
            ")"
        );

        tEnv.executeSql(
            "INSERT INTO gold_zone_heatmap " +
            "SELECT store_id, camera_id, " +
            "  CAST(FLOOR(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0) / 128.0) AS INT), " +
            "  CAST(FLOOR(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0) / 72.0) AS INT), " +
            "  COUNT(*), COUNT(DISTINCT track_id) " +
            "FROM silver_detections " + STREAMING_HINT + " " +
            "GROUP BY store_id, camera_id, " +
            "  CAST(FLOOR(((CAST(bbox_x1 AS DOUBLE) + CAST(bbox_x2 AS DOUBLE)) / 2.0) / 128.0) AS INT), " +
            "  CAST(FLOOR(((CAST(bbox_y1 AS DOUBLE) + CAST(bbox_y2 AS DOUBLE)) / 2.0) / 72.0) AS INT)"
        );
    }
}
