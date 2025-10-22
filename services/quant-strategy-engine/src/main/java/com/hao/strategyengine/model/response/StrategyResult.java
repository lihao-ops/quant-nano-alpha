package com.hao.strategyengine.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StrategyResult {
    private String strategyId;
    private Object data;
    private long durationMs;
}
