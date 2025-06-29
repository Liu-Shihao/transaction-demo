#!/bin/bash

# Stress Test Application Startup Script
# Uses optimized configuration for high concurrency testing

set -e

echo "=========================================="
echo "Starting Application for Stress Testing"
echo "=========================================="

# Load JVM options
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/jvm-options.conf"

# Configuration
APP_JAR="target/transaction-demo-1.0.0.jar"
PROFILE="stress"
JVM_OPTS="$STRESS_JVM_OPTS"

# Check if JAR exists
if [ ! -f "$APP_JAR" ]; then
    echo "Error: Application JAR not found at $APP_JAR"
    echo "Please build the application first: mvn clean package"
    exit 1
fi

# Check if JVM options are loaded
if [ -z "$JVM_OPTS" ]; then
    echo "Error: JVM options not loaded"
    exit 1
fi

echo "Configuration:"
echo "- Profile: $PROFILE"
echo "- JAR: $APP_JAR"
echo "- JVM Options: $JVM_OPTS"
echo ""

# Set environment variables
export SPRING_PROFILES_ACTIVE="$PROFILE"
export JAVA_OPTS="$JVM_OPTS"

echo "Starting application with stress test configuration..."
echo "JVM Options: $JAVA_OPTS"
echo ""

# Show startup information before starting the application
echo "=========================================="
echo "Application Startup Information"
echo "=========================================="
echo "✅ Application will start with stress profile"
echo "✅ Health check: http://localhost:8080/actuator/health"
echo "✅ Metrics: http://localhost:8080/actuator/metrics"
echo "✅ API endpoints: http://localhost:8080/api/v1/transactions"
echo ""
echo "⏳ Starting application... (Press Ctrl+C to stop)"
echo ""

# Start the application (this will block until application stops)
java $JAVA_OPTS -jar "$APP_JAR" \
    --spring.profiles.active="$PROFILE" 