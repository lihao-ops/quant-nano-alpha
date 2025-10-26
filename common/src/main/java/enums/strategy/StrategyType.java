package enums.strategy;

import lombok.Getter;

/**
 * 策略类型枚举
 * =======================================
 * 用于区分不同类型的量化策略
 * - SIGNAL: 信号型策略
 * - INFORMATION: 信息型策略
 * - COMPOSITE: 复合型策略
 * =======================================
 *
 * 【信号型策略（Signal Strategy）】
 *   ⦿ 输入行情或指标数据
 *   ⦿ 输出交易信号（BUY/SELL/HOLD）
 *   ⦿ 示例：MomentumStrategy, MovingAverageStrategy
 *
 * 【信息型策略（Information Strategy）】
 *   ⦿ 提供辅助信息或过滤条件
 *   ⦿ 输出股票池或辅助特征数据
 *   ⦿ 示例：HotTopicStrategy, SectorStrengthStrategy
 *
 * 【复合型策略（Composite Strategy）】
 *   ⦿ 组合多个子策略形成综合判断
 *   ⦿ 输出共振信号或综合分值
 *   ⦿ 示例：CompositeStrategy
 *
 * =======================================
 * @author hli
 * @date 2025-10-26
 */
@Getter
public enum StrategyType {

    /**
     * 信号型策略（Signal Strategy）
     * 负责产生直接的交易信号或方向判断。
     */
    SIGNAL("信号型策略"),

    /**
     * 信息型策略（Information Strategy）
     * 负责提供辅助选股、题材过滤、板块强度等信息。
     */
    INFORMATION("信息型策略"),

    /**
     * 复合型策略（Composite Strategy）
     * 用于将多个信号型/信息型策略组合成复合逻辑。
     */
    COMPOSITE("复合型策略");

    /** 策略类型中文描述 */
    private final String desc;

    StrategyType(String desc) {
        this.desc = desc;
    }

}
