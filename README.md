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
│  ├─ emit/               # JSON emitter cho kết quả detection
│  ├─ ingest/             # Video source handling (CV2, GStreamer)
│  └─ track/              # DeepSort tracker implementation
├─ infrastructure/        # Infrastructure configs và deployment
│  ├─ flink/              # Apache Flink configuration
│  └─ pulsar/             # Apache Pulsar configuration
├─ configs/               # Configuration files
│  └─ .env.example        # Environment variables template
├─ data/                  # Sample data và test videos
│  ├─ synth.avi          # Synthetic test video
│  └─ videos/            # Sample surveillance videos
├─ docs/                  # Documentation và design
│  ├─ architecture.jpg   # System architecture diagram
│  ├─ guide.md          # User guide
│  ├─ CHANGELOG.md      # Project history log
│  └─ HANDOFF.md        # Current status và next steps
├─ scripts/              # Utility scripts
│  └─ make_synth_video.py # Generate synthetic test data
├─ docker-compose.yml    # Docker services orchestration
├─ yolov8n.pt           # Pre-trained YOLOv8 nano model
├─ detections_output.ndjson # Sample detection outputs
└─ README.md
```

---

## ⚙️ Yêu cầu

* Docker & Docker Compose
* GPU (tùy chọn) cho YOLOv8; CPU vẫn chạy được với model nhỏ
* Cổng mặc định (có thể đổi trong `.env`):

  * MinIO: `9000/9001`, Trino: `8080`, Pulsar: `6650/8080`, Prometheus: `9090`, Grafana: `3000`, Iceberg REST: `8181`, Airflow Web: `8088`

## 📦 Pulsar Metadata Producer (Demo)

1. **Chuẩn bị môi trường Python** (khuyến nghị dùng venv):
   ```bash
   python -m venv .venv312
   # Linux/Mac
   source .venv312/bin/activate
   # Windows PowerShell / Git Bash
   .venv312\Scripts\activate
   pip install --upgrade pip
   pip install pulsar-client==3.5.0
   ```
2. **Khai báo biến môi trường** để Python thấy module `ai` và in Unicode đúng:
   ```bash
   export PYTHONPATH=.
   export PYTHONIOENCODING=utf-8
   # PowerShell
   $env:PYTHONPATH='.'; $env:PYTHONIOENCODING='utf-8'
   ```
3. **Khởi động stack hạ tầng** theo `docs/data-flow-guide.md` (ví dụ `docker compose up -d`).
4. **Kiểm thử nhanh (không gửi message)** — có thể chạy ngay cả khi Pulsar chưa bật:
   ```bash
   python scripts/demo_send_to_pulsar.py --dry-run --limit 3
   ```
   Lệnh sẽ đọc NDJSON và in thông tin từng frame mà không tạo connection tới broker.
5. **Gửi dữ liệu thật vào Pulsar** (broker đã chạy và schema đã khởi tạo):
   ```bash
   python scripts/demo_send_to_pulsar.py --ndjson detections_output.ndjson \
     --service-url pulsar://localhost:6650 \
     --topic persistent://retail/metadata/events
   ```
   Có thể bỏ các tham số nếu dùng cấu hình mặc định trong repo.
6. **Chạy producer bằng Docker** (không cần cài Python local):
   ```bash
   docker build -f infrastructure/pulsar/producer.Dockerfile -t retail/pulsar-producer .
   docker run --rm --network=retail-video-analytics_retail-net \
     retail/pulsar-producer \
     --service-url pulsar://pulsar-broker:6650 \
     --topic persistent://retail/metadata/events
   ```
   Nếu đổi tên thư mục project, thay `retail-video-analytics` trong tên network bằng tên mới của bạn.

---

## 📚 Tài liệu chi tiết

- 📄 **Project Doc (Google Drive)**: [Tài liệu Retail Video Analytics](https://drive.google.com/drive/folders/15HIuR8GIeGHsRPt7F2PeaChrG9XlMYoa?usp=sharing)


---

## 👥 Contributors
- [Nguyễn Tấn Hùng](https://github.com/hungfnguyen)
- [Nguyễn Công Đôn](https://github.com/CongDon1207)
