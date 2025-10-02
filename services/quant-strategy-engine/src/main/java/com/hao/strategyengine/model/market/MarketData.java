package com.hao.strategyengine.model.market;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 市场数据
 */
@Data
public class MarketData {
    private String symbol;
    private BigDecimal price;
    private BigDecimal volume;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal open;
    private LocalDateTime timestamp;
    private Map<String, Object> indicators; // 技术指标
}
