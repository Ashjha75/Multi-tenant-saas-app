# ───────────────────────────────────────────────────────
#  Multi-stage Dockerfile for Spring Boot 4.x / Java 21
#  Works with any Spring Boot app – just copy into root.
# ───────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper + pom first (layer-cache dependencies)
COPY .mvn/   .mvn/
COPY mvnw    mvnw
COPY pom.xml pom.xml

# Make the wrapper executable (git on Windows may strip the bit)
RUN chmod +x mvnw

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ src/

# Build the fat JAR, skip tests for faster image builds
RUN ./mvnw package -DskipTests -B \
    && cp target/*.jar app.jar

# ── Stage 2: Runtime ─────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as non-root
RUN addgroup -S appgrp && adduser -S appuser -G appgrp

WORKDIR /app

# Copy only the fat JAR from the builder stage
COPY --from=builder /build/app.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Switch to non-root user
USER appuser

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Entrypoint — all env vars (DB_HOST, DB_PASSWORD, etc.)
# are injected at runtime via docker-compose or AWS task-definition.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
