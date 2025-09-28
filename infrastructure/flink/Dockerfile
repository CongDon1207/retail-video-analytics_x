# syntax=docker/dockerfile:1
FROM flink:1.18.1-scala_2.12-java11
ARG FLINK_VERSION=1.18.1

ARG PULSAR_CONNECTOR_VERSION=4.1.0-1.18
ARG PULSAR_CLIENT_VERSION=4.1.0
ARG PULSAR_CLIENT_API_VERSION=4.1.0
ARG PULSAR_CLIENT_ADMIN_API_VERSION=4.1.0
ARG PULSAR_COMMON_VERSION=4.1.0
ARG ICEBERG_VERSION=1.5.0
ARG ICEBERG_AWS_BUNDLE_VERSION=1.5.0
ARG HADOOP_SHADED_VERSION=2.8.3-10.0

ENV FLINK_LIB_DIR=/opt/flink/lib \
    FLINK_USR_LIB_DIR=/opt/flink/usrlib

RUN set -eux; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/flink/flink-connector-pulsar/${PULSAR_CONNECTOR_VERSION}/flink-connector-pulsar-${PULSAR_CONNECTOR_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/flink-connector-pulsar-${PULSAR_CONNECTOR_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/pulsar/pulsar-client-all/${PULSAR_CLIENT_VERSION}/pulsar-client-all-${PULSAR_CLIENT_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/pulsar-client-all-${PULSAR_CLIENT_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/pulsar/pulsar-client-api/${PULSAR_CLIENT_API_VERSION}/pulsar-client-api-${PULSAR_CLIENT_API_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/pulsar-client-api-${PULSAR_CLIENT_API_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/pulsar/pulsar-client-admin-api/${PULSAR_CLIENT_ADMIN_API_VERSION}/pulsar-client-admin-api-${PULSAR_CLIENT_ADMIN_API_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/pulsar-client-admin-api-${PULSAR_CLIENT_ADMIN_API_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/pulsar/pulsar-common/${PULSAR_COMMON_VERSION}/pulsar-common-${PULSAR_COMMON_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/pulsar-common-${PULSAR_COMMON_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/flink/flink-avro/${FLINK_VERSION}/flink-avro-${FLINK_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/flink-avro-${FLINK_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-flink-runtime-1.18/${ICEBERG_VERSION}/iceberg-flink-runtime-1.18-${ICEBERG_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/iceberg-flink-runtime-1.18-${ICEBERG_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-aws-bundle/${ICEBERG_AWS_BUNDLE_VERSION}/iceberg-aws-bundle-${ICEBERG_AWS_BUNDLE_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/iceberg-aws-bundle-${ICEBERG_AWS_BUNDLE_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/flink/flink-shaded-hadoop-2-uber/${HADOOP_SHADED_VERSION}/flink-shaded-hadoop-2-uber-${HADOOP_SHADED_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/flink-shaded-hadoop-2-uber-${HADOOP_SHADED_VERSION}.jar"
COPY flink-jobs/ "$FLINK_USR_LIB_DIR/"

EXPOSE 6123 8081

