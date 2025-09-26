FROM python:3.12-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

COPY ai/emit /app/ai/emit
COPY scripts/demo_send_to_pulsar.py /app/scripts/demo_send_to_pulsar.py
COPY detections_output.ndjson /app/detections_output.ndjson

RUN pip install --no-cache-dir pulsar-client==3.5.0

ENTRYPOINT ["python", "scripts/demo_send_to_pulsar.py"]
