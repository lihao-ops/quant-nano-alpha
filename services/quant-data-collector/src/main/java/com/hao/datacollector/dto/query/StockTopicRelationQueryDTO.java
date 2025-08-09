package com.hao.datacollector.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "股票主题关联查询结果DTO")
public class StockTopicRelationQueryDTO {

    @Schema(description = "交易日期，格式 yyyyMMdd")
    private String tradeDate;

    @Schema(description = "股票万得代码，例如 600519.SH")
    private String windCode;

    @Schema(description = "主题ID")
    private Integer topicId;

    @Schema(description = "主题名称")
    private String topicName;

    @Schema(description = "主题颜色标识")
    private String color;

    @Schema(description = "主题热度")
    private String topicHot;

    @Schema(description = "关联状态，例如 1-有效，0-无效")
    private Integer status;
}