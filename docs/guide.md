# Hướng dẫn Chạy Pipeline Retail Video Analytics (End-to-End)

Tài liệu này hướng dẫn chi tiết từng bước để khởi chạy hệ thống từ môi trường phát triển (Python) đến hạ tầng Streaming Lakehouse (Pulsar -> Flink -> Iceberg).

---

## 1. Chuẩn bị Môi trường Python

Trước tiên, cần thiết lập môi trường Python để chạy các module AI/Vision và script giả lập dữ liệu.

### 1.1. Tạo và kích hoạt Virtual Environment

Mở terminal (Git Bash hoặc PowerShell) tại thư mục gốc dự án:

```bash
# Tạo môi trường ảo (chỉ làm 1 lần)
python -m venv venv

# Kích hoạt môi trường (Windows)
source venv/Scripts/activate
# Hoặc nếu dùng Command Prompt: venv\Scripts\activate
```

### 1.2. Cài đặt thư viện

Cài đặt các dependencies cần thiết cho cả Vision và Pulsar Client:

```bash
pip install -r setup.txt
```

---

## 2. Khởi chạy Hạ tầng (Infrastructure)

Hệ thống sử dụng Docker Compose để chạy các dịch vụ nền tảng: Pulsar, Flink, MinIO, Iceberg REST.

```bash
# Build và khởi chạy các container
docker-compose up -d --build
```

*Lưu ý: Chờ khoảng 1-2 phút để các service (đặc biệt là Pulsar và Flink) khởi động hoàn toàn.*

---

## 3. Chạy Module Vision (Tạo Dữ liệu)

Bước này chạy mô hình YOLO để phát hiện đối tượng từ video và sinh ra file metadata.

```bash
# Chạy module vision
python vision/main.py
```

*   **Input:** Video tại `vision/video/video3.mp4` (hoặc cấu hình trong `.env`).
*   **Output:** File metadata tại `data/metadata/video.jsonl`.
*   **Thao tác:** Nhấn phím `q` trên cửa sổ video để dừng sớm nếu muốn.

---

## 4. Ingestion: Đẩy dữ liệu vào Pulsar

Sử dụng script để đọc file metadata vừa tạo và đẩy vào Pulsar topic `retail/metadata/events`.

**Lưu ý:** Đảm bảo đã kích hoạt môi trường ảo trước khi chạy:

```bash
# Kích hoạt venv (nếu chưa kích hoạt)
source venv/Scripts/activate

# Chạy script replay
python scripts/replay_jsonl_to_pulsar.py
```

*   Script sẽ giả lập tốc độ 30 FPS.
*   Dữ liệu sẽ được gửi vào topic `persistent://retail/metadata/events`.

---

## 5. Processing: Flink Streaming ETL (Bronze Layer)

Sử dụng Flink SQL để đọc từ Pulsar và ghi vào bảng Iceberg (Bronze Layer).

### 5.1. Truy cập Flink SQL Client

Mở terminal mới:

```bash
# Vào container Flink JobManager và mở SQL Client với file init
docker exec -it flink-jobmanager bash
./bin/sql-client.sh -i conf/sql-client-init.sql
```

### 5.2. Submit Job Bronze

**Cách khuyến nghị (1 lệnh, tránh lỗi “Non-query expression”)**  
Chạy trực tiếp từ host, CLI sẽ tự xử lý file:

```bash
docker exec -it flink-jobmanager bash -lc "./bin/sql-client.sh -i conf/sql-client-init.sql -f /opt/flink/usrlib/sql/bronze_ingest.sql"
```

**Nếu vẫn muốn gõ thủ công trong giao diện `Flink SQL>`** (không dùng `SOURCE` vì SQL Gateway 1.18.1 không hỗ trợ trong interactive mode, dễ gặp lỗi “Non-query expression”): dán lần lượt ba đoạn sau:

```sql
CREATE DATABASE IF NOT EXISTS default_catalog.`default`;

CREATE TABLE IF NOT EXISTS default_catalog.`default`.pulsar_source_raw (
    `value` STRING,
    `event_time` TIMESTAMP(3) METADATA FROM 'publish_time',
    `properties` MAP<STRING, STRING> METADATA FROM 'properties'
) WITH (
    'connector' = 'pulsar',
    'topics' = 'persistent://retail/metadata/events',
    'service-url' = 'pulsar://pulsar-broker:6650',
    'source.start.message-id' = 'earliest',
    'format' = 'raw'
);

CREATE TABLE IF NOT EXISTS iceberg.retail.bronze_detections (
    ingest_ts TIMESTAMP(3),
    publish_ts TIMESTAMP(3),
    raw_payload STRING,
    source_properties MAP<STRING, STRING>
);

INSERT INTO iceberg.retail.bronze_detections
SELECT 
    CURRENT_TIMESTAMP,
    event_time,
    `value`,
    properties
FROM default_catalog.`default`.pulsar_source_raw;
```

### 5.3. Kiểm tra Kết quả

Sau khi job được submit (trả về Job ID), bạn có thể kiểm tra dữ liệu đã vào Iceberg chưa bằng câu lệnh:

```sql
-- Query kiểm tra 5 dòng đầu tiên từ bảng Bronze
SELECT * FROM iceberg.retail.bronze_detections LIMIT 5;
```

Hoặc truy cập Dashboard Flink tại: [http://localhost:8081](http://localhost:8081) để theo dõi trạng thái Job.

---

## 6. Troubleshooting

*   **Lỗi Pulsar Connect:** Nếu script Python không kết nối được Pulsar, hãy đảm bảo `advertisedAddress=localhost` trong `infrastructure/pulsar/conf/standalone.conf` và restart Pulsar container.
*   **Lỗi Flink SQL:** Nếu gặp lỗi "Catalog not found", hãy kiểm tra lại file `sql-client-init.sql` và đảm bảo đã chạy `./bin/sql-client.sh -i ...`.
