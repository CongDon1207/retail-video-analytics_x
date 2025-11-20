# main.py
from track.tracker_factory import create_tracker
from emit.json_emitter import JsonEmitter
from utils.visualizer import Visualizer
from config.settings import settings
from datetime import datetime, timezone
import uuid
import cv2

if __name__ == "__main__":
    # --- metadata chung ---
    pipeline_run_id = uuid.uuid4().hex
    source = {
        "store_id": settings.STORE_ID,
        "camera_id": settings.CAMERA_ID,
        "stream_id": settings.STREAM_ID
    }

    # --- khởi tạo ---
    print(f"[Main] Starting pipeline run: {pipeline_run_id}")
    print(f"[Main] Config: Model={settings.MODEL_NAME}, Tracker={settings.TRACKER_TYPE}, Video={settings.VIDEO_PATH}")
    
    tracker = create_tracker(settings.TRACKER_TYPE, settings.MODEL_NAME, conf_thres=settings.CONF_THRES)
    emitter = JsonEmitter(settings.OUT_JSONL)
    visualizer = Visualizer()

    try:
        # --- chạy tracking ---
        for idx, record in enumerate(tracker.track(settings.VIDEO_PATH, show=False, classes=settings.CLASS_FILTER), start=1):
            frame = record["frame"]
            H, W = frame.shape[:2]
            objects = record["objects"]

            # Hiển thị (Vẽ bbox và label)
            visualizer.draw_tracks(frame, objects)

            # map sang detections theo schema đã thống nhất
            detections = []
            for j, obj in enumerate(objects):
                x1, y1, x2, y2 = map(float, obj["bbox"])
                w = x2 - x1
                h = y2 - y1
                cx = x1 + w/2.0
                cy = y1 + h/2.0
                detections.append({
                    "det_id": f"{idx}-{j}",
                    "class": obj.get("label", "person"),
                    "class_id": int(obj.get("cls", 0)),
                    "conf": float(obj.get("conf", 0.0)),
                    "bbox": {"x1": x1, "y1": y1, "x2": x2, "y2": y2},
                    "bbox_norm": {"x": x1/W, "y": y1/H, "w": w/W, "h": h/H},
                    "centroid": {"x": int(cx), "y": int(cy)},
                    "centroid_norm": {"x": cx/W, "y": cy/H},
                    "track_id": None if obj.get("id", -1) < 0 else int(obj["id"]),
                })

            # emit JSON (1 dòng / frame)
            emitter.emit_frame(
                pipeline_run_id=pipeline_run_id,
                source=source,
                frame_index=idx,
                capture_ts_iso=datetime.now(timezone.utc).isoformat(),
                image_size={"width": W, "height": H},
                detections=detections,
                runtime={
                    "model_name": settings.MODEL_NAME,
                    "tracker_type": settings.TRACKER_TYPE,
                    "conf_thres": settings.CONF_THRES,
                    "class_filter": settings.CLASS_FILTER
                },
                source_uri=settings.VIDEO_PATH
            )

            # hiển thị
            cv2.imshow("YOLO11 Tracking", frame)
            if cv2.waitKey(1) & 0xFF == ord("q"):
                break
    finally:
        emitter.close()
        cv2.destroyAllWindows()
