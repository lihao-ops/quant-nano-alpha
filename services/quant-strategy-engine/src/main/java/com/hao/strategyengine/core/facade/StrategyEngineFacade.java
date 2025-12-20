package com.hao.strategyengine.core.facade;

/**
 * 类说明 / Class Description:
 * 中文：策略引擎外观层，统一封装风控链、分布式锁、并行调度、结果缓存与消息发布，对上层提供简化一致的执行入口。
 * English: Facade for the strategy engine; encapsulates risk chain, distributed lock, parallel dispatch, result caching, and message publishing; exposes a unified execution entry to upper layers.
 *
 * 使用场景 / Use Cases:
 * 中文：为控制器或服务层提供“多策略组合一次执行”的统一接口，适用于实时策略计算与回测聚合。
 * English: Provides a unified "execute multiple strategies as a combo" interface for controllers/services; suitable for real-time runs and backtest aggregation.
 *
 * 设计目的 / Design Purpose:
 * 中文：屏蔽复杂并发与基础设施细节，通过外观模式提升可维护性与可复用性，确保幂等与高吞吐。
 * English: Hide concurrency and infrastructure complexity via facade pattern to improve maintainability and reusability, ensuring idempotency and high throughput.
 */
import com.hao.strategyengine.chain.StrategyChain;
import com.hao.strategyengine.common.cache.StrategyCacheService;
import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import com.hao.strategyengine.common.model.response.StrategyResultBundle;
import com.hao.strategyengine.common.util.KeyUtils;
import com.hao.strategyengine.core.dispatcher.StrategyDispatcher;
import com.hao.strategyengine.integration.kafka.KafkaResultPublisher;
import com.hao.strategyengine.strategy.lock.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * <p>
 * 【功能定位】：
 * ⦿ 本类是整个“策略引擎”的**统一入口（Facade 模式）**
 * ⦿ 向 Controller 层提供一个简化的接口，内部封装了：
 * ① 风控责任链（StrategyChain）
 * ② 并行调度器（StrategyDispatcher）
 * ③ 分布式锁控制（DistributedLockService）
 * ④ 结果缓存服务（StrategyCacheService）
 * ⑤ 结果异步推送（KafkaResultPublisher）
 * <p>
 * 【核心思路】：
 * Controller -> Facade（本类） -> Chain(风控) -> LockService(防重计算)
 * -> Dispatcher(并行策略计算) -> Bundle(聚合结果) -> Kafka/Redis输出
 * <p>
 * 【作用优势】：
 * - 对上层屏蔽复杂的锁、并发、缓存、Kafka 逻辑；
 * - 对下层提供一个统一的策略执行协调点；
 * - 确保同一组策略组合在集群中只执行一次（幂等防击穿）。
 * <p>
 * 【执行流程】：
 * ┌─────────────────────────────────────┐
 * │ Step 1：风控责任链前置校验（chain.apply）       │
 * │ Step 2：生成组合 key（KeyUtils.comboKey）       │
 * │ Step 3：构建 compute supplier（并行调度策略）   │
 * │ Step 4：分布式锁控制，执行 compute 或等待结果    │
 * │ Step 5：结果聚合 → 缓存 → Kafka 异步发布         │
 * └─────────────────────────────────────┘
 * <p>
 * 【对应执行链说明】：
 * ◉ 属于系统主执行链的「第 3 层」：
 * Controller(第1层) → Service(第2层) → Facade(第3层)
 * → Dispatcher(第4层) → Strategy(第5层)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyEngineFacade {

    /**
     * 策略分发器：负责根据策略 ID 调度对应的 QuantStrategy 实例
     */
    private final StrategyDispatcher dispatcher;

    /**
     * 风控责任链：策略执行前进行合规性校验
     */
    private final StrategyChain chain;

    /**
     * 分布式锁：防止多节点重复计算同一策略组合
     */
    private final DistributedLockService lockService;

    /**
     * 策略结果缓存：可在计算完成后异步写入 Redis
     */
    private final StrategyCacheService cacheService;

    /**
     * Kafka 发布器：将计算结果异步广播到消息总线
     */
    @Autowired
    private KafkaResultPublisher kafkaPublisher;

    /**
     * 线程池：用于并行执行多策略计算
     */
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
     * <p>
     * 【方法说明】：
     * 执行用户所选的多个策略（可并行），并在分布式环境下
     * 通过 Redis 锁控制，确保同一组合只执行一次。
     * <p>
     * 【参数说明】：
     *
     * @param userId      用户ID（便于统计与审计）
     * @param strategyIds 策略ID集合（如 ["MA","MOM","DRAGON_TWO"]）
     * @param ctx         策略执行上下文（行情数据、账户参数等）
     *                    <p>
     *                    【返回值】：
     *                    StrategyResultBundle — 聚合后的策略结果包
     *                    <p>
     *                    【执行流程】：
     *                    ① 风控校验
     *                    ② 构建组合Key
     *                    ③ 并行调度策略任务
     *                    ④ 通过分布式锁控制计算与等待
     *                    ⑤ 异步发布结果到 Kafka
     * @throws Exception 可能抛出线程池或锁等待异常
     */
    /**
     * 方法说明 / Method Description:
     * 中文：并行执行用户选择的多个策略，使用分布式锁保障组合级幂等，聚合结果并进行异步缓存与消息发布。
     * English: Execute multiple user-selected strategies in parallel, enforce combo-level idempotency via distributed lock, aggregate results, then asynchronously cache and publish.
     *
     * 参数 / Parameters:
     * @param userId 中文说明：用户标识，用于审计与限流决策 / English: User identifier for auditing and rate-limit decisions
     * @param strategyIds 中文说明：策略ID集合（如 MA、MOM 等） / English: Set of strategy IDs (e.g., MA, MOM)
     * @param ctx 中文说明：策略执行上下文（行情、账户与扩展参数） / English: Strategy execution context (market data, account and extras)
     *
     * 返回值 / Return:
     * 中文：StrategyResultBundle（组合策略的聚合结果包） / English: StrategyResultBundle (aggregated results of combo strategies)
     *
     * 异常 / Exceptions:
     * 中文：可能抛出线程池拒绝、锁等待超时、运行时计算异常；调用方需捕获并按业务容错处理 / English: May throw thread pool rejection, lock wait timeout, runtime compute errors; caller should handle gracefully per business policy.
     */
    public StrategyResultBundle executeAll(Integer userId, List<String> strategyIds, StrategyContext ctx) throws Exception {
        // 中文：前置风险/合规校验，阻断不合法或超限的执行请求
        // English: Pre-run risk and compliance checks to block illegal or exceeded execution requests
        // Step 1⃣ 前置责任链风控校验 —— 防止违规策略执行
        try {
            chain.apply(ctx);
        } catch (Exception e) {
            log.error("责任链执行异常：{}|Log_message", e.getMessage(), e);
            // 可视情况决定：是否中断策略执行 / 返回默认结果
        }


        // 中文：根据策略ID集合生成组合键，用于锁控制与结果标识
        // English: Generate a combo key from strategy ID set for lock control and result identification
        // Step 2⃣ 生成组合Key（如 "MA_MOM_DRAGON_TWO"）
        String comboKey = KeyUtils.comboKey(strategyIds);

        // 中文：以Supplier封装计算体，实现惰性执行与锁保护分离
        // English: Wrap compute body as Supplier to enable lazy execution and separate from lock protection
        // Step 3⃣ 构建计算逻辑 Supplier —— 包含并行执行多个策略的逻辑
        //当执行到这行时：
        // compute 只是被“定义”出来（函数体还没执行）这里的 compute 是一个 惰性执行（lazy execution） 的计算逻辑。
        // 真正的策略计算逻辑（CompletableFuture那段）此时还没跑。
        Supplier<StrategyResultBundle> compute = () -> {
            // 中文：（1）为每个策略创建并行任务，利用线程池提升吞吐
            // English: (1) Create parallel tasks for each strategy to increase throughput using thread pool
            // （1）异步并行执行每个策略
            List<CompletableFuture<StrategyResult>> futures = strategyIds.stream()
                    .map(id -> CompletableFuture.supplyAsync(() -> dispatcher.dispatch(id, ctx), pool))
                    .collect(Collectors.toList());

            // 中文：（2）等待所有策略完成，确保结果一致性与完整性
            // English: (2) Await completion of all strategies to ensure result consistency and completeness
            // （2）阻塞等待全部策略执行完成并收集结果
            List<StrategyResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            // 中文：（3）聚合结果为统一的结果包，便于下游消费
            // English: (3) Aggregate results into a unified bundle for downstream consumption
            // （3）封装为聚合结果包
            StrategyResultBundle bundle = new StrategyResultBundle(comboKey, results);

            // 中文：（4）异步缓存与发布，缩短关键路径并提升整体吞吐
            // English: (4) Cache and publish asynchronously to shorten critical path and improve throughput
            // （4）异步缓存与消息发布
            cacheService.save(bundle);
            kafkaPublisher.publish("quant-strategy-result", bundle);
            return bundle;
        };

        /**
         * todo 目前方案功能上没错：锁在 compute 外，保证了同一组合不会重复计算
         *
         * 优化空间在于：缩小锁粒度 → 锁只保护 CPU/IO 核心计算，不阻塞缓存/Kafka
         *
         * 面试加分点：你可以说：“锁保护核心计算，异步发布和缓存操作不占锁，最大化吞吐量”，这样既安全又高效
         */
        // 中文：通过分布式锁保护计算体，保证同一组合在集群内只执行一次；其他节点等待结果
        // English: Protect compute body with distributed lock to ensure single execution per combo across cluster; other nodes wait for the result
        // Step 4⃣ 分布式锁控制 —— 仅允许一个节点执行计算，其余节点等待结果
        return lockService.acquireOrWait(comboKey, compute);
    }
}
