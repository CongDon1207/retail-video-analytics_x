#!/bin/bash

# MinIO initialization script
# Creates default buckets and policies for the retail video analytics project

set -euo pipefail

ALIAS_NAME="${MC_ALIAS_NAME:-local}"
ENDPOINT="${MINIO_SERVER_URL:-http://localhost:9000}"
ACCESS_KEY="${MINIO_ROOT_USER:?MINIO_ROOT_USER is required}"
SECRET_KEY="${MINIO_ROOT_PASSWORD:?MINIO_ROOT_PASSWORD is required}"

configure_alias() {
  until mc alias set "${ALIAS_NAME}" "${ENDPOINT}" "${ACCESS_KEY}" "${SECRET_KEY}" >/dev/null 2>&1; do
    echo "Waiting for MinIO API..."
    sleep 2
  done
}

wait_until_ready() {
  until mc ready "${ALIAS_NAME}" >/dev/null 2>&1; do
    echo "Waiting for MinIO server..."
    sleep 2
  done
}

echo "Configuring mc alias '${ALIAS_NAME}' ..."
configure_alias

echo "Waiting for MinIO to be ready..."
wait_until_ready

echo "MinIO is ready. Creating buckets..."

# Create buckets for different data types
mc mb "${ALIAS_NAME}/lakehouse" --ignore-existing
mc mb "${ALIAS_NAME}/raw-data" --ignore-existing
mc mb "${ALIAS_NAME}/processed" --ignore-existing
mc mb "${ALIAS_NAME}/models" --ignore-existing

echo "Setting bucket policies..."

# Set public read policy for models bucket (optional)
# mc anonymous set download "${ALIAS_NAME}/models"

# Set lifecycle policy for raw data (optional cleanup after 30 days)
# mc ilm add --expiry-days 30 "${ALIAS_NAME}/raw-data"

echo "MinIO buckets initialized successfully!"
echo "Available buckets:"
mc ls "${ALIAS_NAME}/"
