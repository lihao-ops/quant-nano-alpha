package com.hao.datacollector.dto.f9;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author LiHao
 * @description: 市场表现数据传输对象
 * @Date 2023-08-08 14:59:19
 */
@Data
@Schema(description = "市场表现数据传输对象")
public class MarketPerformanceDataDTO {
    @Schema(description = "股票代码", required = true)
    private String windCode;

    @Schema(description = "股票名称", required = true)
    private String windName;

    @Schema(description = "当前股价", required = true)
    private Double value;

    @Schema(description = "当前单位(例：cny)", required = true)
    private String unit;

    @Schema(description = "涨跌幅(价格)", required = true)
    private Double limitAmount;

    @Schema(description = "涨跌幅(百分比)", required = true)
    private Double limitPrice;

    @Schema(description = "区间最高价", required = true)
    private Double topAmount;

    @Schema(description = "区间最低价", required = true)
    private Double lowAmount;

    @Schema(description = "年迄今涨跌幅", required = true)
    private Double toThisDayPriceLimit;

    @Schema(description = "近一月涨跌幅", required = true)
    private Double oneMonthPriceLimit;

    @Schema(description = "近三月涨跌幅", required = true)
    private Double threeMonthPriceLimit;

    @Schema(description = "近一年涨跌幅", required = true)
    private Double oneYearPriceLimit;

    @Schema(description = "融资融券余额", required = true)
    private Double financeAmount;

    @Schema(description = "区间时间起始", required = true)
    private String startTime;

    @Schema(description = "区间时间结束", required = true)
    private String endTime;

    @Schema(description = "日均", required = true)
    private Double averageDaily;
}