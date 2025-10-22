package com.hao.strategyengine.core.dispatcher;


import com.hao.strategyengine.common.cache.StrategyCacheService;
import com.hao.strategyengine.core.registry.StrategyRegistry;
import com.hao.strategyengine.model.core.StrategyContext;
import com.hao.strategyengine.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import com.hao.strategyengine.strategy.decorator.CachingDecorator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StrategyDispatcher {

    private final StrategyRegistry registry;
    private final StrategyCacheService cacheService;

    public StrategyResult dispatch(String strategyId, StrategyContext ctx) {
        QuantStrategy s = registry.get(strategyId);
        if (s == null) {
            throw new IllegalArgumentException("unknown strategy: " + strategyId);
        }
        // 可以在这里链式装饰：缓存 -> 日志 -> 限流等
        QuantStrategy wrapped = new CachingDecorator(s, cacheService);
        return wrapped.execute(ctx);
    }
}
