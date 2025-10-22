package com.hao.strategyengine.core;


import com.hao.strategyengine.model.core.StrategyContext;

public interface StrategyHandler {
    void handle(StrategyContext ctx) throws Exception;
}
