package com.hao.datacollector.dto.param.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 日频股票信息DTO
 *
 * @author hli
 * @date 2025-06-21
 */
@Data
@Schema(description = "日频股票信息")
public class StockInfoDailyDTO {

    @Schema(description = "股票代码", example = "000001.SZ")
    private String windCode;

    @Schema(description = "股票名称", example = "平安银行")
    private String windName;
}
