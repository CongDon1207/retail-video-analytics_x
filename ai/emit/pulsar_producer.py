from __future__ import annotations

import json
from typing import Any, Dict, Iterable, Optional, Tuple

import pulsar
from pulsar import Client, Producer
from pulsar.schema import JsonSchema, Long, Record, String

from .json_emitter import DetectionDict, JsonEmitter


class RetailDetection(Record):
    """JSON schema khớp với `metadata-json-schema.json`."""

    schema_version = String(required=True)
    pipeline_run_id = String(required=True)
    frame_index = Long(required=True)
    payload = String(required=True)


class PulsarProducer:
    """Producer đơn giản để đẩy metadata detection vào Apache Pulsar."""

    def __init__(
        self,
        *,
        service_url: str = "pulsar://localhost:6650",
        topic: str = "persistent://retail/metadata/events",
        producer_name: Optional[str] = None,
        client_kwargs: Optional[Dict[str, Any]] = None,
        producer_kwargs: Optional[Dict[str, Any]] = None,
    ) -> None:
        client_kwargs = dict(client_kwargs or {})
        producer_kwargs = dict(producer_kwargs or {})
        # Tắt batching để giữ thứ tự khi demo/testing.
        producer_kwargs.setdefault("batching_enabled", False)

        self._client: Client = Client(service_url, **client_kwargs)
        schema = JsonSchema(RetailDetection)
        self._producer: Producer = self._client.create_producer(
            topic,
            schema=schema,
            producer_name=producer_name,
            **producer_kwargs,
        )
        self._topic = topic

    def send_detection(
        self,
        *,
        schema_version: str,
        pipeline_run_id: str,
        source: Dict[str, str],
        frame_index: int,
        capture_ts: str,
        image_size: Tuple[int, int],
        detections: Iterable[DetectionDict],
        message_properties: Optional[Dict[str, str]] = None,
    ) -> None:
        """Build frame metadata và gửi vào topic."""

        frame_record = JsonEmitter.build_frame_record(
            schema_version=schema_version,
            pipeline_run_id=pipeline_run_id,
            source=source,
            frame_index=frame_index,
            capture_ts=capture_ts,
            image_size=image_size,
            detections=detections,
        )

        envelope = RetailDetection(
            schema_version=schema_version,
            pipeline_run_id=pipeline_run_id,
            frame_index=int(frame_index),
            payload=json.dumps(frame_record, ensure_ascii=False),
        )

        properties: Dict[str, str] = dict(message_properties or {})
        for key in ("store_id", "camera_id"):
            if key in source and key not in properties:
                properties[key] = str(source[key])

        partition_key = source.get("camera_id") or source.get("sensor_id")

        try:
            if partition_key:
                self._producer.send(envelope, partition_key=str(partition_key), properties=properties)
            else:
                self._producer.send(envelope, properties=properties)
        except pulsar.PulsarException as exc:
            raise RuntimeError(
                f"Gửi message tới topic '{self._topic}' thất bại: {exc}"
            ) from exc

    def close(self) -> None:
        """Đóng producer & client."""
        try:
            self._producer.flush()
        finally:
            self._producer.close()
            self._client.close()

    def __enter__(self) -> "PulsarProducer":
        return self

    def __exit__(self, exc_type, exc, traceback) -> None:  # type: ignore[override]
        self.close()
