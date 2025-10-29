package com.hao.quant.stocklist.domain.model;

import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 领域对象: 稳定精选股票。
 */
@Builder
public record StablePick(
        String strategyId,
        String stockCode,
        String stockName,
        String industry,
        BigDecimal score,
        Integer ranking,
        BigDecimal marketCap,
        BigDecimal peRatio,
        LocalDate tradeDate,
        Map<String, Object> extraData
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
