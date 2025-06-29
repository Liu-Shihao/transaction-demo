# Banking Transaction Management System

基于Spring Boot 3.2.0和Java 21的高性能银行交易管理系统，支持虚拟线程、缓存、限流、熔断等高并发特性。

## 🚀 技术栈

- **Spring Boot 3.2.0** - 主框架
- **Java 21** - 虚拟线程支持
- **Spring Cache** - 内存缓存
- **自定义限流器** - 滑动窗口算法
- **自定义熔断器** - 三状态模式
- **Docker & Kubernetes** - 容器化部署
- **SpringDoc OpenAPI** - API文档

## 🏗️ 高并发设计

### 虚拟线程
- 支持百万级并发连接
- 内存占用极低（每个虚拟线程约1KB）
- 自动调度，无需手动管理线程池

### 缓存策略
- 单级内存缓存
- 缓存穿透保护
- 原子性操作
- 智能缓存清除

### 保护机制
- 限流器：滑动窗口算法
- 熔断器：三状态模式（CLOSED/OPEN/HALF_OPEN）
- JVM优化：堆内存优化、G1垃圾回收器

## 📊 API接口

### V1 API (传统线程)
- `POST /api/v1/transactions` - 创建交易
- `GET /api/v1/transactions/{id}` - 获取交易详情
- `GET /api/v1/transactions` - 获取交易列表
- `PUT /api/v1/transactions/{id}` - 更新交易
- `DELETE /api/v1/transactions/{id}` - 删除交易
- `GET /api/v1/transactions/statistics` - 获取统计信息

### V2 API (虚拟线程)
- `POST /api/v2/transactions` - 创建交易（虚拟线程）
- `GET /api/v2/transactions/{id}` - 获取交易详情（虚拟线程）
- `GET /api/v2/transactions` - 获取交易列表（虚拟线程）
- `PUT /api/v2/transactions/{id}` - 更新交易（虚拟线程）
- `DELETE /api/v2/transactions/{id}` - 删除交易（虚拟线程）
- `GET /api/v2/transactions/statistics` - 获取统计信息（虚拟线程）

### 监控接口
- `GET /api/v1/monitoring/rate-limiter` - 限流器状态
- `GET /api/v1/monitoring/circuit-breaker` - 熔断器状态
- `GET /actuator/health` - 应用健康状态
- `GET /swagger-ui.html` - API文档

## 🚀 启动方式

### 环境要求
- Java 21+
- Maven 3.8+

### 本地启动
```bash
# 编译项目
mvn clean compile

# 运行应用
mvn spring-boot:run
```

### 指定环境启动
```bash
# 开发环境
./script/start.sh dev

# 测试环境
./script/start.sh test

# 生产环境
./script/start.sh prod

# Docker环境
./script/start.sh docker
```

### 访问应用
- 应用地址: http://localhost:8080
- API文档: http://localhost:8080/swagger-ui.html
- 健康检查: http://localhost:8080/actuator/health

## 🐳 容器化操作

### Docker部署
```bash
# 构建镜像
docker build -t transaction-demo .

# 运行容器
docker run -p 8080:8080 transaction-demo
```

### Kubernetes部署
```bash
# 使用Helm部署
helm install transaction-demo ./helm/transaction-demo
```

## 🛡️ 限流配置策略

### 分层限流架构
```
请求流程:
1. 全局限流: 120,000/min per client (基于QPS 2500 × 0.8安全系数)
2. API限流: 1,500-10,000/min per API (根据操作类型)
3. 账户限流: 100-500/min per account (防止单个账户过载)
4. 熔断器: 30%失败率阈值 (系统保护)
```

### API级别限流配置

#### 写入操作 (Write Operations)
| API | 限流配置 | 说明 |
|-----|----------|------|
| **创建交易** | 3000/min API + 100/min 账户 | 核心业务，平衡性能和安全性 |
| **更新交易** | 3000/min API + 100/min 账户 | 数据修改操作，与创建交易相同限制 |
| **删除交易** | 1500/min API | 敏感操作，限制更严格 |

#### 读取操作 (Read Operations)
| API | 限流配置 | 说明 |
|-----|----------|------|
| **获取单个交易** | 10000/min API | 高频查询，宽松限制 |
| **获取所有交易** | 10000/min API | 分页查询，宽松限制 |
| **按账户查询** | 10000/min API + 500/min 账户 | 账户级别额外保护 |
| **按类型查询** | 10000/min API | 统计分析，宽松限制 |
| **获取统计信息** | 10000/min API | 监控查询，宽松限制 |

#### 监控端点 (Monitoring)
| API | 限流配置 | 说明 |
|-----|----------|------|
| **健康检查** | 无限制 | 监控需要，不设限流 |
| **缓存统计** | 无限制 | 监控需要，不设限流 |

### 配置原则

1. **基于性能数据**: 所有限制基于实际测试QPS 2500+的数据
2. **安全系数**: 使用80%的容量作为安全边界
3. **读写分离**: 读取操作限制宽松，写入操作限制严格
4. **业务重要性**: 核心业务操作有合理的限制
5. **监控友好**: 监控端点不设限流，确保系统可观测性
6. **分层保护**: 全局 → API → 账户的多层限流策略

### 限流实现

- **算法**: 滑动窗口算法
- **存储**: 内存存储，高性能
- **线程安全**: 使用ReentrantLock保证并发安全
- **监控**: 提供实时限流状态查询
- **重置**: 支持手动重置限流计数器

### 预期效果

- **系统保护**: 防止单个客户端过载系统
- **公平分配**: 确保资源在多个客户端间合理分配
- **业务连续性**: 在限流触发时仍保持基本服务
- **可观测性**: 通过监控端点实时了解限流状态

## 📈 压力测试报告

### 测试环境配置

#### Tomcat线程池配置
```yaml
# 高性能Tomcat配置 (application-stress.yml)
server:
  tomcat:
    threads:
      max: 1500         # 最大工作线程数
      min-spare: 800    # 最小空闲线程数
    max-connections: 50000  # 最大连接数
    accept-count: 2000   # 连接队列长度
    connection-timeout: 30000  # 连接超时时间
    keep-alive-timeout: 120000  # Keep-alive超时时间
    max-keep-alive-requests: 200  # 每个连接最大请求数
    # 性能优化参数
    processor-cache: 800  # 处理器缓存大小
    tcp-no-delay: true   # 启用TCP_NODELAY
    so-keep-alive: true  # 启用SO_KEEPALIVE
```

#### JMeter测试配置
```xml
<!-- JMeter线程组配置 -->
<ThreadGroup>
  <stringProp name="ThreadGroup.num_threads">3000</stringProp>  <!-- 并发线程数 -->
  <stringProp name="ThreadGroup.ramp_time">60</stringProp>      <!-- 线程启动时间(秒) -->
  <stringProp name="LoopController.loops">50</stringProp>       <!-- 循环次数 -->
  <!-- 总请求数 = 3000线程 × 50循环 = 150,000请求 -->
</ThreadGroup>
```

#### JVM优化配置
```bash
# 高并发环境JVM参数 (script/jvm-options.conf)
STRESS_JVM_OPTS="\
  -Xms4g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -XX:+TieredCompilation \
  -Dspring.main.lazy-initialization=true"
```

### 性能测试结果对比

#### 创建交易性能对比 (Write QPS/TPS)

| 指标 | V1 API (传统线程) | V2 API (虚拟线程) | 性能提升 |
|------|------------------|------------------|----------|
| **并发配置** | 3000线程 × 50循环 | 3000线程 × 50循环 | - |
| **总请求数** | 150,000 | 150,000 | - |
| **实际请求数** | 150,000 | 150,000 | - |
| **成功请求数** | 149,894 | 149,939 | +45 |
| **失败请求数** | 106 | 61 | -45 |
| **成功率** | 99.93% | 99.96% | +0.03% |
| **错误率** | 0.07% | 0.04% | **-42.9%** |
| **QPS/TPS** | 2,502.79 req/s | 2,502.71 req/s | -0.003% |
| **平均响应时间** | 0.35ms | 0.32ms | **-8.6%** |
| **最大响应时间** | 86ms | 84ms | -2.3% |
| **95%响应时间** | 1ms | 1ms | 0% |
| **限流错误(429)** | 0 | 0 | - |
| **熔断错误(503)** | 0 | 0 | - |
| **超时错误** | 106 | 61 | **-42.5%** |

#### 统计查询性能对比 (Read QPS)

| 指标 | V1 API (传统线程) | V2 API (虚拟线程) | 性能提升 |
|------|------------------|------------------|----------|
| **并发配置** | 3000线程 × 50循环 | 3000线程 × 50循环 | - |
| **总请求数** | 150,000 | 150,000 | - |
| **实际请求数** | 150,000 | 150,000 | - |
| **成功请求数** | 149,770 | 149,950 | +180 |
| **失败请求数** | 230 | 50 | -180 |
| **成功率** | 99.85% | 99.97% | +0.12% |
| **错误率** | 0.15% | 0.03% | **-80.0%** |
| **QPS** | 2,503.09 req/s | 2,502.75 req/s | -0.01% |
| **平均响应时间** | 0.25ms | 0.25ms | 0% |
| **最大响应时间** | 87ms | 74ms | **-14.9%** |
| **95%响应时间** | 1ms | 1ms | 0% |
| **限流错误(429)** | 0 | 0 | - |
| **熔断错误(503)** | 0 | 0 | - |
| **超时错误** | 230 | 50 | **-78.3%** |

### 关键性能发现

#### 🚀 虚拟线程优势
- **错误率显著降低**: 创建交易错误率降低42.9%，查询错误率降低80%
- **响应时间更稳定**: 最大响应时间减少，平均响应时间优化
- **系统稳定性提升**: 超时错误大幅减少，系统在高并发下更稳定

#### 📊 性能表现
- **QPS稳定**: 两个版本都达到2500+ req/s的高性能水平
- **响应时间优异**: 平均响应时间 < 1ms，95%响应时间 ≤ 1ms
- **无保护机制触发**: 无限流错误，无熔断错误，说明系统设计合理

#### 🔧 配置优化效果
- **Tomcat线程池**: 1500个最大线程支持高并发
- **JVM优化**: G1GC + 4GB堆内存提供稳定性能
- **连接池优化**: 50000最大连接数满足大规模并发需求

### 测试策略说明

#### 测试工具配置
- **测试工具**: Apache JMeter 5.6.3
- **测试类型**: 压力测试 + 性能基准测试
- **监控指标**: QPS、响应时间、错误率、成功率

#### 并发策略
- **渐进式加压**: 120秒内逐步启动所有线程，避免瞬时冲击
- **持续负载**: 每个线程执行20-50次循环，模拟真实业务场景
- **错误处理**: 遇到错误继续执行，统计整体成功率

#### 环境隔离
- **专用测试环境**: 使用stress profile配置，关闭非必要功能
- **资源隔离**: 4GB内存 + 4核CPU，避免资源竞争
- **网络优化**: 本地测试，消除网络延迟影响

## 🧪 压力测试

### 运行压力测试

1. **启动应用**
```bash
./script/start.sh stress
```

2. **运行JMeter测试脚本**
```bash
# V1 API 创建交易测试
cd jmeter && ./run-v1-create-test.sh

# V2 API 创建交易测试  
cd jmeter && ./run-v2-create-test.sh

# V1 API 统计查询测试
cd jmeter && ./run-v1-statistics-test.sh

# V2 API 统计查询测试
cd jmeter && ./run-v2-statistics-test.sh
```

3. **查看测试报告**
```bash
# HTML报告
open jmeter/jmeter-results/v1-create-test-report/index.html
open jmeter/jmeter-results/v2-create-test-report/index.html

# 原始数据
ls jmeter/jmeter-results/*.jtl
```

### 测试配置
- **并发线程**: 3000
- **循环次数**: 50
- **总请求数**: 150,000
- **预热时间**: 60秒
- **测试时长**: ~60秒

## 🎯 项目特色

- 🚀 虚拟线程: 支持百万级并发
- ⚡ 高性能: QPS达到2500+
- 🛡️ 保护机制: 限流和熔断
- 📊 全面监控: 实时性能指标
- 🐳 容器化: Docker和K8s支持
- 💾 高级缓存: 防穿透、原子操作
- 🔒 并发安全: 分布式锁和版本控制 

