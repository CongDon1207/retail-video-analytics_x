# HANDOFF - Retail Video Analytics

## Current Status (Trạng thái hiện tại)
**Branch**: `don`  
**Đang làm**: Iceberg lakehouse integration với MinIO; debug Flink bronze ingestion bị `NoSuchMethodError` từ Pulsar client  
**Lý do**: Infrastructure stack complete (Pulsar + Flink + MinIO + Iceberg REST), nhưng job streaming đang restart liên tục do xung đột version Pulsar → chưa ghi được dữ liệu xuống Iceberg
**Mới cập nhật**: Pulsar metadata producer & demo script khả dụng; Flink image đã bundle thêm Avro + Jackson và fix checkpoint directory; đã ghi chú blocker Pulsar client trong data-flow guide

## TODO & Next Steps (Các bước tiếp theo - ưu tiên)

### High Priority
1. **Iceberg-MinIO Integration Verification**
   - Unblock lỗi `NoSuchMethodError` để job Flink chạy ổn định
   - Confirm bảng bronze nhận dữ liệu sau job Flink
   - Kiểm tra lại cấu hình region cho REST catalog nếu còn lỗi
   - Truy vấn dữ liệu bằng Iceberg REST/Trino (khi sẵn sàng)
   
2. **AI Pipeline Integration với Pulsar**
   - Pulsar producer Python (`ai/emit/pulsar_producer.py`) + demo script (`scripts/demo_send_to_pulsar.py`) sẵn sàng thử nghiệm
   - Connect `ai/emit/json_emitter.py` với Pulsar producer trong pipeline chính
   - Test full flow: video input → AI processing → Pulsar topic
   
3. **Flink Jobs Development**
   - Bronze job submit thành công nhưng đang `RESTARTING` do thiếu method trong Pulsar client
   - Sau khi fix dependency, bổ sung monitoring & mở rộng logic xử lý (nếu cần)

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
- **Flink SQL Job**: ⚠️ Submit thành công nhưng runtime `RESTARTING` vì `NoSuchMethodError` tại `PulsarClientImpl.getPartitionedTopicMetadata`; cần đồng bộ lại bộ JAR Pulsar
- **Iceberg REST**: ⚠️ Service running, namespace created, table creation failing (AWS region issue)
- **Cross-Platform**: ✅ PowerShell commands documented, .gitattributes configured
- **Pulsar Producer Demo**: ✅ Python & Docker workflow gửi metadata vào topic persistent://retail/metadata/events
- **Pulsar Producer Image Build**: ✅ retail/pulsar-producer build thành công sau khi bỏ loại trừ detections_output.ndjson khỏi .dockerignore

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
