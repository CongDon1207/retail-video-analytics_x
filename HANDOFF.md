Current status
- Pulsar broker đang healthy (dual-listener):
  - `listeners=internal:pulsar://0.0.0.0:6650,external:pulsar://0.0.0.0:6650`
  - `advertisedListeners=internal:pulsar://pulsar-broker:6650,external:pulsar://127.0.0.1:6650`
  - `webServicePort=8082`
- docker-compose map: `6650:6650`, `8082:8082`.
- Bronze/Silver/Gold jobs đã chạy và commit dữ liệu thành công vào Iceberg.
- **Grafana dashboards redesigned (v5)**: Cải thiện layout, thêm descriptions, icons, pie charts, color thresholds.
- Namespace `lakehouse.rva` đã được tạo trong Iceberg REST catalog.

Grafana Dashboards (2025-11-25)
- **Nguyên nhân gốc lỗi 0 data**: Trino datasource plugin **KHÔNG interpolate** Grafana template variables → bỏ template variables.
- **Redesign v5**: 
  - **People Overview**: 4 stat cards (Detections/Tracks/Cameras/Avg) + timeseries + pie chart + table
  - **Zone Analytics**: 4 stat cards + 2 bar charts (top dwell/visits) + 2 tables (heatmap/details)
  - **Track Behavior**: 4 stat cards + timeseries + bar charts + pie chart camera distribution
- **Data đang hiển thị**:
  - People: 13.5K detections, 69 unique, 1 camera
  - Zone: 132 visits, ~8.5h avg dwell, 71 zones
  - Track: 35 tracks, ~8.4h avg duration, 64.55% confidence, 13,460 frames

Next steps
- Tạo dữ liệu mới (chạy vision/main.py + replay) để có real-time data cho Grafana.
- Thêm alerting rules nếu cần monitor traffic anomalies.
- Cân nhắc thêm map/heatmap visualization cho zone data.

Paths / Artifacts
- Grafana dashboards: `infrastructure/grafana/provisioning/dashboards/*.json` (v5 redesigned)
- Pulsar config: `infrastructure/pulsar/conf/standalone.conf`
- Flink ingest SQL: `flink-jobs/sql/bronze_ingest.sql`
- Replay script: `scripts/replay_jsonl_to_pulsar.py`
- Compose: `docker-compose.yml` (Pulsar ports)
- Vision DeepSORT config: `vision/.env` (DS_* defaults tinh chỉnh)

Latest checks
- Grafana: 3 dashboards v5 hoạt động với data visualization đẹp và đủ metrics.
- Gold tables: `gold_people_per_minute` (2 rows), `gold_zone_dwell` (71 rows), `gold_track_summary` (35 rows).
- pulsar-broker healthy; `pulsar-admin brokers healthcheck` OK.

Environment
- Docker Compose stack; Python venv optional (`venv/`), Pulsar 6650 exposed to host.
