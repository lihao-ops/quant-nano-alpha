package com.hao.strategyengine.core.registry;

import com.hao.strategyengine.strategy.QuantStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class StrategyRegistry {

    private final Map<String, QuantStrategy> strategyMap;

    @Autowired
    public StrategyRegistry(List<QuantStrategy> strategyBeans) {
        this.strategyMap = strategyBeans.stream()
                .collect(Collectors.toMap(QuantStrategy::getId, Function.identity()));
    }

    public QuantStrategy get(String id) {
        return strategyMap.get(id);
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(strategyMap.keySet());
    }
}
