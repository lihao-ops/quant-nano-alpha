package com.hao.strategyengine.strategy.decorator;


import com.hao.strategyengine.common.cache.StrategyCacheService;
import com.hao.strategyengine.model.core.StrategyContext;
import com.hao.strategyengine.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;

/**
 * 装饰者（Decorator）
 * <p>
 * 用处：在 StrategyDispatcher 中动态包装策略（如 CachingDecorator），实现缓存、日志、限流等非侵入式增强。
 * <p>
 * 好处：关心点分离、可组合、运行时可变。
 */
public class CachingDecorator implements QuantStrategy {
    private final QuantStrategy delegate;
    private final StrategyCacheService cacheService;

    public CachingDecorator(QuantStrategy delegate, StrategyCacheService cacheService) {
        this.delegate = delegate;
        this.cacheService = cacheService;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public StrategyResult execute(StrategyContext context) {
        String key = delegate.getId() + ":" + context.getSymbol();
        return cacheService.getOrCompute(key, () -> delegate.execute(context));
    }
}
