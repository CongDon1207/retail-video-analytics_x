#!/usr/bin/env bash
set -euo pipefail

TENANT="retail"
NAMESPACE="${TENANT}/metadata"
TOPIC="persistent://${NAMESPACE}/events"
SCHEMA_PATH="/pulsar/schema/metadata-json-schema.json"

echo "[init] Waiting for Pulsar..."
until /pulsar/bin/pulsar-admin brokers healthcheck >/dev/null 2>&1; do
  sleep 2
done

/pulsar/bin/pulsar-admin tenants create "${TENANT}" \
  --allowed-clusters standalone >/dev/null 2>&1 || true

/pulsar/bin/pulsar-admin namespaces create "${NAMESPACE}" \
  >/dev/null 2>&1 || true

/pulsar/bin/pulsar-admin namespaces set-retention "${NAMESPACE}" \
  --size -1 --time -1 >/dev/null 2>&1

/pulsar/bin/pulsar-admin topics delete-partitioned-topic "${TOPIC}" \
  --force >/dev/null 2>&1 || true

/pulsar/bin/pulsar-admin topics delete "${TOPIC}" \
  --force >/dev/null 2>&1 || true

/pulsar/bin/pulsar-admin schemas delete "${TOPIC}" \
  >/dev/null 2>&1 || true

# Create NON-partitioned topic for Flink compatibility
/pulsar/bin/pulsar-admin topics create "${TOPIC}" \
  >/dev/null 2>&1 || true

if [ -f "${SCHEMA_PATH}" ]; then
  echo "[init] Uploading schema from ${SCHEMA_PATH}"
  /pulsar/bin/pulsar-admin schemas upload "${TOPIC}" \
    --filename "${SCHEMA_PATH}" >/dev/null 2>&1
fi

echo "[init] Done"
