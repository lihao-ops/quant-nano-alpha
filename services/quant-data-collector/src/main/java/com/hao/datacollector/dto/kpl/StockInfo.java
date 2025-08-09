package com.hao.datacollector.dto.kpl;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "股票信息DTO")
class StockInfo {
    @JsonProperty("StockID")
    @Schema(description = "股票ID", example = "601012")
    private String stockId;

    @JsonProperty("Tag")
    @Schema(description = "股票标签列表")
    private List<StockTag> tag;

    @JsonProperty("prod_name")
    @Schema(description = "产品名称", example = "隆基绿能")
    private String prodName;

    @JsonProperty("HotNum")
    @Schema(description = "热度数值", example = "1053")
    private Integer hotNum;
}