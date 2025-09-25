# HANDOFF - Retail Video Analytics

## Current Status (Trạng thái hiện tại)
**Branch**: `don`  
**Đang làm**: Infrastructure setup hoàn thành, đang test integration và tạo documentation  
**Lý do**: Infrastructure đã stable, cần hoàn thành AI pipeline integration

## TODO & Next Steps (Các bước tiếp theo - ưu tiên)

### High Priority
1. **AI Pipeline Integration với Pulsar**
   - Connect `ai/emit/json_emitter.py` với Pulsar producer
   - Test full flow: video input → AI processing → Pulsar topic
   
2. **Flink Jobs Development**
   - Tạo Flink streaming jobs trong `flink-jobs/`
   - Implement Pulsar source → processing → MinIO sink
   
3. **Iceberg Lakehouse Setup**
   - Configure Iceberg catalog với MinIO backend
   - Setup table schemas cho analytics data

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
- **Infrastructure**: `infrastructure/flink/`, `infrastructure/pulsar/`, `infrastructure/minio/`
- **Config**: `.env` (credentials), `docker-compose.yml` (services)
- **Test Data**: `data/videos/`, `yolov8n.pt`
- **Documentation**: `docs/data-flow-guide.md`, `docs/HANDOFF.md`, `docs/CHANGELOG.md`

## Latest Checks (Kết quả test gần nhất)
- **Docker Infrastructure**: ✅ All services running (Pulsar, Flink, MinIO)
- **Port Configuration**: ✅ Resolved conflicts (Pulsar:8082, Flink:8081, MinIO:9000/9001)
- **MinIO Setup**: ✅ Credentials configured, healthcheck passing
- **Pulsar Setup**: ✅ Broker healthy, topics ready for creation
- **Flink Setup**: ✅ JobManager + TaskManager healthy
- **E2E Integration**: ⚠️ Infrastructure ready, cần AI pipeline integration

## Schemas/Contracts (Schema hiện tại)
- **Detection Output**: NDJSON format (xem `detections_output.ndjson`)
- **Video Input**: Support MP4, AVI via CV2/GStreamer
- **Pulsar Schema**: Cần define Avro schema cho detection metadata
- **MinIO Buckets**: `lakehouse/`, `raw-data/`, `processed/`, `models/`

## Environment (Môi trường)
- **Python**: 3.12 (venv: `.venv312/`)
- **Docker Services**: Pulsar (6650,8082), Flink (8081), MinIO (9000,9001)
- **Git**: .gitattributes configured for cross-platform compatibility
- **Line Endings**: LF preserved for .sh, .env, config files

## Notes
- Infrastructure stack hoàn tất và stable
- Data flow guide available tại `docs/data-flow-guide.md`
- All services healthy, ready cho AI integration
- Branch `don` ready for main merge sau integration testing
