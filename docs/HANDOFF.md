# HANDOFF - Retail Video Analytics

## Current Status (Trạng thái hiện tại)
**Branch**: `don`  
**Đang làm**: Iceberg lakehouse integration với MinIO, debugging table creation issues  
**Lý do**: Infrastructure stack complete (Pulsar + Flink + MinIO + Iceberg REST), cần fix AWS region config cho Iceberg S3 connectivity
**Mới cập nhật**: Pulsar metadata producer & demo script khả dụng cho thử nghiệm end-to-end ban đầu

## TODO & Next Steps (Các bước tiếp theo - ưu tiên)

### High Priority
1. **Iceberg-MinIO Integration Fix**
   - Fix AWS region configuration trong Iceberg REST service
   - Complete table creation cho lakehouse layer
   - Test namespace và table operations
   
2. **AI Pipeline Integration với Pulsar**
   - Pulsar producer Python (`ai/emit/pulsar_producer.py`) + demo script (`scripts/demo_send_to_pulsar.py`) sẵn sàng thử nghiệm
   - Connect `ai/emit/json_emitter.py` với Pulsar producer trong pipeline chính
   - Test full flow: video input → AI processing → Pulsar topic
   
3. **Flink Jobs Development**
   - Tạo Flink streaming jobs trong `flink-jobs/`
   - Implement Pulsar source → processing → Iceberg sink

### Medium Priority  
4. **Trino Query Engine**
   - Add Trino service vào docker-compose
   - Connect với Iceberg tables for BI queries
   
5. **Monitoring Stack**
   - Add Prometheus + Grafana services
   - Setup dashboards cho pipeline monitoring

6. **Performance Testing**
   - Load testing với multiple video streams
   - Validate latency requirements (≤ 3-5s E2E)

## Key Paths (Đường dẫn quan trọng)
- **AI Modules**: `ai/detect/`, `ai/track/`, `ai/ingest/`, `ai/emit/`
- **Infrastructure**: `infrastructure/flink/`, `infrastructure/pulsar/`, `infrastructure/minio/`, `infrastructure/iceberg/`
- **Config**: `.env` (credentials), `docker-compose.yml` (4 services), `.gitattributes` (line endings)
- **Jobs**: `flink-jobs/` (streaming jobs development)
- **Test Data**: `data/videos/`, `yolov8n.pt`, `detections_output.ndjson`
- **Documentation**: `docs/data-flow-guide.md`, `docs/HANDOFF.md`, `docs/CHANGELOG.md`

## Latest Checks (Kết quả test gần nhất)
- **Docker Infrastructure**: ✅ All services running (Pulsar, Flink, MinIO, Iceberg REST)
- **Port Configuration**: ✅ Resolved conflicts (Pulsar:8082, Flink:8081, MinIO:9000/9001, Iceberg:8181)
- **MinIO Setup**: ✅ Credentials configured, warehouse bucket created, healthcheck passing
- **Pulsar Setup**: ✅ Broker healthy, admin API accessible on port 8082
- **Flink Setup**: ✅ JobManager + TaskManager healthy, Web UI on port 8081
- **Flink SQL Job**: Pending. Rebuild image with Pulsar admin API jar, rerun bronze_ingest.sql to confirm dependency fix
- **Iceberg REST**: ⚠️ Service running, namespace created, table creation failing (AWS region issue)
- **Cross-Platform**: ✅ PowerShell commands documented, .gitattributes configured
- **Pulsar Producer Demo**: ✅ Python & Docker workflow gửi metadata vào topic persistent://retail/metadata/events

## Schemas/Contracts (Schema hiện tại)
- **Detection Output**: NDJSON format (xem `detections_output.ndjson`)
- **Video Input**: Support MP4, AVI via CV2/GStreamer
- **Pulsar Schema**: JSON schema defined (`infrastructure/pulsar/schema/metadata-json-schema.json`)
- **MinIO Buckets**: `warehouse/` (Iceberg lakehouse), buckets auto-created via scripts
- **Iceberg Tables**: Bronze layer schemas defined (`infrastructure/iceberg/sql/`)

## Environment (Môi trường)
- **Python**: 3.12 (venv: `.venv312/`), packages: ultralytics, opencv-python, deep-sort-realtime
- **Docker Services**: Pulsar (6650,8082), Flink (8081), MinIO (9000,9001), Iceberg REST (8181)
- **Docker Compose**: v4 services stack, environment variables via `.env`
- **Git**: .gitattributes configured for cross-platform compatibility
- **Line Endings**: LF preserved for .sh, .env, config files

## Notes
- Infrastructure stack hoàn tất và stable (4/4 services healthy)
- Data flow guide available tại `docs/data-flow-guide.md` với PowerShell commands
- Iceberg lakehouse partially configured, cần fix AWS region cho S3 connectivity
- Full docker-compose stack deployed, ready cho end-to-end testing
- Branch `don` có complete infrastructure, ready cho AI pipeline integration
- Pulsar demo producer sẵn sàng (`scripts/demo_send_to_pulsar.py`, `infrastructure/pulsar/producer.Dockerfile`) hỗ trợ kiểm thử nhanh
