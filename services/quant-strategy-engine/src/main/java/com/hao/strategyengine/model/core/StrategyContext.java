package com.hao.strategyengine.model.core;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class StrategyContext {
    private String userId;
    private String symbol;
    private Map<String, Object> extra;
    private Instant requestTime;
}
