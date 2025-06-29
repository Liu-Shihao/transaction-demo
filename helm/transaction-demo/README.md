# Transaction Demo Helm Chart

这是用于部署 Transaction Management System 的 Helm Chart。

## 配置架构

本Chart采用分层配置架构：

### values.yaml - 公共配置
`values.yaml` 包含所有环境的公共配置，作为默认值：
- 基础镜像配置
- 服务配置
- 健康检查配置
- 默认资源限制
- 默认环境变量

### 环境特定配置
各个环境的values文件（如`values-dev.yaml`）会覆盖公共配置中的环境特定参数：
- 副本数
- 资源限制
- JVM参数
- 环境变量
- 自动扩缩容设置
- Ingress配置
- 应用特定配置

## 环境配置

本Chart支持以下环境配置：

| 环境 | 配置文件 | 说明 |
|------|----------|------|
| local | values-local.yaml | 本地开发环境 |
| dev | values-dev.yaml | 开发环境 |
| uat | values-uat.yaml | 用户验收测试环境 |
| cob | values-cob.yaml | 业务关闭环境 |
| prod | values-prod.yaml | 生产环境 |

## 安装

### 本地环境
```bash
helm install transaction-demo-local ./helm/transaction-demo -f ./helm/transaction-demo/values-local.yaml
```

### 开发环境
```bash
helm install transaction-demo-dev ./helm/transaction-demo -f ./helm/transaction-demo/values-dev.yaml
```

### UAT环境
```bash
helm install transaction-demo-uat ./helm/transaction-demo -f ./helm/transaction-demo/values-uat.yaml
```

### COB环境
```bash
helm install transaction-demo-cob ./helm/transaction-demo -f ./helm/transaction-demo/values-cob.yaml
```

### 生产环境
```bash
helm install transaction-demo-prod ./helm/transaction-demo -f ./helm/transaction-demo/values-prod.yaml
```

## 升级

```bash
# 升级特定环境
helm upgrade transaction-demo-dev ./helm/transaction-demo -f ./helm/transaction-demo/values-dev.yaml

# 升级所有环境
helm upgrade transaction-demo-local ./helm/transaction-demo -f ./helm/transaction-demo/values-local.yaml
helm upgrade transaction-demo-dev ./helm/transaction-demo -f ./helm/transaction-demo/values-dev.yaml
helm upgrade transaction-demo-uat ./helm/transaction-demo -f ./helm/transaction-demo/values-uat.yaml
helm upgrade transaction-demo-cob ./helm/transaction-demo -f ./helm/transaction-demo/values-cob.yaml
helm upgrade transaction-demo-prod ./helm/transaction-demo -f ./helm/transaction-demo/values-prod.yaml
```

## 卸载

```bash
# 卸载特定环境
helm uninstall transaction-demo-dev

# 卸载所有环境
helm uninstall transaction-demo-local transaction-demo-dev transaction-demo-uat transaction-demo-cob transaction-demo-prod
```

## 环境特性对比

| 特性 | Local | Dev | UAT | COB | Prod |
|------|-------|-----|-----|-----|------|
| 副本数 | 1 | 1 | 2 | 3 | 5 |
| 自动扩缩容 | ❌ | ❌ | ✅ | ✅ | ✅ |
| Swagger UI | ✅ | ✅ | ✅ | ❌ | ❌ |
| 详细日志 | ✅ | ✅ | ❌ | ❌ | ❌ |
| Ingress | ❌ | ❌ | ✅ | ✅ | ✅ |
| TLS | ❌ | ❌ | ❌ | ✅ | ✅ |
| 资源限制 | 低 | 低 | 中 | 高 | 最高 |
| 缓存TTL | 30s | 60s | 180s | 600s | 600s |

## 配置覆盖机制

### 公共配置 (values.yaml)
```yaml
# 基础配置
replicaCount: 1
image:
  repository: transaction-demo
  tag: latest

# 默认资源
resources:
  limits:
    cpu: 500m
    memory: 1Gi

# 默认环境变量
env:
  SPRING_PROFILES_ACTIVE: "local"
  JAVA_OPTS: "-Xms512m -Xmx512m -XX:+UseG1GC"
```

### 环境特定配置 (values-dev.yaml)
```yaml
# 覆盖公共配置
replicaCount: 1
image:
  tag: dev

resources:
  limits:
    cpu: 500m
    memory: 1Gi

env:
  SPRING_PROFILES_ACTIVE: "dev"
  JAVA_OPTS: "-Xms512m -Xmx512m -XX:+UseG1GC -XX:+PrintGCDetails..."

# 环境特定配置
config:
  logging:
    level: DEBUG
  cache:
    ttl: 60
```

## 可配置参数

### 公共参数 (values.yaml)
| 参数 | 默认值 | 说明 |
|------|--------|------|
| image.repository | transaction-demo | 镜像仓库 |
| image.tag | latest | 镜像标签 |
| service.type | ClusterIP | Service类型 |
| service.port | 80 | Service端口 |
| livenessProbe | 见values.yaml | 存活探针配置 |
| readinessProbe | 见values.yaml | 就绪探针配置 |

### 环境特定参数 (各环境values文件)
| 参数 | 说明 |
|------|------|
| replicaCount | 副本数 |
| resources | 资源限制 |
| env.JAVA_OPTS | JVM参数 |
| env.SPRING_PROFILES_ACTIVE | 环境配置 |
| autoscaling | 自动扩缩容配置 |
| ingress | Ingress配置 |
| config | 应用特定配置 |

## 启用Ingress示例

```bash
# 启用Ingress并配置域名
helm install transaction-demo ./helm/transaction-demo \
  --set ingress.enabled=true \
  --set ingress.host=transaction-demo.example.com \
  --set ingress.tls=true
```

## 自定义配置

```bash
# 自定义副本数和资源
helm install transaction-demo ./helm/transaction-demo \
  --set replicaCount=5 \
  --set resources.limits.cpu=2000m \
  --set resources.limits.memory=4Gi
```

## 目录结构

```
helm/transaction-demo/
├── Chart.yaml
├── values.yaml              # 公共配置
├── values-local.yaml        # 本地环境配置
├── values-dev.yaml          # 开发环境配置
├── values-uat.yaml          # UAT环境配置
├── values-cob.yaml          # COB环境配置
├── values-prod.yaml         # 生产环境配置
├── README.md
└── templates/
    ├── _helpers.tpl
    ├── deployment.yaml
    ├── hpa.yaml
    ├── ingress.yaml
    └── service.yaml
```

## 配置最佳实践

1. **公共配置**: 将通用的、不随环境变化的配置放在 `values.yaml` 中
2. **环境特定配置**: 将随环境变化的配置放在对应的环境values文件中
3. **配置覆盖**: 使用 `-f` 参数指定环境特定的values文件来覆盖公共配置
4. **参数验证**: 部署前使用 `helm template` 验证配置是否正确
5. **配置管理**: 使用版本控制管理不同环境的配置变更 