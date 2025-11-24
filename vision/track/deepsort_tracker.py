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
        self.nms_iou = float(_get_env("DS_NMS_IOU", "0.6"))
        self.smooth_alpha = float(_get_env("DS_SMOOTH_ALPHA", "0.6"))
        self.smooth_min_iou = float(_get_env("DS_SMOOTH_MIN_IOU", "0.10"))
        self._prev_boxes: Dict[int, List[float]] = {}

        print(f"[DeepSORT] Model: {model_name}, YOLO conf: {det_conf}")
        print(
            f"[DeepSORT] Params -> max_age={max_age}, n_init={n_init}, "
            f"max_iou_distance={max_iou_distance}, embedder={embedder}, "
            f"embedder_gpu={embedder_gpu}, nms_iou={self.nms_iou}, "
            f"smooth_alpha={self.smooth_alpha}, smooth_min_iou={self.smooth_min_iou}"
        )

        # Khởi tạo YOLO detector
        self.detector = YoloDetector(model_name=model_name, conf_thres=det_conf)

        # Khởi tạo DeepSORT (chỉ truyền tham số an toàn theo API đã dùng)
        # Nếu embedder=torchreid nhưng thiếu dependency (vd: gdown), fallback về mobilenet để tránh crash.
        try:
            self.tracker = DeepSort(
                max_age=max_age,
                n_init=n_init,
                max_iou_distance=max_iou_distance,
                embedder=embedder,
                embedder_gpu=embedder_gpu,
            )
        except Exception as e:
            if embedder == "torchreid":
                print(f"[DeepSORT] Embedder 'torchreid' lỗi ({e}); fallback về 'mobilenet'")
                self.tracker = DeepSort(
                    max_age=max_age,
                    n_init=n_init,
                    max_iou_distance=max_iou_distance,
                    embedder="mobilenet",
                    embedder_gpu=embedder_gpu,
                )
            else:
                raise
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
            # Giảm bớt trường hợp 1 người nhiều bbox gần giống nhau:
            # áp dụng NMS nhẹ theo IoU và cùng class trước khi đưa vào DeepSORT.
            detections = self._suppress_overlaps(detections)
            
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

                # Làm mượt và clip bbox để tránh phóng to/thu nhỏ bất thường
                H, W = frame.shape[:2]
                x1, y1, x2, y2 = self._smooth_and_clip_bbox(
                    track_id, [x1, y1, x2, y2], frame_h=H, frame_w=W
                )
                
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

    def _suppress_overlaps(self, detections: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Loại bớt các bbox trùng lặp (IoU cao, cùng class) để tránh 1 người có nhiều bbox.
        Giữ lại bbox có confidence cao hơn trong mỗi cụm trùng.
        """
        if not detections:
            return detections

        # Sắp xếp theo confidence giảm dần
        dets = sorted(detections, key=lambda d: d.get("conf", 0.0), reverse=True)
        kept: List[Dict[str, Any]] = []

        for d in dets:
            x1, y1, x2, y2 = d["bbox"]
            area1 = max(0.0, x2 - x1) * max(0.0, y2 - y1)
            should_keep = True

            for k in kept:
                if d.get("cls") != k.get("cls"):
                    continue
                kx1, ky1, kx2, ky2 = k["bbox"]

                xx1 = max(x1, kx1)
                yy1 = max(y1, ky1)
                xx2 = min(x2, kx2)
                yy2 = min(y2, ky2)

                w = max(0.0, xx2 - xx1)
                h = max(0.0, yy2 - yy1)
                inter = w * h
                if inter <= 0:
                    continue

                area2 = max(0.0, kx2 - kx1) * max(0.0, ky2 - ky1)
                union = area1 + area2 - inter
                iou = inter / union if union > 0 else 0.0

                if iou >= self.nms_iou:
                    # bbox mới gần trùng hoàn toàn với bbox đã giữ → bỏ
                    should_keep = False
                    break

            if should_keep:
                kept.append(d)

        return kept

    def _smooth_and_clip_bbox(self, track_id: int, bbox: List[float], frame_h: int, frame_w: int) -> List[float]:
        """
        Làm mượt bbox bằng EMA để giảm rung/phóng to nhỏ bất thường, rồi clip vào khung hình.
        """
        x1, y1, x2, y2 = bbox

        # Clip vào khung hình trước khi tính IoU
        x1 = max(0.0, min(float(frame_w - 1), x1))
        y1 = max(0.0, min(float(frame_h - 1), y1))
        x2 = max(x1 + 1.0, min(float(frame_w), x2))
        y2 = max(y1 + 1.0, min(float(frame_h), y2))
        curr = [x1, y1, x2, y2]

        prev = self._prev_boxes.get(track_id)
        if prev:
            iou = self._bbox_iou(curr, prev)
            if iou >= self.smooth_min_iou:
                alpha = self.smooth_alpha
                smoothed = [
                    alpha * curr[i] + (1 - alpha) * prev[i]
                    for i in range(4)
                ]
                curr = smoothed

        # Lưu lại cho frame sau
        self._prev_boxes[track_id] = curr

        return curr

    @staticmethod
    def _bbox_iou(a: List[float], b: List[float]) -> float:
        ax1, ay1, ax2, ay2 = a
        bx1, by1, bx2, by2 = b

        xx1 = max(ax1, bx1)
        yy1 = max(ay1, by1)
        xx2 = min(ax2, bx2)
        yy2 = min(ay2, by2)

        w = max(0.0, xx2 - xx1)
        h = max(0.0, yy2 - yy1)
        inter = w * h
        if inter <= 0:
            return 0.0

        area_a = max(0.0, ax2 - ax1) * max(0.0, ay2 - ay1)
        area_b = max(0.0, bx2 - bx1) * max(0.0, by2 - by1)
        union = area_a + area_b - inter
        return inter / union if union > 0 else 0.0
