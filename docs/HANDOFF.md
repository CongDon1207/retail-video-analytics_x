# HANDOFF - Retail Video Analytics

## Current Status (Trạng thái hiện tại)
**Branch**: `don`  
**Đang làm**: End-to-end pipeline đã hoàn thành và verify thành công  
**Lý do**: Full data flow từ AI detection → Pulsar (JSON) → Flink streaming → Iceberg Bronze (MinIO) đang hoạt động  
**Mới cập nhật**: Bật lại checkpointing (60s) và thêm JVM `--add-opens` để Flink Bronze job commit dữ liệu; 288 messages đã được ghi thành công vào Bronze layer với 7 Parquet files

## TODO & Next Steps (Các bước tiếp theo - ưu tiên)

### High Priority
1. **Silver/Gold Layer Development**
   - Design Silver layer schema cho cleaned/enriched data
   - Implement Flink SQL transformations cho Silver layer
   - Add aggregation logic cho Gold layer (analytics-ready)
   
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
- **AI Modules**: `ai/detect/`, `ai/track/`, `ai/ingest/`, `ai/emit/`
- **Infrastructure**: `infrastructure/flink/`, `infrastructure/pulsar/`, `infrastructure/minio/`, `infrastructure/iceberg/`
- **Config**: `.env` (credentials), `docker-compose.yml` (4 services), `infrastructure/flink/conf/flink-conf.yaml`
- **Jobs**: `flink-jobs/bronze_ingest.sql` (Bronze layer ingestion)
- **Test Data**: `data/videos/`, `yolov8n.pt`, `detections_output.ndjson`
- **Documentation**: `docs/data-flow-guide.md`, `docs/HANDOFF.md`, `docs/CHANGELOG.md`

## Latest Checks (Kết quả test gần nhất)
- **Docker Infrastructure**: ✅ All services running (Pulsar, Flink, MinIO, Iceberg REST)
- **Port Configuration**: ✅ No conflicts (Pulsar:8082, Flink:8081, MinIO:9000/9001, Iceberg:8181)
- **MinIO Setup**: ✅ Warehouse bucket ~155KiB data (7 Parquet + metadata) sau Bronze commit
- **Pulsar Setup**: ✅ JSON schema registered, 288 messages sent successfully
- **Flink Setup**: ✅ JobManager + TaskManager healthy
- **Flink Bronze Job**: ✅ Checkpoint 60s chạy thành công, tiêu thụ hết 288 messages và commit file
- **Iceberg Bronze Table**: ✅ 7 Parquet files (~20KiB/file) tại `warehouse/rva/bronze_raw/data/store_id=store_01/`
- **Data Flow**: ✅ End-to-end pipeline verified (Video → AI → Pulsar → Flink → Iceberg)
- **Schema Format**: ✅ JSON (changed from Avro to fix deserialization issues)

## Schemas/Contracts (Schema hiện tại)
- **Detection Output**: NDJSON format (xem `detections_output.ndjson`)
- **Video Input**: Support MP4, AVI via CV2/GStreamer
- **Pulsar Schema**: JSON schema (`RetailDetection` with 4 fields: schema_version, pipeline_run_id, frame_index, payload)
- **Bronze Table**: 7 columns (schema_version, pipeline_run_id, frame_index, payload, camera_id, store_id, ingest_ts)
- **Partitioning**: By `store_id` field
- **Storage Format**: Parquet (Iceberg format v2)

## Environment (Môi trường)
- **Python**: 3.12 (venv: `.venv312/`), packages: ultralytics, opencv-python, deep-sort-realtime, pulsar-client[avro]==3.5.0
- **Docker Services**: Pulsar 3.3.2 (6650,8082), Flink 1.18 (8081), MinIO (9000,9001), Iceberg REST 0.7.0 (8181)
- **Docker Compose**: 4-service stack fully operational
- **Flink Config**: Checkpointing 60s bật lại; thêm `--add-opens` java.util/java.lang/java.nio/sun.nio.ch
- **Pulsar Client**: Python 3.5.0 (JSON schema), Flink connector 4.1.0-1.18 (JSON format)

## Notes
- **Avro → JSON Migration**: Changed producer from AvroSchema to JsonSchema due to Flink-Pulsar Avro deserialization incompatibility
- **Checkpointing**: Đã bật (60s) sau khi bổ sung JVM `--add-opens` cho java.util, java.lang, java.nio, sun.nio.ch
- **Data Verified**: 288 messages successfully written to 7 Parquet files (~20KiB each), checkpoint commit OK
- **Branch `don`**: Complete working pipeline, ready for Silver/Gold layer development
- **Performance**: Job processed all messages without errors after JSON format change


