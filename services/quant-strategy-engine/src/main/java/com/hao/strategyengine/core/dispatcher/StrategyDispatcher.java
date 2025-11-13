package com.hao.strategyengine.core.dispatcher;

/**
 * 类说明 / Class Description:
 * 中文：策略分发器，根据策略ID获取策略实现并可通过装饰器增强（缓存/日志/限流），最终执行并返回结果。
 * English: Strategy dispatcher that retrieves strategy implementation by ID, enhances via decorators (cache/log/limit), executes and returns result.
 *
 * 使用场景 / Use Cases:
 * 中文：统一的策略调用入口；当需要在执行前后附加横切能力（如缓存）时使用。
 * English: Unified strategy invocation entry; used when attaching cross-cutting capabilities (e.g., caching) around execution.
 *
 * 设计目的 / Design Purpose:
 * 中文：解耦策略注册与执行路径，通过装饰器扩展非功能性需求，保持核心策略简单纯粹。
 * English: Decouple strategy registry from execution path; extend non-functional requirements via decorators, keeping core strategies simple.
 */
import com.hao.strategyengine.common.cache.StrategyCacheService;
import com.hao.strategyengine.core.registry.StrategyRegistry;
import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import com.hao.strategyengine.strategy.decorator.CachingDecorator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * StrategyDispatcher
 *
 * <p>策略分发器，用于根据策略ID获取对应策略并执行。
 * 支持链式装饰（Decorator）模式，可在策略执行前后添加缓存、日志、限流等功能。</p>
 *
 * <p>职责说明：</p>
 * <ul>
 *     <li>从 StrategyRegistry 中查找策略实例</li>
 *     <li>对策略进行装饰（例如缓存）</li>
 *     <li>执行策略并返回 StrategyResult</li>
 * </ul>
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * StrategyResult result = dispatcher.dispatch("strategy1", ctx);
 * }</pre>
 *
 * <p>注意事项：</p>
 * <ul>
 *     <li>策略ID不存在时会抛出 IllegalArgumentException</li>
 *     <li>可通过在装饰器链中增加新的装饰器扩展功能，如日志、限流、权限校验等</li>
 * </ul>
 *
 * @author hli
 * @date 2025-10-22
 */
@Component
@RequiredArgsConstructor
public class StrategyDispatcher {

    /** 策略注册表，用于根据策略ID获取策略实例 */
    private final StrategyRegistry registry;

    /** 策略缓存服务，用于装饰策略执行，提升性能 */
    private final StrategyCacheService cacheService;

    /**
     * 根据策略ID分发并执行策略
     *
     * @param strategyId 策略ID
     * @param ctx        策略执行上下文
     * @return 策略执行结果 StrategyResult
     * @throws IllegalArgumentException 如果策略ID不存在
     */
    /**
     * 方法说明 / Method Description:
     * 中文：根据策略ID查找策略实现，应用缓存装饰并执行，返回结构化结果。
     * English: Look up strategy by ID, apply caching decoration, execute and return structured result.
     *
     * 参数 / Parameters:
     * @param strategyId 中文说明：策略唯一标识 / English: Unique strategy identifier
     * @param ctx 中文说明：策略执行上下文（行情、账户与扩展参数） / English: Strategy execution context (market, account, extras)
     *
     * 返回值 / Return:
     * 中文：StrategyResult（包含信号、分数、元数据等） / English: StrategyResult (signals, scores, metadata)
     *
     * 异常 / Exceptions:
     * 中文：IllegalArgumentException 当策略ID不存在；运行时异常来自策略实现的内部错误 / English: IllegalArgumentException if ID not found; runtime errors from strategy internals.
     */
    public StrategyResult dispatch(String strategyId, StrategyContext ctx) {
        // 中文：从注册表按ID检索策略实例，若不存在则抛出非法参数异常
        // English: Retrieve strategy instance by ID from registry; throw illegal argument if missing
        QuantStrategy s = registry.get(strategyId);
        if (s == null) {
            throw new IllegalArgumentException("unknown strategy: " + strategyId);
        }
        // 中文：使用缓存装饰器包裹策略，降低重复计算与外部IO成本
        // English: Wrap strategy with caching decorator to reduce repeated computation and external IO cost
        QuantStrategy wrapped = new CachingDecorator(s, cacheService);
        // 中文：执行被装饰后的策略并返回结构化结果
        // English: Execute the decorated strategy and return structured result
        return wrapped.execute(ctx);
    }
}
