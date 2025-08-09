package com.hao.datacollector.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "每日涨停股票明细查询结果DTO")
public class LimitUpStockDailyQueryDTO {

    @Schema(description = "交易日期，格式 yyyyMMdd")
    private String tradeDate;

    @Schema(description = "股票万得代码，例如 600519.SH")
    private String windCode;

    @Schema(description = "股票名称")
    private String name;

    @Schema(description = "首次涨停时间，格式 HH:mm:ss")
    private String firstTime;

    @Schema(description = "涨停状态描述")
    private String statusDesc;

    @Schema(description = "封单额(元)")
    private BigDecimal orderTotal;

    @Schema(description = "封单量占成交量比")
    private BigDecimal volumeNetin;

    @Schema(description = "是否是新股")
    private Integer listedStock;

    @Schema(description = "最新价")
    private BigDecimal price;

    @Schema(description = "换手率(%)")
    private BigDecimal turnoverRate;

    @Schema(description = "开板时间，格式 HH:mm:ss")
    private String openTime;

    @Schema(description = "开板次数")
    private Integer openTimes;

    @Schema(description = "涨跌幅(%)")
    private BigDecimal changeRange;

    @Schema(description = "近5日涨跌幅(%)")
    private BigDecimal changeRange5;

    @Schema(description = "近5日最大涨跌幅(%)")
    private BigDecimal changeRange5Max;

    @Schema(description = "近1日涨跌幅(%)")
    private BigDecimal changeRange1;

    @Schema(description = "近1日最大涨跌幅(%)")
    private BigDecimal changeRange1Max;

    @Schema(description = "连板天数")
    private Integer continuousBoardDays;

    @Schema(description = "所属行业")
    private String industry;

    @Schema(description = "所属概念")
    private String concept;

    @Schema(description = "总市值(元)")
    private BigDecimal totalMarketCap;

    @Schema(description = "流通市值(元)")
    private BigDecimal circulatingMarketCap;

    @Schema(description = "市盈率(PE)")
    private BigDecimal peRatio;

    @Schema(description = "市净率(PB)")
    private BigDecimal pbRatio;

    @Schema(description = "成交额(元)")
    private BigDecimal turnover;

    @Schema(description = "成交量(股)")
    private BigDecimal volume;
}