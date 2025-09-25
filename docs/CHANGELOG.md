# CHANGELOG

Ghi lại tất cả các công việc đã hoàn thành trong dự án Retail Video Analytics.

## 2025-09-25

### Infrastructure Completion
- **2025-09-25: Add MinIO service và complete docker-compose stack at infrastructure/minio/ - Setup object storage cho Lakehouse architecture; docker-compose fully functional (completed)**
- **2025-09-25: Fix port conflicts between Pulsar và Flink at docker-compose.yml - Resolve 8080 port conflict, Pulsar admin now on 8082; commit XXX (completed)**
- **2025-09-25: Setup MinIO configuration với proper credentials at .env - MinIO healthcheck passing với secure credentials; commit XXX (completed)**

### Documentation & Guides
- **2025-09-25: Create comprehensive data flow guide at docs/data-flow-guide.md - Complete tutorial cho AI → Pulsar → Flink → MinIO pipeline; commit XXX (completed)**
- **2025-09-25: Add cross-platform compatibility at .gitattributes - Prevent CRLF/LF issues cho .sh, .env files; commit XXX (completed)**
- **2025-09-25: Update project status at docs/HANDOFF.md và docs/CHANGELOG.md - Current status và next steps documentation; commit XXX (completed)**

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
- **Hoàn thành**: Full infrastructure stack (Pulsar, Flink, MinIO), Core AI pipeline, Documentation suite, Cross-platform compatibility
- **Đang tiến hành**: AI pipeline integration với Pulsar, Flink jobs development, Iceberg setup
