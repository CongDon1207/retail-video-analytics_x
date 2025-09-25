# Hướng dẫn Luồng Dữ liệu - Retail Video Analytics

> **Mục đích**: Hướng dẫn chi tiết cách chạy và kiểm tra luồng dữ liệu từ AI detection → Pulsar → Flink → MinIO

## 🚀 Khởi động Stack

### Bước 1: Khởi động Infrastructure
```bash
# Đảm bảo có file .env với credentials hợp lệ
cp infrastructure/minio/.env.example .env

# Khởi động tất cả services
docker compose up -d

# Kiểm tra trạng thái
docker compose ps
```

**Expected output:**
- Pulsar: `Up (healthy)` - ports 6650, 8082
- Flink JobManager: `Up (healthy)` - port 8081  
- Flink TaskManager: `Up` - internal
- MinIO: `Up (healthy)` - ports 9000, 9001

### Bước 2: Khởi tạo MinIO Buckets
```bash
# Chạy script khởi tạo buckets (optional - tự động tạo khi cần)
docker exec minio bash /data/scripts/init.sh 2>/dev/null || echo "Script not found, buckets will be auto-created"
```

## 📊 Luồng Dữ liệu Chính

### Phase 1: AI Detection → Pulsar
```
Video Input (OpenCV/GStreamer) 
    ↓ [ai/ingest]
YOLOv8 Object Detection 
    ↓ [ai/detect]  
DeepSort Tracking
    ↓ [ai/track]
JSON Metadata Emit
    ↓ [ai/emit]
Pulsar Topic: detection-results
```

**Cách test Phase 1:**
```bash
# Run AI detection pipeline (giả định có video test)
cd ai/ingest
python -m . --source ../../data/synth.avi --output-topic detection-results

# Kiểm tra Pulsar topic đã nhận dữ liệu
docker exec pulsar-broker /pulsar/bin/pulsar-admin topics stats persistent://public/default/detection-results
```

### Phase 2: Pulsar → Flink Processing  
```
Pulsar Topic: detection-results
    ↓ [Flink Source Connector]
Stream Processing (CEP, Windowing)
    ↓ [Flink Transformation]
Aggregated Results
    ↓ [Flink Sink Connector]
Pulsar Topic: processed-analytics
```

**Cách test Phase 2:**
```bash
# Submit Flink job (cần có job JAR)
docker exec flink-jobmanager /opt/flink/bin/flink run \
  /opt/flink/usrlib/video-analytics-job.jar \
  --input-topic detection-results \
  --output-topic processed-analytics

# Kiểm tra job đang chạy
curl http://localhost:8081/jobs
```

### Phase 3: Flink → MinIO Lakehouse
```
Pulsar Topic: processed-analytics  
    ↓ [Flink S3 Sink]
MinIO Bucket: lakehouse/
    ↓ [Iceberg Table Format]
Parquet Files + Metadata
    ↓ [Trino Query Engine - future]
BI Dashboard
```

**Cách test Phase 3:**
```bash
# Kiểm tra MinIO buckets đã được tạo
curl -u minioadmin:minioadmin123 http://localhost:9000/

# Access MinIO Console để xem dữ liệu
echo "Open: http://localhost:9001 (user: minioadmin, pass: minioadmin123)"

# List objects trong bucket lakehouse
docker exec minio mc ls local/lakehouse/
```

## 🔍 Monitoring & Debugging

### Kiểm tra Logs
```bash
# Xem logs tất cả services
docker compose logs -f

# Logs từng service riêng
docker compose logs pulsar-broker -f
docker compose logs flink-jobmanager -f  
docker compose logs minio -f
```

### Health Checks
```bash
# Pulsar broker health
curl http://localhost:8082/admin/v2/brokers/health

# Flink cluster overview  
curl http://localhost:8081/overview

# MinIO health
curl http://localhost:9000/minio/health/live
```

### Topic Management
```bash
# Tạo topic Pulsar thủ công
docker exec pulsar-broker /pulsar/bin/pulsar-admin topics create persistent://public/default/detection-results

# List topics
docker exec pulsar-broker /pulsar/bin/pulsar-admin topics list public/default

# Consume messages từ topic
docker exec pulsar-broker /pulsar/bin/pulsar-client consume detection-results -s test-sub -n 10
```

## 🎯 Performance Tuning

### Pulsar Configuration
- Memory: 512MB-1GB (adjust `PULSAR_MEM` in docker-compose.yml)
- Retention: configured in `infrastructure/pulsar/conf/standalone.conf`

### Flink Configuration  
- Slots: 4 per TaskManager (adjust `taskmanager.numberOfTaskSlots`)
- Parallelism: 2 default (adjust `parallelism.default`)

### MinIO Configuration
- Console: http://localhost:9001
- Storage: persistent volume `minio_data`

## 🔧 Troubleshooting

### Common Issues

**1. Port conflicts**
```bash
# Check ports in use
netstat -tulpn | grep :8080
# Solution: Change ports in docker-compose.yml
```

**2. MinIO credentials invalid**  
```bash
# Error: "MINIO_ROOT_PASSWORD length at least 8 characters"
# Solution: Update .env with longer password
```

**3. Flink job not starting**
```bash  
# Check Flink cluster connection
curl http://localhost:8081/taskmanagers
# Solution: Verify jobmanager/taskmanager communication
```

**4. Pulsar topics not creating**
```bash
# Manual topic creation
docker exec pulsar-broker /pulsar/bin/pulsar-admin topics create persistent://public/default/your-topic
```

## 📈 Next Steps

1. **AI Integration**: Connect `ai/ingest` with Pulsar producer
2. **Flink Jobs**: Develop stream processing jobs in `flink-jobs/`  
3. **Iceberg Setup**: Configure Iceberg catalog with MinIO backend
4. **Trino Integration**: Add Trino for lakehouse queries
5. **Monitoring**: Add Prometheus + Grafana stack

## 🔗 Useful URLs

- **Flink Dashboard**: http://localhost:8081
- **Pulsar Admin**: http://localhost:8082  
- **MinIO Console**: http://localhost:9001
- **Pulsar Manager** (if added): http://localhost:9527

---

> **Lưu ý**: Các AI components trong `ai/` chưa được tích hợp hoàn toàn với infrastructure. Cần thêm Pulsar client vào `ai/emit/json_emitter.py` để hoàn thành luồng dữ liệu end-to-end.