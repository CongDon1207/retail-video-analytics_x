# Data Flow Guide

## 🎯 Tổng quan

Hướng dẫn chạy end-to-end pipeline từ video input → AI detection → Pulsar messaging → Lakehouse storage.

---

## 📥 Bước 1: Chạy AI Pipeline với xuất NDJSON

Chạy AI detection trên video để tạo ra file metadata `detections_output.ndjson`.

```bash
# Chạy AI pipeline với video
py -3.12 -m ai.ingest \
  --backend cv \
  --src "data/videos/video.mp4" \
  --yolo 1 \
  --track 1 \
  --display 1 \
  --emit detection \
  --out detections_output.ndjson
```

**Tham số:**
- `--backend cv`: Dùng OpenCV làm video backend
- `--src`: Đường dẫn tới video input
- `--yolo 1`: Bật YOLOv8 detection
- `--track 1`: Bật DeepSort tracking
- `--display 1`: Hiển thị video realtime (hoặc `0` để tắt)
- `--emit detection`: Xuất kết quả detection
- `--out`: File output NDJSON chứa metadata

---

## 🚀 Bước 2: Gửi metadata vào Pulsar

### Build và chạy Producer

```bash
# Build producer image
docker build -f infrastructure/pulsar/producer.Dockerfile -t retail/pulsar-producer .

# Chạy producer
docker run --rm --network=retail-video-analytics_retail-net \
  retail/pulsar-producer \
  --service-url pulsar://pulsar-broker:6650 \
  --topic persistent://retail/metadata/events
```

**Lưu ý:** Producer sử dụng JSON schema (không phải Avro) để tương thích với Flink SQL JSON deserializer.

---

## �️ Bước 3: Đồng bộ lớp Bronze vào Iceberg

### 3.1 Build Flink image với connectors

```bash
docker compose build flink-jobmanager flink-taskmanager
```

**Connectors tự động tải:**
- `flink-connector-pulsar-4.1.0-1.18.jar`
- `iceberg-flink-runtime-1.18-1.5.0.jar`
- `iceberg-aws-bundle-1.5.0.jar`
- `flink-shaded-hadoop-2-uber-2.8.3-10.0.jar`

### 3.2 Khởi động Flink cluster

```bash
docker compose up -d flink-jobmanager flink-taskmanager
```

### 3.3 Submit Bronze ingestion job

```bash
MSYS_NO_PATHCONV=1 docker exec -it flink-jobmanager bash -lc \
  "/opt/flink/bin/sql-client.sh -f /opt/flink/usrlib/bronze_ingest.sql"
```

**Job sẽ:**
- Tạo Iceberg catalog `lakehouse` kết nối với MinIO
- Tạo database `rva` và table `bronze_raw`
- Consume messages từ Pulsar topic (JSON format)
- Parse JSON payload để extract `camera_id`, `store_id`
- Ghi dữ liệu vào Iceberg table (Parquet format)

---

## ✅ Bước 4: Kiểm tra kết quả

### Kiểm tra dữ liệu trong MinIO

```bash
# Setup alias
docker exec minio mc alias set local http://localhost:9000 minioadmin minioadmin123

# Liệt kê dữ liệu Bronze
docker exec minio mc ls -r local/warehouse/rva/bronze_raw/data/
docker exec minio mc ls -r local/warehouse/rva/bronze_raw/metadata/

# Kiểm tra dung lượng
docker exec minio mc du local/warehouse/rva/
```

### Kiểm tra Flink jobs

```bash
# Xem danh sách jobs
curl http://localhost:8081/jobs

# Xem chi tiết job
curl http://localhost:8081/jobs/<job-id>
```

### Kiểm tra Pulsar topic

```bash
# Xem schema
MSYS_NO_PATHCONV=1 docker exec pulsar-broker \
  /pulsar/bin/pulsar-admin schemas get persistent://retail/metadata/events

# Xem stats
MSYS_NO_PATHCONV=1 docker exec pulsar-broker \
  /pulsar/bin/pulsar-admin topics partitioned-stats persistent://retail/metadata/events
```


## 📊 Data Flow Architecture

```
Video Input 
  → AI Pipeline (YOLOv8 + DeepSort)
    → NDJSON metadata
      → Pulsar Producer (JSON schema)
        → Pulsar Topic (persistent://retail/metadata/events)
          → Flink Streaming Job (JSON deserialization)
            → Iceberg Bronze Table (Parquet on MinIO)
```