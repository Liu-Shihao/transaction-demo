# Multi-stage build for Transaction Demo Application
# Stage 1: Build stage - compiles the Java application
FROM openjdk:21-jdk-slim AS builder

# Set working directory
WORKDIR /app

# Install Maven for building the application
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy pom.xml first to leverage Docker layer caching for dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src src

# Build the application (skip tests for faster build)
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage - creates the final lightweight image
FROM openjdk:21-jre-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy JAR file from build stage
COPY --from=builder /app/target/transaction-demo-1.0.0.jar app.jar

# Change file ownership to non-root user
RUN chown appuser:appuser app.jar

# Switch to non-root user for security
USER appuser

# Expose application port
EXPOSE 8080

# Set default JVM options for JDK 21 (stable, mainstream parameters only)
ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Health check configuration
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/transactions/health || exit 1

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 