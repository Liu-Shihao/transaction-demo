#!/bin/bash

# Startup script - supports JVM configuration for different environments

# Default environment
ENV=${1:-dev}

# Load JVM configuration from script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/jvm-options.conf"

# Select JVM parameters based on environment
case $ENV in
    "local")
        JAVA_OPTS="$LOCAL_JVM_OPTS"
        PROFILE="local"
        echo "Starting local environment..."
        ;;
    "dev")
        JAVA_OPTS="$DEV_JVM_OPTS"
        PROFILE="dev"
        echo "Starting development environment..."
        ;;
    "uat")
        JAVA_OPTS="$UAT_JVM_OPTS"
        PROFILE="uat"
        echo "Starting UAT environment..."
        ;;
    "cob")
        JAVA_OPTS="$COB_JVM_OPTS"
        PROFILE="cob"
        echo "Starting COB environment..."
        ;;
    "prod")
        JAVA_OPTS="$PROD_JVM_OPTS"
        PROFILE="prod"
        echo "Starting production environment..."
        ;;
    "docker")
        JAVA_OPTS="$DOCKER_JVM_OPTS"
        PROFILE="docker"
        echo "Starting Docker environment..."
        ;;
    *)
        echo "Unknown environment: $ENV"
        echo "Supported environments: local, dev, uat, cob, prod, docker"
        exit 1
        ;;
esac

# Create logs directory
mkdir -p logs

# Start application
echo "Spring profile: $PROFILE"
echo "JVM parameters: $JAVA_OPTS"
echo "Starting application..."

java $JAVA_OPTS -jar target/transaction-demo-1.0.0.jar 