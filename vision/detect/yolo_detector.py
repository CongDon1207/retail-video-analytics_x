# ai/detect/yolo_detector.py
from ultralytics import YOLO
import torch
import numpy as np
from pathlib import Path
from utils.path_utils import resolve_model_path

class YoloDetector:
    """
    Bộ phát hiện đối tượng dùng YOLO11 (Ultralytics).
    - Tự động chọn GPU nếu có.
    - Hỗ trợ inference từng frame (numpy array BGR).
    - Trả về danh sách dict: [{bbox, conf, cls, label}, ...]
    """

    def __init__(self, model_name="yolo11s.pt", conf_thres=0.2):
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.model_path = resolve_model_path(model_name)
        print(f"[YOLO] Loading model on device: {self.device}")
        print(f"[YOLO] Model path: {self.model_path}")

        self.model = YOLO(self.model_path)
        self.model.to(self.device)
        self.conf_thres = conf_thres
        self.names = self.model.names

    def predict(self, frame: np.ndarray, class_filter=None):
        """
        Nhận 1 frame (numpy BGR), trả danh sách detection.
        Mỗi detection gồm: {'bbox': [x1, y1, x2, y2], 'conf': float, 'cls': int, 'label': str}
        """
        # YOLO nhận RGB → convert từ BGR sang RGB
        results = self.model.predict(
            source=frame[..., ::-1],
            verbose=False,
            conf=self.conf_thres,
            device=self.device
        )

        detections = []
        if not results or len(results) == 0:
            return detections
        


        r = results[0]
        boxes = r.boxes.xyxy.cpu().numpy()
        confs = r.boxes.conf.cpu().numpy()
        classes = r.boxes.cls.cpu().numpy().astype(int)

        for i in range(len(boxes)):
            cls_id = int(classes[i])
            if class_filter and cls_id not in class_filter:
                continue


            x1, y1, x2, y2 = boxes[i].tolist()
            detections.append({
                "bbox": [x1, y1, x2, y2],
                "conf": float(confs[i]),
                "cls": int(classes[i]),
                "label": r.names[int(classes[i])]
            })
        return detections
