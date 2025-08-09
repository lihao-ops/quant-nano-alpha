package com.hao.datacollector.dto.table.limitup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-08-06 16:59:49
 * @description: 涨停股票对应交易日映射DTO
 */
@Data
@Schema(name = "涨停股票对应交易日映射DTO")
public class LimitUpStockTradeDTO {
    @Schema(description = "股票代码")
    private String windCode;

    @Schema(description = "交易日期")
    private LocalDate tradeDate;
}
