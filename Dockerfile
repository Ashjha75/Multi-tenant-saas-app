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

# openssl: used by the entrypoint to decode injected keys
# wget comes from busybox and backs the HEALTHCHECK below
RUN apk add --no-cache openssl

# Security: run as non-root
RUN addgroup -S appgrp && adduser -S appuser -G appgrp

WORKDIR /app

# Copy only the fat JAR from builder
COPY --from=builder /build/app.jar app.jar

# Entrypoint resolves the JWT keys, then execs the JVM as PID 1
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN dos2unix /usr/local/bin/docker-entrypoint.sh 2>/dev/null || \
    sed -i 's/\r$//' /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Create certs directory (PEM keys are mounted as a volume, or injected as
# JWT_PRIVATE_KEY / JWT_PUBLIC_KEY env vars — never baked into the image)
RUN mkdir -p certs && chown -R appuser:appgrp /app

# Expose the default Spring Boot port
EXPOSE 8080

# Switch to non-root user
USER appuser

# JVM tuning for containers — UseContainerSupport reads cgroup limits automatically
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# The app owns its lifecycle inside the container; nothing here starts a database
ENV SPRING_DOCKER_COMPOSE_ENABLED=false

# Reports health to `docker ps`, plain `docker run`, and any orchestrator that
# reads container health. ECS/Fargate can use this or its own target-group check.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -q -O - "http://127.0.0.1:${SERVER_PORT:-8080}/api/v1/health" >/dev/null 2>&1 || exit 1

# All sensitive env vars (DB_PASSWORD, JWT_*, etc.) are injected at runtime
# via docker-compose env_file or your VPS / Fargate secret manager — never baked into the image
ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
