package com.hao.datacollector.dto.table.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "上市公司财务指标插入对象")
public class StockFinancialMetricsInsertDTO {

    @Schema(description = "证券代码", example = "600519.SH")
    private String windCode;

    @Schema(description = "交易日期", example = "2024-05-31")
    private String tradeDate;

    // 股东与机构数据
    @Schema(description = "重要股东二级市场交易区间持仓市值变动(元)")
    private BigDecimal shareholderHoldingsChange;

    @Schema(description = "A股市值(含限售股)(元)")
    private BigDecimal totalMarketCap;

    @Schema(description = "前十大流通股东持股比例合计(%)")
    private BigDecimal top10FloatHoldersRatio;

    @Schema(description = "机构席位买入次数(次)")
    private Double instBuyTimes;

    @Schema(description = "机构股东名称")
    private String instHolderNames;

    @Schema(description = "机构持股数量合计(股)")
    private Double instHoldingsTotal;

    @Schema(description = "机构股东类型")
    private String instHolderTypes;

    // 估值指标
    @Schema(description = "市盈率PE(TTM)(倍)")
    private BigDecimal peTtm;

    @Schema(description = "综合评级(数值)")
    private BigDecimal ratingScore;

    @Schema(description = "综合评级(中文)")
    private String ratingText;

    @Schema(description = "评级机构家数")
    private Double ratingAgencyCount;

    @Schema(description = "一致预测目标价(元)")
    private BigDecimal targetPrice;

    @Schema(description = "市净率PB(倍)")
    private BigDecimal pb;

    @Schema(description = "市盈率PE(倍)")
    private BigDecimal pe;

    @Schema(description = "市销率PS")
    private BigDecimal ps;

    // 财务指标
    @Schema(description = "净利润(TTM)(元)")
    private BigDecimal netProfitTtm;

    @Schema(description = "净资产收益率3年增长率(%)")
    private BigDecimal roeGrowth3y;

    @Schema(description = "股息率(近12个月)(%)")
    private BigDecimal dividendYield;

    @Schema(description = "Wind ESG综合得分")
    private BigDecimal esgScore;

    @Schema(description = "发明专利个数(个)")
    private Double patentCount;

    @Schema(description = "ESG争议事件得分")
    private BigDecimal esgControversyScore;

    @Schema(description = "总资产净利率ROA(%)")
    private BigDecimal roa;

    @Schema(description = "净利润/营业总收入(TTM)(%)")
    private BigDecimal netProfitMargin;

    // 技术指标
    @Schema(description = "均线多空头排列看涨看跌")
    private String maTrend;

    @Schema(description = "RSI相对强弱指标")
    private BigDecimal rsi6;
}
