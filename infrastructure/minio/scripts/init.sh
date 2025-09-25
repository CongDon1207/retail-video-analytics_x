#!/bin/bash

# MinIO initialization script
# Creates default buckets and policies for the retail video analytics project

set -e

echo "Waiting for MinIO to be ready..."
until mc ready local; do
  echo "Waiting for MinIO server..."
  sleep 2
done

echo "MinIO is ready. Creating buckets..."

# Create buckets for different data types
mc mb local/lakehouse --ignore-existing
mc mb local/raw-data --ignore-existing  
mc mb local/processed --ignore-existing
mc mb local/models --ignore-existing

echo "Setting bucket policies..."

# Set public read policy for models bucket (optional)
# mc anonymous set download local/models

# Set lifecycle policy for raw data (optional cleanup after 30 days)
# mc ilm add --expiry-days 30 local/raw-data

echo "MinIO buckets initialized successfully!"
echo "Available buckets:"
mc ls local/
