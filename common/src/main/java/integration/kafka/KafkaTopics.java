package integration.kafka;

import java.util.Arrays;
import java.util.Optional;

/**
 * Kafka 主题枚举（运行期使用的语义化元数据）。
 * 说明：
 * - 注解（如 @KafkaListener）的属性需要编译期常量，请使用 KafkaConstants 中的常量；
 * - 业务代码在运行期可使用本枚举提供的 displayName/desc/category 等元信息。
 */
public enum KafkaTopics {
    /** 行情报价主题（生产者侧发布，供策略/审计消费） */
    QUOTATION("quotation", "分时行情", "行情数据主题，供策略/风控/日志模块消费", Category.BOTH),

    /** 各服务日志主题 */
    LOG_QUANT_XXL_JOB("log-quant-xxl-job", "调度中心日志", "quant-xxl-job 服务运行日志与调度事件", Category.BOTH),
    LOG_QUANT_DATA_COLLECTOR("log-quant-data-collector", "数据采集服务日志", "quant-data-collector 服务运行日志与采集任务记录", Category.BOTH),
    LOG_QUANT_STRATEGY_ENGINE("log-quant-strategy-engine", "策略引擎服务日志", "quant-strategy-engine 服务运行日志与策略执行记录", Category.BOTH),
    LOG_QUANT_RISK_CONTROL("log-quant-risk-control", "风控服务日志", "quant-risk-control 服务运行日志与风控事件", Category.BOTH),
    LOG_QUANT_AUDIT_SERVICE("log-quant-audit-service", "审计服务日志", "quant-audit-service 服务运行日志与审计事件", Category.BOTH);

    /** 实际 Kafka 主题编码（物理名称） */
    private final String code;
    /** 展示名称（人类可读） */
    private final String displayName;
    /** 主题描述（用途说明） */
    private final String desc;
    /** 主题类别：生产/消费/两者兼有 */
    private final Category category;

    KafkaTopics(String code, String displayName, String desc, Category category) {
        this.code = code;
        this.displayName = displayName;
        this.desc = desc;
        this.category = category;
    }

    public String code() { return code; }
    public String displayName() { return displayName; }
    public String desc() { return desc; }
    public Category category() { return category; }

    /** 是否用于生产端（发布消息） */
    public boolean isProducer() { return category == Category.PRODUCER || category == Category.BOTH; }
    /** 是否用于消费端（处理消息） */
    public boolean isConsumer() { return category == Category.CONSUMER || category == Category.BOTH; }

    /** 根据物理主题名查找枚举 */
    public static Optional<KafkaTopics> fromCode(String code) {
        return Arrays.stream(values()).filter(t -> t.code.equals(code)).findFirst();
    }

    /** 主题类别定义：生产者/消费者/两者兼有 */
    public enum Category {
        PRODUCER,
        CONSUMER,
        BOTH
    }
}