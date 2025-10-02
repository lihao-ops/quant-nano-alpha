package com.hao.strategyengine.alert;

import com.hao.strategyengine.event.StrategyEvent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 告警管理器
 */
@Slf4j
@Component
public class AlertManager {

    private final ConcurrentMap<String, AlertRule> alertRules = new ConcurrentHashMap<>();
    private final List<AlertChannel> channels = new ArrayList<>();

    public AlertManager() {
        // 初始化默认告警规则
        initDefaultRules();
        // 初始化告警通道
        initChannels();
    }

    /**
     * 初始化默认告警规则
     */
    private void initDefaultRules() {
        // 风控拒绝告警
        AlertRule riskRejectRule = new AlertRule();
        riskRejectRule.setName("RISK_REJECT_ALERT");
        riskRejectRule.setEventType(StrategyEvent.EventType.RISK_REJECTED);
        riskRejectRule.setThreshold(5);  // 5次风控拒绝触发告警
        riskRejectRule.setTimeWindowMinutes(10);  // 10分钟窗口
        riskRejectRule.setSeverity(AlertSeverity.HIGH);
        alertRules.put(riskRejectRule.getName(), riskRejectRule);

        // 策略异常告警
        AlertRule errorRule = new AlertRule();
        errorRule.setName("STRATEGY_ERROR_ALERT");
//        errorRule.setEventType(StrategyEvent.EventType.ERROR);
        errorRule.setThreshold(3);
        errorRule.setTimeWindowMinutes(5);
        errorRule.setSeverity(AlertSeverity.CRITICAL);
        alertRules.put(errorRule.getName(), errorRule);

        // 收益异常告警
        AlertRule profitRule = new AlertRule();
        profitRule.setName("PROFIT_ABNORMAL_ALERT");
        profitRule.setThreshold(1);
        profitRule.setSeverity(AlertSeverity.MEDIUM);
        alertRules.put(profitRule.getName(), profitRule);
    }

    /**
     * 初始化告警通道
     */
    private void initChannels() {
        // 添加日志通道
        channels.add(new LogAlertChannel());
        // 可以添加更多通道：邮件、短信、钉钉、企业微信等
        // channels.add(new EmailAlertChannel());
        // channels.add(new DingTalkAlertChannel());
    }

    /**
     * 检查并触发告警
     */
    public void checkAndAlert(StrategyEvent event) {
        alertRules.values().forEach(rule -> {
            if (rule.matches(event)) {
                Alert alert = createAlert(rule, event);
                sendAlert(alert);
            }
        });
    }

    /**
     * 创建告警
     */
    private Alert createAlert(AlertRule rule, StrategyEvent event) {
        Alert alert = new Alert();
        alert.setRuleName(rule.getName());
        alert.setSeverity(rule.getSeverity());
        alert.setTitle(String.format("策略告警: %s", rule.getName()));
        alert.setMessage(String.format(
                "策略: %s, 事件: %s, 详情: %s",
                event.getStrategyName()
//                ,
//            event.getEventType(),
//            event.getMessage()
        ));
        alert.setTimestamp(LocalDateTime.now());
//        alert.setMetadata(event.getMetadata());
        return alert;
    }

    /**
     * 发送告警
     */
    private void sendAlert(Alert alert) {
        log.warn("触发告警: {}", alert);

        for (AlertChannel channel : channels) {
            try {
                channel.send(alert);
            } catch (Exception e) {
                log.error("告警发送失败: channel={}", channel.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * 告警规则
     */
    @Data
    public static class AlertRule {
        private String name;
        private StrategyEvent.EventType eventType;
        private int threshold;
        private int timeWindowMinutes;
        private AlertSeverity severity;
        private int currentCount = 0;
        private LocalDateTime windowStart = LocalDateTime.now();

        public boolean matches(StrategyEvent event) {
//            if (eventType != null && !eventType.equals(event.getEventType())) {
//                return false;
//            }

            // 检查时间窗口
            LocalDateTime now = LocalDateTime.now();
            if (windowStart.plusMinutes(timeWindowMinutes).isBefore(now)) {
                // 重置窗口
                windowStart = now;
                currentCount = 0;
            }

            currentCount++;
            return currentCount >= threshold;
        }
    }

    /**
     * 告警严重级别
     */
    public enum AlertSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * 告警对象
     */
    @Data
    public static class Alert {
        private String ruleName;
        private AlertSeverity severity;
        private String title;
        private String message;
        private LocalDateTime timestamp;
        private Object metadata;
    }

    /**
     * 告警通道接口
     */
    public interface AlertChannel {
        void send(Alert alert);
    }

    /**
     * 日志告警通道
     */
    public static class LogAlertChannel implements AlertChannel {
        @Override
        public void send(Alert alert) {
            log.warn("【{}】{} - {}",
                    alert.getSeverity(),
                    alert.getTitle(),
                    alert.getMessage());
        }
    }
}
