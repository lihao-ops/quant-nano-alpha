package com.hao.datacollector.dto.f9;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author LiHao
 * @description: F9关键统计数据传输对象
 */
@Data
@Schema(description = "F9关键统计数据传输对象")
public class KeyStatisticsDTO {
    @Schema(description = "总市值", required = true)
    private Double totalValue;

    @Schema(description = "PE(TTM)", required = true)
    private Double peT;

    @Schema(description = "PE(2023E)", required = true)
    private Double pe;

    @Schema(description = "PB(MRQ)", required = true)
    private Double pb;

    @Schema(description = "PS(TTM)", required = true)
    private Double ps;

    @Schema(description = "归母净利润(TTM)", required = true)
    private Double ttm;

    @Schema(description = "beta(100周)", required = true)
    private Double beta;

    @Schema(description = "总股本", required = true)
    private Double generalCapital;

    @Schema(description = "BPS(LF)", required = true)
    private Double bps;

    @Schema(description = "EPS(TTM)", required = true)
    private Double eps;

    @Schema(description = "预测EPS(平均)", required = true)
    private Double forecastEps;

    @Schema(description = "营业总收入(TTM)", required = true)
    private Double grossRevenue;

    @Schema(description = "扣非后净利润(TTM)", required = true)
    private Double retainedProfits;

    @Schema(description = "一致目标价", required = true)
    private Double targetPrice;
}