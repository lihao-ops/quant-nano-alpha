package com.hao.strategyengine.model.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MarketDataRequest {
    private String symbol;
    private BigDecimal price;
    private BigDecimal volume;
}