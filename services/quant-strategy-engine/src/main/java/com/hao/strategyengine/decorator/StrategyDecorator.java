package com.hao.strategyengine.decorator;


import com.hao.strategyengine.model.Signal;
import com.hao.strategyengine.core.Strategy;
import com.hao.strategyengine.core.StrategyContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 策略装饰器抽象类
 * 使用装饰器模式为策略添加额外功能
 */
@Slf4j
public abstract class StrategyDecorator implements Strategy {
    
    protected Strategy decoratedStrategy;
    
    public StrategyDecorator(Strategy strategy) {
        this.decoratedStrategy = strategy;
    }
    
    @Override
    public String getName() {
        return decoratedStrategy.getName() + "_" + getDecoratorName();
    }
    
    @Override
    public void initialize(StrategyContext context) {
        decoratedStrategy.initialize(context);
    }
    
    @Override
    public boolean isReady() {
        return decoratedStrategy.isReady();
    }
    
    @Override
    public Signal analyze(StrategyContext context) {
        // 调用被装饰策略的分析方法
        Signal signal = decoratedStrategy.analyze(context);
        // 应用装饰逻辑
        return decorate(context, signal);
    }
    
    /**
     * 装饰器名称
     */
    protected abstract String getDecoratorName();
    
    /**
     * 装饰逻辑（由子类实现）
     */
    protected abstract Signal decorate(StrategyContext context, Signal signal);
}
