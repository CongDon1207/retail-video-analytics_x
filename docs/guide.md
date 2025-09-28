# Hướng dẫn Video Analytics Pipeline (Ingest → YOLOv8 → DeepSORT → Export)

Video pipeline thực hiện luồng xử lý video hoàn chỉnh: **Ingest video** → **Object Detection** → **Object Tracking** → **Export Metadata**

## 🎯 Tổng quan Pipeline

**Pipeline Components:**
- **Ingest**: Đọc video từ file MP4/RTSP qua GStreamer hoặc OpenCV
- **Detect**: Phát hiện đối tượng (người, xe, đồ vật) bằng YOLOv8 
- **Track**: Theo dõi đối tượng qua các frame bằng DeepSORT
- **Emit**: Xuất metadata detection/tracking dạng NDJSON

**Luồng xử lý**: `Video Frame` → `YOLO Detection` → `DeepSORT Tracking` → `JSON Metadata` → `Display/Export`

## 📁 Cấu trúc chi tiết thư mục /ai

```
ai/
├── ingest/                   # Module đọc và điều phối video
│   ├── __init__.py          # Package init (4 dòng)
│   ├── __main__.py          # ⭐ CLI chính điều phối pipeline (160 dòng)
│   ├── gst_source.py        # GStreamer video backend (90 dòng)
│   └── cv_source.py         # OpenCV video backend (32 dòng)
├── detect/                  # Module object detection
│   └── yolo_detector.py     # ⭐ YOLOv8 wrapper (33 dòng)
├── track/                   # Module object tracking  
│   └── deepsort_tracker.py  # ⭐ DeepSORT wrapper (80 dòng)
└── emit/                    # Module xuất kết quả
    └── json_emitter.py      # ⭐ NDJSON metadata exporter (90 dòng)
```
## 🔧 Cài đặt môi trường

**Python 3.12** (khuyến nghị trên Windows)

1) Tạo virtual environment (venv)

```bash
py -3.12 -m venv .venv312
```

2) Kích hoạt venv — chọn lệnh phù hợp với shell bạn đang dùng:

- cmd.exe (Command Prompt):

```powershell
.venv312\Scripts\activate.bat
```

- PowerShell:

```powershell
.venv312\Scripts\Activate.ps1
```

- Git Bash / WSL / bash.exe:

```bash
source .venv312/Scripts/activate
```

Lưu ý: nếu bạn không muốn/không thể kích hoạt venv, có thể chạy pip thông qua Python cụ thể:`py -3.12 -m pip ...`.

3) Cài dependencies (chạy sau khi đã activate hoặc dùng `py -3.12 -m pip`)

```bash
# (sau khi đã activate) 
py -3.12 -m pip install --upgrade pip wheel setuptools
py -3.12 -m pip install ultralytics opencv-python deep-sort-realtime
```

4) Kiểm tra cài đặt (tùy shell)

- Trên bash (Git Bash / WSL):

```bash
py -3.12 -m pip list | grep -E "(ultralytics|opencv|deep-sort)"
```

- Trên Windows cmd / PowerShell (dùng findstr thay cho grep):

```powershell
py -3.12 -m pip list | findstr /R "ultralytics opencv deep-sort"
```

### Lựa chọn nhanh: Docker producer (không cần venv)

```bash
docker build -f infrastructure/pulsar/producer.Dockerfile -t retail/pulsar-producer .
docker run --rm --network=retail-video-analytics_retail-net \
  retail/pulsar-producer \
  --service-url pulsar://pulsar-broker:6650 \
  --topic persistent://retail/metadata/events \
  --limit 10
```

> Gợi ý: dùng `--dry-run` nếu chỉ muốn xem payload, và nhớ đổi `--network` nếu bạn đặt tên stack khác.

## 🚀 Cách chạy Pipeline từng bước

### Bước 1: Chuẩn bị video test

```bash
# Tạo video tổng hợp để test (nếu chưa có video thực)
py -3.12 scripts/make_synth_video.py
# → Tạo data/synth.avi

# Hoặc dùng video thực có sẵn
ls "data/videos/"
```

### Bước 2: Chạy Pipeline cơ bản (với display)

```bash
# Test với video thực - hiển thị cửa sổ preview
py -3.12 -m ai.ingest \
  --backend cv \
  --src "data/videos/Midtown corner store surveillance video 11-25-18.mp4" \
  --yolo 1 \
  --track 1 \
  --display 1
```

**Ý nghĩa từng tham số:**
- `--backend cv`: Dùng OpenCV để đọc video (ổn định, không cần GStreamer)
- `--src`: Đường dẫn file video input  
- `--yolo 1`: Bật YOLO detection (phát hiện người, xe, đồ vật)
- `--track 1`: Bật DeepSORT tracking (gán ID cho đối tượng qua frames)
- `--display 1`: **Hiển thị cửa sổ preview** để xem trực quan quá trình detect/track

### Bước 3: Chạy Pipeline với xuất NDJSON

```bash
# Chạy đầy đủ + export metadata
py -3.12 -m ai.ingest \
  --backend cv \
  --src "data/videos/Midtown corner store surveillance video 11-25-18.mp4" \
  --yolo 1 \
  --track 1 \
  --display 1 \
  --emit detection \
  --out detections_output.ndjson
```

```bash
py -3.12 -m ai.ingest \
  --backend cv \
  --src "data/videos/video.mp4" \
  --yolo 1 \
  --track 1 \
  --display 1 \
  --emit detection \
  --out detections_output.ndjson
```

