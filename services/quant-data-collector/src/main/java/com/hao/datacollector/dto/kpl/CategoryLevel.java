package com.hao.datacollector.dto.kpl;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "分类级别DTO")
public class CategoryLevel {
    @JsonProperty("ID")
    @Schema(description = "分类ID", example = "1534")
    private String id;

    @JsonProperty("Name")
    @Schema(description = "分类名称", example = "材料")
    private String name;

    @JsonProperty("ZSCode")
    @Schema(description = "指数代码")
    private String zsCode;

    @JsonProperty("Stocks")
    @Schema(description = "股票详情列表")
    private List<StockDetail> stocks;

    @JsonProperty("IsNew")
    @Schema(description = "是否新增", example = "1")
    private Integer isNew;

    @JsonProperty("FirstShelveTime")
    @Schema(description = "首次上架时间", example = "2024-01-01 10:30:00")
    private String firstShelveTime;

    @JsonProperty("UpdateCacheTime")
    @Schema(description = "缓存更新时间", example = "2024-01-01 10:30:00")
    private String updateCacheTime;
}