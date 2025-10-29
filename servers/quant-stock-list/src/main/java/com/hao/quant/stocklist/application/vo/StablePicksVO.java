package com.hao.quant.stocklist.application.vo;

import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 返回给前端的股票视图对象。
 * <p>
 * 通过值对象封装,避免外部修改内部结构。
 * </p>
 */
@Value
@Builder
public class StablePicksVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 策略标识 */
    String strategyId;
    /** 股票代码 */
    String stockCode;
    /** 股票名称 */
    String stockName;
    /** 所属行业 */
    String industry;
    /** 综合得分 */
    BigDecimal score;
    /** 排名 */
    Integer ranking;
    /** 流通市值 */
    BigDecimal marketCap;
    /** 市盈率 */
    BigDecimal peRatio;
    /** 交易日期 */
    LocalDate tradeDate;
    /** 扩展信息 JSON 字符串 */
    String extraData;
}
