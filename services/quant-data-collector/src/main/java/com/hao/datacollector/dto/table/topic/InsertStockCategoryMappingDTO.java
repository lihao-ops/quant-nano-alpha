package com.hao.datacollector.dto.table.topic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "插入股票题材类别映射DTO")
public class InsertStockCategoryMappingDTO {

    @Schema(description = "股票代码(需转换后缀)", example = "300537.SH", requiredMode = Schema.RequiredMode.REQUIRED)
    private String windCode;

    @Schema(description = "股票名称", example = "广信材料")
    private String windName;

    @Schema(description = "所属类别ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer categoryId;

    @Schema(description = "是否主做：1.是，0.否", example = "1")
    private String isZz;

    @Schema(description = "是否热门：1.是，0.否", example = "1")
    private String isHot;

    @Schema(description = "入选原因", example = "光伏板块的BC电池用光伏绝缘胶开始逐渐放量")
    private String reason;

    @Schema(description = "是否新增：1.是，0.否", example = "1")
    private Integer isNew;

    @Schema(description = "热度值", example = "999")
    private Integer hot;

    @Schema(description = "首次上架时间", example = "2024-01-01 10:30:00")
    private String firstShelveTime;

    @Schema(description = "缓存更新时间", example = "2024-01-01 10:30:00")
    private String updateCacheTime;
}