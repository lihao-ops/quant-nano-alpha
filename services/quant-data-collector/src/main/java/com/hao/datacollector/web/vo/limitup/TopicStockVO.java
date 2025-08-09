package com.hao.datacollector.web.vo.limitup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "TopicStockVO")
public class TopicStockVO extends BaseVO {
    private static final long serialVersionUID = 1L;


    @Schema(description = "股票代码 1")
    private String windCode;


    @Schema(description = "股票名称 2")
    private String name;


    @Schema(description = "首次涨停时间 3")
    private String firstTime;


    @Schema(description = "连板情况，几天几板 4")
    private String status;


    @Schema(description = "涨停原因 关联的热度最高的两个标签id 5")
    private TopicInfoVO[] topics;


    @Schema(description = "涨停封单额 6")
    private Double orderTotal;


    @Schema(description = "主力流入 7")
    private Double volumeNetin;


    @Schema(description = "流通市值 8")
    private Double listedStock;


    @Schema(description = "当前价格 9")
    private Double price;

// @Schema(description = "换手率 10")
// private Double turnoverRate;

// @Schema(description = "开板时间 11")
// private String openTime;

// @Schema(description = "开板次数 12")
// private Integer openTimes;

// @Schema(description = "涨幅 13")
// private Double changeRange;

// @Schema(description = "时间(HHMMSSmmm)")
// private Integer time;

// @Schema(description = "5分钟涨速")
// private Double changeRange5;

// @Schema(description = "最高5分钟涨速")
// private Double changeRange5Max;

// @Schema(description = "1分钟涨速")
// private Double changeRange1;

// @Schema(description = "最高1分钟涨速")
// private Double changeRange1Max;

// @Schema(description = "竞价开板 涨停价委托买")
// private Double callAuctionOrderTotal;

    /**
     * 连扳情况X系数,
     * x系数意思为第x天前有涨停，0-5之间的整数
     * 1）x=0，则代表前一天有涨停，显示N连板或N天M板
     * 2）0<x<=4,则代表前x天有涨停，显示x天前N连板或x天前N天M板
     * 3）x=5，则代表前五天均没有涨停，则显示未涨停过
     */
    @Schema(description = "连扳情况X系数, x系数意思为第x天前有涨停，0-5之间的整数 1）x=0，则代表前一天有涨停，显示N连板或N天M板 2）0<x<=4,则代表前x天有涨停，显示x天前N连板或x天前N天M板 3）x=5，则代表前五天均没有涨停，则显示未涨停过")
    private Integer limitUpX;

    @Schema(description = "连扳情况N系数,若N=M，则N连板，若N>M，则N天M板")
    private Integer limitUpN;

    @Schema(description = "连扳情况M系数,若N=M，则N连板，若N>M，则N天M板")
    private Integer limitUpM;

// @Schema(description = "计算标签热度和")
// private Double topicHotSum;

// @Schema(description = "低位涨幅")
// private Double lowRange;


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

// @Schema(description = "主力时间")
// private Integer mainForcesTime;


    @Schema(description = "主力撤买")
    private Double mainForcesCB;


    @Schema(description = "主力撤卖")
    private Double mainForcesCS;


    @Schema(description = "主力买入（次)")
    private Integer mainForcesBtimes;


    @Schema(description = "主力卖出（次）")
    private Integer mainForcesStimes;

// @Schema(description = "买入方向")
// private String direction;


    @Schema(description = "买入体量")
    private double buyAvgAmount;


    @Schema(description = "卖出体量")
    private double sellAvgAmount;

}