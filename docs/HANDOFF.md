# HANDOFF - Retail Video Analytics

## Current Status (Trạng thái hiện tại)
**Branch**: `main`  
**Đang làm**: End-to-end pipeline đã hoàn thành và verify thành công; lớp Silver trong notebook đã bổ sung bước làm sạch cơ bản  
**Lý do**: Full data flow từ AI detection → Pulsar (JSON) → Flink streaming → Iceberg Bronze (MinIO) đang hoạt động; Silver cho analytics (Trino) đã có rule khử null, trùng (det_id/track_id) và lọc nhiễu theo confidence  
**Mới cập nhật**: Fix lỗi Flink SQL "Non-query expression" bằng cách tạo database `default` cho `default_catalog` trước khi khai báo Pulsar source; job Bronze submit OK. Cập nhật docs/guide.md: lưu ý SQL Gateway 1.18.1 không hỗ trợ `SOURCE` interactive, khuyến nghị chạy 1-lệnh `-f` hoặc dán full nội dung bronze_ingest thủ công. Thêm logic cleaning Silver trong `notebooks/explore_analytics.ipynb`.

## TODO & Next Steps (Các bước tiếp theo - ưu tiên)

### High Priority
1. **Silver/Gold Layer Development**
   - Silver (streaming/Flink Java): đã chạy với schema hiện tại; logic cleaning (null/duplicate/conf>=0.4) đã align với notebook thông qua `SilverJob` + UDTF `ParseDetections`.
   - Silver (notebook/Trino): đã có bảng `silver_detections_v2` với rule: bỏ bản ghi thiếu key chính, lọc conf < 0.4, khử trùng lặp theo det_id/track_id (ROW_NUMBER).
   - Gold (Java batch): `GoldBatchJob` giờ tạo thêm các bảng Gold giống notebook (`gold_people_per_minute`, `gold_zone_heatmap`, `gold_zone_dwell`, `gold_track_summary`) dựa trên Silver đã clean, ngoài các bảng minute/hour cũ.
   - Gold (Trino views / Grafana): Grafana được connect sẵn tới Trino (`Trino Lakehouse` datasource) với 3 dashboard core: People Overview, Zone Dwell & Heatmap, Track Summary, đọc trực tiếp từ các bảng Gold.
   - Runbook Silver: `silver_setup.sql` → `silver_create_table.sql` → `silver_insert.sql` hoặc chạy Java job.
   - Runbook Gold: chạy Java job `org.rva.gold.GoldBatchJob` từ Flink hoặc bổ sung file `gold_views.sql` nếu cần Trino-only.
   
2. **Monitoring & Observability**
   - Add Prometheus + Grafana services
   - Setup dashboards cho pipeline monitoring
   - Configure alerts cho job failures
   
3. **Performance Optimization**
   - Theo dõi checkpoint duration & cảnh báo thất bại
   - Tune Flink parallelism và resource allocation
   - Optimize Iceberg write patterns

### Medium Priority  
4. **Trino Query Engine**
   - Add Trino service vào docker-compose
   - Connect với Iceberg tables for BI queries
   - Create sample analytics queries
   
5. **AI Pipeline Enhancement**
   - Support multiple video streams concurrently
   - Add real-time streaming mode (không chỉ batch)
   - Implement error handling và retry logic

6. **Production Readiness**
   - Add authentication/authorization
   - Setup backup & recovery procedures
   - Load testing với high-volume data

## Key Paths (Đường dẫn quan trọng)
- **AI Modules**: `vision/detect/`, `vision/track/`, `vision/ingest/`, `vision/emit/`
- **Infrastructure**: `infrastructure/flink/`, `infrastructure/pulsar/`, `infrastructure/minio/`, `infrastructure/iceberg/`
- **Config**: `.env` (credentials), `docker-compose.yml` (4 services), `infrastructure/flink/conf/flink-conf.yaml`
- **Jobs**: `flink-jobs/bronze_ingest.sql` (Bronze layer ingestion)
- **Test Data**: `data/videos/`, `yolov8n.pt`, `detections_output.ndjson`
- **Documentation**: `docs/data-flow-guide.md`, `docs/HANDOFF.md`, `docs/CHANGELOG.md`

## Latest Checks (Kết quả test gần nhất)
- **Docker Infrastructure**: ✅ All services running (Pulsar, Flink, MinIO, Iceberg REST)
- **Port Configuration**: ✅ No conflicts (Pulsar:8082, Flink:8081, MinIO:9000/9001, Iceberg:8181)
- **MinIO Setup**: ✅ Warehouse bucket ready
- **Pulsar Setup**: ✅ Topic `persistent://retail/metadata/events` created
- **Flink Setup**: ✅ JobManager + TaskManager healthy
- **Flink Bronze Job**: ✅ Checkpoint 60s chạy thành công, tiêu thụ hết 288 messages và commit file; test lại 2025-11-20 với `bronze_ingest.sql` (có bước tạo DB default) submit OK
- **Iceberg Bronze Table**: ✅ 7 Parquet files (~20KiB/file) tại `warehouse/rva/bronze_raw/data/store_id=store_01/`
- **Data Flow**: ✅ End-to-end pipeline verified (Video → AI → Pulsar → Flink → Iceberg)
- **Schema Format**: ✅ JSON (changed from Avro to fix deserialization issues)

## Schemas/Contracts (Schema hiện tại)
- **Detection Output**: JSONL format (`data/metadata/video.jsonl`)
- **Pulsar Schema**: Raw JSON payload
- **Bronze Table**: 4 columns (ingest_ts, publish_ts, raw_payload, source_properties)
- **Storage Format**: Parquet (Iceberg format v2)

## Environment (Môi trường)
- **Python**: 3.12 (venv: `venv/`), packages: ultralytics, opencv-python, deep-sort-realtime, pulsar-client
- **Docker Services**: Pulsar 3.3.2 (6650,8082), Flink 1.18 (8081), MinIO (9000,9001), Iceberg REST 0.7.0 (8181)
- **Docker Compose**: 4-service stack fully operational
- **Pulsar Client**: Python client (JSON format), Flink connector 4.1.0-1.18 (Raw format)

## Notes
- **Avro → JSON Migration**: Changed producer from AvroSchema to JsonSchema due to Flink-Pulsar Avro deserialization incompatibility; updated `metadata-json-schema.json` from AVRO to JSON type (2025-11-20)
- **Checkpointing**: Đã bật (60s) sau khi bổ sung JVM `--add-opens` cho java.util, java.lang, java.nio, sun.nio.ch
- **Data Verified**: 288 messages successfully written to 7 Parquet files (~20KiB each), checkpoint commit OK
- **Branch `don`**: Complete working pipeline, ready for Silver/Gold layer development
- **Performance**: Job processed all messages without errors after JSON format change
- **Localhost → 127.0.0.1**: Changed Pulsar external listener and AI default URL to 127.0.0.1 to avoid Windows DNS issues


