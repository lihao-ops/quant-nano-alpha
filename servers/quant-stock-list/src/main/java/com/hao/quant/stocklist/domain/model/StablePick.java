package com.hao.quant.stocklist.domain.model;

import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 领域对象: 稳定精选股票。
 * <p>
 * 以不可变记录结构保存策略结果,确保在各层之间传递时具备线程安全性。
 * </p>
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
