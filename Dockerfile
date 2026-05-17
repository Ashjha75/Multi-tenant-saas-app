# ───────────────────────────────────────────────────────────────
#  Multi-stage Dockerfile  |  Spring Boot 4.x / Java 21 / Alpine
# ───────────────────────────────────────────────────────────────

# ── Stage 1: Build ─────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Install dos2unix so the mvnw wrapper (written on Windows) works on Linux
RUN apk add --no-cache dos2unix

# Copy Maven wrapper + pom first (layer-cache dependencies)
COPY .mvn/   .mvn/
COPY mvnw    mvnw
COPY pom.xml pom.xml

# Fix Windows CRLF line endings and make executable
RUN dos2unix mvnw && chmod +x mvnw

# Download dependencies only (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B --no-transfer-progress

# Copy source code
COPY src/ src/

# Build fat JAR, skip tests for speed
RUN ./mvnw package -DskipTests -B --no-transfer-progress \
    && cp target/*.jar app.jar

# ── Stage 2: Runtime ───────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as non-root
RUN addgroup -S appgrp && adduser -S appuser -G appgrp

WORKDIR /app

# Copy only the fat JAR from builder
COPY --from=builder /build/app.jar app.jar

# Create certs directory (PEM keys will be mounted as a volume or secret)
RUN mkdir -p certs && chown -R appuser:appgrp /app

# Expose the default Spring Boot port
EXPOSE 8080

# Switch to non-root user
USER appuser

# JVM tuning for containers — UseContainerSupport reads cgroup limits automatically
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# All sensitive env vars (DB_PASSWORD, JWT_*, etc.) are injected at runtime
# via docker-compose env_file or your VPS secret manager — never baked into image
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
