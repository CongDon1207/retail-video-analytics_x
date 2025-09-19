from __future__ import annotations

from typing import Optional, Tuple

import numpy as np

try:
    import gi

    gi.require_version("Gst", "1.0")
    from gi.repository import Gst

    Gst.init(None)
    GST_READY = True
    GST_ERROR: Optional[Exception] = None
except Exception as exc:  # pragma: no cover - phụ thuộc máy
    GST_READY = False
    GST_ERROR = exc
    Gst = None  # type: ignore


class GstSource:
    """Nguồn video dựa trên GStreamer (file hoặc RTSP)."""

    def __init__(self, uri: str) -> None:
        if not GST_READY:
            raise RuntimeError(f"GStreamer chưa sẵn sàng: {GST_ERROR}")
        self._uri = uri
        self._pipeline: Optional[Gst.Pipeline] = None
        self._sink = None

    def _build_description(self) -> str:
        if self._uri.startswith(("rtsp://", "rtsps://")):
            return (
                f"rtspsrc location={self._uri} latency=200 ! "
                "rtph264depay ! h264parse ! avdec_h264 ! "
                "videoconvert ! video/x-raw,format=BGR ! "
                "appsink name=sink emit-signals=false sync=false max-buffers=1 drop=true"
            )
        return (
            f"filesrc location={self._uri} ! "
            "decodebin ! videoconvert ! video/x-raw,format=BGR ! "
            "appsink name=sink emit-signals=false sync=false max-buffers=1 drop=true"
        )

    def open(self) -> bool:
        description = self._build_description()
        try:
            self._pipeline = Gst.parse_launch(description)
            self._sink = self._pipeline.get_by_name("sink")
            if not self._sink:
                return False
            self._pipeline.set_state(Gst.State.PLAYING)
            bus = self._pipeline.get_bus()
            msg = bus.timed_pop_filtered(3 * Gst.SECOND, Gst.MessageType.ERROR)
            if msg is not None:
                self.release()
                return False
            return True
        except Exception:
            self.release()
            return False

    def read(self) -> Tuple[bool, Optional[np.ndarray]]:
        if not self._sink:
            return False, None
        try:
            sample = self._sink.emit("pull-sample")
            if not sample:
                return False, None
            buffer = sample.get_buffer()
            if not buffer:
                return False, None
            success, map_info = buffer.map(Gst.MapFlags.READ)
            if not success:
                return False, None
            try:
                caps = sample.get_caps()
                structure = caps.get_structure(0)
                width = int(structure.get_value("width"))
                height = int(structure.get_value("height"))
                data = np.frombuffer(map_info.data, dtype=np.uint8)
                frame = data.reshape((height, width, 3)).copy()
                return True, frame
            finally:
                buffer.unmap(map_info)
        except Exception:
            return False, None

    def release(self) -> None:
        if self._pipeline:
            self._pipeline.set_state(Gst.State.NULL)
        self._pipeline = None
        self._sink = None

    def __del__(self) -> None:  # pragma: no cover - đảm bảo giải phóng
        self.release()
