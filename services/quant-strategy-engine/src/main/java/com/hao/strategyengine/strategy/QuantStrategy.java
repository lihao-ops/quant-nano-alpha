package com.hao.strategyengine.strategy;


import com.hao.strategyengine.model.core.StrategyContext;
import com.hao.strategyengine.model.response.StrategyResult;

/**
 * 策略模式（Strategy）
 *
 * 用处：把每种算法（MovingAverage/Momentum等）封装为实现 QuantStrategy 的独立类。
 *
 * 好处：新增策略只需新增类并注册到 Spring，不影响其他代码。
 */
public interface QuantStrategy {
    String getId();
    StrategyResult execute(StrategyContext context);
}
