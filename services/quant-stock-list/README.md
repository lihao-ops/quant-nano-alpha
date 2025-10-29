# Quant Stock List Module

该模块实现“每日稳定策略精选股票”查询服务,用于支撑高并发、低延迟的股票列表查询场景。整体方案遵循 CQRS + DDD 设计,聚焦在查询与缓存治理,与策略计算写入侧解耦。

## 架构概览

```
Client -> StablePicksController -> StablePicksService
          |-- BloomFilter(布隆过滤器)
          |-- Cache(L1 Caffeine + L2 Redis)
          |-- MySQL(回源) + 分布式锁
          |-- Kafka 事件驱动缓存刷新
```

- **多级缓存:** L1 使用 Caffeine, L2 使用 Redis, 支持逻辑过期与异步刷新。
- **布隆过滤器:** 以交易日、策略 ID 为维度构建,快速拦截非法请求,避免缓存穿透。
- **分布式锁:** 借助 Redisson 实现,防止缓存击穿,并允许在锁超时场景走降级策略。
- **事件驱动:** 策略结果入库后通过 Kafka 广播事件,触发查询侧进行缓存失效与预热。
- **可观测性:** Micrometer 暴露多级缓存命中率、DB 回源次数、布隆过滤器拦截率等指标。

## 目录结构

```
quant-stock-list
├── src
│   ├── main
│   │   ├── java/com/hao/quant/stocklist
│   │   │   ├── application (接口、Assembler)
│   │   │   ├── common (通用 DTO/异常)
│   │   │   ├── domain (领域模型与服务)
│   │   │   └── infrastructure (配置、缓存、持久化、消息、监控)
│   │   └── resources
│   │       ├── application.yml (配置示例)
│   │       ├── mapper/StablePicksMapper.xml (MyBatis SQL)
│   │       └── schema/strategy_daily_picks.sql (建表脚本)
│   └── test (预留)
└── pom.xml
```

## 功能清单

- 提供分页查询、最新交易日查询、指定股票详情、缓存刷新、缓存预热等 API。
- 支持 Kafka 消费通知执行缓存失效与布隆过滤器刷新。
- 通过 `StablePicksMetrics` 暴露 Prometheus 指标。
- 内置 Resilience4j 限流配置,默认 1000 QPS。

## 本地启动

1. 配置 MySQL/Redis/Kafka 环境,执行 `schema/strategy_daily_picks.sql` 初始化表结构。
2. 调整 `application.yml` 中的数据源及 Redis、Kafka、Sentinel 等配置。
3. 执行 `mvn spring-boot:run -pl servers/quant-stock-list -am` 启动服务。

## 参考压测指标

- 正常查询: 1000 QPS, P99 < 80ms。
- 热点 Key: 分布式锁保障 DB 回源次数 < 10。
- 非法日期: 布隆过滤器拦截率 > 99%。

更多设计细节请参考 `docs` 目录及代码注释。
