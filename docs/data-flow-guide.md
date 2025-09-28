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
