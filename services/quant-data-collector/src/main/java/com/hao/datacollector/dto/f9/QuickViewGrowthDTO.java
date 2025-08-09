package com.hao.datacollector.dto.f9;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @description: 成长能力DTO
 */
@Data
@Schema(description = "成长能力")
public class QuickViewGrowthDTO {
    @Schema(description = "年份", required = true)
    private String name;

    @Schema(description = "营业收入", required = true)
    private Double income;

    @Schema(description = "净利润", required = true)
    private Double netprofit;

    @Schema(description = "ebit", required = true)
    private Double ebit;

    @Schema(description = "ebitda", required = true)
    private Double ebitda;

    @Schema(description = "总资产", required = true)
    private Double asset;

    @Schema(description = "自由现金流", required = true)
    private Double freecash;
}