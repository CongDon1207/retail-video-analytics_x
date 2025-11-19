from .yolo_tracker_base import YoloTrackerBase

class YoloByteTracker(YoloTrackerBase):
    def __init__(self, model_path: str):
        super().__init__(model_path, "bytetrack.yaml")
