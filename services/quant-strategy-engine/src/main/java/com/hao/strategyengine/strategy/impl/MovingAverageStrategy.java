package com.hao.strategyengine.strategy.impl;


import com.hao.strategyengine.model.core.StrategyContext;
import com.hao.strategyengine.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import org.springframework.stereotype.Component;

@Component
public class MovingAverageStrategy implements QuantStrategy {
    @Override
    public String getId() { return "MA"; }

    @Override
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();
        // TODO: 真正实现要从DB/Feign拿数据计算，这里以模拟占位
        double value = Math.random() * 100;
        return StrategyResult.builder()
                .strategyId(getId())
                .data(value)
                .durationMs(System.currentTimeMillis() - start)
                .build();
    }
}
