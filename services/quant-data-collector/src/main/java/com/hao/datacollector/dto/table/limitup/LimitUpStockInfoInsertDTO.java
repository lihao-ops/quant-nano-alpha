package com.hao.datacollector.dto.table.limitup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;

@Data
@Schema(name = "涨停股票信息表对象")
public class LimitUpStockInfoInsertDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "股票代码")
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

    @Schema(description = "连扳情况X系数, x系数意思为第x天前有涨停，0-5之间的整数 1）x=0，则代表前一天有涨停，显示N连板或N天M板 2）0<x<=4,则代表前x天有涨停，显示x天前N连板或x天前N天M板 3）x=5，则代表前五天均没有涨停，则显示未涨停过")
    private Integer limitUpX;

    @Schema(description = "连扳情况N系数,若N=M，则N连板，若N>M，则N天M板")
    private Integer limitUpN;

    @Schema(description = "连扳情况M系数,若N=M，则N连板，若N>M，则N天M板")
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
}