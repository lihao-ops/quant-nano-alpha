package com.hao.datacollector.dto.kpl;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "股票详情DTO")
public class StockDetail {
    @JsonProperty("StockID")
    @Schema(description = "股票ID", example = "300537")
    private String stockId;

    @JsonProperty("IsZz")
    @Schema(description = "是否主做", example = "1")
    private String isZz;

    @JsonProperty("IsHot")
    @Schema(description = "是否热门", example = "1")
    private String isHot;

    @JsonProperty("Reason")
    @Schema(description = "入选原因", example = "光伏板块的BC电池用光伏绝缘胶开始逐渐放量")
    private String reason;

    @JsonProperty("IsNew")
    @Schema(description = "是否新增", example = "1")
    private Integer isNew;

    @JsonProperty("prod_name")
    @Schema(description = "产品名称", example = "广信材料")
    private String prodName;

    @JsonProperty("Hot")
    @Schema(description = "热度值", example = "999")
    private Integer hot;

    @JsonProperty("FirstShelveTime")
    @Schema(description = "首次上架时间", example = "2024-01-01 10:30:00")
    private String firstShelveTime;

    @JsonProperty("UpdateCacheTime")
    @Schema(description = "缓存更新时间", example = "2024-01-01 10:30:00")
    private String updateCacheTime;
}