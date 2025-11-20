-- Define Iceberg Catalog (using MinIO as S3)
CREATE CATALOG iceberg WITH (
  'type' = 'iceberg',
  'catalog-type' = 'rest',
  'uri' = 'http://iceberg-rest:8181',
  'warehouse' = 's3a://warehouse',
  's3.endpoint' = 'http://minio:9000',
  's3.path-style-access' = 'true',
  's3.access-key-id' = 'minioadmin',
  's3.secret-access-key' = 'minioadmin123'
);

-- Create Database in Iceberg if not exists
CREATE DATABASE IF NOT EXISTS iceberg.retail;

-- Use Iceberg catalog by default
USE CATALOG iceberg;
USE retail;
