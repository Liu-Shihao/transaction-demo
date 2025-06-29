#!/bin/bash

# V2 Create Transaction QPS/TPS Test Script
# This script runs a dedicated test for V2 Create Transaction API to measure maximum write QPS/TPS

set -e

echo "=========================================="
echo "V2 Create Transaction QPS/TPS Test"
echo "=========================================="

# Configuration
JMETER_HOME="${JMETER_HOME:-/opt/apache-jmeter-5.6.3}"

# Smart path detection
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/v2-create-test.jmx" ]; then
    # Running from jmeter directory
    TEST_PLAN="$SCRIPT_DIR/v2-create-test.jmx"
    RESULTS_DIR="$SCRIPT_DIR/jmeter-results"
else
    # Running from project root
    TEST_PLAN="$SCRIPT_DIR/v2-create-test.jmx"
    RESULTS_DIR="$SCRIPT_DIR/jmeter-results"
fi

RESULTS_FILE="$RESULTS_DIR/v2-create-test.jtl"
REPORT_DIR="$RESULTS_DIR/v2-create-test-report"

# JMeter JVM options to suppress warnings
JMETER_JVM_OPTS="-Djava.awt.headless=true -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true"

# Create results directory
mkdir -p "$RESULTS_DIR"

# Clean up previous results
echo "Cleaning up previous test results..."
rm -f "$RESULTS_FILE"
rm -rf "$REPORT_DIR"

echo "Test Configuration:"
echo "- API: V2 Create Transaction (Virtual Thread)"
echo "- Threads: 3000"
echo "- Loops: 50"
echo "- Total Requests: 150,000"
echo "- Ramp-up: 60 seconds (1 minute)"
echo "- Expected Duration: ~5-8 minutes"
echo "- Target: Maximum Write QPS/TPS"
echo ""

# Check if JMeter is available
if [ ! -f "$JMETER_HOME/bin/jmeter" ]; then
    echo "Error: JMeter not found at $JMETER_HOME/bin/jmeter"
    echo "Please set JMETER_HOME environment variable or install JMeter"
    exit 1
fi

# Check if application is running
echo "Checking application status..."
if ! curl -s http://localhost:8080/api/v1/transactions/health > /dev/null; then
    echo "Error: Application is not running on localhost:8080"
    echo "Please start the application first:"
    echo "  ./script/start.sh"
    exit 1
fi

echo "Application is running. Starting V2 Create Transaction test..."
echo ""

# Run JMeter test
echo "Running JMeter test..."
JAVA_OPTS="$JMETER_JVM_OPTS" "$JMETER_HOME/bin/jmeter" \
    -n \
    -t "$TEST_PLAN" \
    -l "$RESULTS_FILE" \
    -e \
    -o "$REPORT_DIR"

echo ""
echo "Test completed. Analyzing results..."

# Results analysis
if [ -f "$RESULTS_FILE" ]; then
    echo ""
    echo "=========================================="
    echo "V2 Create Transaction Performance Analysis"
    echo "=========================================="
    
    # Count total requests (CSV format)
    TOTAL_REQUESTS=$(wc -l < "$RESULTS_FILE" 2>/dev/null || echo "0")
    # Subtract 1 for header line
    TOTAL_REQUESTS=$((TOTAL_REQUESTS - 1))
    
    # Count successful requests (true in success column)
    SUCCESS_REQUESTS=$(grep -c ',true,' "$RESULTS_FILE" 2>/dev/null || echo "0")
    
    # Count failed requests (false in success column)
    FAILED_REQUESTS=$(grep -c ',false,' "$RESULTS_FILE" 2>/dev/null || echo "0")
    
    # Count specific error types
    RATE_LIMIT_ERRORS=$(grep -c ',429,' "$RESULTS_FILE" 2>/dev/null || echo "0")
    CIRCUIT_BREAKER_ERRORS=$(grep -c ',503,' "$RESULTS_FILE" 2>/dev/null || echo "0")
    TIMEOUT_ERRORS=$(grep -c ',Non HTTP response code: java.net.SocketTimeoutException,' "$RESULTS_FILE" 2>/dev/null || echo "0")
    
    echo "Total Requests: $TOTAL_REQUESTS"
    echo "Successful Requests: $SUCCESS_REQUESTS"
    echo "Failed Requests: $FAILED_REQUESTS"
    echo "Rate Limit Errors (429): $RATE_LIMIT_ERRORS"
    echo "Circuit Breaker Errors (503): $CIRCUIT_BREAKER_ERRORS"
    echo "Timeout Errors: $TIMEOUT_ERRORS"
    
    if [ "$TOTAL_REQUESTS" -gt 0 ]; then
        SUCCESS_RATE=$(echo "scale=2; $SUCCESS_REQUESTS * 100 / $TOTAL_REQUESTS" | bc 2>/dev/null || echo "0")
        echo "Success Rate: ${SUCCESS_RATE}%"
        
        # Calculate QPS and TPS from actual test data
        FIRST_TS=$(awk -F',' 'NR==2{print $1}' "$RESULTS_FILE")
        LAST_TS=$(awk -F',' 'END{print $1}' "$RESULTS_FILE")
        DURATION_MS=$((LAST_TS - FIRST_TS))
        DURATION_SEC=$(echo "scale=3; $DURATION_MS/1000" | bc)
        QPS=$(echo "scale=2; $TOTAL_REQUESTS / $DURATION_SEC" | bc)
        TPS=$QPS  # For create operations, TPS = QPS
        
        # Calculate response time metrics
        AVG_RESP_TIME=$(awk -F',' 'NR>1{sum+=$2}END{if(NR>1) printf "%.2f", sum/(NR-1); else print "0"}' "$RESULTS_FILE")
        MIN_RESP_TIME=$(awk -F',' 'NR>1{print $2}' "$RESULTS_FILE" | sort -n | head -1)
        MAX_RESP_TIME=$(awk -F',' 'NR>1{print $2}' "$RESULTS_FILE" | sort -n | tail -1)
        P95_RESP_TIME=$(awk -F',' 'NR>1{print $2}' "$RESULTS_FILE" | sort -n | awk '{all[NR] = $0} END{print all[int(NR*0.95)]}')
        
        echo ""
        echo "Performance Metrics:"
        echo "- Duration: ${DURATION_SEC}s"
        echo "- QPS: $QPS requests/second"
        echo "- TPS: $TPS transactions/second"
        echo "- Average Response Time: ${AVG_RESP_TIME}ms"
        echo "- Min Response Time: ${MIN_RESP_TIME}ms"
        echo "- Max Response Time: ${MAX_RESP_TIME}ms"
        echo "- 95th Percentile Response Time: ${P95_RESP_TIME}ms"
        echo "- Success Rate: ${SUCCESS_RATE}%"
        echo "- Error Rate: $(echo "scale=2; $FAILED_REQUESTS * 100 / $TOTAL_REQUESTS" | bc 2>/dev/null || echo "0")%"
        
        # Performance assessment
        echo ""
        echo "Performance Assessment:"
        if (( $(echo "$SUCCESS_RATE >= 95" | bc -l) )); then
            echo "✓ Excellent: Success rate >= 95%"
        elif (( $(echo "$SUCCESS_RATE >= 90" | bc -l) )); then
            echo "✓ Good: Success rate >= 90%"
        elif (( $(echo "$SUCCESS_RATE >= 80" | bc -l) )); then
            echo "⚠ Fair: Success rate >= 80%"
        else
            echo "✗ Poor: Success rate < 80%"
        fi
        
        if (( $(echo "$TPS >= 1000" | bc -l) )); then
            echo "✓ High TPS: >= 1000 transactions/second"
        elif (( $(echo "$TPS >= 500" | bc -l) )); then
            echo "✓ Medium TPS: >= 500 transactions/second"
        else
            echo "⚠ Low TPS: < 500 transactions/second"
        fi
        
        # Generate test report
        REPORT_FILE="$RESULTS_DIR/v2-create-test-report.txt"
        cat > "$REPORT_FILE" << EOF
==========================================
V2 Create Transaction Test Report
==========================================
Test Date: $(date)
API Version: V2 (Virtual Thread)
Test Type: Create Transaction (Write QPS/TPS)

Test Configuration:
- Threads: 3000
- Loops: 50
- Total Requests: 150,000
- Ramp-up: 60 seconds
- Duration: ${DURATION_SEC}s

Results Summary:
- Total Requests: $TOTAL_REQUESTS
- Successful Requests: $SUCCESS_REQUESTS
- Failed Requests: $FAILED_REQUESTS
- Success Rate: ${SUCCESS_RATE}%
- Error Rate: $(echo "scale=2; $FAILED_REQUESTS * 100 / $TOTAL_REQUESTS" | bc 2>/dev/null || echo "0")%

Error Breakdown:
- Rate Limit Errors (429): $RATE_LIMIT_ERRORS
- Circuit Breaker Errors (503): $CIRCUIT_BREAKER_ERRORS
- Timeout Errors: $TIMEOUT_ERRORS

Performance Metrics:
- QPS: $QPS requests/second
- TPS: $TPS transactions/second
- Average Response Time: ${AVG_RESP_TIME}ms
- Min Response Time: ${MIN_RESP_TIME}ms
- Max Response Time: ${MAX_RESP_TIME}ms
- 95th Percentile Response Time: ${P95_RESP_TIME}ms

Performance Assessment:
$(if (( $(echo "$SUCCESS_RATE >= 95" | bc -l) )); then echo "- Success Rate: Excellent (>= 95%)"; elif (( $(echo "$SUCCESS_RATE >= 90" | bc -l) )); then echo "- Success Rate: Good (>= 90%)"; elif (( $(echo "$SUCCESS_RATE >= 80" | bc -l) )); then echo "- Success Rate: Fair (>= 80%)"; else echo "- Success Rate: Poor (< 80%)"; fi)
$(if (( $(echo "$TPS >= 1000" | bc -l) )); then echo "- TPS: High (>= 1000 tps)"; elif (( $(echo "$TPS >= 500" | bc -l) )); then echo "- TPS: Medium (>= 500 tps)"; else echo "- TPS: Low (< 500 tps)"; fi)

Files:
- HTML Report: $REPORT_DIR/index.html
- Raw Results: $RESULTS_FILE
- Text Report: $REPORT_FILE
EOF
        
        echo ""
        echo "Test Report generated: $REPORT_FILE"
    fi
    
    echo ""
    echo "HTML Report generated at: $REPORT_DIR/index.html"
    echo "Raw results file: $RESULTS_FILE"
    
else
    echo "Error: Results file not found: $RESULTS_FILE"
    exit 1
fi

echo ""
echo "V2 Create Transaction test completed successfully!" 