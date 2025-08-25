package enums;

import lombok.Getter;

/**
 * @description: speed指标枚举
 */
@Getter
public enum SpeedIndicatorEnum {
    CHG(81, "涨跌幅"),
    NEW_PRICE(3, "最新价"),
    NET_INFLOW_AMOUNT_ONE_DAY(390, "1日净流入额"),
    NET_INFLOW_AMOUNT_FIVE_DAY(392, "5日净流入额"),
    NET_INFLOW_AMOUNT_TEN_DAY(395, "10日净流入额"),
    NET_INFLOW_AMOUNT_TWENTY_DAY(398, "20日净流入额"),
    INSTITUTION_BUY_AMOUNT_CURRENT_DAY(463, "当日机构买入成交额"),
    INSTITUTION_SELL_AMOUNT_CURRENT_DAY(464, "当日机构卖出成交额"),
    LARGE_INVESTOR_BUY_AMOUNT_CURRENT_DAY(465, "当日大户买入成交额"),
    LARGE_INVESTOR_SELL_AMOUNT_CURRENT_DAY(466, "当日大户卖出成交额"),
    MEDIUM_INVESTOR_BUY_AMOUNT_CURRENT_DAY(467, "当日中户买入成交额"),
    MEDIUM_INVESTOR_SELL_AMOUNT_CURRENT_DAY(468, "当日中户卖出成交额"),
    RETAIL_INVESTOR_BUY_AMOUNT_CURRENT_DAY(469, "当日散户买入成交额"),
    RETAIL_INVESTOR_SELL_AMOUNT_CURRENT_DAY(470, "当日散户卖出成交额"),
    CHANGE_HAND_RATE(187, "换手率"),
    FLUCTUATION(190, "振幅"),
    CHANGE_FIVE_DAYS(191, "5个交易日涨幅"),
    CHANGE_TEN_DAYS(192, "10个交易日涨幅"),
    CHANGE_VALUE(80, "涨跌"),
    SHORT_NAME_CHS(131, "中文简称"),
    PE_TTM(205, "市盈率"),
    PB(182, "市净率LF"),
    CAPITAL_MARKET_VALUE(198, "总市值"),
    TOTAL_AMOUNT(59, "总成交额"),
    CHG_TWO_HUNDRED_FIFTY_DAY(196, "250日涨跌幅"),
    HIGH_PRICE_FIFTY_TWO_WEEK(200, "52周最高价"),
    LOW_PRICE_FIFTY_TWO_WEEK(201, "52周最低价"),
    TRADE_DATE(1, "交易日期"),
    TRADE_TIME(2, "交易时间"),
    RISE_COUNT(217, "上涨家数"),
    FALL_COUNT(218, "下跌家数"),
    LIMIT_UP_COUNT(483, "涨停数"),
    DIVIDEND_YIELD(879, "股息率"),
    PREVIOUS_DAY_CLOSE_PRICE(83, "前一日收盘价"),
    PREVIOUS_SETTLEMENT_PRICE(75, "前结算价"),
    OPEN_INTEREST(76, "持仓量"),
    CURRENT_VOLUME(10, "现量"),
    HIGH_PRICE(6, "最高价"),
    LOW_PRICE(7, "最低价"),
    TOTAL_VOLUME(8, "总成交量"),
    POSITION_INCREASE(210, "增仓"),
    PRICE_UNIT(136, "价格单位（量纲）"),
    SOUTHBOUND_NET_PURCHASE_AMOUNT(877, "南向资金净买入额"),
    CHANGE_YEAR_BEGIN(197, "年初至今涨跌幅");

    private int indicator;

    private String desc;

    SpeedIndicatorEnum(int indicator, String desc) {
        this.indicator = indicator;
        this.desc = desc;
    }

    public int getIndicator() {
        return indicator;
    }

    public void setIndicator(int indicator) {
        this.indicator = indicator;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getIndicatorStr() {
        return indicator + "";
    }
}