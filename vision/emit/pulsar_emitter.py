# vision/emit/pulsar_emitter.py
import json
import pulsar
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

class PulsarEmitter:
    def __init__(self, service_url: str, topic: str):
        print(f"[PulsarEmitter] Connecting to {service_url}...")
        self.client = pulsar.Client(service_url)
        self.producer = self.client.create_producer(topic)
        print(f"[PulsarEmitter] Connected to topic '{topic}'")

    def close(self):
        try:
            self.producer.close()
            self.client.close()
            print("[PulsarEmitter] Closed connection.")
        except Exception as e:
            print(f"[PulsarEmitter] Error closing: {e}")

    @staticmethod
    def _now_iso() -> str:
        return datetime.now(timezone.utc).isoformat()

    def emit_frame(
        self,
        *,
        pipeline_run_id: str,
        source: Dict[str, Any],
        frame_index: int,
        capture_ts_iso: Optional[str],
        image_size: Dict[str, int],
        detections: List[Dict[str, Any]],
        runtime: Optional[Dict[str, Any]] = None,
        source_uri: Optional[str] = None
    ):
        # 1. Tạo bản ghi giống hệt JsonEmitter
        record = {
            "schema_version": "1.0",
            "pipeline_run_id": pipeline_run_id,
            "source": source,
            "frame_index": frame_index,
            "capture_ts": capture_ts_iso or self._now_iso(),
            "image_size": image_size,
            "detections": detections,
        }
        if runtime:     record["runtime"] = runtime
        if source_uri:  record["source_uri"] = source_uri
        
        # 2. Bắn lên Pulsar thay vì ghi file
        try:
            json_str = json.dumps(record, ensure_ascii=False)
            self.producer.send(json_str.encode('utf-8'))
            # Log nhẹ để biết đang chạy
            if frame_index % 30 == 0:
                print(f"[Pulsar] Sent frame {frame_index}")
        except Exception as e:
            print(f"[PulsarEmitter] Failed to send frame {frame_index}: {e}")