# 🚀 Retail Video Analytics Pipeline - Hướng Dẫn End-to-End

> **Streaming Lakehouse Architecture**: Vision AI → Pulsar → Flink → Iceberg → Trino

Hướng dẫn chi tiết từng bước để khởi chạy pipeline phân tích video bán lẻ theo kiến trúc Medallion (Bronze-Silver-Gold).

---

## 📋 Mục Lục

1. [Chuẩn bị Môi trường Python](#1-chuẩn-bị-môi-trường-python)
2. [Khởi chạy Hạ tầng](#2-khởi-chạy-hạ-tầng)
3. [Tạo Dữ liệu từ Video](#3-tạo-dữ-liệu-từ-video)
4. [Submit Bronze Job](#4-submit-bronze-job)
5. [Ingestion vào Pulsar](#5-ingestion-vào-pulsar)
6. [Submit Silver & Gold Jobs](#6-submit-silver--gold-jobs)
7. [Truy vấn Lakehouse](#7-truy-vấn-lakehouse)
8. [Monitoring & Troubleshooting](#8-monitoring--troubleshooting)

> ⚠️ **Lưu ý quan trọng về thứ tự:**
> 1. Submit **Bronze Job** (Section 4) trước khi replay data
> 2. Replay data vào Pulsar (Section 5)
> 3. Submit **Silver & Gold Jobs** (Section 6) sau khi đã có data trong Bronze

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

## 4. Submit Bronze Job

> ⚠️ **QUAN TRỌNG:** Submit Bronze Job **TRƯỚC** khi gửi data vào Pulsar để đảm bảo consumer sẵn sàng nhận messages.

### 4.1. Submit Bronze Job (Streaming)

```bash
docker exec flink-jobmanager sh -c \
  "./bin/flink run -d -c org.rva.BronzeIngestJob /opt/flink/usrlib/bronze-job.jar"
```

**Job Details:**
- **Class:** `org.rva.BronzeIngestJob`
- **Mode:** Detached (`-d`) - chạy background
- **Source:** Pulsar topic `persistent://retail/metadata/events`
- **Sink:** Iceberg table `lakehouse.rva.bronze_raw`
- **Checkpoint:** Every 60 seconds

### 4.2. Kiểm tra Job Status

```bash
# Xem danh sách jobs đang chạy
docker exec flink-jobmanager sh -c "./bin/flink list"

# Hoặc truy cập Flink Web UI: http://localhost:8081
```

✅ **Xác nhận:** Đảm bảo Bronze Job đang ở trạng thái `RUNNING` trước khi tiếp tục Section 5.

---

## 5. Ingestion vào Pulsar

> 💡 **Lưu ý:** Chỉ chạy bước này **SAU KHI** Flink Bronze Job đã `RUNNING`.

### 5.1. Replay Messages từ JSONL

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

### 5.2. Verify Dữ liệu trong Pulsar

#### Kiểm tra Topic Stats

```bash
docker exec pulsar-broker bin/pulsar-admin topics stats \
  persistent://retail/metadata/events
```

**Các metrics quan trọng:**
- `msgInCounter` - Tổng messages đã nhận
- `msgOutCounter` - Messages đã consume
- `msgBacklog` - Messages chưa xử lý (nên = 0 nếu Flink đang consume)
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

### 5.3. Test với Sample Data (Optional)

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

## 6. Submit Silver & Gold Jobs

> 💡 **Lưu ý:** Chạy sau khi đã replay data và chờ Bronze checkpoint (~60s) để có data trong `bronze_raw`.

### 6.1. Verify Bronze Data

```bash
# Chờ checkpoint
sleep 65

# Kiểm tra data đã có trong Bronze
docker exec trino sh -c "trino --catalog lakehouse --schema rva --execute \
  'SELECT COUNT(*) FROM bronze_raw'"
```

### 6.2. Submit Silver Job (Streaming)

```bash
docker exec flink-jobmanager sh -c \
  "./bin/flink run -d -c org.rva.silver.SilverJob /opt/flink/usrlib/silver-job.jar"
```



**Job Details:**
- **Source:** Iceberg table `lakehouse.rva.bronze_raw`
- **Sink:** Iceberg table `lakehouse.rva.silver_detection`

### 6.3. Submit Gold Batch Job

```bash
# Chờ Silver checkpoint trước
sleep 65

docker exec flink-jobmanager sh -c \
  "./bin/flink run -d -c org.rva.gold.GoldBatchJob /opt/flink/usrlib/gold-job.jar"

# chạy streaming

docker exec flink-jobmanager sh -c \
  "./bin/flink run -d -c org.rva.gold.GoldStreamingJob /opt/flink/usrlib/gold-job.jar"

```

**Job Details:**
- **Source:** Iceberg table `lakehouse.rva.silver_detection`
- **Sink:** Các tables: `gold_people_per_minute`, `gold_track_summary`, `gold_zone_dwell`, `gold_zone_heatmap`

### 6.4. Kiểm tra All Jobs

```bash
docker exec flink-jobmanager sh -c "./bin/flink list"
```

✅ **Xác nhận:** Cả 3 jobs (Bronze, Silver, Gold) đều ở trạng thái `RUNNING`.

---

## 7. Truy vấn Lakehouse

### 7.1. Kiểm tra MinIO (Storage)

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

### 7.2. Query với Trino

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

### 7.3. Truy cập Trino Console

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

## 8. Monitoring & Troubleshooting

### 8.0. Grafana Dashboards

**Grafana UI:** http://localhost:3000 (user/pass mặc định `admin` / `admin` nếu chưa đổi)

Datasource `Trino Lakehouse` đã được provision sẵn (trỏ tới Trino catalog `iceberg`, schema `rva`).  
Các dashboard chính:

- **RVA - People Overview**: đọc từ `gold_people_per_minute`, cho bảng detections/unique_people theo phút và camera.
- **RVA - Zone Dwell & Heatmap**: đọc từ `gold_zone_dwell`, cho visits và dwell time theo zone_x/zone_y.
- **RVA - Track Summary**: đọc từ `gold_track_summary`, cho danh sách track với duration, movement (delta_x/delta_y) và avg_conf.

Chỉ cần đảm bảo Bronze/Silver/Gold jobs đã chạy xong, sau đó mở Grafana và chọn các dashboard này để xem số liệu.

### 8.1. Flink Monitoring

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

### 8.2. Pulsar Monitoring

```bash
# Kiểm tra broker health
curl http://localhost:8084/admin/v2/brokers/health

# Xem cluster info
docker exec pulsar-broker bin/pulsar-admin clusters list

# Xem namespace policies
docker exec pulsar-broker bin/pulsar-admin namespaces policies retail/metadata
```

### 8.3. Common Issues

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

### 8.4. Cleanup & Reset

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

**📝 Last Updated:** November 25, 2025  
**🔖 Version:** 1.2.0


