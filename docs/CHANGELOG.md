# CHANGELOG

Ghi lại tất cả các công việc đã hoàn thành trong dự án Retail Video Analytics.

## 2025-09-28

### Pulsar Demo Payload Fix
- **2025-09-28: Document parallel venv + Docker quick-start paths at docs/guide.md - Users can choose workflow per environment (completed)**
- **2025-09-28: Restore detections_output.ndjson sample at repo root - Dockerized Pulsar producer build succeeds with bundled demo data (completed)**
- **2025-09-28: Set PYTHONPATH for Pulsar producer container at infrastructure/pulsar/producer.Dockerfile - Fix ModuleNotFoundError for ai package during docker run (completed)**
- **2025-09-28: Install pulsar-client[avro] in Pulsar producer image at infrastructure/pulsar/producer.Dockerfile - Enable Avro schema support during container run (completed)**
- **2025-09-28: Align producer Avro record with registered schema at ai/emit/pulsar_producer.py & infrastructure/pulsar/schema/metadata-json-schema.json - Resolve IncompatibleSchema errors when sending messages (completed)**
- **2025-09-28: Normalize bbox input handling at ai/emit/json_emitter.py - Fix TypeError when detections provide bbox as dict strings (completed)**
- **2025-09-28: Convert init-topics.sh to LF endings at infrastructure/pulsar/scripts/init-topics.sh - Fix pulsar-init pipefail errors under Linux entrypoint (completed)**

## 2025-09-26

### Pulsar Integration Enablement
- **2025-09-26: Add Pulsar metadata producer module tại ai/emit/pulsar_producer.py & scripts/demo_send_to_pulsar.py - Cho phép gửi metadata detection vào topic events phục vụ test nhanh (completed)**
- **2025-09-26: Add Docker workflow cho producer tại infrastructure/pulsar/producer.Dockerfile - Chạy producer không cần môi trường Python local (completed)**

## 2025-09-25

### Lakehouse Architecture Completion
- **2025-09-25: Add Iceberg REST catalog service at docker-compose.yml - Lakehouse table format support với MinIO backend; Iceberg service on port 8181 (completed)**
- **2025-09-25: Add MinIO service và complete docker-compose stack at infrastructure/minio/ - Setup object storage cho Lakehouse architecture; docker-compose fully functional (completed)**
- **2025-09-25: Fix port conflicts between Pulsar và Flink at docker-compose.yml - Resolve 8080 port conflict, Pulsar admin now on 8082; full 4-service stack healthy (completed)**
- **2025-09-25: Setup MinIO configuration với proper credentials at .env - MinIO healthcheck passing với secure credentials, warehouse bucket created (completed)**

### Documentation & Cross-Platform Support
- **2025-09-25: Create comprehensive data flow guide at docs/data-flow-guide.md - Complete tutorial cho AI → Pulsar → Flink → MinIO pipeline với PowerShell commands (completed)**
- **2025-09-25: Add cross-platform compatibility at .gitattributes - Prevent CRLF/LF issues cho .sh, .env files; Windows/Linux compatibility (completed)**
- **2025-09-25: Update project status at docs/HANDOFF.md và docs/CHANGELOG.md - Current status với infrastructure completion, next steps defined (completed)**

### Iceberg Integration Development
- **2025-09-25: Setup Iceberg configuration at infrastructure/iceberg/ - Table schemas và namespace definitions; catalog config templates (completed)**
- **2025-09-25: Test Iceberg-MinIO connectivity at lakehouse layer - Namespace creation successful, table creation pending AWS region fix (in progress)**

## 2025-09-24

### Infrastructure & Setup
- **2025-09-24: Add Apache Pulsar stack và fix Flink compose config at infrastructure/ - Setup message broker cho streaming pipeline; commit 0c2b318 (completed)**
- **2025-09-24: Add Apache Flink setup at infrastructure/flink/ - Setup stream processing engine; commit 68ac796 (completed)**
- **2025-09-24: Fix Flink volume mount error at docker-compose.yml - Sửa lỗi mount volume trong Flink configuration; commit 3ab05e4 (completed)**

### Code Quality & Organization
- **2025-09-24: Remove __pycache__ files from tracking và update .gitignore at ai/ - Cleanup Python cache files và cải thiện git ignore rules; commit f9639ce (completed)**
- **2025-09-24: Refactor code structure at ai/ - Tổ chức lại cấu trúc code cho AI modules; commit f330a91 (completed)**
- **2025-09-24: Simplify và optimize codebase at multiple modules - Tối ưu hóa và đơn giản hóa code; commit 4f448c1 (completed)**

### AI Components Implementation
- **2025-09-24: Implement YOLOv8 detector at ai/detect/yolo_detector.py - Core object detection functionality (completed)**
- **2025-09-24: Implement JSON emitter at ai/emit/json_emitter.py - Output formatting cho detection results (completed)**
- **2025-09-24: Implement video source handling at ai/ingest/ - Support CV2 và GStreamer input sources (completed)**
- **2025-09-24: Implement DeepSort tracker at ai/track/deepsort_tracker.py - Object tracking functionality (completed)**

### Documentation & Project Setup
- **2025-09-24: Create synthetic test data generator at scripts/make_synth_video.py - Tool để tạo video test (completed)**
- **2025-09-24: Add sample videos và test data at data/ - Test datasets cho development (completed)**
- **2025-09-24: Setup project configuration at configs/.env.example - Environment template (completed)**

### Current Status
- **Hoàn thành**: Full infrastructure stack (Pulsar + Flink + MinIO + Iceberg REST), Core AI pipeline, Documentation suite, Cross-platform compatibility
- **Đang tiến hành**: Iceberg lakehouse integration (AWS region config), AI pipeline integration với Pulsar, Flink jobs development
