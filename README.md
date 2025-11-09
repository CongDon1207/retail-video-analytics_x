# Retail Video Analytics (Lakehouse, Realtime)

> Realtime pipeline thu thập & xử lý **metadata video** cho chuỗi bán lẻ.
> Stack: **GStreamer + YOLOv8 + DeepSort → Pulsar → Flink → Iceberg on MinIO → Trino → Grafana**
> Orchestration (optional): **Airflow** cho maintenance/batch.

![architecture](docs/architecture.png) 

## 📦 Thành phần chính

  * **Ingestion Service**: `gstreamer + yolo v8 + deepsort` → phát hiện & tracking, xuất **JSON metadata** (không đẩy khung hình).
  * **Transport**: **Apache Pulsar** (`Key_Shared` theo `camera_id`, schema Avro/JSON).
  * **Stream Compute**: **Apache Flink** (để xử lý, làm sạch, và ghi dữ liệu).
  * **Lakehouse**: **Apache Iceberg** (table format) + **REST Catalog** trên **MinIO** (kho lưu trữ).
  * **Query**: **Trino** (Iceberg connector).
  * **Visualization**: **Grafana** (BI near-real-time qua Trino).
  * **(Optional) Orchestration**: **Airflow** (chạy các tác vụ bảo trì, dọn dẹp Iceberg).

-----

## ⚙️ Yêu cầu & Cài đặt

  * Docker & Docker Compose
  * (Tùy chọn) GPU cho YOLOv8

### 1\. Chuẩn bị Biến môi trường

Copy tệp `.env.example` thành `.env` và điền các thông tin credentials (ví dụ: `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`).

```bash
cp .env.example .env
# Mở file .env và chỉnh sửa
```

### 2\. Xây dựng (Build) và Khởi chạy

Các image Flink và Airflow tùy chỉnh (nếu có) sẽ được tự động build.

```bash
# Khởi động toàn bộ hạ tầng ở chế độ nền (detached)
docker compose up -d
```

### 3\. Cổng dịch vụ (Default Ports)

  * **Trino**: `8080`
  * **Flink UI**: `8081`
  * **Pulsar Admin**: `8082`
  * **Airflow UI**: `8088`
  * **MinIO API**: `9000`
  * **MinIO Console**: `9001`
  * **Iceberg REST**: `8181`
  * **Grafana**: `3000`
  * **Pulsar Broker**: `6650`

-----

## 📚 Tài liệu chi tiết

  * 📄 **Project Doc (Google Drive)**: [Tài liệu Retail Video Analytics](https://drive.google.com/drive/folders/15HIuR8GIeGHsRPt7F2PeaChrG9XlMYoa?usp=sharing)
  * 📄 **Hướng dẫn chạy (Local)**: Xem `docs/guide.md`
  * 📄 **Luồng dữ liệu E2E**: Xem `docs/data-flow-guide.md`

-----

## 👥 Contributors

  * [Nguyễn Tấn Hùng](https://github.com/hungfnguyen)
  * [Nguyễn Công Đôn](https://github.com/CongDon1207)