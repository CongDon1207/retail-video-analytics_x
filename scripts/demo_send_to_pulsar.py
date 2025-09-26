from __future__ import annotations

import argparse
import json
from contextlib import nullcontext
from pathlib import Path
from typing import ContextManager, Iterable, Optional

from ai.emit.pulsar_producer import PulsarProducer


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Gửi file NDJSON metadata lên Apache Pulsar."
    )
    parser.add_argument(
        "--ndjson",
        type=Path,
        default=Path("detections_output.ndjson"),
        help="Đường dẫn tới file NDJSON đầu vào",
    )
    parser.add_argument(
        "--service-url",
        default="pulsar://localhost:6650",
        help="Pulsar service URL",
    )
    parser.add_argument(
        "--topic",
        default="persistent://retail/metadata/events",
        help="Topic đích",
    )
    parser.add_argument(
        "--producer-name",
        default="demo-retail-producer",
        help="Tên producer (tuỳ chọn)",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="Giới hạn số message gửi (mặc định gửi toàn bộ)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Chỉ in thông tin, không gửi lên Pulsar",
    )
    return parser.parse_args()


def iter_frames(ndjson_path: Path) -> Iterable[dict]:
    with ndjson_path.open("r", encoding="utf-8") as handle:
        for raw_line in handle:
            line = raw_line.strip()
            if not line:
                continue
            yield json.loads(line)


def main() -> None:
    args = parse_args()

    if not args.ndjson.exists():
        raise FileNotFoundError(f"Không tìm thấy file: {args.ndjson}")

    sent = 0
    if args.dry_run:
        print("[dry-run] Không gửi message, chỉ hiển thị payload")

    producer_cm: ContextManager[Optional[PulsarProducer]]
    if args.dry_run:
        producer_cm = nullcontext(None)
    else:
        producer_cm = PulsarProducer(
            service_url=args.service_url,
            topic=args.topic,
            producer_name=args.producer_name,
        )

    with producer_cm as producer:
        for frame in iter_frames(args.ndjson):
            image_meta = frame.get("image_size", {})
            image_size = (
                int(image_meta.get("width", 0)),
                int(image_meta.get("height", 0)),
            )
            detections = frame.get("detections", [])

            if args.dry_run:
                print(
                    f"Frame #{frame.get('frame_index')}: {len(detections)} detections, "
                    f"camera={frame.get('source', {}).get('camera_id', 'unknown')}"
                )
            else:
                if producer is None:
                    raise RuntimeError("Producer chưa khởi tạo")
                producer.send_detection(
                    schema_version=frame["schema_version"],
                    pipeline_run_id=frame["pipeline_run_id"],
                    source=frame.get("source", {}),
                    frame_index=int(frame["frame_index"]),
                    capture_ts=frame.get("capture_ts", ""),
                    image_size=image_size,
                    detections=detections,
                )

            sent += 1
            if args.limit and sent >= args.limit:
                break

    print(f"Đã xử lý {sent} message")


if __name__ == "__main__":
    main()
