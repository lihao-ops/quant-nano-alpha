使用到的设计模式（清单 + 设计理由 — 面试速讲版）

策略模式（Strategy）

用处：把每种算法（MovingAverage/Momentum等）封装为实现 QuantStrategy 的独立类。

好处：新增策略只需新增类并注册到 Spring，不影响其他代码。

装饰者（Decorator）

用处：在 StrategyDispatcher 中动态包装策略（如 CachingDecorator），实现缓存、日志、限流等非侵入式增强。

好处：关心点分离、可组合、运行时可变。

复合模式（Composite）

用处：对“强相关”复合策略（CompositeStrategy），把多个策略组合为一个策略节点，支持并行/串联聚合。

好处：表达复合业务逻辑更自然，便于复用与复测。

责任链（Chain of Responsibility）

用处：StrategyChain + 一系列 StrategyHandler（风控、校验、限流）做前置/后置处理。

好处：可以按需插拔规则、顺序可控。

外观（Facade）

用处：StrategyEngineFacade 将并发调度、合并逻辑、cache、lock 隐藏为简单接口给 Controller 使用。

好处：上层调用简单、实现细节封装，便于维护。

观察者 / 发布-订阅（Observer / PubSub）

用处：执行结果异步发布到 Kafka，解耦下游（持久化、告警、日志等）。

好处：高吞吐、可伸缩、解耦。

Request Coalescing + 分布式锁（设计思想）

用处：使用 Redisson 分布式锁 + pending future 合并相同组合请求，避免重复计算。

好处：显著节省 CPU 与线程资源，提升整体吞吐。

4) 面试时如何简洁陈述（推荐 90 秒版本）

“我的策略引擎是一个高并发分布式微服务：一千用户并发、每用户多策略组合（万级任务）。我们用 CompletableFuture + 线程池做本地并行，用 Redis/Redisson 做分布式锁并实现 Request Coalescing（相同组合只计算一次，其他请求等待结果），结果异步推 Kafka，下游消费持久化到 MySQL。架构中使用了策略、装饰者、责任链、复合等模式来实现可扩展、可观测和可控的执行流程。优化后系统吞吐与延迟指标都有明显提升。”


tree /f > project-structure.txt



# 🚀 量化策略引擎 (Quant Strategy Engine)

> 高并发量化策略计算引擎，支持多策略并行执行，单机 QPS 3200+

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-7.0-red.svg)](https://redis.io/)
[![Kafka](https://img.shields.io/badge/Kafka-3.2-black.svg)](https://kafka.apache.org/)

## ✨ 核心特性

- 🔥 **高并发**：单机 3200+ QPS，P99 延迟 < 420ms
- 🎯 **多策略**：支持 100+ 种量化策略组合（MA/MOM/龙二等）
- 💪 **高可用**：Redis 哨兵 + Kafka 集群，可用性 99.9%
- ⚡ **低延迟**：二级缓存命中率 87%，降低 70% 数据库压力
- 🛡️ **稳定性**：Sentinel 熔断限流，故障自动降级
- 📊 **可观测**：Prometheus + Grafana 实时监控

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                     用户请求                              │
└─────────────────────┬───────────────────────────────────┘
                      │
         ┌────────────▼──────────────┐
         │   StrategyController      │  SSE 流式返回
         │   (API 层)                │
         └────────────┬──────────────┘
                      │
         ┌────────────▼──────────────┐
         │  StrategyEngineFacade     │  外观模式
         │  (核心层)                  │
         └────────────┬──────────────┘
                      │
         ┌────────────▼──────────────┐
         │   StrategyChain           │  责任链模式
         │   (风控/限流/鉴权)          │
         └────────────┬──────────────┘
                      │
         ┌────────────▼──────────────┐
         │ DistributedLockService    │  Redis 分布式锁
         │ (请求合并)                  │
         └────────────┬──────────────┘
                      │
         ┌────────────▼──────────────┐
         │  StrategyDispatcher       │  策略分发
         └────────────┬──────────────┘
                      │
         ┌────────────▼──────────────┐
         │  QuantStrategy 实现类      │  策略模式
         │  + CachingDecorator       │  装饰者模式
         └────────────┬──────────────┘
                      │
         ┌────────────▼──────────────┐
         │  KafkaResultPublisher     │  异步发布
         └───────────────────────────┘
```

## 📊 性能指标

| 指标 | 数值 | 说明 |
|------|------|------|
| **单机 QPS** | 3200+ | 3节点集群可达 9000+ |
| **P95 延迟** | 180ms | 95% 的请求在 180ms 内返回 |
| **P99 延迟** | 420ms | 99% 的请求在 420ms 内返回 |
| **Redis 命中率** | 87% | 二级缓存，降低 70% DB 压力 |
| **错误率** | < 0.1% | 熔断降级保护 |
| **可用性** | 99.9% | 高可用架构 |

## 🛠️ 技术栈

### 核心框架
- **Spring Boot 2.7.x** - 基础框架
- **Spring Cloud Alibaba** - 微服务治理

### 缓存 & 存储
- **Redis Sentinel** - 二级缓存（L2）+ 分布式锁
- **Caffeine** - 本地缓存（L1）
- **MySQL 8.0** - 主从集群，数据持久化

### 消息队列
- **Kafka 3.2** - 异步消息，每秒 1 万条吞吐

### 治理 & 监控
- **Sentinel** - 熔断限流降级
- **Nacos** - 配置中心 + 服务发现
- **Prometheus + Grafana** - 监控告警
- **SkyWalking** - 链路追踪

### 其他
- **MyBatis-Plus** - ORM 框架
- **OpenFeign** - 服务调用
- **JMeter** - 性能压测

## 📖 核心设计模式

| 模式 | 应用场景 | 代码位置 |
|------|---------|---------|
| **外观模式** | 统一入口，屏蔽复杂性 | `StrategyEngineFacade` |
| **责任链模式** | 风控/限流/鉴权 | `StrategyChain` |
| **策略模式** | 不同算法独立实现 | `QuantStrategy` 子类 |
| **装饰者模式** | 动态增加缓存能力 | `CachingDecorator` |
| **观察者模式** | Kafka 异步通知 | `KafkaResultPublisher` |
| **单例模式** | Redis 连接池 | `RedisClient` |

## 🚀 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- Docker & Docker Compose

### 启动步骤

```bash
# 1. 克隆项目
git clone https://github.com/your-repo/quant-strategy-engine.git
cd quant-strategy-engine

# 2. 启动基础设施（Redis/Kafka/MySQL）
docker-compose up -d

# 3. 编译项目
mvn clean package -DskipTests

# 4. 启动应用
java -jar target/quant-strategy-engine.jar \
  -Xms6g -Xmx6g -Xmn2g \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# 5. 验证服务
curl http://localhost:8080/health
```

### 压测验证

```bash
# 使用 JMeter 压测
cd src/test/java/performance/jmeter
jmeter -n -t strategy-load-test.jmx -l result.jtl

# 查看压测报告
cat src/test/java/performance/reports/performance-report.md
```

## 📂 项目结构

```
quant-strategy-engine
├─src/main/java/com/hao/strategyengine
│  ├─api                    # 接口层（Controller）
│  ├─core                   # 核心层（Facade/Dispatcher/Registry）
│  ├─service                # 服务层（业务逻辑）
│  ├─strategy               # 策略层（具体策略实现）
│  ├─chain                  # 责任链（风控/限流/鉴权）
│  ├─resilience             # 降级熔断（Sentinel）
│  ├─integration            # 外部集成（Redis/Kafka/Feign）
│  ├─monitoring             # 监控模块（Metrics/Alert）
│  └─common                 # 公共模块（Model/Cache/Config）
├─src/test/java
│  ├─integration            # 集成测试
│  ├─performance            # 性能测试（JMeter）
│  └─service                # 单元测试
└─docs                      # 项目文档
   ├─architecture.md        # 架构设计
   ├─performance-tuning.md  # 性能调优记录
   └─deployment.md          # 部署文档
```

## 📈 性能优化历程

### 优化 1：Redis 热 Key 导致 CPU 100%
- **问题**：热门股票被高频访问，Redis 主节点 CPU 100%
- **方案**：本地缓存 + 热 Key 拆分
- **效果**：CPU 使用率从 100% 降到 30%，P99 延迟从 2s 降到 180ms

### 优化 2：Full GC 频繁导致接口超时
- **问题**：老年代 96% 满，Full GC 每 2 分钟触发一次
- **方案**：堆扩容 6G + 新生代扩大到 2G + 切换 G1 GC
- **效果**：Full GC 频率降低 98%，GC 停顿从 1.5s 降到 80ms

### 优化 3：Kafka 消息堆积 10 万条
- **问题**：消费速度 1000/s，生产速度 5000/s
- **方案**：批量消费 + 手动提交 offset + 动态扩容
- **效果**：消费速度提升到 8000/s，5 分钟内清空堆积

详见：[性能调优文档](docs/performance-tuning.md)

## 🧪 测试覆盖

```
单元测试：85% 覆盖率
集成测试：Redis/Kafka/Feign 全覆盖
性能测试：JMeter 压测 30 分钟，3200+ QPS 稳定
```

## 📋 API 示例

### 执行策略组合

```bash
POST /api/strategy/execute
Content-Type: application/json

{
  "userId": "U001",
  "symbol": "600519",
  "strategyIds": ["MA", "MOM"]
}
```

**响应（SSE 流式）**：
```json
{
  "comboKey": "MA_MOM_600519",
  "results": [
    {
      "strategyId": "MA",
      "signal": "BUY",
      "confidence": 0.85,
      "executionTime": 120
    },
    {
      "strategyId": "MOM",
      "signal": "BUY",
      "confidence": 0.78,
      "executionTime": 95
    }
  ],
  "totalTime": 215
}
```

## 🔧 配置说明

### JVM 参数（推荐）
```bash
-Xms6g -Xmx6g           # 堆内存 6GB
-Xmn2g                  # 新生代 2GB
-XX:+UseG1GC            # 使用 G1 垃圾回收器
-XX:MaxGCPauseMillis=200 # 目标停顿 200ms
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/logs/heapdump.hprof
```

### 应用配置
```yaml
spring:
  redis:
    sentinel:
      master: mymaster
      nodes: 192.168.1.10:26379,192.168.1.11:26379
  kafka:
    bootstrap-servers: 192.168.1.20:9092,192.168.1.21:9092
    
strategy:
  thread-pool:
    core-size: 50
    max-size: 200
  cache:
    ttl: 300  # 缓存 5 分钟
```

## 📊 监控大盘

Grafana 监控地址：http://your-grafana:3000

**核心指标**：
- QPS 实时曲线
- P95/P99 延迟分布
- Redis 命中率
- Kafka 消费延迟
- JVM GC 统计

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 👨‍💻 作者

- **小浩** - [GitHub](https://github.com/your-profile)
- **邮箱**：your-email@example.com

---

⭐ 如果觉得项目不错，请给个 Star 支持一下！