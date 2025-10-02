package com.hao.strategyengine.template;

import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.core.Strategy;
import com.hao.strategyengine.core.StrategyContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象策略模板
 * 使用模板模式定义策略执行流程
 */
@Slf4j
public abstract class AbstractStrategy implements Strategy {
    
    protected String name;
    protected boolean ready = false;
    
    public AbstractStrategy(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean isReady() {
        return ready;
    }
    
    /**
     * 模板方法：定义策略分析流程
     */
    @Override
    public final Signal analyze(StrategyContext context) {
        log.debug("策略[{}]开始分析，标的：{}", name, context.getSymbol());
        
        // 1. 前置检查
        if (!preCheck(context)) {
            log.warn("策略[{}]前置检查未通过", name);
            return Signal.hold(context.getSymbol());
        }
        
        // 2. 计算指标
        calculateIndicators(context);
        
        // 3. 生成信号（由子类实现）
        Signal signal = generateSignal(context);
        
        // 4. 后置处理
        postProcess(context, signal);
        
        log.debug("策略[{}]分析完成，信号类型：{}", name, signal.getType());
        return signal;
    }
    
    /**
     * 前置检查（可覆盖）
     */
    protected boolean preCheck(StrategyContext context) {
        return context.getCurrentData() != null && ready;
    }
    
    /**
     * 计算技术指标（由子类实现）
     */
    protected abstract void calculateIndicators(StrategyContext context);
    
    /**
     * 生成交易信号（由子类实现）
     */
    protected abstract Signal generateSignal(StrategyContext context);
    
    /**
     * 后置处理（可覆盖）
     */
    protected void postProcess(StrategyContext context, Signal signal) {
        signal.setStrategyName(name);
        signal.setTimestamp(java.time.LocalDateTime.now());
    }
}
