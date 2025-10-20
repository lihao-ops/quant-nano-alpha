package integration.kafka;

/**
 * Kafka相关常量统一管理（跨模块复用）
 */
public final class KafkaConstants {
    private KafkaConstants() {}

    // 主题
    public static final String TOPIC_QUOTATION = "quotation";

    // 消费组
    public static final String GROUP_AUDIT_SERVICE = "audit-service-group";

    // Bean名称
    public static final String LISTENER_CONTAINER_FACTORY = "kafkaListenerContainerFactory";
}