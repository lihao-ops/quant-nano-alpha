package com.hao.quant.stocklist.infrastructure.persistence.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * MyBatis 数据持久化对象。
 * <p>
 * 映射数据库中的每日策略选股表,供 Mapper 使用。
 * </p>
 */
@Data
public class StrategyDailyPickPO {

    private Long id;
    private LocalDate tradeDate;
    private String strategyId;
    private String stockCode;
    private String stockName;
    private String industry;
    private BigDecimal score;
    private Integer ranking;
    private BigDecimal marketCap;
    private BigDecimal peRatio;
    private Map<String, Object> extraData;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
