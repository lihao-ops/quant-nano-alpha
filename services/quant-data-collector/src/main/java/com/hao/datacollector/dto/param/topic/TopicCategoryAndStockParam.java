package com.hao.datacollector.dto.param.topic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Schema(description = "题材分类及股票映射查询参数对象")
@Data
@Builder
public class TopicCategoryAndStockParam {
    @Schema(description = "页码，从1开始", example = "1")
    private Integer pageNo;
    @Schema(description = "每页大小", example = "10")
    private Integer pageSize;

    @Schema(description = "主题ID", example = "22")
    private Integer topicId;
    @Schema(description = "主题名称（模糊查询）", example = "BC电池")
    private String topicName;

    @Schema(description = "类别id", example = "1534")
    private Integer categoryId;
    @Schema(description = "分类名称（模糊查询）", example = "材料")
    private String categoryName;
    @Schema(description = "父类别id", example = "1534")
    private Integer parentCategoryId;
    @Schema(description = "分类表指数代码", example = "880880")
    private String categoryZsCode;

    @Schema(description = "股票代码", example = "300537.SH")
    private String windCode;
    @Schema(description = "股票名称（模糊查询）", example = "广信材料")
    private String windName;
}