# 分布式数据一致性与多级缓存架构落地指南 (Outbox Pattern 完全体)

作为技术负责人，针对量化选股系统中“**信号中心完成最终信号落库后，通知 stock-list 服务刷新 Redis 缓存的 Kafka 消息未发出，导致缓存与数据库长期不一致**”的原子性缺口，我们设计了这套基于 Outbox Pattern（本地消息表）的完整工程落地方案。

此方案遵循大厂标准，涵盖了强本地原子性写、毫秒级事务同步事件分发、防 OOM 异步线程池隔离、XXL-JOB 定时补偿与死信熔断、以及下游三级缓存一致性保障。

## 架构职责分层说明

1. **数据层 (Data)**：同一数据源下的 `tb_quant_stock_signal` (业务主表) 与 `tb_signal_outbox` (本地发件箱表)，保障物理原子写。
2. **业务层 (Service)**：在 `@Transactional` 中双写表，并发布内部解耦的 Spring Domain Event。
3. **分发层 (Dispatcher)**：利用 `@TransactionalEventListener(AFTER_COMMIT)` 监听主事务成功，**即刻发送 Kafka 消息通知 downstream `stock-list` 服务刷新对应股票信号的缓存**。利用 `CallerRunsPolicy` 线程池进行异步隔离。
4. **离线调度层 (Job)**：XXL-JOB 提供延迟扫表重投补偿（防宕机断链），及历史脏数据物理删除（防扫表拖垮数据库）。

---

## 核心改造模块详解 (Proposed Changes)

我们将以 `quant-signal-center`（信号中心模块）作为实施主体进行改造。

### 1. 数据库改造 (Database Layer)

由于强依赖本地事务原子性，Outbox 表必须与业务主表在同一个库。

#### [NEW] `schema/outbox_init.sql`
```sql
CREATE TABLE `tb_signal_outbox` (
    `id`             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    `event_id`       VARCHAR(64)  NOT NULL COMMENT '全局唯一幂等键(如 UUID)',
    `event_type`     VARCHAR(32)  NOT NULL COMMENT '事件类型，如 SIGNAL_SAVED',
    `payload`        TEXT         NOT NULL COMMENT '事件负载内容(JSON)',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '发送状态: 0待发送, 1已发送, 2发送最终失败',
    `retry_count`    INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `sent_at`        DATETIME     COMMENT '最终发送成功时间',
    UNIQUE KEY `uk_event_id` (`event_id`),
    INDEX `idx_status_created` (`status`, `created_at`) COMMENT '用于定时任务扫描的覆盖索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息发件箱表';
```

---
### 2. 线程池安全与配置改造 (Config Layer)

必须显式配置异步线程池，避免高并发下 Spring 默认 `SimpleAsyncTaskExecutor` (每次新建线程) 导致的 OOM 灾难。

#### [NEW] `com/hao/signalcenter/config/AsyncConfig.java`
- 开启 `@EnableAsync`。
- 定义 `ThreadPoolTaskExecutor` (名为 `kafkaDispatchThreadPool`)。
- **核心要点**：设置 `RejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())`。当线程池满时不丢弃任务，交由主业务线程同步发送。这样既有隔离保护，又是 XXL-JOB 兜底前的最后一道极效保险。

---
### 3. 核心业务层架构 (Service Layer)

#### [MODIFY] `com/hao/signalcenter/service/SignalPersistenceService.java`
- `@Transactional`: 双写 `stockSignalMapper` 与 `signalOutboxMapper`。
- `ApplicationEventPublisher`: 双写完成后，`publishEvent(new SignalSavedEvent(outbox))` 解耦发布事件，不让网络调用侵入核心写库流程。

---
### 4. 毫秒级即时分发层 (Dispatcher Layer)

#### [NEW] `com/hao/signalcenter/dispatcher/OutboxMessageDispatcher.java`
- 监听器：`@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`。
- 异步隔离：类/方法打上 `@Async("kafkaDispatchThreadPool")`。
- 逻辑：事务成功提交后，立刻执行 `KafkaTemplate.send`。成功则回写 outbox 的 `status = 1`；遇到网络异常不卡点，直接异常返回交由兜底（如果失败，Outbox 状态依然为 `0`）。

---
### 5. 离线兜底补偿与垃圾回收层 (Job Layer)

#### [NEW] `com/hao/signalcenter/job/OutboxCompensationJob.java`
- 注解：`@XxlJob("outboxPublishJob")`，每 15/30 秒执行一次。
- **扫描重推**：查询 `status = 0 and created_at < current - 1分钟` 被积压的“死信”。
- **重试熔断机制**（重点）：
  ```java
  if (outbox.getRetryCount() >= 5) {
      signalOutboxMapper.updateStatus(outbox.getId(), 2, LocalDateTime.now());
      // 面试得分点：这里可以扩展为发钉钉/企业微信级别的高危告警
      log.error("outbox补偿超过最大重试次数，标记为最终失败 eventId={}", outbox.getEventId());
      continue;
  }
  ```

#### [NEW] `com/hao/signalcenter/job/OutboxCleanupJob.java`
- 注解：`@XxlJob("outboxCleanupJob")`，每日凌晨 2 点一次。
- **物理回收**：定期删除 `status = 1` 且时间大于 3 天的冗余成功记录，永久保证 outbox 表的扫表轻量级与查询极速。

---

## 💡 必考架构追问指南

### 追问一：Spring AOP 执行原理解析

> **真实面试修罗场**：“你在事件监听前既加了 `@Async` 又加了 `@TransactionalEventListener`，Spring 会不会在原事务里发网络请求导致长事务？”

**防连环追问标准答案：**
如果在同一个方法上同时存在这两个注解，Spring 底层的真实执行链路分为严格的两段：
1. **先判断事务阶段**：主业务的 `@Transactional` 率先完成 `Commit`，清空当前线程绑定的数据库连接（Connection 会归还池）。
2. **触发侦听器**：由于满足了 `AFTER_COMMIT` 条件，Spring Event 被唤醒。
3. **后经过 AOP 拦截**：在真实执行前，`AsyncAnnotationBeanPostProcessor` 介入，**将发送 Kafka 的动作包装成任务丢入 `kafkaDispatchThreadPool`，随后立刻向业务主线程返回。**
绝对杜绝了网络 IO 引发数据库假死的风险。

### 追问二：下游消费方的三层缓存更新闭环构建

> **真实面试修罗场**：“你说发了 Kafka 通知下游，那下游服务是谁？你们的三层缓存 `(Caffeine + Redis)` 是怎么保证刷新的？多节点部署下如何防止并发脏写？”

**防连环追问标准答案：**
当外部消费方 `stock-list` 收到信号更新消息后，它的三层链路是构建在严格的 **Cache-Aside 机制**基础上的，其核心精髓体现在**只删不改 + Redis 增强广播**两处设计：

1. **坚持删除（Invalidate）而非直接更新（Update）**：
   在微服务环境下，并发消费 MQ 更新 Redis 极易导致新值被旧值踩踏。执行删除操作天生具备幂等性，后续的第一个 C 端请求会自动击穿到数据库并将保证绝对正确的新值重建回缓存池，根治了并发写乱序。
2. **Redis Pub/Sub 广播解决本地缓存孤岛**：
   由于 Kafka 通知只会被同一个消费组里的一个实例处理，该节点清除 Caffeine 缓存后，**集群下其它实例的 Caffeine 里仍存在旧数据**。
   为实现强一致，消费信号后，我们**追加广播一条 `cache:invalidate:stock` 消息**。其他 `stock-list` 实例通过此订阅频道接收指令后同步销毁本地 Caffeine 缓存，实现了多节点本地内存集群级别的一致性淘汰。

---

## 最终版简历高频对线储备话术

在面试时，将过去的普通事务/缓存描述直接升级为这两大杀器：

> ▸ **分布式原子性事件治理**：引入 Outbox Pattern 应对复杂状态推送，信号落库与 outbox 事件写入同本地事务；采用 `@TransactionalEventListener(AFTER_COMMIT)` 结合防溢出线程池实现毫秒级异步发布，通知下游 `stock-list` 更新信号状态；XXL-JOB 定时补偿漏发死信与重试熔断告警，端到端解决并发信号吞吐与缓存的一致性断带痛点。

> ▸ **多级缓存失效链路与并发调优**：主导设计 `Caffeine + Redis + MySQL` 三层防抖缓存架构；面对高频选股信号下发引发的一致性问题，推行**“淘汰替代更新”幂等策略**避让写库冲突；并通过 **Redis Pub/Sub 广播通知机制**彻底根治多节点隔离情况下的 Caffeine 内存孤岛污染，实现 100% 自动容错回填 Cache-Aside 闭环。
