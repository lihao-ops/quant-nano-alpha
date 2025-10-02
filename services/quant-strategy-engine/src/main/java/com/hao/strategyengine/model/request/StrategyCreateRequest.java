package com.hao.strategyengine.model.request;

import com.hao.strategyengine.factory.StrategyConfig;
import lombok.Data;

@Data
public class StrategyCreateRequest {
    private StrategyConfig config;
}