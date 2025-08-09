package com.hao.datacollector.dto.kpl;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "热门主题(开盘啦)DTO")
public class HotTopicKpl {
    @JsonProperty("ID")
    @Schema(description = "主题ID", example = "22")
    private String id;

    @JsonProperty("Name")
    @Schema(description = "主题名称", example = "BC电池")
    private String name;

    @JsonProperty("BriefIntro")
    @Schema(description = "简介", example = "BC电池是将PN结和金属接触都设于太阳电池背面...")
    private String briefIntro;

    @JsonProperty("ClassLayer")
    @Schema(description = "分类层级", example = "3")
    private String classLayer;

    @JsonProperty("Desc")
    @Schema(description = "描述")
    private String desc;

    @JsonProperty("PlateSwitch")
    @Schema(description = "板块开关", example = "2")
    private String plateSwitch;

    @JsonProperty("StkSwitch")
    @Schema(description = "股票开关", example = "2")
    private String stkSwitch;

    @JsonProperty("Introduction")
    @Schema(description = "详细介绍")
    private String introduction;

    @JsonProperty("CreateTime")
    @Schema(description = "创建时间")
    private String createTime;

    @JsonProperty("UpdateTime")
    @Schema(description = "更新时间")
    private String updateTime;

    @JsonProperty("Table")
    @Schema(description = "主题分类表")
    private List<TopicTable> table;

    @JsonProperty("Stocks")
    @Schema(description = "股票列表")
    private List<Object> stocks;

    @JsonProperty("StockList")
    @Schema(description = "股票信息列表")
    private List<StockInfo> stockList;

    @JsonProperty("IsNew")
    @Schema(description = "是否新增", example = "1")
    private Integer isNew;

    @JsonProperty("Power")
    @Schema(description = "权重", example = "100")
    private Integer power;

    @JsonProperty("Subscribe")
    @Schema(description = "订阅数", example = "1000")
    private Integer subscribe;

    @JsonProperty("ZT")
    @Schema(description = "涨停相关数据")
    private Object zt;

    @JsonProperty("IsGood")
    @Schema(description = "是否点赞", example = "1")
    private Integer isGood;

    @JsonProperty("GoodNum")
    @Schema(description = "点赞数", example = "48")
    private Integer goodNum;

    @JsonProperty("ComNum")
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