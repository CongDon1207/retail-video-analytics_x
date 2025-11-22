# CHANGELOG

Ghi lại tất cả các công việc đã hoàn thành trong dự án Retail Video Analytics.

## 2025-11-20

- **2025-11-20: Fix Flink SQL Bronze submit error at flink-jobs/sql/bronze_ingest.sql - Tạo database default cho default_catalog trước khi khai báo Pulsar source, hết lỗi “Non-query expression” và job submit thành công (completed)**
 - **2025-11-20: Update docs/guide.md - Nêu rõ SQL Gateway 1.18.1 không support SOURCE interactive; khuyến nghị 1-lệnh -f hoặc dán full nội dung bronze_ingest thủ công (completed)**

## 2025-10-06

### Flink SQL & Catalog Context Fix
- **2025-10-06: Fix Flink SQL Catalog Context error at flink-jobs/bronze_ingest.sql - Switch to `default_catalog` for Pulsar source and fully qualified `iceberg.retail.bronze_detections` for sink to resolve CalciteException (completed)**
- **2025-10-06: Remove unsupported Pulsar Catalog definition at flink-jobs/bronze_ingest.sql - Use inline connector definition to avoid ValidationException (completed)**

### Infrastructure & Ingestion
- **2025-10-06: Create JSONL replay script at scripts/replay_jsonl_to_pulsar.py - Enable replaying historical metadata to Pulsar topic `persistent://retail/metadata/events` (completed)**
- **2025-10-06: Fix Pulsar connection error at infrastructure/pulsar/conf/standalone.conf - Set `advertisedAddress` to `localhost` to allow external client connections (completed)**
- **2025-10-06: Fix Vision module path error at vision/config/settings.py - Use absolute paths for `BASE_DIR` to resolve FileNotFoundError (completed)**

### Documentation
- **2025-10-06: Create comprehensive execution guide at docs/guide.md - Add step-by-step instructions for environment setup, ingestion, and Flink job submission (completed)**

## 2025-10-05

### Flink Bronze Checkpoint Fix
- **2025-10-05: Re-enable Flink checkpointing and add JVM `--add-opens` at infrastructure/flink/conf/flink-conf.yaml - Allow Iceberg Bronze streaming sink to commit Parquet files to MinIO (completed)**
- **2025-10-05: Update docs/HANDOFF.md - Document checkpoint status, new MinIO data footprint, and follow-up actions (completed)**

## 2025-09-30

### End-to-End Pipeline Completion
- **2025-09-30: Fix Avro deserialization issue by migrating to JSON schema at ai/emit/pulsar_producer.py & flink-jobs/bronze_ingest.sql - Resolve ArrayIndexOutOfBoundsException between Python Pulsar client and Flink Avro deserializer; 288 messages successfully written to Bronze layer (completed)**
- **2025-09-30: Disable Flink checkpointing at infrastructure/flink/conf/flink-conf.yaml - Workaround Java reflection error with Arrays$ArrayList; allows Bronze job to complete successfully (completed)**
- **2025-09-30: Verify end-to-end data flow at warehouse/rva/bronze_raw/data/ - 3 Parquet files (67KiB) written to MinIO, partitioned by store_id; full pipeline validated (completed)**
- **2025-09-30: Update documentation at docs/data-flow-guide.md, docs/HANDOFF.md, docs/CHANGELOG.md - Simplify workflow guide, remove outdated troubleshooting, document current working state (completed)**

### Pulsar Version Upgrade & Topic Policies Fix
- **2025-09-30: Upgrade Pulsar from 3.2.0 to 3.3.2 at docker-compose.yml - Improve stability and security patches while maintaining Flink connector compatibility (completed)**
- **2025-09-30: Fix Topic Policies cache timeout at infrastructure/pulsar/conf/standalone.conf - Add systemTopicEnabled=true + topicLevelPoliciesEnabled=true to resolve producer TimeOut errors (completed)**

### Pulsar Client Compatibility
- **2025-09-30: Pin Pulsar client libs to 3.0.0 at infrastructure/flink/Dockerfile - Restore getPartitionedTopicMetadata signature required by Flink connector (completed)**

## 2025-09-29NGELOG

Ghi láº¡i táº¥t cáº£ cÃ¡c cÃ´ng viá»‡c Ä‘Ã£ hoÃ n thÃ nh trong dá»± Ã¡n Retail Video Analytics.

## 2025-09-29

### Flink Pulsar Dependency Alignment
- **2025-09-29: Bundle OpenTelemetry API + incubator jars at infrastructure/flink/Dockerfile - Fix ClassNotFoundException for Pulsar consumer metrics (completed)**
- **2025-09-29: Replace Pulsar all-in-one jar with client + original pair at infrastructure/flink/Dockerfile - Resolve NoSuchMethodError for bronze SQL job (completed)**

## 2025-09-28

### Pulsar Demo Payload Fix
- **2025-09-28: Align Pulsar client libs for Flink image at infrastructure/flink/Dockerfile - Added admin API jar to unblock SQL client (completed)**
- **2025-09-28: Bundle Avro 1.11.3 + Jackson 2.15.2 and pre-create checkpoint dirs at infrastructure/flink/Dockerfile - Flink SQL bronze ingestion job runs without missing classes/checkpoint errors (completed)**
- **2025-09-28: Build Flink lakehouse image at infrastructure/flink/Dockerfile & update docker-compose to preload connectors (completed)**
- **2025-09-28: Automate MinIO warehouse bootstrap with minio-init service & cleanup scripts - Ensure ICEBERG_WAREHOUSE bucket exists on startup (completed)**
- **2025-09-28: Document parallel venv + Docker quick-start paths at docs/guide.md - Users can choose workflow per environment (completed)**
- **2025-09-28: Restore detections_output.ndjson sample at repo root - Dockerized Pulsar producer build succeeds with bundled demo data (completed)**
- **2025-09-28: Set PYTHONPATH for Pulsar producer container at infrastructure/pulsar/producer.Dockerfile - Fix ModuleNotFoundError for ai package during docker run (completed)**
- **2025-09-28: Install pulsar-client[avro] in Pulsar producer image at infrastructure/pulsar/producer.Dockerfile - Enable Avro schema support during container run (completed)**
- **2025-09-28: Align producer Avro record with registered schema at ai/emit/pulsar_producer.py & infrastructure/pulsar/schema/metadata-json-schema.json - Resolve IncompatibleSchema errors when sending messages (completed)**
- **2025-09-28: Normalize bbox input handling at ai/emit/json_emitter.py - Fix TypeError when detections provide bbox as dict strings (completed)**
- **2025-09-28: Allow detections_output.ndjson in producer Docker build context at .dockerignore - Unblock retail/pulsar-producer image build (completed)**
- **2025-09-28: Convert init-topics.sh to LF endings at infrastructure/pulsar/scripts/init-topics.sh - Fix pulsar-init pipefail errors under Linux entrypoint (completed)**
- **2025-09-28: Bổ sung hướng dẫn lỗi NoSuchMethodError của Pulsar client tại docs/data-flow-guide.md - Ghi chú blocker Flink bronze job chờ khắc phục (completed)**

## 2025-09-26

### Pulsar Integration Enablement
- **2025-09-26: Add Pulsar metadata producer module táº¡i ai/emit/pulsar_producer.py & scripts/demo_send_to_pulsar.py - Cho phÃ©p gá»­i metadata detection vÃ o topic events phá»¥c vá»¥ test nhanh (completed)**
- **2025-09-26: Add Docker workflow cho producer táº¡i infrastructure/pulsar/producer.Dockerfile - Cháº¡y producer khÃ´ng cáº§n mÃ´i trÆ°á»ng Python local (completed)**

## 2025-09-25

### Lakehouse Architecture Completion
- **2025-09-25: Add Iceberg REST catalog service at docker-compose.yml - Lakehouse table format support vá»›i MinIO backend; Iceberg service on port 8181 (completed)**
- **2025-09-25: Add MinIO service vÃ  complete docker-compose stack at infrastructure/minio/ - Setup object storage cho Lakehouse architecture; docker-compose fully functional (completed)**
- **2025-09-25: Fix port conflicts between Pulsar vÃ  Flink at docker-compose.yml - Resolve 8080 port conflict, Pulsar admin now on 8082; full 4-service stack healthy (completed)**
- **2025-09-25: Setup MinIO configuration vá»›i proper credentials at .env - MinIO healthcheck passing vá»›i secure credentials, warehouse bucket created (completed)**

### Documentation & Cross-Platform Support
- **2025-09-25: Create comprehensive data flow guide at docs/data-flow-guide.md - Complete tutorial cho AI â†’ Pulsar â†’ Flink â†’ MinIO pipeline vá»›i PowerShell commands (completed)**
- **2025-09-25: Add cross-platform compatibility at .gitattributes - Prevent CRLF/LF issues cho .sh, .env files; Windows/Linux compatibility (completed)**
- **2025-09-25: Update project status at docs/HANDOFF.md vÃ  docs/CHANGELOG.md - Current status vá»›i infrastructure completion, next steps defined (completed)**

### Iceberg Integration Development
- **2025-09-25: Setup Iceberg configuration at infrastructure/iceberg/ - Table schemas vÃ  namespace definitions; catalog config templates (completed)**
- **2025-09-25: Test Iceberg-MinIO connectivity at lakehouse layer - Namespace creation successful, table creation pending AWS region fix (in progress)**

## 2025-09-24

### Infrastructure & Setup
- **2025-09-24: Add Apache Pulsar stack vÃ  fix Flink compose config at infrastructure/ - Setup message broker cho streaming pipeline; commit 0c2b318 (completed)**
- **2025-09-24: Add Apache Flink setup at infrastructure/flink/ - Setup stream processing engine; commit 68ac796 (completed)**
- **2025-09-24: Fix Flink volume mount error at docker-compose.yml - Sá»­a lá»—i mount volume trong Flink configuration; commit 3ab05e4 (completed)**

### Code Quality & Organization
- **2025-09-24: Remove __pycache__ files from tracking vÃ  update .gitignore at ai/ - Cleanup Python cache files vÃ  cáº£i thiá»‡n git ignore rules; commit f9639ce (completed)**
- **2025-09-24: Refactor code structure at ai/ - Tá»• chá»©c láº¡i cáº¥u trÃºc code cho AI modules; commit f330a91 (completed)**
- **2025-09-24: Simplify vÃ  optimize codebase at multiple modules - Tá»‘i Æ°u hÃ³a vÃ  Ä‘Æ¡n giáº£n hÃ³a code; commit 4f448c1 (completed)**

### AI Components Implementation
- **2025-09-24: Implement YOLOv8 detector at ai/detect/yolo_detector.py - Core object detection functionality (completed)**
- **2025-09-24: Implement JSON emitter at ai/emit/json_emitter.py - Output formatting cho detection results (completed)**
- **2025-09-24: Implement video source handling at ai/ingest/ - Support CV2 vÃ  GStreamer input sources (completed)**
- **2025-09-24: Implement DeepSort tracker at ai/track/deepsort_tracker.py - Object tracking functionality (completed)**

### Documentation & Project Setup
- **2025-09-24: Create synthetic test data generator at scripts/make_synth_video.py - Tool Ä‘á»ƒ táº¡o video test (completed)**
- **2025-09-24: Add sample videos vÃ  test data at data/ - Test datasets cho development (completed)**
- **2025-09-24: Setup project configuration at configs/.env.example - Environment template (completed)**

### Current Status
- **HoÃ n thÃ nh**: Full infrastructure stack (Pulsar + Flink + MinIO + Iceberg REST), Core AI pipeline, Documentation suite, Cross-platform compatibility
- **Äang tiáº¿n hÃ nh**: Iceberg lakehouse integration (AWS region config), AI pipeline integration vá»›i Pulsar, Flink jobs development
