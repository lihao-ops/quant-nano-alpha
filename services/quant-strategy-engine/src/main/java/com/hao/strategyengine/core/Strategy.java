package com.hao.strategyengine.core;

import com.hao.strategyengine.model.Signal;

/**
 * 策略接口
 */
public interface Strategy {
    /**
     * 策略名称
     */
    String getName();
    
    /**
     * 分析市场数据，生成交易信号
     */
    Signal analyze(StrategyContext context);
    
    /**
     * 初始化策略
     */
    void initialize(StrategyContext context);
    
    /**
     * 策略是否就绪
     */
    boolean isReady();
}
