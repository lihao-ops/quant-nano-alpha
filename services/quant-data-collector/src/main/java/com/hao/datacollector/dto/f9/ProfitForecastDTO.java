package com.hao.datacollector.dto.f9;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author LiHao
 * @description: F9盈利预测数据传输对象
 */
@Data
@Schema(description = "F9盈利预测数据传输对象")
public class ProfitForecastDTO {
    @Schema(description = "机构评级-买入", required = true)
    private Double buyNum;

    @Schema(description = "机构评级-增持", required = true)
    private Double addGradeNum;

    @Schema(description = "机构评级-中性", required = true)
    private Double neutralNum;

    @Schema(description = "机构评级-减持", required = true)
    private Double loseNum;

    @Schema(description = "机构评级-卖出", required = true)
    private Double sellNum;

    @Schema(description = "EPS", required = true)
    private Double eps;

    @Schema(description = "净利润增长率", required = true)
    private Double profitGrowthRate;

    @Schema(description = "180天内共几家预测机构", required = true)
    private Integer count;

    @Schema(description = "180天内count家预测机构预增数", required = true)
    private Integer addNum;

    @Schema(description = "180天内count家预测机构预减数", required = true)
    private Integer reduceNum;

    /**
     * 一致变化率
     */
    @Schema(description = "7天变化率", required = true)
    private Double rateChange7;

    @Schema(description = "30天变化率", required = true)
    private Double rateChange30;

    @Schema(description = "90天变化率", required = true)
    private Double rateChange90;

    @Schema(description = "180天变化率", required = true)
    private Double rateChange180;

    @Schema(description = "最新：", required = true)
    private String newest;

    @Schema(description = "最新：数值", required = true)
    private Double newestNum;

    @Schema(description = "上月：", required = true)
    private String lastMonth;

    @Schema(description = "上月：数值", required = true)
    private Double lastMonthNum;

    @Schema(description = "一致目标价", required = true)
    private Double targetPrice;

    @Schema(description = "上涨空间=(targetPrice - 现价) / 现价", required = true)
    private Double upsideSpace;
}