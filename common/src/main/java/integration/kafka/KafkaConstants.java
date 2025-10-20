package integration.kafka;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kafka 相关常量统一管理（跨模块复用）。
 * 设计原则：
 * - 编译期常量：用于注解（如 @KafkaListener）和硬编码配置，确保编译期常量要求；
 * - 运行期元数据：通过 TopicMeta 提供 name/desc/category 等语义信息，便于业务层统一使用；
 * - 主题与元数据分离：字符串常量是“权威编码”，元数据用于展示与治理，不影响编译约束。
 */
public final class KafkaConstants {
    private KafkaConstants() {}

    // ======================== 编译期常量（注解/配置使用） ========================
    // 主题编码（业务数据）
    public static final String TOPIC_QUOTATION = "quotation";

    // 主题编码（各服务日志流）
    public static final String TOPIC_LOG_SERVICE_ORDER = "log-service-order";
    public static final String TOPIC_LOG_QUANT_XXL_JOB = "log-quant-xxl-job";
    public static final String TOPIC_LOG_QUANT_DATA_COLLECTOR = "log-quant-data-collector";
    public static final String TOPIC_LOG_QUANT_STRATEGY_ENGINE = "log-quant-strategy-engine";
    public static final String TOPIC_LOG_QUANT_RISK_CONTROL = "log-quant-risk-control";
    public static final String TOPIC_LOG_QUANT_AUDIT_SERVICE = "log-quant-audit-service";

    // 消费组（示例：审计服务）
    public static final String GROUP_AUDIT_SERVICE = "audit-service-group";
    // 可按需扩展：public static final String GROUP_STRATEGY_ENGINE = "strategy-engine-group";

    // Bean 名称
    public static final String LISTENER_CONTAINER_FACTORY = "kafkaListenerContainerFactory";

    // ======================== 运行期元数据（展示/治理） ========================
    /** 主题元数据对象：封装 code/name/desc/category 等信息 */
    public static final class TopicMeta {
        private final String code;
        private final String displayName;
        private final String desc;
        private final KafkaTopics.Category category;

        public TopicMeta(String code, String displayName, String desc, KafkaTopics.Category category) {
            this.code = code;
            this.displayName = displayName;
            this.desc = desc;
            this.category = category;
        }
        public String code() { return code; }
        public String displayName() { return displayName; }
        public String desc() { return desc; }
        public KafkaTopics.Category category() { return category; }
        public boolean isProducer() { return category == KafkaTopics.Category.PRODUCER || category == KafkaTopics.Category.BOTH; }
        public boolean isConsumer() { return category == KafkaTopics.Category.CONSUMER || category == KafkaTopics.Category.BOTH; }
    }

    /**
     * 主题元数据注册表（按物理主题名索引）。
     * 注意：此为运行期数据，供业务层使用；注解仍引用上面的编译期字符串常量。
     */
    public static final Map<String, TopicMeta> TOPIC_META_REGISTRY;
    static {
        Map<String, TopicMeta> m = new LinkedHashMap<>();
        // 业务主题
        m.put(TOPIC_QUOTATION, new TopicMeta(
                TOPIC_QUOTATION,
                "行情报价",
                "行情数据主题，供策略/审计消费",
                KafkaTopics.Category.PRODUCER
        ));
        // 日志主题：各服务产生应用日志
        m.put(TOPIC_LOG_SERVICE_ORDER, new TopicMeta(
                TOPIC_LOG_SERVICE_ORDER,
                "服务订单日志",
                "service-order 服务运行日志消息，用于集中收集与分析",
                KafkaTopics.Category.BOTH
        ));
        m.put(TOPIC_LOG_QUANT_XXL_JOB, new TopicMeta(
                TOPIC_LOG_QUANT_XXL_JOB,
                "调度中心日志",
                "quant-xxl-job 服务运行日志与调度事件",
                KafkaTopics.Category.BOTH
        ));
        m.put(TOPIC_LOG_QUANT_DATA_COLLECTOR, new TopicMeta(
                TOPIC_LOG_QUANT_DATA_COLLECTOR,
                "数据采集服务日志",
                "quant-data-collector 服务运行日志与采集任务记录",
                KafkaTopics.Category.BOTH
        ));
        m.put(TOPIC_LOG_QUANT_STRATEGY_ENGINE, new TopicMeta(
                TOPIC_LOG_QUANT_STRATEGY_ENGINE,
                "策略引擎服务日志",
                "quant-strategy-engine 服务运行日志与策略执行记录",
                KafkaTopics.Category.BOTH
        ));
        m.put(TOPIC_LOG_QUANT_RISK_CONTROL, new TopicMeta(
                TOPIC_LOG_QUANT_RISK_CONTROL,
                "风控服务日志",
                "quant-risk-control 服务运行日志与风控事件",
                KafkaTopics.Category.BOTH
        ));
        m.put(TOPIC_LOG_QUANT_AUDIT_SERVICE, new TopicMeta(
                TOPIC_LOG_QUANT_AUDIT_SERVICE,
                "审计服务日志",
                "quant-audit-service 服务运行日志与审计事件",
                KafkaTopics.Category.BOTH
        ));
        TOPIC_META_REGISTRY = Collections.unmodifiableMap(m);
    }
}