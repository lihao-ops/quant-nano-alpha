package com.hao.datacollector.web.vo.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author hli
 * @program datacollector
 * @date 2025-01-15
 * @description 股票行情数据查询结果传输对象
 */
@Data
@Schema(name = "股票行情数据查询结果传输对象")
public class StockMarketDataQueryResultVO {

    @Schema(description = "股票代码", required = true)
    private String windCode;

    @Schema(description = "股票名称")
    private String windName;

    @Schema(description = "交易日期")
    private LocalDate tradeDate;

    @Schema(description = "开盘价")
    private BigDecimal open;

    @Schema(description = "收盘价")
    private BigDecimal close;

    @Schema(description = "最高价")
    private BigDecimal high;

    @Schema(description = "最低价")
    private BigDecimal low;

    @Schema(description = "成交量加权平均价")
    private BigDecimal vwap;

    @Schema(description = "成交量")
    private Long volumeBtin;

    @Schema(description = "成交额")
    private BigDecimal amountBtin;

    @Schema(description = "涨跌幅")
    private BigDecimal pctChg;

    @Schema(description = "最新概念")
    private String latestconcept;

    @Schema(description = "所属产业链板块")
    private String chain;

    @Schema(description = "换手率")
    private BigDecimal turn;

    @Schema(description = "自由换手率")
    private BigDecimal freeTurn;

    @Schema(description = "振幅")
    private BigDecimal maxup;

    @Schema(description = "跌幅")
    private BigDecimal maxdown;

    @Schema(description = "交易状态")
    private String tradeStatus;

    @Schema(description = "总市值")
    private BigDecimal ev;

    @Schema(description = "流通市值")
    private BigDecimal mktFreeshares;

    @Schema(description = "开盘集合竞价价格")
    private BigDecimal openAuctionPrice;

    @Schema(description = "开盘集合竞价成交量")
    private Long openAuctionVolume;

    @Schema(description = "开盘集合竞价成交额")
    private BigDecimal openAuctionAmount;

    @Schema(description = "主力资金买入金额")
    private BigDecimal mfdBuyamtAt;

    @Schema(description = "主力资金卖出金额")
    private BigDecimal mfdSellamtAt;

    @Schema(description = "主力资金买入量")
    private Long mfdBuyvolAt;

    @Schema(description = "主力资金卖出量")
    private Long mfdSellvolAt;

    @Schema(description = "主力资金净流入")
    private BigDecimal mfdInflowM;

    @Schema(description = "主力资金净流入占比")
    private BigDecimal mfdInflowproportionM;

    @Schema(description = "技术指标换手率5日")
    private BigDecimal techTurnoverrate5;

    @Schema(description = "技术指标换手率10日")
    private BigDecimal techTurnoverrate10;

    @Schema(description = "ESG评级")
    private String esgRatingWind;

    @Schema(description = "创建时间")
    private java.time.LocalDateTime createTime;

    @Schema(description = "更新时间")
    private java.time.LocalDateTime updateTime;
}