package com.hao.datacollector.dto.param.limitup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * @author hli
 * @program datacollector
 * @date 2025-01-15
 * @description 涨停股票查询参数实体类
 */
@Data
@Schema(name = "涨停股票查询参数实体类")
public class LimitUpStockQueryParam {

    @Schema(description = "股票代码")
    private String windCode;

    @Schema(description = "交易日期开始")
    private LocalDate tradeDateStart;

    @Schema(description = "交易日期结束")
    private LocalDate tradeDateEnd;

    @Schema(description = "股票名称")
    private String windName;

    @Schema(description = "首次涨停时间开始(HHMMSS格式)")
    private String firstTimeStart;

    @Schema(description = "首次涨停时间结束(HHMMSS格式)")
    private String firstTimeEnd;

    @Schema(description = "连板情况")
    private String limitStatus;

    @Schema(description = "流通市值最小值")
    private Double listedStockMin;

    @Schema(description = "流通市值最大值")
    private Double listedStockMax;

    @Schema(description = "涨停封单额最小值")
    private Double orderTotalMin;

    @Schema(description = "涨停封单额最大值")
    private Double orderTotalMax;

    @Schema(description = "主力流入最小值")
    private Double volumeNetInMin;

    @Schema(description = "主力流入最大值")
    private Double volumeNetInMax;

    @Schema(description = "当前价格最小值")
    private Double priceMin;

    @Schema(description = "当前价格最大值")
    private Double priceMax;

    @Schema(description = "连扳情况X系数")
    private Integer limitUpX;

    @Schema(description = "连扳情况N系数最小值")
    private Integer limitUpNMin;

    @Schema(description = "连扳情况N系数最大值")
    private Integer limitUpNMax;

    @Schema(description = "连扳情况M系数最小值")
    private Integer limitUpMMin;

    @Schema(description = "连扳情况M系数最大值")
    private Integer limitUpMMax;

    @Schema(description = "主力强度最小值")
    private Double mainForcesMin;

    @Schema(description = "主力强度最大值")
    private Double mainForcesMax;

    @Schema(description = "主力成本最小值")
    private Double costMin;

    @Schema(description = "主力成本最大值")
    private Double costMax;

    @Schema(description = "主力浮盈最小值")
    private Double profitMin;

    @Schema(description = "主力浮盈最大值")
    private Double profitMax;

    @Schema(description = "主力流入最小值")
    private Double mainForcesInMin;

    @Schema(description = "主力流入最大值")
    private Double mainForcesInMax;

    @Schema(description = "主力分歧最小值")
    private Double divergencyMin;

    @Schema(description = "主力分歧最大值")
    private Double divergencyMax;

    @Schema(description = "主力撤买最小值")
    private Double mainForcesCBMin;

    @Schema(description = "主力撤买最大值")
    private Double mainForcesCBMax;

    @Schema(description = "主力撤卖最小值")
    private Double mainForcesCSMin;

    @Schema(description = "主力撤卖最大值")
    private Double mainForcesCSMax;

    @Schema(description = "主力买入次数最小值")
    private Integer mainForcesNtimesMin;

    @Schema(description = "主力买入次数最大值")
    private Integer mainForcesNtimesMax;

    @Schema(description = "主力卖出次数最小值")
    private Integer mainForcesStimesMin;

    @Schema(description = "主力卖出次数最大值")
    private Integer mainForcesStimesMax;

    @Schema(description = "买入体量最小值")
    private Double buyAvgAmountMin;

    @Schema(description = "买入体量最大值")
    private Double buyAvgAmountMax;

    @Schema(description = "卖出体量最小值")
    private Double sellAvgAmountMin;

    @Schema(description = "卖出体量最大值")
    private Double sellAvgAmountMax;

    @Schema(description = "标签ID")
    private Integer topicId;

    @Schema(description = "标签名称")
    private String topicName;

    @Schema(description = "标签颜色")
    private String color;

    @Schema(description = "标签股票数量最小值")
    private Integer stockNumMin;

    @Schema(description = "标签股票数量最大值")
    private Integer stockNumMax;

    @Schema(description = "标签热度最小值")
    private Double topicHotMin;

    @Schema(description = "标签热度最大值")
    private Double topicHotMax;

    @Schema(description = "页码", example = "1")
    private Integer pageNo;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize;
}