package com.hao.datacollector.web.vo.topic;

import com.hao.datacollector.dto.table.topic.InsertStockCategoryMappingDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author Hao Li
 * @Date 2025-07-24 16:59:25
 * @description: 题材版本包含所属股票代码VO对象
 */
@Data
public class TopicCategoryAndStockVO {
    @Schema(description = "数据总量", example = "100")
    private Integer totalNum;

    @Schema(description = "主题ID", example = "22", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer topicId;
    @Schema(description = "主题名称", example = "BC电池", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;


    @Schema(description = "分类ID", example = "1534", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer categoryId;
    @Schema(description = "分类名称(name)", example = "材料", requiredMode = Schema.RequiredMode.REQUIRED)
    private String categoryName;
    @Schema(description = "指数代码(zsCode)")
    private String categoryZsCode;
    @Schema(description = "是否新增：1.是，0.否(IsNew)", example = "1")
    private Integer categoryIsNew;
    @Schema(description = "首次上架时间(firstShelveTime)", example = "2024-01-01 10:30:00")
    private String categoryFirstShelveTime;
    @Schema(description = "缓存更新时间(updateCacheTime)", example = "2024-01-01 10:30:00")
    private String categoryUpdateCacheTime;
    @Schema(description = "父节点ID，0表示一级节点", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer parentId;

    @Schema(description = "题材版本信息和所属股票信息List")
    private List<InsertStockCategoryMappingDTO> stockCategoryList;
}