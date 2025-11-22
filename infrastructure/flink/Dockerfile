# syntax=docker/dockerfile:1
FROM flink:1.18.1-scala_2.12-java17
ARG FLINK_VERSION=1.18.1

ARG PULSAR_CONNECTOR_VERSION=4.1.0-1.18
ARG PULSAR_CLIENT_VERSION=3.0.0
ARG PULSAR_CLIENT_API_VERSION=3.0.0
ARG PULSAR_CLIENT_ADMIN_API_VERSION=3.0.0
ARG PULSAR_COMMON_VERSION=3.0.0
ARG AVRO_VERSION=1.11.3
ARG JACKSON_VERSION=2.15.2
ARG ICEBERG_VERSION=1.5.0
ARG ICEBERG_AWS_BUNDLE_VERSION=1.5.0
ARG HADOOP_SHADED_VERSION=2.8.3-10.0
ARG OPENTELEMETRY_VERSION=1.32.0

ENV FLINK_LIB_DIR=/opt/flink/lib \
    FLINK_USR_LIB_DIR=/opt/flink/usrlib

RUN set -eux; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/flink/flink-connector-pulsar/${PULSAR_CONNECTOR_VERSION}/flink-connector-pulsar-${PULSAR_CONNECTOR_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/flink-connector-pulsar-${PULSAR_CONNECTOR_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/pulsar/pulsar-client/${PULSAR_CLIENT_VERSION}/pulsar-client-${PULSAR_CLIENT_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/pulsar-client-${PULSAR_CLIENT_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/pulsar/pulsar-client-original/${PULSAR_CLIENT_VERSION}/pulsar-client-original-${PULSAR_CLIENT_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/pulsar-client-original-${PULSAR_CLIENT_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/pulsar/pulsar-client-api/${PULSAR_CLIENT_API_VERSION}/pulsar-client-api-${PULSAR_CLIENT_API_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/pulsar-client-api-${PULSAR_CLIENT_API_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/pulsar/pulsar-client-admin-api/${PULSAR_CLIENT_ADMIN_API_VERSION}/pulsar-client-admin-api-${PULSAR_CLIENT_ADMIN_API_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/pulsar-client-admin-api-${PULSAR_CLIENT_ADMIN_API_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/pulsar/pulsar-common/${PULSAR_COMMON_VERSION}/pulsar-common-${PULSAR_COMMON_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/pulsar-common-${PULSAR_COMMON_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/flink/flink-avro/${FLINK_VERSION}/flink-avro-${FLINK_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/flink-avro-${FLINK_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/avro/avro/${AVRO_VERSION}/avro-${AVRO_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/avro-${AVRO_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/${JACKSON_VERSION}/jackson-core-${JACKSON_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/jackson-core-${JACKSON_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/${JACKSON_VERSION}/jackson-databind-${JACKSON_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/jackson-databind-${JACKSON_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/${JACKSON_VERSION}/jackson-annotations-${JACKSON_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/jackson-annotations-${JACKSON_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-flink-runtime-1.18/${ICEBERG_VERSION}/iceberg-flink-runtime-1.18-${ICEBERG_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/iceberg-flink-runtime-1.18-${ICEBERG_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-aws-bundle/${ICEBERG_AWS_BUNDLE_VERSION}/iceberg-aws-bundle-${ICEBERG_AWS_BUNDLE_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/iceberg-aws-bundle-${ICEBERG_AWS_BUNDLE_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/org/apache/flink/flink-shaded-hadoop-2-uber/${HADOOP_SHADED_VERSION}/flink-shaded-hadoop-2-uber-${HADOOP_SHADED_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/flink-shaded-hadoop-2-uber-${HADOOP_SHADED_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-api/${OPENTELEMETRY_VERSION}/opentelemetry-api-${OPENTELEMETRY_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/opentelemetry-api-${OPENTELEMETRY_VERSION}.jar"; \
    curl -fsSL "https://repo1.maven.org/maven2/io/opentelemetry/opentelemetry-context/${OPENTELEMETRY_VERSION}/opentelemetry-context-${OPENTELEMETRY_VERSION}.jar" \
      -o "$FLINK_LIB_DIR/opentelemetry-context-${OPENTELEMETRY_VERSION}.jar"; \
    mkdir -p /opt/flink/state/checkpoints /opt/flink/state/savepoints; \
    chown -R flink:flink /opt/flink/state

# -----------------------------------------------------------------------------
# [FIX] ENABLE S3 PLUGIN
# Copy plugin từ thư mục opt (có sẵn trong image) sang thư mục plugins để kích hoạt
# -----------------------------------------------------------------------------
RUN mkdir -p /opt/flink/plugins/s3-fs-hadoop && \
    cp /opt/flink/opt/flink-s3-fs-hadoop-*.jar /opt/flink/plugins/s3-fs-hadoop/

COPY flink-jobs/ "$FLINK_USR_LIB_DIR/"

EXPOSE 6123 8081