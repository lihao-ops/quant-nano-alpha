package com.hao.strategyengine.strategy.impl;



import com.hao.strategyengine.model.core.StrategyContext;
import com.hao.strategyengine.model.response.StrategyResult;
import com.hao.strategyengine.strategy.QuantStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * 复合模式（Composite）
 *
 * 用处：对“强相关”复合策略（CompositeStrategy），把多个策略组合为一个策略节点，支持并行/串联聚合。
 *
 * 好处：表达复合业务逻辑更自然，便于复用与复测。
 */
public class CompositeStrategy implements QuantStrategy {
    private final String id;
    private final List<QuantStrategy> children = new ArrayList<>();

    public CompositeStrategy(String id) { this.id = id; }

    public void add(QuantStrategy s) { children.add(s); }

    @Override
    public String getId() { return id; }

    @Override
    public StrategyResult execute(StrategyContext context) {
        long start = System.currentTimeMillis();
        List<Object> arr = new ArrayList<>();
        for (QuantStrategy s : children) {
            StrategyResult r = s.execute(context);
            arr.add(r.getData());
        }
        return StrategyResult.builder()
                .strategyId(id)
                .data(arr)
                .durationMs(System.currentTimeMillis() - start)
                .build();
    }
}
