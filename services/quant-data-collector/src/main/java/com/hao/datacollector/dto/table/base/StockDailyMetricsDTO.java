package com.hao.datacollector.dto.table.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Date;

@Data
@Schema(description = "A股日频行情指标DTO")
public class StockDailyMetricsDTO {

    @Schema(description = "交易日期", example = "2024-06-01", required = true)
    private Date tradeDate;

    @Schema(description = "Wind证券代码", example = "000001.SZ", required = true)
    private String windcode;

    @Schema(description = "证券简称", example = "平安银行")
    private String secName;

    @Schema(description = "所属最新概念", example = "数字货币;金融科技")
    private String latestconcept;

    @Schema(description = "所属产业链板块", example = "金融服务")
    private String chain;

    @Schema(description = "开盘价", example = "12.34")
    private BigDecimal open;

    @Schema(description = "最高价", example = "13.00")
    private BigDecimal high;

    @Schema(description = "最低价", example = "12.00")
    private BigDecimal low;

    @Schema(description = "收盘价", example = "12.80")
    private BigDecimal close;

    @Schema(description = "均价", example = "12.60")
    private BigDecimal vwap;

    @Schema(description = "成交量(含大宗交易,股)", example = "12345678")
    private Long volumeBtin;

    @Schema(description = "成交额(含大宗交易,元)", example = "123456789.00")
    private BigDecimal amountBtin;

    @Schema(description = "涨跌幅(%)", example = "2.56")
    private BigDecimal pctChg;

    @Schema(description = "换手率(%)", example = "3.25")
    private BigDecimal turn;

    @Schema(description = "自由流通股本换手率(%)", example = "1.75")
    private BigDecimal freeTurn;

    @Schema(description = "涨停价", example = "13.80")
    private BigDecimal maxup;

    @Schema(description = "跌停价", example = "11.50")
    private BigDecimal maxdown;

    @Schema(description = "交易状态", example = "交易")
    private String tradeStatus;

    @Schema(description = "总市值(元)", example = "5000000000.00")
    private BigDecimal ev;

    @Schema(description = "自由流通市值(元)", example = "3000000000.00")
    private BigDecimal mktFreeshares;

    @Schema(description = "开盘集合竞价成交价", example = "12.10")
    private BigDecimal openAuctionPrice;

    @Schema(description = "开盘集合竞价成交量(股)", example = "123456")
    private Long openAuctionVolume;

    @Schema(description = "开盘集合竞价成交额(元)", example = "1500000.00")
    private BigDecimal openAuctionAmount;

    @Schema(description = "主动买入额(元)", example = "2500000.00")
    private BigDecimal mfdBuyamtAt;

    @Schema(description = "主动卖出额(元)", example = "2000000.00")
    private BigDecimal mfdSellamtAt;

    @Schema(description = "主动买入量(股)", example = "50000")
    private Long mfdBuyvolAt;

    @Schema(description = "主动卖出量(股)", example = "45000")
    private Long mfdSellvolAt;

    @Schema(description = "主力净流入额(元)", example = "500000.00")
    private BigDecimal mfdInflowM;

    @Schema(description = "主力净流入额占比(%)", example = "3.50")
    private BigDecimal mfdInflowproportionM;

    @Schema(description = "5日平均换手率(%)", example = "2.45")
    private BigDecimal techTurnoverrate5;

    @Schema(description = "10日平均换手率(%)", example = "2.10")
    private BigDecimal techTurnoverrate10;

    @Schema(description = "ESG评级", example = "A")
    private String esgRatingWind;
}
