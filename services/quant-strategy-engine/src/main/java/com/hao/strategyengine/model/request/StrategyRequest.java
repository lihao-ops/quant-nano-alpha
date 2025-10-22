package com.hao.strategyengine.model.request;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class StrategyRequest {
    private String userId;
    private String symbol;
    private List<String> strategyIds;
    private Map<String, Object> extra;
}
