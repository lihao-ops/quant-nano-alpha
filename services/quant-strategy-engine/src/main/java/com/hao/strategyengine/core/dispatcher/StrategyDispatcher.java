package com.hao.strategyengine.core.dispatcher;

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
    public StrategyResult dispatch(String strategyId, StrategyContext ctx) {
        QuantStrategy s = registry.get(strategyId);
        if (s == null) {
            throw new IllegalArgumentException("unknown strategy: " + strategyId);
        }
        // 链式装饰策略，可在这里添加更多装饰器，例如日志、限流等
        QuantStrategy wrapped = new CachingDecorator(s, cacheService);
        return wrapped.execute(ctx);
    }
}
