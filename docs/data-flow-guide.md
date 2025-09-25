# Huong Dan Luong Du Lieu - Pulsar -> Flink -> MinIO

> Muc dich: huong dan tung buoc chay luong du lieu tu Pulsar toi Flink va day ket qua vao MinIO su dung cac lenh Git Bash co san trong repo.

## 0. Yeu cau truoc khi bat dau
- Docker 24 tro len va Docker Compose V2 (da bao gom trong Docker Desktop).
- Git Bash (Windows) hoac bat ky shell tuong thich POSIX.
- File `.env` da duoc tao tu `infrastructure/minio/.env.example` va cap nhat thong tin truy cap MinIO.
- Thu muc `flink-jobs/lib/` chua job jar sau khi build (se duoc mount vao Flink JobManager/TaskManager).

## 1. Khoi dong stack co so

### 1.1 Sao chep bien moi truong (lan dau tien)
```bash
echo "Sao chep mau .env (chi thuc hien neu chua co)"
cp infrastructure/minio/.env.example .env
```

### 1.2 Khoi dong cac dich vu
```bash
docker compose pull
TIMEOUT=70s docker compose up -d
TIMEOUT=30s docker compose ps
```

**Ky vong**: `pulsar-broker` va `minio` o trang thai `Up (healthy)`, `pulsar-init` se ket thuc sau khi tao topic, Flink JobManager/TaskManager o trang thai `Up`.

### 1.3 Kiem tra suc khoe nhanh
```bash
docker compose logs --tail=20 pulsar-init
docker compose logs --tail=20 pulsar-broker
docker compose logs --tail=20 flink-jobmanager
```
Neu `pulsar-init` khong chay tu dong, su dung `docker compose run --rm pulsar-init` de kich hoat script tao topic va schema.

## 2. Thiet lap Pulsar va nap du lieu mau

### 2.1 Xac nhan tenant/namespace/topic
```bash
docker exec pulsar-broker /pulsar/bin/pulsar-admin tenants list
docker exec pulsar-broker /pulsar/bin/pulsar-admin namespaces list retail
docker exec pulsar-broker /pulsar/bin/pulsar-admin topics list retail/metadata
```
Chu y topic mac dinh: `persistent://retail/metadata/events` (4 partition, JSON schema).

### 2.2 Nap file NDJSON lam stream mau
```bash
echo "Copy file NDJSON vao container"
docker cp detections_output.ndjson pulsar-broker:/tmp/detections_output.ndjson

echo "Day tung dong vao topic (1 dong = 1 message)"
docker exec pulsar-broker bash -lc '
  set -euo pipefail
  while IFS= read -r line; do
    if [[ -n "${line}" ]]; then
      /pulsar/bin/pulsar-client produce \
        -m "${line}" \
        persistent://retail/metadata/events \
        >/dev/null
    fi
  done < /tmp/detections_output.ndjson
  echo "Pushed $(wc -l < /tmp/detections_output.ndjson) messages"
'
```

### 2.3 Giam sat topic
a) Doc nhanh 5 message vua day:
```bash
docker exec pulsar-broker /pulsar/bin/pulsar-client consume \
  persistent://retail/metadata/events \
  -s verify-sub \
  -n 5
```

b) Theo doi thong ke topic:
```bash
docker exec pulsar-broker /pulsar/bin/pulsar-admin topics stats \
  persistent://retail/metadata/events | head -n 20
```
Lenh `head` duoc goi ben trong container Linux nen tuong thich.

## 3. Chuan bi Flink xu ly stream

### 3.1 Dat job jar vao mountpoint
- Build Flink job (VD: su dung Maven hoac Gradle) va tao file jar, vi du: `retail-stream-job-0.1.0.jar`.
- Copy jar vao `flink-jobs/lib/`. Docker Compose da mount thu muc nay vao `/opt/flink/usrlib`.

```bash
ls flink-jobs/lib
docker exec flink-jobmanager ls /opt/flink/usrlib
```
Dam bao jar hien dien o ca hai lenh tren.

### 3.2 Cau hinh bien moi truong ho tro (chon lua)
```bash
export PULSAR_SERVICE_URL="pulsar://pulsar-broker:6650"
export PULSAR_ADMIN_URL="http://pulsar-broker:8080"
export MINIO_ENDPOINT="http://minio:9000"
export MINIO_ACCESS_KEY="$(grep MINIO_ROOT_USER .env | cut -d'=' -f2)"
export MINIO_SECRET_KEY="$(grep MINIO_ROOT_PASSWORD .env | cut -d'=' -f2)"
```

### 3.3 Submit job Flink (thay doi ten jar/option theo project)
```bash
JOB_JAR="retail-stream-job-0.1.0.jar"

docker exec flink-jobmanager flink run \
  /opt/flink/usrlib/${JOB_JAR} \
  --source-topic persistent://retail/metadata/events \
  --pulsar-service-url ${PULSAR_SERVICE_URL:-pulsar://pulsar-broker:6650} \
  --pulsar-admin-url ${PULSAR_ADMIN_URL:-http://pulsar-broker:8080} \
  --minio-endpoint ${MINIO_ENDPOINT:-http://minio:9000} \
  --minio-access-key ${MINIO_ACCESS_KEY:-minioadmin} \
  --minio-secret-key ${MINIO_SECRET_KEY:-minioadmin123} \
  --sink-table detections_iceberg
```
Cap nhat ten doi so phu hop voi job thuc te. Neu job ho tro Table/SQL client, co the su dung `docker exec -it flink-jobmanager ./bin/sql-client.sh` thay cho `flink run`.

### 3.4 Theo doi job
```bash
docker exec flink-jobmanager flink list
```
Mo trinh duyet toi `http://localhost:8081` de xem trang thai job, watermarks, metrics.

## 4. Khoi tao MinIO va xac thuc output

### 4.1 Thiet lap alias mc
```bash
docker exec minio mc alias set local \
  http://localhost:9000 \
  "${MINIO_ROOT_USER:-minioadmin}" \
  "${MINIO_ROOT_PASSWORD:-minioadmin123}"
```

### 4.2 Chay script khoi tao bucket
```bash
docker cp infrastructure/minio/scripts/init.sh minio:/tmp/minio-init.sh
TIMEOUT=70s docker exec minio bash /tmp/minio-init.sh
```
Script tao cac bucket `lakehouse/`, `raw-data/`, `processed/`, `models/` neu chua ton tai.

### 4.3 Kiem tra ket qua ghi tu Flink
```bash
docker exec minio mc ls local/
docker exec minio mc ls local/lakehouse/
```
Neu job Flink ghi theo duong `s3a://lakehouse/<subpath>/`, ban se thay parquet/metadata xuat hien tai day.

### 4.4 Truy cap giao dien MinIO
- URL: `http://localhost:9001`
- Username: gia tri `MINIO_ROOT_USER`
- Password: gia tri `MINIO_ROOT_PASSWORD`

## 5. Giam sat, don dep, ghi chu

### 5.1 Giam sat log
```bash
TIMEOUT=75s docker compose logs -f pulsar-broker
TIMEOUT=75s docker compose logs -f flink-jobmanager
TIMEOUT=75s docker compose logs -f minio
```
Nhan `Ctrl+C` de thoat khoi streaming log truoc khi timeout tu dong.

### 5.2 Don dep stack (sau khi hoan thanh)
```bash
docker compose down
# Xoa volume neu muon lam moi hoan toan
# docker compose down -v
```

### 5.3 Meo khac phuc su co nhanh
- Neu Flink khong thay jar: kiem tra quyen truy cap folder `flink-jobs/lib/`.
- Neu Pulsar thong bao schema conflict: xoa schema cu bang `pulsar-admin schemas delete persistent://retail/metadata/events` roi chay lai `pulsar-init`.
- Neu MinIO tu choi truy cap: dam bao mat khau trong `.env` dai tu 8 ky tu tro len.

---

> Ghi chu: Tai lieu nay tap trung vao luong Pulsar -> Flink -> MinIO. Luong AI Ingest/Detect/Track se duoc dieu khien tu cac module trong thu muc `ai/` va co the chay doc lap de sinh NDJSON truoc khi day vao Pulsar.
