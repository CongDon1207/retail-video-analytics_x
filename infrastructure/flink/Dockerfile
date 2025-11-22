# syntax=docker/dockerfile:1

# -----------------------------------------------------------------------------
# Stage 1: Build Flink Jobs với Maven
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-11 AS builder

WORKDIR /build

# Copy pom.xml và source code (context là project root)
COPY flink-jobs/java/pom.xml ./
COPY flink-jobs/java/src ./src

# Build JAR (skip tests để nhanh hơn)
RUN mvn clean package -DskipTests

# -----------------------------------------------------------------------------
# Stage 2: Flink Runtime Image với Dependencies đầy đủ
# -----------------------------------------------------------------------------
FROM hungfnguyen28/retail-flink:1.18-v1

ENV FLINK_USR_LIB_DIR=/opt/flink/usrlib

# -----------------------------------------------------------------------------
# Copy Pre-built Jobs từ Builder Stage
# -----------------------------------------------------------------------------
RUN mkdir -p "$FLINK_USR_LIB_DIR"

# Copy compiled JAR từ build stage
COPY --from=builder /build/target/silver-job-0.1.0.jar "$FLINK_USR_LIB_DIR/bronze-job.jar"

# Copy SQL scripts (context là project root, nên path đúng)
COPY flink-jobs/sql/ "$FLINK_USR_LIB_DIR/sql/"

EXPOSE 6123 8081