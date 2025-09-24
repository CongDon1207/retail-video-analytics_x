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

<<<<<<< HEAD
```bash
py -3.12 -m ai.ingest \
  --backend cv \
  --src "data/videos/video.mp4" \
  --yolo 1 \
  --track 1 \
  --display 1 \
  --emit detection \
  --out detections_midtown_t3.ndjson \
  --conf 0.25 \
  --track_max_age 90 \
  --track_n_init 3 \
  --track_iou 0.8 \
  --track_nms_overlap 0.9
```

Nếu có GPU, có thể bật embedder GPU để tăng re-identification: `--track_embedder_gpu 1`

### Bước 4: Kiểm tra kết quả

```bash
# Xem thông tin file output
ls -la detections_output.ndjson
wc -l detections_output.ndjson

# Xem sample output
head -2 detections_output.ndjson | jq .
```

## 📊 Đọc hiểu Log Output

```
[INFO] Frames=30 | Res=1280x720 | ~9.8 FPS | det_total=33 | active_tracks=0
```

**Giải thích:**
- `Frames=30`: Đã xử lý 30 frames
- `Res=1280x720`: Resolution video 
- `~9.8 FPS`: Tốc độ xử lý pipeline
- `det_total=33`: Tổng số detection từ đầu
- `active_tracks=0`: Số đối tượng đang được track

## 🎛️ Tuỳ chỉnh Pipeline

### Chỉ Detection (không Tracking)
```bash
py -3.12 -m ai.ingest --backend cv --src video.mp4 --yolo 1 --track 0 --display 1
```

### Thay đổi model YOLO
```bash
py -3.12 -m ai.ingest --backend cv --src video.mp4 --model yolov8s.pt --conf 0.5 --display 1
```

### Lọc chỉ detect người
```bash
py -3.12 -m ai.ingest --backend cv --src video.mp4 --classes person --display 1
```

### Điều chỉnh metadata nguồn
```bash
py -3.12 -m ai.ingest \
  --src video.mp4 \
  --store_id "store_downtown" \
  --camera_id "cam_entrance" \
  --stream_id "main_feed" \
  --display 1
```

## 🔍 GStreamer Backend (Advanced)

**Khi nào dùng GStreamer:**
- Xử lý RTSP streams từ IP cameras
- Cần hiệu năng tốt với H.264 decoding
- Xử lý nhiều stream đồng thời

```bash
# Thử GStreamer (sẽ fallback OpenCV nếu thiếu 'gi')
py -3.12 -m ai.ingest --backend gst --src video.mp4 --display 1

# RTSP stream (khi có GStreamer)
py -3.12 -m ai.ingest --backend gst --src "rtsp://user:pass@192.168.1.100/stream" --display 1
```

## 📄 Format NDJSON Output

Mỗi dòng là JSON của 1 frame:

```json
{
  "schema_version": "1.0",
  "pipeline_run_id": "unique-run-id", 
  "source": {
    "store_id": "store_01",
    "camera_id": "cam_01", 
    "stream_id": "stream_01"
  },
  "frame_index": 1,
  "capture_ts": "2025-09-02T03:00:13.891188+00:00",
  "detections": [
    {
      "det_id": "1-0",
      "class": "person",
      "class_id": 0, 
      "conf": 0.823,
      "bbox": {"x1": 344, "y1": 58, "x2": 438, "y2": 430},
      "bbox_norm": {"x": 0.269, "y": 0.081, "w": 0.073, "h": 0.517},
      "centroid": {"x": 391, "y": 244},
      "track_id": null
    }
  ]
}
```

## ⚠️ Troubleshooting

### Lỗi thiếu dependencies
```bash
# Kiểm tra packages
py -3.12 -m pip list | grep -E "(ultralytics|opencv|deep-sort)"

# Cài lại nếu thiếu
py -3.12 -m pip install ultralytics opencv-python deep-sort-realtime
```

### Video không load được
```bash
# Kiểm tra file tồn tại
ls -la "data/videos/"

# Test với video đơn giản
py -3.12 scripts/make_synth_video.py
py -3.12 -m ai.ingest --backend cv --src data/synth.avi --display 1
```

### GStreamer fallback
```
[WARN] GStreamer backend không sẵn sàng (thiếu gi). Tự động chuyển sang OpenCV.
```
**→ Bình thường**, pipeline vẫn hoạt động với OpenCV backend.

### Không thấy detection
- Video tổng hợp (`synth.avi`) có thể không có đối tượng thực → bình thường
- Dùng video surveillance có người/xe để thấy detection
- Giảm `--conf` (mặc định 0.25) để detection nhạy hơn

## 🎯 Use Cases thực tế

**Retail Surveillance:**
```bash
py -3.12 -m ai.ingest \
  --src "rtsp://camera-ip/stream" \
  --classes "person,bag,bottle" \
  --store_id "walmart_downtown" \
  --camera_id "checkout_cam_03" \
  --emit detection \
  --out retail_detections.ndjson \
  --display 1
```

**Traffic Monitoring:**
```bash  
py -3.12 -m ai.ingest \
  --src traffic_video.mp4 \
  --classes "car,truck,bus,motorcycle" \
  --track 1 \
  --emit detection \
  --out traffic_analysis.ndjson \
  --display 1
```

## 🛠️ Khởi động hạ tầng Pulsar & Flink

```bash
# Bật Pulsar (broker + script init schema/topic)
docker compose up -d pulsar-broker pulsar-init

# Bật Flink JobManager + TaskManager
docker compose up -d flink-jobmanager flink-taskmanager

# Kiểm tra nhanh
docker compose ps
curl http://localhost:8081/overview
docker compose logs pulsar-init | tail
```

**Lưu ý:**
- `pulsar-init` chỉ chạy một lần để tạo tenant/topic, có thể xoá container sau khi hoàn tất.
- Muốn khởi chạy lại init, dùng `docker compose run --rm pulsar-init`.
- Các volume `pulsar_data` và `flink_state` giữ trạng thái giữa các lần restart.

---

**💡 Tip**: Luôn dùng `--display 1` khi test để theo dõi trực quan pipeline hoạt động!

## Các tham số nâng cao DeepSORT

- `--track_max_age INT` (ENV: `TRACK_MAX_AGE`): số frame giữ track khi mất (mặc định 30)
- `--track_n_init INT` (ENV: `TRACK_N_INIT`): số lần hit để xác nhận track (mặc định 3)
- `--track_iou FLOAT` (ENV: `TRACK_IOU`): ngưỡng IoU cho matching (mặc định 0.7)
- `--track_nms_overlap FLOAT` (ENV: `TRACK_NMS_OVERLAP`): NMS overlap (1.0 = tắt NMS)
- `--track_embedder STR` (ENV: `TRACK_EMBEDDER`): embedder appearance (`mobilenet`, v.v.)
- `--track_embedder_gpu {0|1}` (ENV: `TRACK_EMBEDDER_GPU`): bật GPU cho embedder
- `--track_half {0|1}` (ENV: `TRACK_EMBEDDER_HALF`): dùng FP16 cho mobilenet embedder

## Các tham số CLI cơ bản

- `--src`: đường dẫn file hoặc RTSP URL (bắt buộc)
- `--backend {gst,cv}`: chọn backend ingest (mặc định: `gst`, tự fallback `cv` nếu thiếu `gi`)
- `--yolo {0|1}`: bật/tắt YOLO detect (mặc định 1)
- `--track {0|1}`: bật/tắt DeepSORT tracking (mặc định 1)
- `--display {0|1}`: hiển thị preview bằng OpenCV (mặc định 1)
- `--fps_log N`: chu kỳ log FPS (mặc định 30)
- `--emit {none|detection}` + `--out PATH`: xuất NDJSON theo frame
- Metadata nguồn: `--store_id`, `--camera_id`, `--stream_id`, `--run_id`

## Định dạng NDJSON chi tiết

Mỗi dòng là một JSON với các trường: `schema_version`, `pipeline_run_id`, `source{store_id,camera_id,stream_id}`, `frame_index`, `capture_ts`, `detections[]`.

Mỗi detection: `class`, `class_id`, `conf`, `bbox{x1,y1,x2,y2}`, `bbox_norm{x,y,w,h}`, `centroid{x,y}`, `track_id|null`.

## Hiệu năng & GPU

- YOLOv8 có thể dùng GPU nếu PyTorch/CUDA sẵn sàng; mặc định chạy CPU để đơn giản.
- DeepSORT embedder mặc định 'mobilenet', chạy CPU (chúng tôi bật `embedder_gpu=False`).

## Khắc phục sự cố nhanh

- Lỗi import `ultralytics/opencv-python/deep_sort_realtime`: kiểm tra đã cài đúng Python phiên bản đang chạy (`py -3.12 -m pip list`).
- Lỗi `gi` không có: backend GStreamer sẽ fallback sang OpenCV; nếu muốn dùng GStreamer, xem mục GStreamer Backend.
- Không thấy detection trên video mẫu: bình thường. Hãy chạy với video thực tế có người/vật.
- RTSP lag: thử GStreamer backend (`--backend gst`) sẽ ổn định hơn OpenCV cho H.264/RTSP.
=======
>>>>>>> main
