# Banking Transaction Management System

High-performance banking transaction management system based on Spring Boot 3.2.0 and Java 21, supporting virtual threads, caching, rate limiting, circuit breaking, and other high-concurrency features.

## 🚀 Technology Stack

- **Spring Boot 3.2.0** - Main framework
- **Java 21** - Virtual thread support
- **Spring Cache** - In-memory caching
- **Custom Rate Limiter** - Sliding window algorithm
- **Custom Circuit Breaker** - Three-state pattern
- **Docker & Kubernetes** - Containerized deployment
- **SpringDoc OpenAPI** - API documentation

## 🏗️ High Concurrency Design

### Virtual Threads
- Support for million-level concurrent connections
- Extremely low memory usage (approximately 1KB per virtual thread)
- Automatic scheduling, no manual thread pool management required

### Caching Strategy
- Single-level in-memory cache
- Cache penetration protection
- Atomic operations
- Intelligent cache invalidation

### Protection Mechanisms
- Rate Limiter: Sliding window algorithm
- Circuit Breaker: Three-state pattern (CLOSED/OPEN/HALF_OPEN)
- JVM Optimization: Heap memory optimization, G1 garbage collector

## 📊 API Endpoints

### V1 API (Traditional Threads)
- `POST /api/v1/transactions` - Create transaction
- `GET /api/v1/transactions/{id}` - Get transaction details
- `GET /api/v1/transactions` - Get transaction list
- `PUT /api/v1/transactions/{id}` - Update transaction
- `DELETE /api/v1/transactions/{id}` - Delete transaction
- `GET /api/v1/transactions/statistics` - Get statistics

### V2 API (Virtual Threads)
- `POST /api/v2/transactions` - Create transaction (virtual threads)
- `GET /api/v2/transactions/{id}` - Get transaction details (virtual threads)
- `GET /api/v2/transactions` - Get transaction list (virtual threads)
- `PUT /api/v2/transactions/{id}` - Update transaction (virtual threads)
- `DELETE /api/v2/transactions/{id}` - Delete transaction (virtual threads)
- `GET /api/v2/transactions/statistics` - Get statistics (virtual threads)

### Monitoring Endpoints
- `GET /api/v1/monitoring/rate-limiter` - Rate limiter status
- `GET /api/v1/monitoring/circuit-breaker` - Circuit breaker status
- `GET /actuator/health` - Application health status
- `GET /swagger-ui.html` - API documentation

## 🚀 Startup Instructions

### Requirements
- Java 21+
- Maven 3.8+

### Local Startup
```bash
# Compile project
mvn clean compile

# Run application
mvn spring-boot:run
```

### Environment-Specific Startup
```bash
# Development environment
./script/start.sh dev

# Test environment
./script/start.sh test

# Production environment
./script/start.sh prod
```

### Access Application
- Application URL: http://localhost:8080
- API Documentation: http://localhost:8080/swagger-ui.html
- Health Check: http://localhost:8080/actuator/health

## 🐳 Containerization

### Docker Deployment
```bash
# Build image
docker build -t transaction-demo .

# Run container
docker run -p 8080:8080 transaction-demo
```

### Kubernetes Deployment
```bash
# Deploy using Helm
helm install transaction-demo ./helm/transaction-demo
```

## 🛡️ Rate Limiting Configuration Strategy

### Layered Rate Limiting Architecture
```
Request Flow:
1. Global Rate Limit: 120,000/min per client (based on QPS 2500 × 0.8 safety factor)
2. API Rate Limit: 1,500-10,000/min per API (based on operation type)
3. Account Rate Limit: 100-500/min per account (prevent single account overload)
4. Circuit Breaker: 30% failure rate threshold (system protection)
```

### API-Level Rate Limiting Configuration

#### Write Operations
| API | Rate Limit Configuration | Description |
|-----|-------------------------|-------------|
| **Create Transaction** | 3000/min API + 100/min account | Core business, balance performance and security |
| **Update Transaction** | 3000/min API + 100/min account | Data modification operations, same limits as create |
| **Delete Transaction** | 1500/min API | Sensitive operations, stricter limits |

#### Read Operations
| API | Rate Limit Configuration | Description |
|-----|-------------------------|-------------|
| **Get Single Transaction** | 10000/min API | High-frequency queries, relaxed limits |
| **Get All Transactions** | 10000/min API | Paginated queries, relaxed limits |
| **Query by Account** | 10000/min API + 500/min account | Additional account-level protection |
| **Query by Type** | 10000/min API | Statistical analysis, relaxed limits |
| **Get Statistics** | 10000/min API | Monitoring queries, relaxed limits |


### Configuration Principles

1. **Performance-Based**: All limits based on actual test QPS 2500+ data
2. **Safety Factor**: Use 80% capacity as safety boundary
3. **Read-Write Separation**: Relaxed limits for read operations, strict limits for write operations
4. **Business Importance**: Reasonable limits for core business operations
5. **Monitoring Friendly**: No rate limiting on monitoring endpoints, ensuring system observability
6. **Layered Protection**: Global → API → Account multi-layer rate limiting strategy

### Rate Limiting Implementation

- **Algorithm**: Sliding window algorithm
- **Storage**: In-memory storage, high performance
- **Thread Safety**: Uses ReentrantLock for concurrent safety
- **Monitoring**: Provides real-time rate limiting status queries
- **Reset**: Supports manual reset of rate limiting counters

### Expected Effects

- **System Protection**: Prevent single client from overloading system
- **Fair Distribution**: Ensure reasonable resource allocation among multiple clients
- **Business Continuity**: Maintain basic services when rate limiting is triggered
- **Observability**: Real-time understanding of rate limiting status through monitoring endpoints

## 📈 Performance Test Report

### Test Environment Configuration
- tomcat thread max: 1500 
- jvm heap size: 4GB
- JMeter Thread Group: 3000 threads × 50 loops = 150,000 requests
### Performance Test Results Comparison

#### Create Transaction Performance Comparison (Write QPS/TPS)

| Metric | V1 API (Traditional Threads) | V2 API (Virtual Threads) | Performance Improvement |
|--------|------------------------------|--------------------------|------------------------|
| **Concurrency Config** | 3000 threads × 50 loops | 3000 threads × 50 loops | - |
| **Total Requests** | 150,000 | 150,000 | - |
| **Actual Requests** | 150,000 | 150,000 | - |
| **Successful Requests** | 149,894 | 149,939 | +45 |
| **Failed Requests** | 106 | 61 | -45 |
| **Success Rate** | 99.93% | 99.96% | +0.03% |
| **Error Rate** | 0.07% | 0.04% | **-42.9%** |
| **QPS/TPS** | 2,502.79 req/s | 2,502.71 req/s | -0.003% |
| **Average Response Time** | 0.35ms | 0.32ms | **-8.6%** |
| **Max Response Time** | 86ms | 84ms | -2.3% |
| **95% Response Time** | 1ms | 1ms | 0% |
| **Rate Limit Errors (429)** | 0 | 0 | - |
| **Circuit Breaker Errors (503)** | 0 | 0 | - |
| **Timeout Errors** | 106 | 61 | **-42.5%** |

#### Statistics Query Performance Comparison (Read QPS)

| Metric | V1 API (Traditional Threads) | V2 API (Virtual Threads) | Performance Improvement |
|--------|------------------------------|--------------------------|------------------------|
| **Concurrency Config** | 3000 threads × 50 loops | 3000 threads × 50 loops | - |
| **Total Requests** | 150,000 | 150,000 | - |
| **Actual Requests** | 150,000 | 150,000 | - |
| **Successful Requests** | 149,770 | 149,950 | +180 |
| **Failed Requests** | 230 | 50 | -180 |
| **Success Rate** | 99.85% | 99.97% | +0.12% |
| **Error Rate** | 0.15% | 0.03% | **-80.0%** |
| **QPS** | 2,503.09 req/s | 2,502.75 req/s | -0.01% |
| **Average Response Time** | 0.25ms | 0.25ms | 0% |
| **Max Response Time** | 87ms | 74ms | **-14.9%** |
| **95% Response Time** | 1ms | 1ms | 0% |
| **Rate Limit Errors (429)** | 0 | 0 | - |
| **Circuit Breaker Errors (503)** | 0 | 0 | - |
| **Timeout Errors** | 230 | 50 | **-78.3%** |

### Key Performance Findings

#### 🚀 Virtual Thread Advantages
- **Significantly Reduced Error Rates**: Create transaction error rate reduced by 42.9%, query error rate reduced by 80%
- **More Stable Response Times**: Reduced maximum response time, optimized average response time
- **Enhanced System Stability**: Dramatically reduced timeout errors, more stable system under high concurrency

#### 📊 Performance Performance
- **Stable QPS**: Both versions achieve high-performance levels of 2500+ req/s
- **Excellent Response Times**: Average response time < 1ms, 95% response time ≤ 1ms
- **No Protection Mechanism Triggers**: No rate limit errors, no circuit breaker errors, indicating reasonable system design

#### 🔧 Configuration Optimization Effects
- **Tomcat Thread Pool**: 1500 maximum threads support high concurrency
- **JVM Optimization**: G1GC + 4GB heap memory provide stable performance
- **Connection Pool Optimization**: 50000 maximum connections meet large-scale concurrency requirements

### Test Strategy Description

#### Test Tool Configuration
- **Test Tool**: Apache JMeter 5.6.3
- **Test Type**: Stress testing + Performance benchmarking
- **Monitoring Metrics**: QPS, response time, error rate, success rate

#### Concurrency Strategy
- **Progressive Loading**: Gradually start all threads over 120 seconds, avoiding instantaneous impact
- **Sustained Load**: Each thread executes 20-50 loops, simulating real business scenarios
- **Error Handling**: Continue execution when encountering errors, statistics overall success rate

#### Environment Isolation
- **Dedicated Test Environment**: Use stress profile configuration, disable non-essential features
- **Resource Isolation**: 4GB memory + 4-core CPU, avoid resource competition
- **Network Optimization**: Local testing, eliminate network latency impact

## 🧪 Stress Testing

### Running Stress Tests

1. **Start Application**
```bash
./script/start.sh stress
```

2. **Run JMeter Test Scripts**
```bash
# V1 API Create Transaction Test
cd jmeter && ./run-v1-create-test.sh

# V2 API Create Transaction Test  
cd jmeter && ./run-v2-create-test.sh

# V1 API Statistics Query Test
cd jmeter && ./run-v1-statistics-test.sh

# V2 API Statistics Query Test
cd jmeter && ./run-v2-statistics-test.sh
```

3. **View Test Reports**
```bash
# HTML Reports
open jmeter/jmeter-results/v1-create-test-report/index.html
open jmeter/jmeter-results/v2-create-test-report/index.html

# Raw Data
ls jmeter/jmeter-results/*.jtl
```

### Test Configuration
- **Concurrent Threads**: 3000
- **Loop Count**: 50
- **Total Requests**: 150,000
- **Warm-up Time**: 60 seconds
- **Test Duration**: ~60 seconds

## 🎯 Project Features

- 🚀 Virtual Threads: Support for million-level concurrency
- ⚡ High Performance: QPS reaches 2500+
- 🛡️ Protection Mechanisms: Rate limiting and circuit breaking
- 📊 Comprehensive Monitoring: Real-time performance metrics
- 🐳 Containerization: Docker and K8s support
- 💾 Advanced Caching: Penetration protection, atomic operations
- 🔒 Concurrent Safety: Distributed locks and version control

