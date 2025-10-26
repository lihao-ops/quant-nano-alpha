package com.hao.strategyengine.strategy.impl.signal;

import com.hao.strategyengine.common.model.core.StrategyContext;
import com.hao.strategyengine.common.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import enums.strategy.StrategyMetaEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MovingAverageStrategy
 *
 * <p>均线策略示例实现，用于量化交易系统演示或测试。</p>
 *
 * <p>策略特点：</p>
 * <ul>
 *     <li>策略 ID 为 "MA"，用于标识该策略</li>
 *     <li>执行逻辑当前为占位模拟值，实际使用时需从数据库或通过 Feign 获取历史行情数据计算均线</li>
 *     <li>记录策略执行耗时（毫秒），便于性能监控</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *     <li>在 StrategyDispatcher 中调用，支持装饰器增强（如缓存、日志、限流等）</li>
 *     <li>可作为策略组合中的一个实例参与组合计算</li>
 * </ul>
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * StrategyContext ctx = StrategyContext.builder()
 *     .userId("user123")
 *     .symbol("AAPL")
 *     .build();
 * MovingAverageStrategy strategy = new MovingAverageStrategy();
 * StrategyResult result = strategy.execute(ctx);
 * System.out.println(result.getData());
 * }</pre>
 * <p>
 * Lombok/注解说明：
 * - @Component 注入 Spring 容器，允许自动装配
 * </p>
 *
 * @author hli
 * @date 2025-10-22
 */
@Slf4j
@Component
public class MovingAverageStrategy implements QuantStrategy {

    /**
     * 策略唯一标识
     */
    @Override
    public String getId() {
        return StrategyMetaEnum.SIG_MOVING_AVERAGE.getId();
    }

    /**
     * 执行策略
     *
     * @param context 策略上下文，包含 userId、symbol、额外参数等
     * @return StrategyResult 策略执行结果，包含策略ID、数据和耗时
     */
    @Override
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();

        // TODO: 真正实现需从DB或Feign调用历史行情数据计算均线
        double value = Math.random() * 100; // 模拟计算结果
        try {
            Thread.sleep(3000);
            log.info("Thread={},execute={}", Thread.currentThread().getName(), StrategyMetaEnum.SIG_MOVING_AVERAGE.getId());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return StrategyResult.builder()
                .strategyId(getId())
                .data(StrategyMetaEnum.SIG_MOVING_AVERAGE.getId() + value)
                .durationMs(System.currentTimeMillis() - start)
                .build();
    }
}
