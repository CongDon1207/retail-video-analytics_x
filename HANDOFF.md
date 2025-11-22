Current status
- Pulsar broker đang healthy (dual-listener):
  - `listeners=internal:pulsar://0.0.0.0:6650,external:pulsar://0.0.0.0:6650`
  - `advertisedListeners=internal:pulsar://pulsar-broker:6650,external:pulsar://127.0.0.1:6650`
  - `webServicePort=8082`
- docker-compose map: `6650:6650`, `8082:8082`.
- Bronze ingest job đang chạy (Job ID: 3c1504a41ec1b6a35334d0eaeca29824).
- Namespace `lakehouse.rva` đã được tạo trong Iceberg REST catalog.

Next steps
- Xác nhận backlog giảm và file `.parquet` xuất hiện tại `warehouse/rva/bronze_raw/data/` trên MinIO.
- Kiểm tra dữ liệu trong bảng Bronze qua Trino: `SELECT * FROM lakehouse.rva.bronze_raw LIMIT 10`.
- Tiếp tục setup Silver layer theo hướng dẫn trong README.md.
- Replay từ host: `PULSAR_SERVICE_URL=pulsar://127.0.0.1:6650`, `PULSAR_LISTENER_NAME=external`.
- Replay không cần sửa hosts (docker Python cùng network):  
  `docker run --rm --network retail-net -v "$PWD":/app -w /app python:3.10-slim /bin/bash -lc "pip install -r setup.txt && PULSAR_SERVICE_URL=pulsar://pulsar-broker:6650 PULSAR_LISTENER_NAME=internal python scripts/replay_jsonl_to_pulsar.py"`.

Paths / Artifacts
- Pulsar config: `infrastructure/pulsar/conf/standalone.conf`
- Flink ingest SQL: `flink-jobs/sql/bronze_ingest.sql`
- Replay script: `scripts/replay_jsonl_to_pulsar.py`
- Compose: `docker-compose.yml` (Pulsar ports)

Latest checks
- pulsar-broker healthy; `pulsar-admin brokers healthcheck` OK; host nc tới `host.docker.internal:6650` vẫn fail (vì external advertise là 127.0.0.1).

Environment
- Docker Compose stack; Python venv optional (`venv/`), Pulsar 6650 exposed to host.
