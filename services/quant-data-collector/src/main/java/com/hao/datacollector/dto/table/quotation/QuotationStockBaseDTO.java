package com.hao.datacollector.dto.table.quotation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "股票基础行情数据 DTO")
public class QuotationStockBaseDTO {

    @Schema(description = "股票代码，例如：600000.SH", required = true)
    private String windCode;

    @Schema(description = "交易日，格式为 yyyyMMdd，例如：20250704", required = true)
    private LocalDate tradeDate;

    @Schema(description = "开盘价，单位：元，保留2位小数，例如：1415.70", required = true)
    private BigDecimal openPrice;

    @Schema(description = "最高价，单位：元，保留2位小数，例如：1431.89", required = true)
    private BigDecimal highPrice;

    @Schema(description = "最低价，单位：元，保留2位小数，例如：1410.01", required = true)
    private BigDecimal lowPrice;

    @Schema(description = "成交量，单位：手，保留2位小数，例如：28766.91", required = true)
    private BigDecimal volume;

    @Schema(description = "成交额，单位：亿元，保留4位小数，例如：40.87", required = true)
    private BigDecimal amount;

    @Schema(description = "收盘价，单位：元，保留2位小数，例如：1422.22", required = true)
    private BigDecimal closePrice;

    @Schema(description = "换手率，单位：%，保留2位小数，例如：0.23 表示 23%", required = true)
    private BigDecimal turnoverRate;
}
