package com.hao.riskcontrol.common.enums.market;

import lombok.Getter;

/**
 * @description: 市场指数枚举
 * 计算“综合市场情绪分数”
 * 这是一个核心公式：
 * 综合分数 = (涨跌幅_hs300 * W_hs300) + (涨跌幅_zz500 * W_zz500) + (涨跌幅_cyb * W_cyb) + (涨跌幅_kc50 * W_kc50)
 * <p>
 * 核心思想是：不再单独判断每个指数，而是计算一个“综合市场情绪分数”，用一个分数来代表整个市场的恶劣程度。
 * <p>
 * 第1步：定义“指数日内涨跌幅”
 * 对于每个监控的指数，计算其从昨日收盘到当前时刻的日内涨跌幅。
 * 涨跌幅_i = (当前价_i - 昨日收盘价_i) / 昨日收盘价_i
 * <p>
 * 第2步：定义“指数贡献度”（加权计算）
 * 为每个指数分配一个权重，体现其在你风控体系中的重要程度。例如，采用我们上面建议的权重：
 * W_hs300 = 0.40 (40%)
 * W_zz500 = 0.30 (30%)
 * W_cyb = 0.20 (20%)
 * W_kc50 = 0.10 (10%)
 * 确保所有权重之和为1。
 * <p>
 * 第3步：计算“综合市场情绪分数”
 * 这是一个核心公式：
 * 综合分数 = (涨跌幅_hs300 * W_hs300) + (涨跌幅_zz500 * W_zz500) + (涨跌幅_cyb * W_cyb) + (涨跌幅_kc50 * W_kc50)
 * <p>
 * 这个分数是一个加权平均后的涨跌幅，它比任何一个单一指数都更能代表市场的真实整体状况。
 * <p>
 * 第4步：基于“综合分数”制定风控规则
 * 现在，你的风控规则变得非常清晰和简单：
 * <p>
 * Level 1 风控（预警）：IF 综合分数 < -0.02 THEN ... （市场整体跌2%，发出警报，考虑降低部分仓位）
 * Level 2 风控（轻度）：IF 综合分数 < -0.04 THEN ... （市场整体跌4%，所有策略仓位降低至50%）
 * Level 3 风控（重度）：IF 综合分数 < -0.06 THEN ... （市场整体跌6%，平掉所有仓位，停止交易）
 */

/**
 * 结论与建议
 * 监控组合：强烈建议至少采用 沪深300 + 中证500 + 创业板指 这个组合，它几乎能捕捉到A股所有的系统性风险风格。
 *
 * 权重调整：初始权重按建议设置，之后您可以根据历史回测和自身的风险偏好进行微调。例如，如果你的策略更偏向小盘股，可以适当提高创业板指的权重。
 *
 * 批量接口：为了性能，一定要在data-collect-service中提供一个批量获取指数数据的接口，避免循环调用4次。
 *
 * 可视化：将这个“综合市场情绪分数”在你的监控界面上展示出来，它会是一个非常直观的仪表盘指标。
 */
@Getter
public enum RiskMarketIndexEnum {

    SHANGHAI_COMPOSITE("000001.SH", "上证指数", "上海证券交易所全部上市股票的整体表现", 0.0),
    SHENZHEN_COMPONENT("399001.SZ", "深证成指", "深圳证券交易所市值大、流动性好的500家公司", 0.0),
    CSI_300("000300.SH", "沪深300", "沪深两市规模最大、流动性最好的300只股票", 0.4),
    CSI_500("000905.SH", "中证500", "剔除沪深300后，沪深两市规模最大的500只股票", 0.3),
    CHINEXT("399006.SZ", "创业板指", "创业板市场最具代表性的100家公司", 0.2),
    STAR_50("000688.SH", "科创50", "科创板中市值大、流动性好的50只证券", 0.1),
    SSE_50("000016.SH", "上证50", "上海证券交易所规模最大的50只超级大盘股", 0.0);

    private final String code;
    private final String name;
    private final String description;
    private final double defaultWeight;

    RiskMarketIndexEnum(String code, String name, String description, double defaultWeight) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.defaultWeight = defaultWeight;
    }

    /**
     * 获取所有需要默认加权的指数
     */
    public static RiskMarketIndexEnum[] getWeightedIndices() {
        return new RiskMarketIndexEnum[]{CSI_300, CSI_500, CHINEXT, STAR_50};
    }

    /**
     * 通过代码获取枚举
     */
    public static RiskMarketIndexEnum getByCode(String code) {
        for (RiskMarketIndexEnum index : values()) {
            if (index.getCode().equals(code)) {
                return index;
            }
        }
        return null;
    }
}