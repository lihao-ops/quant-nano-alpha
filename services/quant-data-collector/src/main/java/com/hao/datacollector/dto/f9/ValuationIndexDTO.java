package com.hao.datacollector.dto.f9;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author LiHao
 * @description: 简版F9估值指标数据传输对象
 */
@Data
@Schema(description = "简版F9估值指标数据传输对象")
public class ValuationIndexDTO {
    @Schema(description = "指标名", required = true)
    private String name;

    @Schema(description = "最新股票数据", required = true)
    private Double now;

    @Schema(description = "最新行业数据", required = true)
    private Double industry;

    @Schema(description = "2024E股票数据", required = true)
    private Double now2024E;

    @Schema(description = "2024E行业数据", required = true)
    private Double industry2024E;
}