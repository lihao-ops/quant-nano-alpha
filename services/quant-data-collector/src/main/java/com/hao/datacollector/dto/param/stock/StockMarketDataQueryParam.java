package com.hao.datacollector.dto.param.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author hli
 * @program datacollector
 * @date 2025-01-15
 * @description 股票行情数据查询参数实体类
 */
@Data
@Schema(name = "股票行情数据查询参数实体类")
public class StockMarketDataQueryParam {
    
    @Schema(description = "股票代码")
    private String windCode;
    
    @Schema(description = "交易日期开始")
    private LocalDate tradeDateStart;
    
    @Schema(description = "交易日期结束")
    private LocalDate tradeDateEnd;
    
    @Schema(description = "股票名称")
    private String windName;

    @Schema(description = "最新概念")
    private String latestconcept;
    
    @Schema(description = "所属产业链板块")
    private String chain;
    
    @Schema(description = "开盘价最小值")
    private BigDecimal openMin;
    
    @Schema(description = "开盘价最大值")
    private BigDecimal openMax;
    
    @Schema(description = "最高价最小值")
    private BigDecimal highMin;
    
    @Schema(description = "最高价最大值")
    private BigDecimal highMax;
    
    @Schema(description = "最低价最小值")
    private BigDecimal lowMin;
    
    @Schema(description = "最低价最大值")
    private BigDecimal lowMax;
    
    @Schema(description = "收盘价最小值")
    private BigDecimal closeMin;
    
    @Schema(description = "收盘价最大值")
    private BigDecimal closeMax;
    
    @Schema(description = "成交量加权平均价最小值")
    private BigDecimal vwapMin;
    
    @Schema(description = "成交量加权平均价最大值")
    private BigDecimal vwapMax;
    
    @Schema(description = "成交量最小值")
    private Long volumeBtinMin;
    
    @Schema(description = "成交量最大值")
    private Long volumeBtinMax;
    
    @Schema(description = "成交额最小值")
    private BigDecimal amountBtinMin;
    
    @Schema(description = "成交额最大值")
    private BigDecimal amountBtinMax;
    
    @Schema(description = "涨跌幅最小值")
    private BigDecimal pctChgMin;
    
    @Schema(description = "涨跌幅最大值")
    private BigDecimal pctChgMax;
    
    @Schema(description = "换手率最小值")
    private BigDecimal turnMin;
    
    @Schema(description = "换手率最大值")
    private BigDecimal turnMax;
    
    @Schema(description = "自由换手率最小值")
    private BigDecimal freeTurnMin;
    
    @Schema(description = "自由换手率最大值")
    private BigDecimal freeTurnMax;
    
    @Schema(description = "振幅最小值")
    private BigDecimal maxupMin;
    
    @Schema(description = "振幅最大值")
    private BigDecimal maxupMax;
    
    @Schema(description = "跌幅最小值")
    private BigDecimal maxdownMin;
    
    @Schema(description = "跌幅最大值")
    private BigDecimal maxdownMax;
    
    @Schema(description = "交易状态")
    private String tradeStatus;
    
    @Schema(description = "总市值最小值")
    private BigDecimal evMin;
    
    @Schema(description = "总市值最大值")
    private BigDecimal evMax;
    
    @Schema(description = "流通市值最小值")
    private BigDecimal mktFreesharesMin;
    
    @Schema(description = "流通市值最大值")
    private BigDecimal mktFreesharesMax;
    
    @Schema(description = "开盘集合竞价价格最小值")
    private BigDecimal openAuctionPriceMin;
    
    @Schema(description = "开盘集合竞价价格最大值")
    private BigDecimal openAuctionPriceMax;
    
    @Schema(description = "开盘集合竞价成交量最小值")
    private Long openAuctionVolumeMin;
    
    @Schema(description = "开盘集合竞价成交量最大值")
    private Long openAuctionVolumeMax;
    
    @Schema(description = "开盘集合竞价成交额最小值")
    private BigDecimal openAuctionAmountMin;
    
    @Schema(description = "开盘集合竞价成交额最大值")
    private BigDecimal openAuctionAmountMax;
    
    @Schema(description = "主力资金买入金额最小值")
    private BigDecimal mfdBuyamtAtMin;
    
    @Schema(description = "主力资金买入金额最大值")
    private BigDecimal mfdBuyamtAtMax;
    
    @Schema(description = "主力资金卖出金额最小值")
    private BigDecimal mfdSellamtAtMin;
    
    @Schema(description = "主力资金卖出金额最大值")
    private BigDecimal mfdSellamtAtMax;
    
    @Schema(description = "主力资金买入量最小值")
    private Long mfdBuyvolAtMin;
    
    @Schema(description = "主力资金买入量最大值")
    private Long mfdBuyvolAtMax;
    
    @Schema(description = "主力资金卖出量最小值")
    private Long mfdSellvolAtMin;
    
    @Schema(description = "主力资金卖出量最大值")
    private Long mfdSellvolAtMax;
    
    @Schema(description = "主力资金净流入最小值")
    private BigDecimal mfdInflowMMin;
    
    @Schema(description = "主力资金净流入最大值")
    private BigDecimal mfdInflowMMax;
    
    @Schema(description = "主力资金净流入占比最小值")
    private BigDecimal mfdInflowproportionMMin;
    
    @Schema(description = "主力资金净流入占比最大值")
    private BigDecimal mfdInflowproportionMMax;
    
    @Schema(description = "技术指标换手率5日最小值")
    private BigDecimal techTurnoverrate5Min;
    
    @Schema(description = "技术指标换手率5日最大值")
    private BigDecimal techTurnoverrate5Max;
    
    @Schema(description = "技术指标换手率10日最小值")
    private BigDecimal techTurnoverrate10Min;
    
    @Schema(description = "技术指标换手率10日最大值")
    private BigDecimal techTurnoverrate10Max;
    
    @Schema(description = "ESG评级")
    private String esgRatingWind;
    
    @Schema(description = "页码", example = "1")
    private Integer pageNo = 1;
    
    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}