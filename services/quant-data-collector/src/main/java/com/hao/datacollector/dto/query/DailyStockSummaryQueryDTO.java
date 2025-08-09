package com.hao.datacollector.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "每日股票功能数量汇总查询结果DTO")
public class DailyStockSummaryQueryDTO {

    @Schema(description = "交易日期，格式 yyyyMMdd")
    private String tradeDate;

    @Schema(description = "功能ID，例如 limit_up_stocks, new_high_stocks")
    private String functionId;

    @Schema(description = "股票数量")
    private Integer stockNum;

    @Schema(description = "状态，例如 1-有效，0-无效")
    private Integer status;

    // 如果需要关联查询其他表获取股票代码，可以在这里添加，但通常汇总表不直接关联股票代码
    // @Schema(description = "股票代码")
    // private String windCode;
}