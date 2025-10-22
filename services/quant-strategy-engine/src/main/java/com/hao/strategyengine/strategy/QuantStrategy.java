package com.hao.strategyengine.strategy;


import com.hao.strategyengine.model.core.StrategyContext;
import com.hao.strategyengine.model.response.StrategyResult;

/**
 * 策略模式（Strategy）
 * <p>
 * 用处：把每种算法（MovingAverage/Momentum等）封装为实现 QuantStrategy 的独立类。
 * <p>
 * 好处：新增策略只需新增类并注册到 Spring，不影响其他代码。
 */
public interface QuantStrategy {
    /**
     * 获取策略唯一标识
     *
     * @return 策略ID
     */
    String getId();

    /**
     * 执行策略逻辑
     *
     * @param context 策略上下文，包含用户信息、标的、额外参数等
     * @return StrategyResult 策略执行结果，包含策略ID、计算数据和耗时
     */
    StrategyResult execute(StrategyContext context);
}
