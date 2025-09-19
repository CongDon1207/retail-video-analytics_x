from __future__ import annotations

from dataclasses import dataclass
from typing import List, Sequence, Tuple

import numpy as np

try:
    from deep_sort_realtime.deepsort_tracker import DeepSort
except ImportError as exc:  # pragma: no cover - phá»¥ thuá»™c mÃ´i trÆ°á»ng
    raise ImportError("Cáº§n cÃ i: pip install deep-sort-realtime") from exc


DetectionTuple = Tuple[int, int, int, int, float, int, str]


@dataclass
class TrackResult:
    x1: int
    y1: int
    x2: int
    y2: int
    track_id: int
    confidence: float
    class_name: str


class DeepSortTracker:
    """Wrapper gá»n cho DeepSORT."""

    def __init__(
        self,
        *,
        max_age: int = 30,
        n_init: int = 3,
        max_iou_distance: float = 0.7,
        nms_max_overlap: float = 1.0,
        embedder: str = "mobilenet",
        embedder_gpu: bool = False,
        half: bool = False,
    ) -> None:
        self._tracker = DeepSort(
            max_age=max_age,
            n_init=n_init,
            max_iou_distance=max_iou_distance,
            nms_max_overlap=nms_max_overlap,
            embedder=embedder,
            embedder_gpu=embedder_gpu,
            half=half,
        )

    def update(self, detections: Sequence[DetectionTuple], frame: np.ndarray) -> List[TrackResult]:
        formatted = []
        for x1, y1, x2, y2, conf, _, cls_name in detections:
            width = max(0, x2 - x1)
            height = max(0, y2 - y1)
            if width <= 0 or height <= 0:
                continue
            formatted.append(([x1, y1, width, height], conf, cls_name))
        tracks = self._tracker.update_tracks(formatted, frame=frame)

        results: List[TrackResult] = []
        for track in tracks:
            if not track.is_confirmed():
                continue
            left, top, width, height = track.to_ltwh()
            x1 = int(round(left))
            y1 = int(round(top))
            x2 = int(round(left + width))
            y2 = int(round(top + height))
            results.append(
                TrackResult(
                    x1=x1,
                    y1=y1,
                    x2=x2,
                    y2=y2,
                    track_id=int(track.track_id),
                    confidence=float(track.get_det_conf() or 0.0),
                    class_name=str(track.get_det_class() or ""),
                )
            )
        return results


