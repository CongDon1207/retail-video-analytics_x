# Data Flow Guide

## 🎯 Tổng quan

Hướng dẫn chạy end-to-end pipeline từ video input → AI detection → Pulsar messaging → Lakehouse storage.

---

## 📥 Bước 1: Chạy AI Pipeline với xuất NDJSON

Chạy AI detection trên video để tạo ra file metadata `detections_output.ndjson`.

### **Video mẫu 1 - Surveillance camera:**
```bash
# Chạy AI pipeline với video surveillance
py -3.12 -m ai.ingest \
  --backend cv \
  --src "data/videos/Midtown corner store surveillance video 11-25-18.mp4" \
  --yolo 1 \
  --track 1 \
  --display 1 \
  --emit detection \
  --out detections_output.ndjson
```

### **Video mẫu 2 - General video:**
```bash
# Chạy AI pipeline với video thông thường
py -3.12 -m ai.ingest \
  --backend cv \
  --src "data/videos/video.mp4" \
  --yolo 1 \
  --track 1 \
  --display 1 \
  --emit detection \
  --out detections_output.ndjson
```

### **Giải thích các tham số:**
- `--backend cv`: Dùng OpenCV làm video backend
- `--src`: Đường dẫn tới video input
- `--yolo 1`: Bật YOLOv8 detection
- `--track 1`: Bật DeepSort tracking
- `--display 1`: Hiển thị video realtime (có thể tắt với `0`)
- `--emit detection`: Xuất kết quả detection
- `--out`: File output NDJSON chứa metadata

---

## 🚀 Bước 2: Gửi metadata vào Pulsar

### **Phương pháp 1: Chạy Producer bằng Docker (Khuyến nghị)**

Build Docker image và chạy producer:

```bash
# Build producer image
docker build -f infrastructure/pulsar/producer.Dockerfile -t retail/pulsar-producer .

# Chạy producer trong Docker network
docker run --rm --network=retail-video-analytics_retail-net \
  retail/pulsar-producer \
  --service-url pulsar://pulsar-broker:6650 \
  --topic persistent://retail/metadata/events
```

## 🚀 Bước 3: Đồng bộ lớp Bronze vào Iceberg

### 3.1 Build image Flink đã kèm connector

Thay vì tải JAR thủ công, hãy build image `infrastructure/flink/Dockerfile`:

```bash
docker compose build flink-jobmanager flink-taskmanager
```

Dockerfile sẽ tự động tải:
- `flink-connector-pulsar-4.1.0-1.18.jar`
- `iceberg-flink-runtime-1.18-1.5.0.jar`
- `iceberg-aws-bundle-1.5.0.jar`
- `flink-shaded-hadoop-2-uber-2.8.3-10.0.jar`

### 3.2 Khởi động lại cụm Flink

```bash
docker compose up -d flink-jobmanager flink-taskmanager
```

### 3.3 Chạy job Bronze

File `bronze_ingest.sql` đã được copy vào image tại `/opt/flink/usrlib/`. Thực thi job:

```bash
MSYS_NO_PATHCONV=1 docker exec -it flink-jobmanager bash -lc "/opt/flink/bin/sql-client.sh -f /opt/flink/usrlib/bronze_ingest.sql"
```

### 3.4 Kiểm tra nhanh

```bash
# Xem bucket trong MinIO
docker exec minio mc ls local/warehouse

# Kiểm tra trạng thái job Flink
curl http://localhost:8081/jobs
```

> Job `bronze_ingest.sql` tạo catalog `lakehouse` (Iceberg REST + S3FileIO) và ghi payload NDJSON vào bảng `rva.bronze_raw`. Đây là lớp Bronze nền tảng cho các bước Silver/Gold.

---

## ⚠️ Troubleshooting

### **Lỗi: ClassNotFoundException: org.apache.pulsar.client.api.SubscriptionType**

**Nguyên nhân:** Pulsar connector thiếu client API JARs trong classpath.

**Fix nhanh:**
```bash
# Tải pulsar-client JARs vào runtime
docker exec flink-jobmanager curl -L -o /opt/flink/lib/pulsar-client-3.2.0.jar \
  https://repo1.maven.org/maven2/org/apache/pulsar/pulsar-client/3.2.0/pulsar-client-3.2.0.jar

docker exec flink-jobmanager curl -L -o /opt/flink/lib/pulsar-client-api-3.2.0.jar \
  https://repo1.maven.org/maven2/org/apache/pulsar/pulsar-client-api/3.2.0/pulsar-client-api-3.2.0.jar

# Restart services
docker compose restart flink-jobmanager flink-taskmanager

# Chờ 10 giây và retry
sleep 10
MSYS_NO_PATHCONV=1 docker exec -it flink-jobmanager bash -lc "/opt/flink/bin/sql-client.sh -f /opt/flink/usrlib/bronze_ingest.sql"
```

**Fix vĩnh viễn:** Rebuild Dockerfile với đầy đủ dependencies (đang được fix trong version tiếp theo).
