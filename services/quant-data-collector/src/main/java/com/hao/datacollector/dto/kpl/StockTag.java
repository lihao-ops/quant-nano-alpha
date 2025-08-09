package com.hao.datacollector.dto.kpl;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "股票标签DTO")
class StockTag {
    @JsonProperty("ID")
    @Schema(description = "标签ID", example = "1532")
    private String id;

    @JsonProperty("Name")
    @Schema(description = "标签名称", example = "电池组件")
    private String name;

    @JsonProperty("Reason")
    @Schema(description = "标签原因", example = "BC产能主要为30GW HPBC电池项目")
    private String reason;
}