Current status
- Docker Compose stack đang chạy (11 containers healthy).
- **Automated Job Submission**: Service `flink-job-submitter` tự động submit 8 jobs khi khởi động stack.
- **8 Flink jobs đang RUNNING** (sau khi restart stack và tách Gold jobs):
  - `bronze_raw` - Pulsar → Bronze Iceberg
  - `silver_detections` - Bronze → Silver (parse, dedupe, filter)
  - `gold_minute_by_cam` - Silver → Gold aggregation per minute
  - `gold_hour_by_cam` - Silver → Gold aggregation per hour
  - `gold_people_per_minute` - Silver → Gold people count
  - `gold_zone_heatmap` - Silver → Gold zone heatmap
  - `gold_zone_dwell` - Silver → Gold zone dwell time
  - `gold_track_summary` - Silver → Gold track statistics

**Root cause đã fix (2025-12-01)**:
- Gold job trước đó có 6 sinks ghi đồng thời vào 6 tables gây **SQLite BUSY lock** trong REST Catalog.
- **Giải pháp**: Tách thành 6 job riêng biệt với shared `GoldJobBase.java`, mỗi job chỉ ghi 1 table.

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
- Chạy vision/main.py + replay để có real-time data cho các Gold tables.
- Monitor Gold jobs để đảm bảo không bị FINISHED sớm (expect RUNNING liên tục).
- Thêm alerting rules nếu cần monitor traffic anomalies.

Paths / Artifacts
- **Gold job classes (NEW)**: `flink-jobs/java/src/main/java/org/rva/gold/Gold*Job.java` (6 jobs + base)
- **Backup file**: `flink-jobs/java/src/main/java/org/rva/gold/GoldStreamingJob.java.bak`
- Grafana dashboards: `infrastructure/grafana/provisioning/dashboards/*.json` (v5 redesigned)
- Pulsar config: `infrastructure/pulsar/conf/standalone.conf`
- Flink ingest SQL: `flink-jobs/sql/bronze_ingest.sql`
- Replay script: `scripts/replay_jsonl_to_pulsar.py`
- Compose: `docker-compose.yml` (Pulsar ports)
- Vision DeepSORT config: `vision/.env` (DS_* defaults tinh chỉnh)

Latest checks
- **Flink UI (localhost:8081)**: 8 jobs RUNNING, 8 slots available.
- Gold tables đang được populate từ các job riêng biệt.
- Không còn lỗi SQLite lock hoặc clock skew.

Environment
- Docker Compose stack; Python venv optional (`venv/`), Pulsar 6650 exposed to host.
