package com.hao.datacollector.dto.quotation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author hli
 * @program: quant-nano-alpha
 * @Date 2025-09-30 14:44:45
 * @description: 指标历史行情数据
 */
@Data
@Schema(description = "历史指标分时数据传输对象")
public class HistoryTrendIndexDTO {

    @Schema(description = "指标代码", example = "600519.SH")
    private String windCode;

    @Schema(description = "2.交易时间", example = "20250711130101")
    private LocalDateTime tradeDate;

    @Schema(description = "3.最新价", example = "142.316")
    private Double latestPrice;

    @Schema(description = "59.总成交额", example = "1423160")
    private Double totalAmount;

    @Schema(description = "8.总成交量", example = "142.50")
    private Double totalVolume;
}