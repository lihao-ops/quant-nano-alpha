package com.hao.strategyengine.strategy.impl;


import com.hao.strategyengine.model.core.StrategyContext;
import com.hao.strategyengine.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;
import org.springframework.stereotype.Component;

@Component
public class MomentumStrategy implements QuantStrategy {
    @Override
    public String getId() { return "MOM"; }

    @Override
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();
        double value = Math.random() * 10;
        return StrategyResult.builder()
                .strategyId(getId())
                .data(value)
                .durationMs(System.currentTimeMillis() - start)
                .build();
    }
}
