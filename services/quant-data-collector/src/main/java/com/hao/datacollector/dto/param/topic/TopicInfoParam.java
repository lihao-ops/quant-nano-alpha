package com.hao.datacollector.dto.param.topic;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * @author Hao Li
 * @Date 2025-07-24 14:55:02
 * @description: 热门题材主表查询参数对象
 */
@Builder
@Data
@Schema(description = "热门题材主表查询参数对象")
public class TopicInfoParam {

    @Schema(description = "页码", example = "1")
    private Integer pageNo;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize;

    @Schema(description = "题材ID")
    private Long topicId;

    @Schema(description = "题材名称（模糊查询）")
    private String name;

    @Schema(description = "简介（模糊查询）")
    private String briefIntro;

    @Schema(description = "分类层级")
    private String classLayer;

    @Schema(description = "板块开关")
    private String plateSwitch;

    @Schema(description = "股票开关")
    private String stkSwitch;

    @Schema(description = "是否新增：1.是，0.否")
    private Integer isNew;

    @Schema(description = "最小权重")
    private Integer minPower;

    @Schema(description = "最大权重")
    private Integer maxPower;

    @Schema(description = "最小订阅数")
    private Integer minSubscribe;

    @Schema(description = "最大订阅数")
    private Integer maxSubscribe;

    @Schema(description = "最小点赞数")
    private Integer minGoodNum;

    @Schema(description = "最大点赞数")
    private Integer maxGoodNum;

    @Schema(description = "最小评论数")
    private Integer minComNum;

    @Schema(description = "是否点赞：1.是，0.否")
    private Integer isGood;

    @Schema(description = "状态：0.无效，1.有效", example = "1")
    private Integer status = 1;

    @Schema(description = "创建开始时间", example = "2024-01-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTimeStart;

    @Schema(description = "创建结束时间", example = "2024-12-31 23:59:59")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTimeEnd;

    @Schema(description = "题材创建开始时间", example = "2024-01-01 00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date topicCreateTimeStart;

    @Schema(description = "题材创建结束时间", example = "2024-12-31 23:59:59")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date topicCreateTimeEnd;

    @Schema(description = "排序字段：power-权重，goodNum-点赞数，subscribe-订阅数，createTime-创建时间", example = "power")
    private String orderBy = "power";

    @Schema(description = "排序方式：asc-升序，desc-降序", example = "desc")
    private String orderDirection = "desc";
}