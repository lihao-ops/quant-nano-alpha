package com.hao.datacollector.dto.f9;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "PE估值带数据对象")
public class PeBandVO {

    @Schema(description = "日期", example = "20220729")
    private String date;

    @Schema(description = "收盘价", example = "1726.74")
    private Double closePrice;

    @Schema(description = "指标值1-EPS,BPS", example = "44.38")
    private Double indicatorValue;

    @Schema(description = "复权因子", example = "0.91")
    private Double adjustmentFactor;

    @Schema(description = "每份DR代表股份数", example = "1.0")
    private Double drShareRatio;
}