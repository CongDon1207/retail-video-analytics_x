# 🚀 Retail Video Analytics Pipeline - Hướng Dẫn End-to-End

> **Streaming Lakehouse Architecture**: Vision AI → Pulsar → Flink → Iceberg → Trino

Hướng dẫn chi tiết từng bước để khởi chạy pipeline phân tích video bán lẻ theo kiến trúc Medallion (Bronze-Silver-Gold).

---

## 📋 Mục Lục

1. [Chuẩn bị Môi trường Python](#1-chuẩn-bị-môi-trường-python)
2. [Khởi chạy Hạ tầng](#2-khởi-chạy-hạ-tầng)
3. [Tạo Dữ liệu từ Video](#3-tạo-dữ-liệu-từ-video)
4. [Ingestion vào Pulsar](#4-ingestion-vào-pulsar)
5. [Bronze Layer Processing](#5-bronze-layer-processing)
6. [Truy vấn Lakehouse](#6-truy-vấn-lakehouse)
7. [Monitoring & Troubleshooting](#7-monitoring--troubleshooting)

---

## 1. Chuẩn bị Môi trường Python

### 1.1. Tạo Virtual Environment

Mở terminal (Git Bash/PowerShell) tại thư mục gốc:

```bash
# Tạo môi trường ảo (chỉ chạy 1 lần)
python -m venv venv

# Kích hoạt môi trường
# Windows (Git Bash/PowerShell):
source venv/Scripts/activate

# Windows (Command Prompt):
venv\Scripts\activate
```

### 1.2. Cài đặt Dependencies

```bash
pip install -r setup.txt
```

**Các thư viện chính:**
- `ultralytics` - YOLO11 object detection
- `opencv-python` - Video processing
- `pulsar-client` - Apache Pulsar client
- `deep-sort-realtime` - Object tracking

---

## 2. Khởi chạy Hạ tầng

### 2.1. Khởi động Docker Compose

```bash
docker-compose up -d --build
```

⏱️ **Chờ 1-2 phút** để các service khởi động hoàn toàn.

### 2.2. Kiểm tra Services

```bash
# Kiểm tra containers đang chạy
docker ps

# Kiểm tra logs nếu có vấn đề
docker-compose logs -f [service_name]
```

**Services & Ports:**
- **Pulsar Broker**: `6650` (client), `8084` (admin)
- **Flink JobManager**: `8081` (Web UI)
- **MinIO**: `9001` (Console)
- **Trino**: `8082` (Query Engine)
- **Iceberg REST**: `8181` (Catalog)

---

## 3. Tạo Dữ liệu từ Video

### 3.1. Chạy Vision Module

```bash
# Đảm bảo venv đã kích hoạt
python vision/main.py
```

**📊 Output:**
- File JSONL: `data/metadata/video.jsonl`
- Real-time video window với bounding boxes

**⌨️ Controls:**
- `q` - Dừng processing
- `ESC` - Thoát

**Cấu trúc Output JSON:**
```json
{
  "source": {
    "store_id": "store_01",
    "camera_id": "cam_01",
    "stream_id": "stream_001"
  },
  "detections": [
    {
      "det_id": "d_1732276800_001",
      "class": "person",
      "bbox": {"x1": 100, "y1": 200, "x2": 300, "y2": 400},
      "conf": 0.92,
      "track_id": 5
    }
  ],
  "frame_index": 1234,
  "capture_ts": "2025-11-22T10:30:00.123Z",
  "image_size": {"width": 1280, "height": 720}
}
```

---

## 4. Ingestion vào Pulsar

### 4.1. Replay Messages

```bash
# Kích hoạt venv (nếu chưa)
source venv/Scripts/activate

# Chạy script replay (30 FPS simulation)
python scripts/replay_jsonl_to_pulsar.py
```

**Cấu hình mặc định:**
- Topic: `persistent://retail/metadata/events`
- Service URL: `pulsar://localhost:6650`
- FPS: 30 (có thể điều chỉnh trong script)

### 4.2. Verify Dữ liệu trong Pulsar

#### Kiểm tra Topic Stats

```bash
docker exec pulsar-broker bin/pulsar-admin topics stats \
  persistent://retail/metadata/events
```

**Các metrics quan trọng:**
- `msgInCounter` - Tổng messages đã nhận
- `msgOutCounter` - Messages đã consume
- `msgBacklog` - Messages chưa xử lý
- `storageSize` - Dung lượng topic

#### Xem Subscriptions

```bash
docker exec pulsar-broker bin/pulsar-admin topics subscriptions \
  persistent://retail/metadata/events
```

#### Xem Metadata

```bash
docker exec pulsar-broker bin/pulsar-admin topics stats-internal \
  persistent://retail/metadata/events
```

---

## 5. Bronze Layer Processing

### 5.1. Build Flink Job (Java)

```bash
cd flink-jobs/java
mvn clean package -DskipTests
```

**Output:** `target/silver-job-0.1.0.jar`

### 5.2. Deploy vào Flink Cluster

```bash
# Copy JAR vào JobManager container
docker cp target/silver-job-0.1.0.jar \
  flink-jobmanager:/opt/flink/usrlib/bronze-job.jar
```

### 5.3. Submit Bronze Job

```bash
docker exec flink-jobmanager sh -c \
  "./bin/flink run -d -c org.rva.BronzeIngestJob /opt/flink/usrlib/bronze-job.jar"
```

**Job Details:**
- **Class:** `org.rva.BronzeIngestJob`
- **Mode:** Detached (`-d`)
- **Source:** Pulsar topic `persistent://retail/metadata/events`
- **Sink:** Iceberg table `lakehouse.rva.bronze_raw`
- **Checkpoint:** Every 60 seconds

### 5.4. Kiểm tra Job Status

```bash
# Xem danh sách jobs đang chạy
docker exec flink-jobmanager sh -c "./bin/flink list"

# Hoặc truy cập Flink Web UI
# http://localhost:8081
```

### 5.5. Test với Sample Data

```bash
# Gửi 5 test messages vào Pulsar
docker exec pulsar-broker sh -c "python3 -c \"
import pulsar, json, time
client = pulsar.Client('pulsar://localhost:6650')
producer = client.create_producer('persistent://retail/metadata/events')
msg = {
  'source': {'store_id': 'S001', 'camera_id': 'CAM01', 'stream_id': 'stream1'},
  'detections': [{'det_id': 'd1', 'class': 'person', 'bbox': {'x1': 100, 'y1': 200}}],
  'image_size': {'width': 1280, 'height': 720}
}
for i in range(5):
    producer.send(json.dumps(msg).encode('utf-8'))
    print(f'Sent message {i+1}')
    time.sleep(0.5)
producer.close()
client.close()
print('Done')
\""
```

---

## 6. Truy vấn Lakehouse

### 6.1. Kiểm tra MinIO (Storage)

```bash
# Setup MinIO client alias
docker exec minio mc alias set local \
  http://localhost:9000 minioadmin minioadmin123

# Xem cấu trúc thư mục
docker exec minio mc ls -r local/warehouse/rva/bronze_raw/

# Kiểm tra data files
docker exec minio mc ls -r local/warehouse/rva/bronze_raw/data/

# Kiểm tra metadata files
docker exec minio mc ls -r local/warehouse/rva/bronze_raw/metadata/
```

### 6.2. Query với Trino

⏱️ **Lưu ý:** Chờ ~60 giây sau khi submit job để Flink checkpoint commit data.

```bash
# Chờ checkpoint
sleep 65

# Query aggregate
docker exec trino sh -c "trino --catalog lakehouse --schema rva --execute \
  'SELECT store_id, camera_id, COUNT(*) as cnt 
   FROM bronze_raw 
   GROUP BY store_id, camera_id'"

# Query chi tiết
docker exec trino sh -c "trino --catalog lakehouse --schema rva --execute \
  'SELECT * FROM bronze_raw LIMIT 10'"

# Kiểm tra schema
docker exec trino sh -c "trino --catalog lakehouse --schema rva --execute \
  'DESCRIBE bronze_raw'"
```

### 6.3. Truy cập Trino Console

Mở browser: **http://localhost:8082**

```sql
-- Query mẫu
SELECT 
  store_id,
  camera_id,
  DATE_FORMAT(ingest_ts, '%Y-%m-%d %H:%i') as hour,
  COUNT(*) as message_count
FROM lakehouse.rva.bronze_raw
GROUP BY 
  store_id, 
  camera_id, 
  DATE_FORMAT(ingest_ts, '%Y-%m-%d %H:%i')
ORDER BY hour DESC
LIMIT 20;
```

---

## 7. Monitoring & Troubleshooting

### 7.1. Flink Monitoring

**Flink Web UI:** http://localhost:8081

**Metrics quan trọng:**
- `numRecordsIn` - Records đọc từ Pulsar
- `numRecordsOut` - Records ghi vào Iceberg
- `checkpointDuration` - Thời gian checkpoint
- `lastCheckpointSize` - Kích thước checkpoint

**CLI Commands:**
```bash
# Xem job details
docker exec flink-jobmanager sh -c "./bin/flink list -r"

# Cancel job (thay JOB_ID)
docker exec flink-jobmanager sh -c "./bin/flink cancel <JOB_ID>"

# Xem logs
docker logs flink-taskmanager -f
```

### 7.2. Pulsar Monitoring

```bash
# Kiểm tra broker health
curl http://localhost:8084/admin/v2/brokers/health

# Xem cluster info
docker exec pulsar-broker bin/pulsar-admin clusters list

# Xem namespace policies
docker exec pulsar-broker bin/pulsar-admin namespaces policies retail/metadata
```

### 7.3. Common Issues

#### Issue 1: Job không consume messages

**Kiểm tra:**
```bash
# Verify subscription tồn tại
docker exec pulsar-broker bin/pulsar-admin topics subscriptions \
  persistent://retail/metadata/events

# Xem subscription stats
docker exec pulsar-broker bin/pulsar-admin topics stats \
  persistent://retail/metadata/events | grep -A 20 "subscriptions"
```

#### Issue 2: Data không xuất hiện trong Trino

**Nguyên nhân:** Chưa có checkpoint commit.

**Giải pháp:** Chờ 60+ giây hoặc force checkpoint:
```bash
docker exec flink-jobmanager sh -c \
  "./bin/flink savepoint <JOB_ID>"
```

#### Issue 3: S3/MinIO connection error

**Kiểm tra:**
```bash
# Test MinIO connectivity
docker exec flink-jobmanager curl -I http://minio:9000/minio/health/live

# Verify S3 plugin
docker exec flink-jobmanager ls -la /opt/flink/plugins/s3-fs-hadoop/
```

### 7.4. Cleanup & Reset

```bash
# Stop tất cả services
docker-compose down

# Xóa volumes (⚠️ mất dữ liệu)
docker-compose down -v

# Xóa old jobs
docker exec flink-jobmanager sh -c "./bin/flink cancel <JOB_ID>"

# Reset Pulsar topic
docker exec pulsar-broker bin/pulsar-admin topics delete \
  persistent://retail/metadata/events
```

---

## 📚 Tham Khảo

- **Flink Documentation:** https://flink.apache.org/
- **Pulsar Documentation:** https://pulsar.apache.org/
- **Iceberg Documentation:** https://iceberg.apache.org/
- **Trino Documentation:** https://trino.io/docs/

---

## 🎯 Next Steps

1. ✅ **Bronze Layer** - Raw data ingestion (completed)
2. 🔄 **Silver Layer** - Data cleaning & transformation
3. 🔄 **Gold Layer** - Business aggregations
4. 🔄 **Monitoring** - Grafana dashboards
5. 🔄 **Airflow** - Orchestration & scheduling

---

**📝 Last Updated:** November 22, 2025  
**🔖 Version:** 1.0.0


