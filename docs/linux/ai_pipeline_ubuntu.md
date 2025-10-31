# Hướng dẫn chạy pipeline AI trên Ubuntu

## 1. Chuẩn bị hệ thống
- Cập nhật gói và cài Python cùng công cụ build:
  ```bash
  sudo apt update
  sudo apt install python3 python3-venv python3-pip build-essential
  ```
- (Tùy chọn) Cài GStreamer nếu cần ingest RTSP ổn định:
  ```bash
  sudo apt install gstreamer1.0-tools gstreamer1.0-plugins-base \
    gstreamer1.0-plugins-good gstreamer1.0-plugins-bad
  ```

## 2. Tạo virtualenv và cài thư viện
```bash
cd ~/project/retail-video-analytics
python3 -m venv .venv
source .venv/bin/activate
export PYTHONPATH="$PWD"    # đảm bảo import được package `ai`
pip install --upgrade pip wheel setuptools
pip install ultralytics opencv-python deep-sort-realtime
```

## 3. Chạy pipeline với video mẫu
- CLI chính: `python -m ai.ingest`
- Ví dụ với file surveillance (bật preview):
  ```bash
  python -m ai.ingest \
    --backend cv \
    --src "data/videos/Midtown corner store surveillance video 11-25-18.mp4" \
    --yolo 1 --track 1 --display 1
  ```
- Nếu muốn chạy headless, tắt preview (`--display 0`).

## 4. Chạy pipeline với RTSP
```bash
python -m ai.ingest \
  --backend gst \
  --src "rtsp://user:pass@camera-ip:554/stream" \
  --yolo 1 --track 1 --display 0
```
> CLI tự fallback về OpenCV nếu GStreamer gặp lỗi.

## 5. Xuất metadata NDJSON
```bash
python -m ai.ingest \
  --backend cv \
  --src data/videos/video.mp4 \
  --yolo 1 --track 1 --display 0 \
  --emit detection \
  --out detections_output.ndjson
```

## 6. Gửi metadata lên Pulsar (tùy chọn)
```bash
# Luôn chạy từ thư mục gốc của project
python -m scripts.demo_send_to_pulsar \
  --ndjson detections_output.ndjson \
  --service-url pulsar://localhost:6650 \
  --topic persistent://retail/metadata/events
```

## 7. Lưu ý
- Nhấn `Ctrl+C` để dừng pipeline; chạy `deactivate` để thoát virtualenv.
- `--classes person,car` giúp giới hạn lớp cần detect.
- Mỗi lần chạy mới, file `detections_output.ndjson` sẽ được ghi đè; nếu muốn lưu lịch sử, đổi đường dẫn `--out`.
- Đảm bảo các service Pulsar/MinIO/Flink đã chạy nếu tiếp tục pipeline downstream.
