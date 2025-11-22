2025-11-22: Fix bronze_ingest.sql NoSuchNamespaceException; bỏ CREATE DATABASE, tạo namespace `lakehouse.rva` qua Trino trước; sửa warehouse path từ `s3://warehouse/iceberg` thành `s3://warehouse`; Bronze ingest job chạy thành công (completed).
2025-11-21: Fix Pulsar advertised address to host.docker.internal for cross-host/container clients; aligned Python replay default URL - ingestion unblocks (completed).
2025-11-21: Set Pulsar webServicePort to 8082 and map 8082:8082 (advertisedAddress=host.docker.internal); updated client.conf/README/compose; broker recreated and healthy.
2025-11-21: Switch to dual-listener (internal:pulsar-broker:6650, external:127.0.0.1:6650), update Flink/Python configs and compose docs for stable host + container connectivity.
2025-11-21: Add guide section for running replay script via docker-run (no Windows hosts edit); updated HANDOFF with command.
