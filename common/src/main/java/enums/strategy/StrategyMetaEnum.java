package enums.strategy;

import lombok.Getter;

/**
 * 策略元信息统一管理 (v3)
 * ==========================================
 * ⦿ 统一管理策略ID、名称、类型、说明
 * ⦿ 每种策略可配置单机QPS / 分布式QPS
 * ⦿ 支持动态注册与限流策略识别
 * ==========================================
 */
@Getter
public enum StrategyMetaEnum {

    // ===================== 信号型（Signal） =====================
    SIG_MOMENTUM("SIG_MOM", "动量策略", StrategyType.SIGNAL, "基于价格变化率判断趋势强度的信号策略", 50, 200),

    SIG_MOVING_AVERAGE("SIG_MA", "均线策略", StrategyType.SIGNAL, "基于短期与长期均线交叉判断买卖信号", 50, 200),

    // ===================== 信息型（Information） =====================
    INFO_HOT_TOPIC("INFO_TOPIC", "热点题材策略", StrategyType.INFORMATION, "根据题材热点筛选关联股票集合", 20, 100),

    INFO_SECTOR_STRENGTH("INFO_SECTOR", "行业强度策略", StrategyType.INFORMATION, "计算板块强度指标，作为选股过滤条件", 10, 50),

    // ===================== 复合型（Composite） =====================
    COMBO_BASIC("COMBO_BASIC", "基础复合策略", StrategyType.COMPOSITE, "组合多种子策略实现共振选股", 5, 20),

    COMBO_ADVANCED("COMBO_ADV", "高级复合策略", StrategyType.COMPOSITE, "引入权重、信号融合的多因子复合策略", 3, 10);

    private final String id;
    private final String name;
    private final StrategyType type;
    private final String desc;

    /**
     * 单机限流阈值
     */
    private final int localQps;

    /**
     * 分布式限流阈值
     */
    private final int distributedQps;

    StrategyMetaEnum(String id, String name, StrategyType type,
                     String desc, int localQps, int distributedQps) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.desc = desc;
        this.localQps = localQps;
        this.distributedQps = distributedQps;
    }

    public static StrategyMetaEnum fromId(String id) {
        for (StrategyMetaEnum e : values()) {
            if (e.id.equalsIgnoreCase(id)) {
                return e;
            }
        }
        return null;
    }
}
