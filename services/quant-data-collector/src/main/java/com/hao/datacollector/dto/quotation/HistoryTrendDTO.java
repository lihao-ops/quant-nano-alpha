package com.hao.datacollector.dto.quotation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "历史分时数据传输对象")
public class HistoryTrendDTO {

    @Schema(description = "股票代码", example = "600519.SH")
    private String windCode;

    @Schema(description = "1.交易日期", example = "20250711130101")
    private LocalDateTime tradeDate;
//
//    @Schema(description = "2.交易时间", example = "92501")
//    private int time;

    @Schema(description = "3.最新价", example = "142.316")
    private Double latestPrice;

    @Schema(description = "8.总成交量(手)1手=100股,总成交量不需要累加", example = "1423160")
    private Double totalVolume;

    @Schema(description = "79.均价", example = "142.50")
    private Double averagePrice;
}