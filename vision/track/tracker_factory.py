from .yolo_tracker_botsort import YoloBoTSORTTracker
from .yolo_tracker_bytetrack import YoloByteTracker
from .deepsort_tracker import DeepSORTTracker


def create_tracker(tracker_name: str, model_path: str, **kwargs):
    t = tracker_name.lower().strip()
    if t == "botsort":
        return YoloBoTSORTTracker(model_path)
    if t == "bytetrack":
        return YoloByteTracker(model_path)
    if t == "deepsort":
        return DeepSORTTracker(model_path, conf_thres=kwargs.get("conf_thres", 0.25))
    raise ValueError(f"Unknown tracker: {tracker_name}. Hỗ trợ: botsort | bytetrack | deepsort")
