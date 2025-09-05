# TradeMaster Subscription Service Dockerfile
# Multi-stage build for Java 24 + Virtual Threads

# Stage 1: Build the application
FROM openjdk:24-jdk-slim AS builder

# Set working directory
WORKDIR /app

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Copy gradle wrapper and build files
COPY gradle/ gradle/
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (for better Docker layer caching)
COPY gradle.properties* ./
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src/ src/

# Build the application with Java 24 preview features
RUN ./gradlew build -x test --no-daemon

# Stage 2: Runtime image
FROM openjdk:24-jdk-slim

# Set working directory
WORKDIR /app

# Create app user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Install required runtime packages
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Copy configuration files
COPY --from=builder /app/src/main/resources/application.yml application.yml
COPY --from=builder /app/src/main/resources/logback-spring.xml logback-spring.xml

# Create logs directory
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Environment variables for container
ENV JAVA_OPTS="-XX:+UseZGC \
               -XX:+UnlockExperimentalVMOptions \
               -XX:+EnableDynamicAgentLoading \
               --enable-preview \
               -Xmx2g \
               -Xms1g \
               -XX:MaxMetaspaceSize=512m \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/app/logs/ \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=docker \
               -Dspring.threads.virtual.enabled=true"

# JVM optimizations for containers
ENV JVM_OPTS="-XX:+UseContainerSupport \
              -XX:MaxRAMPercentage=75.0 \
              -XX:InitialRAMPercentage=50.0 \
              -XX:+ExitOnOutOfMemoryError \
              -XX:+PrintGCDetails \
              -XX:+PrintGCTimeStamps \
              -Xloggc:/app/logs/gc.log"

# Spring Boot specific configurations
ENV SPRING_OPTS="-Dserver.port=8086 \
                 -Dmanagement.endpoints.web.exposure.include=health,info,metrics,prometheus \
                 -Dmanagement.endpoint.health.show-details=always \
                 -Dlogging.file.name=/app/logs/subscription-service.log"

# Expose the application port
EXPOSE 8086

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8086/actuator/health || exit 1

# Entry point with proper JVM options for Virtual Threads
ENTRYPOINT exec java \
    $JAVA_OPTS \
    $JVM_OPTS \
    $SPRING_OPTS \
    -jar app.jar

# Labels for metadata
LABEL maintainer="TradeMaster Development Team <dev@trademaster.com>"
LABEL version="1.0.0"
LABEL description="TradeMaster Subscription Service with Java 24 Virtual Threads"
LABEL java.version="24"
LABEL spring.boot.version="3.5.3"