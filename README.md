# Retail Video Analytics (Lakehouse, Realtime)

> Realtime pipeline thu thập & xử lý **metadata video** cho chuỗi bán lẻ.
> Stack: **GStreamer + YOLOv8 + DeepSort → Pulsar → Flink → Iceberg (REST Catalog) on MinIO → Trino → Grafana**
> Monitoring: **Prometheus + Alertmanager (+ Telegram) + Grafana**
> Orchestration (optional): **Airflow** cho maintenance/batch.

![architecture](docs/architecture.jpg)

## 🎯 Mục tiêu

* **Latency E2E**: ≤ 3–5s (từ khung hình → biểu đồ).
* **Throughput**: 50–200 msg/s (tùy số camera demo).
* **Exactly-once** vào Lakehouse; **replay** không mất dữ liệu.
* Dữ liệu mở: **Parquet + Iceberg** (ACID, time-travel, schema/partition evolution).

---

## 📦 Thành phần chính

* **Ingestion Service**: `gstreamer + yolo v8 + deepsort` → phát hiện & tracking, xuất **JSON metadata** (không đẩy khung hình).
* **Transport**: **Apache Pulsar** (`Key_Shared` theo `camera_id`, schema Avro/JSON, tiered storage → MinIO).
* **Stream Compute**: **Apache Flink** (event-time, watermark, CEP, exactly-once sink).
* **Lakehouse**: **Apache Iceberg** (table format) + **REST Catalog** (backend JDBC) trên **MinIO** (warehouse).
* **Query**: **Trino** (Iceberg connector).
* **Visualization**: **Grafana** (BI near-real-time qua Trino).
* **Monitoring**: **Prometheus + Alertmanager (+ Telegram)**, **Grafana** dashboards.
* **(Optional)** **Airflow**: chạy maintenance/batch/quality (expire snapshots, compaction, export).

---

## 🗂 Cấu trúc thư mục hiện tại

```
.
├─ ai/                    # AI modules cho video analytics
│  ├─ detect/             # YOLOv8 detector implementation
│  │  ├─ yolo_detector.py # YOLOv8 detection core logic
│  │  └─ __pycache__/     # Python bytecode cache
│  ├─ emit/               # JSON emitter cho kết quả detection
│  │  ├─ json_emitter.py  # Xuất detection results dưới dạng JSON
│  │  └─ __pycache__/     # Python bytecode cache
│  ├─ ingest/             # Video source handling (CV2, GStreamer)
│  │  ├─ __init__.py      # Package initialization
│  │  ├─ __main__.py      # Main entry point cho video ingestion
│  │  ├─ cv_source.py     # OpenCV video source handler
│  │  ├─ gst_source.py    # GStreamer video source handler
│  │  └─ __pycache__/     # Python bytecode cache
│  └─ track/              # DeepSort tracker implementation
│     ├─ deepsort_tracker.py # Object tracking với DeepSort algorith
│     └─ __pycache__/     # Python bytecode cache
├─ infrastructure/        # Infrastructure configs và deployment
│  ├─ flink/              # Apache Flink stream processing
│  │  └─ conf/            # Flink configuration files
│  │     ├─ flink-conf.yaml        # Flink cluster configuration
│  │     └─ log4j-console.properties # Logging configuration
│  ├─ iceberg/            # Apache Iceberg lakehouse configs
│  │  ├─ conf/            # Iceberg catalog configuration
│  │  │  └─ application.properties # Iceberg REST catalog config
│  │  └─ sql/             # Iceberg table definitions
│  │     ├─ 01-create-namespaces.sql # Database namespaces
│  │     └─ 02-create-bronze-tables.sql # Bronze layer tables
│  ├─ minio/              # MinIO object storage setup
│  │  ├─ Dockerfile       # MinIO container build
│  │  ├─ .env.example     # MinIO environment template
│  │  ├─ conf/            # MinIO configuration
│  │  │  └─ minio.env     # MinIO server configuration
│  │  └─ scripts/         # MinIO utility scripts
│  │     ├─ entrypoint.sh # MinIO container entrypoint
│  │     └─ init.sh       # MinIO bucket initialization
│  └─ pulsar/             # Apache Pulsar message broker
│     ├─ conf/            # Pulsar configuration files
│     │  ├─ client.conf   # Pulsar client configuration
│     │  └─ standalone.conf # Standalone broker configuration
│     ├─ schema/          # Pulsar schema definitions
│     │  └─ metadata-json-schema.json # JSON schema cho metadata
│     └─ scripts/         # Pulsar utility scripts
│        └─ init-topics.sh # Script tạo topics và subscriptions
├─ flink-jobs/            # Flink streaming jobs (development)
│  └─ lib/                # Flink job JAR files và dependencies
├─ configs/               # Configuration files
├─ data/                  # Sample data và test videos
│  ├─ synth.avi          # Synthetic test video (generated)
│  └─ videos/            # Sample surveillance videos
│     ├─ Midtown corner store surveillance video 11-25-18.mp4 # Real surveillance footage
│     └─ video.mp4       # Test video sample
├─ docs/                  # Documentation và design
│  ├─ architecture.jpg   # System architecture diagram
│  ├─ data-flow-guide.md # Complete pipeline tutorial với commands
│  ├─ guide.md          # User guide và tutorial
│  ├─ CHANGELOG.md      # Project history log
│  └─ HANDOFF.md        # Current status và next steps
├─ scripts/              # Utility scripts
│  ├─ make_synth_video.py # Generate synthetic test data
│  └─ __pycache__/       # Python bytecode cache
├─ .serena/              # Serena MCP server configuration
│  └─ project.yml        # Project settings cho Serena
├─ .venv312/             # Python virtual environment (Python 3.12)
├─ .env                  # Environment variables (local config)
├─ .gitattributes        # Git line ending configuration
├─ AGENTS.md             # Agent code rules và guidelines
├─ docker-compose.yml    # Docker services orchestration (Pulsar + Flink + MinIO + Iceberg)
├─ yolov8n.pt           # Pre-trained YOLOv8 nano model weights
├─ detections_output.ndjson # Sample detection outputs (NDJSON format)
└─ README.md             # Project documentation (this file)
```

---

## ⚙️ Yêu cầu

* Docker & Docker Compose
* GPU (tùy chọn) cho YOLOv8; CPU vẫn chạy được với model nhỏ
* Cổng mặc định (có thể đổi trong `.env`):

  * **Pulsar**: `6650` (broker), `8082` (admin) 
  * **Flink**: `8081` (JobManager Web UI)
  * **MinIO**: `9000` (API), `9001` (Console) 
  * **Iceberg REST**: `8181` (catalog API)
  * **Trino**: `8080` (query engine) - *chưa deploy*
  * **Prometheus**: `9090` (metrics) - *chưa deploy*
  * **Grafana**: `3000` (dashboards) - *chưa deploy*

---

## 📚 Tài liệu chi tiết

- 📄 **Project Doc (Google Drive)**: [Tài liệu Retail Video Analytics](https://drive.google.com/drive/folders/15HIuR8GIeGHsRPt7F2PeaChrG9XlMYoa?usp=sharing)


---

## 👥 Contributors
- [Nguyễn Tấn Hùng](https://github.com/hungfnguyen)
- [Nguyễn Công Đôn](https://github.com/CongDon1207)

