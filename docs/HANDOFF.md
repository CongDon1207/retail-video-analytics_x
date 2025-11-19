# HANDOFF - Retail Video Analytics

## Current Status (Trạng thái hiện tại)
**Branch**: `main`  
**Đang làm**: End-to-end pipeline đã hoàn thành và verify thành công  
**Lý do**: Full data flow từ AI detection → Pulsar (JSON) → Flink streaming → Iceberg Bronze (MinIO) đang hoạt động  
**Mới cập nhật**: Fix lỗi Flink SQL "Non-query expression" bằng cách sử dụng `default_catalog` cho Pulsar source và reference trực tiếp `iceberg.retail.bronze_detections` cho sink.

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
- **AI Modules**: `vision/detect/`, `vision/track/`, `vision/ingest/`, `vision/emit/`
- **Infrastructure**: `infrastructure/flink/`, `infrastructure/pulsar/`, `infrastructure/minio/`, `infrastructure/iceberg/`
- **Config**: `.env` (credentials), `docker-compose.yml` (4 services), `infrastructure/flink/conf/flink-conf.yaml`
- **Jobs**: `flink-jobs/bronze_ingest.sql` (Bronze layer ingestion)
- **Test Data**: `vision/video/`, `data/metadata/video.jsonl`
- **Documentation**: `docs/guide.md`, `docs/HANDOFF.md`, `docs/CHANGELOG.md`

## Latest Checks (Kết quả test gần nhất)
- **Docker Infrastructure**: ✅ All services running (Pulsar, Flink, MinIO, Iceberg REST)
- **Port Configuration**: ✅ No conflicts (Pulsar:8082, Flink:8081, MinIO:9000/9001, Iceberg:8181)
- **MinIO Setup**: ✅ Warehouse bucket ready
- **Pulsar Setup**: ✅ Topic `persistent://retail/metadata/events` created
- **Flink Setup**: ✅ JobManager + TaskManager healthy
- **Flink Bronze Job**: ✅ Job submitted successfully, reading from Pulsar and writing to Iceberg
- **Data Flow**: ✅ End-to-end pipeline verified (Video → Vision → Pulsar → Flink → Iceberg)

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
- **Flink SQL Fix**: Resolved "Non-query expression" error by explicitly using `default_catalog` for Pulsar source table definition and fully qualified name for Iceberg sink table.
- **Pulsar Config**: Updated `advertisedAddress=localhost` in `standalone.conf` to allow host-based Python scripts to connect.
- **Metadata Location**: Moved metadata to `data/metadata/` to separate code and data.


