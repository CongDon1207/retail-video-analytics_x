#!/usr/bin/env bash
set -euo pipefail

TENANT="retail"
NAMESPACE="${TENANT}/metadata"
TOPIC="persistent://${NAMESPACE}/events"
SCHEMA_PATH="/pulsar/schema/metadata-json-schema.json"

echo "[init] Waiting for Pulsar admin service..."
until /pulsar/bin/pulsar-admin brokers healthcheck >/dev/null 2>&1; do
  sleep 2
done

echo "[init] Creating tenant '${TENANT}' (ignored if exists)"
/pulsar/bin/pulsar-admin tenants create "${TENANT}" \
  --allowed-clusters standalone >/dev/null 2>&1 || true

echo "[init] Creating namespace '${NAMESPACE}'"
/pulsar/bin/pulsar-admin namespaces create "${NAMESPACE}" >/dev/null 2>&1 || true

echo "[init] Setting namespace policies"
/pulsar/bin/pulsar-admin namespaces set-retention "${NAMESPACE}" \
  --size -1 --time -1 >/dev/null

echo "[init] Cleaning old topic/schema if present"
/pulsar/bin/pulsar-admin topics delete-partitioned-topic "${TOPIC}" --force >/dev/null 2>&1 || true
/pulsar/bin/pulsar-admin schemas delete "${TOPIC}" >/dev/null 2>&1 || true

echo "[init] Creating partitioned topic '${TOPIC}'"
/pulsar/bin/pulsar-admin topics create-partitioned-topic "${TOPIC}" \
  --partitions 4 >/dev/null 2>&1 || true

if [ -f "${SCHEMA_PATH}" ]; then
  echo "[init] Uploading JSON schema"
  /pulsar/bin/pulsar-admin schemas upload "${TOPIC}" \
    --filename "${SCHEMA_PATH}" >/dev/null
else
  echo "[init] Schema file not found: ${SCHEMA_PATH} (skip)"
fi

echo "[init] Done"
