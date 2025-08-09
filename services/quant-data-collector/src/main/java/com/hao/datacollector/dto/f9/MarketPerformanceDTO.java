package com.hao.datacollector.dto.f9;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author LiHao
 * @description: 简版F9市场表现数据传输结果对象
 */
@Data
@Schema(description = "简版F9市场表现数据传输结果对象")
public class MarketPerformanceDTO {

    @Schema(description = "市场表现数据传输对象", required = true)
    private MarketPerformanceDataDTO marketDTO;

    @Schema(description = "折线图List", required = true)
    private List<MarketPerformanceLineDTO> lineList;
}