package com.hao.datacollector.web.vo.limitup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author hli
 * @program datacollector
 * @date 2025-01-15
 * @description 涨停股票查询结果传输对象
 */
@Data
@Schema(name = "涨停股票查询结果传输对象")
public class LimitUpStockQueryResultVO {

    @Schema(description = "股票代码", required = true)
    private String windCode;

    @Schema(description = "交易日期")
    private LocalDate tradeDate;

    @Schema(description = "股票名称")
    private String windName;

    @Schema(description = "首次涨停时间(HHMMSS格式)")
    private String firstTime;

    @Schema(description = "连板情况，几天几板")
    private String limitStatus;

    @Schema(description = "流通市值")
    private Double listedStock;

    @Schema(description = "涨停封单额")
    private Double orderTotal;

    @Schema(description = "主力流入")
    private Double volumeNetIn;

    @Schema(description = "当前价格")
    private Double price;

    @Schema(description = "连扳情况X系数")
    private Integer limitUpX;

    @Schema(description = "连扳情况N系数")
    private Integer limitUpN;

    @Schema(description = "连扳情况M系数")
    private Integer limitUpM;

    @Schema(description = "主力强度")
    private Double mainForces;

    @Schema(description = "主力成本")
    private Double cost;

    @Schema(description = "主力浮盈")
    private Double profit;

    @Schema(description = "主力流入")
    private Double mainForcesIn;

    @Schema(description = "主力分歧")
    private Double divergency;

    @Schema(description = "主力撤买")
    private Double mainForcesCB;

    @Schema(description = "主力撤卖")
    private Double mainForcesCS;

    @Schema(description = "主力买入次数")
    private Integer mainForcesNtimes;

    @Schema(description = "主力卖出次数")
    private Integer mainForcesStimes;

    @Schema(description = "买入体量")
    private Double buyAvgAmount;

    @Schema(description = "卖出体量")
    private Double sellAvgAmount;

    @Schema(description = "标签ID")
    private Integer topicId;

    @Schema(description = "标签名称")
    private String topicName;

    @Schema(description = "标签颜色")
    private String color;

    @Schema(description = "标签股票数量")
    private Integer stockNum;

    @Schema(description = "标签热度")
    private Double topicHot;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}