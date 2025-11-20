# track/deepsort_tracker.py
from pathlib import Path
from typing import Dict, Iterator, List, Any
import os
from deep_sort_realtime.deepsort_tracker import DeepSort
from detect.yolo_detector import YoloDetector
from ingest.CVSource import ingest_video
import numpy as np

def _resolve_model_path(model_name: str) -> str:
    """Tìm model trong detect/models/"""
    detect_dir = Path(__file__).resolve().parents[1] / "detect" / "models"
    model_path = detect_dir / model_name
    if not model_path.is_file():
        raise FileNotFoundError(f"Không tìm thấy model '{model_name}' trong {detect_dir}")
    return str(model_path)

def _get_env(name: str, default: str) -> str:
    v = os.getenv(name)
    return v if v is not None and v != "" else default


def _get_env(name: str, default: str) -> str:
    v = os.getenv(name)
    return v if v is not None and v != "" else default


class DeepSORTTracker:
    """
    DeepSORT tracker sử dụng deep-sort-realtime + YOLO detector.
    Khác với BoTSORT/ByteTrack vì không dùng Ultralytics .track()
    """
    def __init__(self, model_name: str, conf_thres: float = 0.25):
        print(f"[DeepSORT] Khởi tạo DeepSORT tracker")
        # Đọc tham số từ ENV (tối ưu cho camera tĩnh, occlusion ~1-3s)
        max_age = int(float(_get_env("DS_MAX_AGE", "90")))            # giữ track tối đa ~3s @30FPS
        n_init = int(float(_get_env("DS_N_INIT", "3")))               # số frame xác nhận track
        max_iou_distance = float(_get_env("DS_MAX_IOU_DISTANCE", "0.7"))
        embedder = _get_env("DEEPSORT_EMBEDDER", "mobilenet")          # mobilenet | torchreid
        embedder_gpu = _get_env("DEEPSORT_EMBEDDER_GPU", "1") in {"1", "true", "True"}
        det_conf = float(_get_env("DS_DET_CONF", str(conf_thres)))

        print(f"[DeepSORT] Model: {model_name}, YOLO conf: {det_conf}")
        print(f"[DeepSORT] Params -> max_age={max_age}, n_init={n_init}, max_iou_distance={max_iou_distance}, embedder={embedder}, embedder_gpu={embedder_gpu}")

        # Khởi tạo YOLO detector
        self.detector = YoloDetector(model_name=model_name, conf_thres=det_conf)

        # Khởi tạo DeepSORT (chỉ truyền tham số an toàn theo API đã dùng)
        self.tracker = DeepSort(
            max_age=max_age,
            n_init=n_init,
            max_iou_distance=max_iou_distance,
            embedder=embedder,
            embedder_gpu=embedder_gpu,
        )
        print(f"[DeepSORT] Tracker initialized")
        print("-" * 60)

    def track(self, source: str, show: bool = False, classes: List[int] = None) -> Iterator[Dict[str, Any]]:
        """
        Track objects từ video/stream.
        
        Args:
            source: đường dẫn video
            show: hiển thị (không dùng, để tương thích API)
            classes: list class ID cần track
        
        Yields:
            Dict chứa frame_index, frame, objects
        """
        for idx, item in enumerate(ingest_video(source, realtime=False), start=0):
            frame = item["frame"]
            
            # Detect bằng YOLO
            detections = self.detector.predict(frame, class_filter=classes)
            
            # Tạo mapping detection -> (cls_id, label) để lưu thông tin
            det_info_map = {}
            
            # Chuyển sang format DeepSORT: ([x1,y1,w,h], confidence, class_name)
            deepsort_detections = []
            for i, det in enumerate(detections):
                x1, y1, x2, y2 = det["bbox"]
                w = x2 - x1
                h = y2 - y1
                
                # Lưu thông tin gốc
                det_info_map[i] = {
                    "cls": det["cls"],
                    "label": det["label"],
                    "conf": det["conf"]
                }
                
                deepsort_detections.append((
                    [x1, y1, w, h],
                    det["conf"],
                    str(i)  # Dùng index làm class_name tạm
                ))
            
            # Update tracker
            tracks = self.tracker.update_tracks(deepsort_detections, frame=frame)
            
            # Chuyển sang format output chuẩn
            objects = []
            for track in tracks:
                if not track.is_confirmed():
                    continue
                
                track_id = track.track_id
                ltrb = track.to_ltrb()  # [left, top, right, bottom]
                x1, y1, x2, y2 = ltrb
                
                # Lấy thông tin class từ detection gốc
                det_class_str = track.det_class if hasattr(track, 'det_class') else "0"
                try:
                    det_idx = int(det_class_str)
                    det_info = det_info_map.get(det_idx, {})
                    cls_id = det_info.get("cls", 0)
                    label = det_info.get("label", "person")
                    conf_value = det_info.get("conf", 0.0)
                except (ValueError, KeyError):
                    cls_id = 0
                    label = "person"
                    conf_value = 0.0
                
                objects.append({
                    "id": int(track_id),
                    "bbox": [float(x1), float(y1), float(x2), float(y2)],
                    "cls": cls_id,
                    "label": label,
                    "conf": float(conf_value),
                })
            
            yield {
                "frame_index": idx,
                "type": "track",
                "frame": frame,
                "objects": objects
            }
