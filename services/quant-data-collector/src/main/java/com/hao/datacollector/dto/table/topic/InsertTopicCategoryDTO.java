package com.hao.datacollector.dto.table.topic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "插入题材类别信息DTO")
public class InsertTopicCategoryDTO {

    @Schema(description = "分类ID", example = "1534", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer categoryId;

    @Schema(description = "所属题材表id", example = "25", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer topicId;

    @Schema(description = "分类名称", example = "材料", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "指数代码")
    private String zsCode;

    @Schema(description = "是否新增：1.是，0.否", example = "1")
    private Integer isNew;

    @Schema(description = "首次上架时间", example = "2024-01-01 10:30:00")
    private String firstShelveTime;

    @Schema(description = "缓存更新时间", example = "2024-01-01 10:30:00")
    private String updateCacheTime;

    @Schema(description = "父节点ID，0表示一级节点", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer parentId;
}