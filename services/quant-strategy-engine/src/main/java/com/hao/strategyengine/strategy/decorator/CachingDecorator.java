package com.hao.strategyengine.strategy.decorator;

import com.hao.strategyengine.common.cache.StrategyCacheService;
import com.hao.strategyengine.model.core.StrategyContext;
import com.hao.strategyengine.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;

/**
 * CachingDecorator
 *
 * <p>策略装饰器（Decorator模式实现），用于给 QuantStrategy 添加缓存功能。</p>
 *
 * <p>设计目的：</p>
 * <ul>
 *     <li>在 StrategyDispatcher 中动态包装策略，实现非侵入式增强</li>
 *     <li>支持缓存策略执行结果，减少重复计算，提高性能</li>
 *     <li>遵循开闭原则：无需修改原有策略类即可添加功能</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *     <li>策略计算耗时较长且重复调用频繁时</li>
 *     <li>系统对性能敏感，希望通过缓存减少数据库或计算压力</li>
 * </ul>
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * QuantStrategy strategy = new MyStrategy();
 * QuantStrategy cachedStrategy = new CachingDecorator(strategy, cacheService);
 * StrategyResult result = cachedStrategy.execute(context);
 * }</pre>
 *
 * <p>实现细节：</p>
 * <ul>
 *     <li>通过 StrategyCacheService.getOrCompute 获取或计算缓存</li>
 *     <li>缓存 key 由策略 ID + 标的 symbol 构成，确保不同策略或标的独立缓存</li>
 *     <li>遵循装饰器模式，实现 QuantStrategy 接口并委托原策略方法</li>
 * </ul>
 *
 * Lombok/其他注解说明：此类未使用 Lombok，构造器和方法手动实现
 * </p>
 *
 * @author hli
 * @date 2025-10-22
 */
public class CachingDecorator implements QuantStrategy {

    /** 被装饰的策略对象 */
    private final QuantStrategy delegate;

    /** 缓存服务 */
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
        // 缓存 key = 策略ID + 标的symbol
        String key = delegate.getId() + ":" + context.getSymbol();
        // 获取缓存或计算
        return cacheService.getOrCompute(key, () -> delegate.execute(context));
    }
}
