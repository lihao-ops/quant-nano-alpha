package com.hao.quant.stocklist.application.vo;

import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 返回给前端的股票视图对象。
 */
@Value
@Builder
public class StablePicksVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    String strategyId;
    String stockCode;
    String stockName;
    String industry;
    BigDecimal score;
    Integer ranking;
    BigDecimal marketCap;
    BigDecimal peRatio;
    LocalDate tradeDate;
    String extraData;
}
