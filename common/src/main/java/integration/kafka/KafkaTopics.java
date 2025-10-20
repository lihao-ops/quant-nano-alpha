package integration.kafka;

/**
 * Kafka主题枚举统一管理（运行期使用）。
 * 注意：注解中的属性需要编译期常量，仍使用 KafkaConstants。
 */
public enum KafkaTopics {
    QUOTATION("quotation");

    private final String value;

    KafkaTopics(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}