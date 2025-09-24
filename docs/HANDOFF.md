# HANDOFF - Retail Video Analytics

## Current Status (Trạng thái hiện tại)
**Branch**: `infra/setup`  
**Đang làm**: Hoàn thiện infrastructure setup và integration testing  
**Lý do**: Cần hoàn thành full pipeline trước khi merge về main

## TODO & Next Steps (Các bước tiếp theo - ưu tiên)

### High Priority
1. **Complete Docker infrastructure setup** 
   - Thêm MinIO, Trino, Grafana vào docker-compose.yml
   - Test integration giữa Pulsar ↔ Flink ↔ Iceberg
   
2. **End-to-end pipeline testing**
   - Test full flow: video input → AI processing → Pulsar → Flink → storage
   - Validate latency requirements (≤ 3-5s E2E)

3. **Configuration management**
   - Hoàn thiện .env configuration
   - Add production-ready configs cho các services

### Medium Priority  
4. **Monitoring & Alerting setup**
   - Add Prometheus + Alertmanager configs
   - Setup Grafana dashboards cho monitoring
   
5. **Documentation completion**
   - Update installation/deployment guide
   - API documentation cho AI modules

6. **Performance optimization**
   - GPU support cho YOLOv8
   - Throughput testing (target: 50-200 msg/s)

## Key Paths (Đường dẫn quan trọng)
- **AI Modules**: `ai/detect/`, `ai/track/`, `ai/ingest/`, `ai/emit/`
- **Infrastructure**: `infrastructure/flink/`, `infrastructure/pulsar/`
- **Config**: `configs/.env.example`, `docker-compose.yml`
- **Test Data**: `data/videos/`, `yolov8n.pt`
- **Scripts**: `scripts/make_synth_video.py`

## Latest Checks (Kết quả test gần nhất)
- **AI Pipeline**: ✅ Individual modules working (detect, track, emit)
- **Pulsar Setup**: ✅ Configuration complete, cần test integration  
- **Flink Setup**: ✅ Configuration complete, cần test với Pulsar
- **Docker Compose**: ⚠️ Partial - thiếu MinIO, Trino, Grafana services
- **E2E Integration**: ❌ Chưa test - đợi infrastructure hoàn thành

## Schemas/Contracts (Schema hiện tại)
- **Detection Output**: NDJSON format (xem `detections_output.ndjson`)
- **Video Input**: Support MP4, AVI via CV2/GStreamer
- **Pulsar Schema**: Chưa define - cần implement Avro schema

## Environment (Môi trường)
- **Python**: 3.12 (venv: `.venv312/`)
- **Dependencies**: Requirements chưa được define trong requirements.txt
- **Docker**: docker-compose.yml (partial implementation)
- **Models**: YOLOv8 nano (`yolov8n.pt`) - 6MB model cho testing

## Notes
- Branch `infra/setup` có infrastructure configs mới nhất
- Main branch stable với basic AI functionality
- Cần merge về main sau khi hoàn thành infrastructure testing
