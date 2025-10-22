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