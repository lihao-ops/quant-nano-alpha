package com.hao.strategyengine.core;

import com.hao.strategyengine.model.market.MarketData;
import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 策略上下文
 */
@Data
public class StrategyContext {
    private String symbol;
    private MarketData currentData;
    private List<MarketData> HistoricalData;
    private Map<String, Object> parameters;
    private BigDecimal availableFund;      // 可用资金
    private BigDecimal currentPosition;    // 当前持仓
    private Map<String, Object> state;     // 策略状态

    // 新增：专门用于数据源缓存/中间数据的容器
    private Map<String, Object> dataStore;

    public StrategyContext() {
        this.parameters = new HashMap<>();
        this.state = new HashMap<>();
        this.dataStore = new HashMap<>();
    }

    public Object getParameter(String key) {
        return parameters.get(key);
    }

    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }

    // 新增的便捷访问方法（可选，但推荐使用）
    public Object getData(String key) {
        return dataStore.get(key);
    }

    public void setData(String key, Object value) {
        dataStore.put(key, value);
    }
}