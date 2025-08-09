package com.hao.datacollector.dto.f9;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author LiHao
 * @description: 市场表现折线图数据传输对象
 * @Date 2023-08-08 14:48:15
 */
@Data
@Schema(description = "市场表现折线图数据传输对象")
public class MarketPerformanceLineDTO {
    @Schema(description = "时间", required = true)
    private String date;

    @Schema(description = "收盘价", required = true)
    private Double closePrice;

    @Schema(description = "沪深300收盘价", required = true)
    private Double closePrice300;
}