package com.hao.strategyengine.core.facade;

import com.hao.strategyengine.chain.StrategyChain;
import com.hao.strategyengine.common.cache.StrategyCacheService;
import com.hao.strategyengine.common.util.KeyUtils;
import com.hao.strategyengine.core.dispatcher.StrategyDispatcher;
import com.hao.strategyengine.integration.kafka.KafkaResultPublisher;
import com.hao.strategyengine.model.core.StrategyContext;
import com.hao.strategyengine.model.response.StrategyResult;
import com.hao.strategyengine.model.response.StrategyResultBundle;
import com.hao.strategyengine.strategy.lock.DistributedLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * ===============================================================
 * 【类名】：StrategyEngineFacade（策略引擎外观层）
 * ===============================================================
 *
 * 【功能定位】：
 *   ⦿ 本类是整个“策略引擎”的**统一入口（Facade 模式）**
 *   ⦿ 向 Controller 层提供一个简化的接口，内部封装了：
 *       ① 风控责任链（StrategyChain）
 *       ② 并行调度器（StrategyDispatcher）
 *       ③ 分布式锁控制（DistributedLockService）
 *       ④ 结果缓存服务（StrategyCacheService）
 *       ⑤ 结果异步推送（KafkaResultPublisher）
 *
 * 【核心思路】：
 *   Controller -> Facade（本类） -> Chain(风控) -> LockService(防重计算)
 *        -> Dispatcher(并行策略计算) -> Bundle(聚合结果) -> Kafka/Redis输出
 *
 * 【作用优势】：
 *   - 对上层屏蔽复杂的锁、并发、缓存、Kafka 逻辑；
 *   - 对下层提供一个统一的策略执行协调点；
 *   - 确保同一组策略组合在集群中只执行一次（幂等防击穿）。
 *
 * 【执行流程】：
 *   ┌─────────────────────────────────────┐
 *   │ Step 1：风控责任链前置校验（chain.apply）       │
 *   │ Step 2：生成组合 key（KeyUtils.comboKey）       │
 *   │ Step 3：构建 compute supplier（并行调度策略）   │
 *   │ Step 4：分布式锁控制，执行 compute 或等待结果    │
 *   │ Step 5：结果聚合 → 缓存 → Kafka 异步发布         │
 *   └─────────────────────────────────────┘
 *
 * 【对应执行链说明】：
 *   ◉ 属于系统主执行链的「第 3 层」：
 *      Controller(第1层) → Service(第2层) → Facade(第3层)
 *      → Dispatcher(第4层) → Strategy(第5层)
 */
@Service
@RequiredArgsConstructor
public class StrategyEngineFacade {

    /** 策略分发器：负责根据策略 ID 调度对应的 QuantStrategy 实例 */
    private final StrategyDispatcher dispatcher;

    /** 风控责任链：策略执行前进行合规性校验 */
    private final StrategyChain chain;

    /** 分布式锁：防止多节点重复计算同一策略组合 */
    private final DistributedLockService lockService;

    /** 策略结果缓存：可在计算完成后异步写入 Redis */
    private final StrategyCacheService cacheService;

    /** Kafka 发布器：将计算结果异步广播到消息总线 */
    @Autowired
    private KafkaResultPublisher kafkaPublisher;

    /** 线程池：用于并行执行多策略计算 */
    private final ExecutorService pool = new ThreadPoolExecutor(
            8, // corePoolSize
            64, // maximumPoolSize
            60L, // keepAliveTime
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2000), // 任务队列
            r -> new Thread(r, "strategy-worker"), // 自定义线程名
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：回退执行
    );

    /**
     * ===============================================================
     * 【方法名】：executeAll
     * ===============================================================
     *
     * 【方法说明】：
     *   执行用户所选的多个策略（可并行），并在分布式环境下
     *   通过 Redis 锁控制，确保同一组合只执行一次。
     *
     * 【参数说明】：
     *   @param userId      用户ID（便于统计与审计）
     *   @param strategyIds 策略ID集合（如 ["MA","MOM","DRAGON_TWO"]）
     *   @param ctx          策略执行上下文（行情数据、账户参数等）
     *
     * 【返回值】：
     *   StrategyResultBundle — 聚合后的策略结果包
     *
     * 【执行流程】：
     *   ① 风控校验
     *   ② 构建组合Key
     *   ③ 并行调度策略任务
     *   ④ 通过分布式锁控制计算与等待
     *   ⑤ 异步发布结果到 Kafka
     *
     * @throws Exception 可能抛出线程池或锁等待异常
     */
    public StrategyResultBundle executeAll(String userId, List<String> strategyIds, StrategyContext ctx) throws Exception {
        // Step 1️⃣ 前置责任链风控校验 —— 防止违规策略执行
        chain.apply(ctx);

        // Step 2️⃣ 生成组合Key（如 "MA_MOM_DRAGON_TWO"）
        String comboKey = KeyUtils.comboKey(strategyIds);

        // Step 3️⃣ 构建计算逻辑 Supplier —— 包含并行执行多个策略的逻辑
        Supplier<StrategyResultBundle> compute = () -> {
            // （1）异步并行执行每个策略
            List<CompletableFuture<StrategyResult>> futures = strategyIds.stream()
                    .map(id -> CompletableFuture.supplyAsync(() -> dispatcher.dispatch(id, ctx), pool))
                    .collect(Collectors.toList());

            // （2）阻塞等待全部策略执行完成并收集结果
            List<StrategyResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            // （3）封装为聚合结果包
            StrategyResultBundle bundle = new StrategyResultBundle(comboKey, results);

            // （4）异步缓存与消息发布
            cacheService.save(bundle);
            kafkaPublisher.publish("quant-strategy-result", bundle);
            return bundle;
        };

        // Step 4️⃣ 分布式锁控制 —— 仅允许一个节点执行计算，其余节点等待结果
        return lockService.acquireOrWait(comboKey, compute);
    }
}
