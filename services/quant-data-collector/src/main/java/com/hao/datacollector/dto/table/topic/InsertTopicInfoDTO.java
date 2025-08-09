package com.hao.datacollector.dto.table.topic;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "插入热门题材信息DTO")
public class InsertTopicInfoDTO {

    @Schema(description = "主题ID", example = "22", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer topicId;

    @Schema(description = "主题名称", example = "BC电池", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "简介", example = "BC电池是将PN结和金属接触都设于太阳电池背面...")
    private String briefIntro;

    @Schema(description = "分类层级", example = "3")
    private String classLayer;

    @Schema(description = "描述")
    private String desc;

    @Schema(description = "板块开关", example = "2")
    private String plateSwitch;

    @Schema(description = "股票开关", example = "2")
    private String stkSwitch;

    @Schema(description = "详细介绍")
    private String introduction;

    @Schema(description = "主题创建时间")
    private String topicCreateTime;

    @Schema(description = "主题更新时间")
    private String topicUpdateTime;

    @Schema(description = "是否新增：1.是，0.否", example = "1")
    private Integer isNew;

    @Schema(description = "权重", example = "100")
    private Integer power;

    @Schema(description = "订阅数", example = "1000")
    private Integer subscribe;

    @Schema(description = "是否点赞：1.是，0.否", example = "1")
    private Integer isGood;

    @Schema(description = "点赞数", example = "48")
    private Integer goodNum;

    @Schema(description = "评论数", example = "483")
    private Integer comNum;

    @Schema(description = "错误码")
    private String errcode;

    @Schema(description = "时间戳")
    private Double t;

    @JsonProperty("FirstShelveTime")
    @Schema(description = "首次上架时间", example = "2024-01-01 10:30:00")
    private String firstShelveTime;

    @JsonProperty("UpdateCacheTime")
    @Schema(description = "缓存更新时间", example = "2024-01-01 10:30:00")
    private String updateCacheTime;
}