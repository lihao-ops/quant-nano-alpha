package com.hao.datacollector.config;

import ch.qos.logback.classic.LoggerContext;
import com.github.danielwegener.logback.kafka.KafkaAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 解决logback先启动，未读取到Nacos配置的Kafka服务器地址问题
 * 会在Spring Boot启动完成后（Nacos配置已加载）重新设置KafkaAppender的bootstrap.servers
 */
@Component
@RefreshScope
//todo 配置未生效
public class KafkaLogbackConfig implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(KafkaLogbackConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaServerUrl;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            KafkaAppender kafkaAppender = (KafkaAppender) loggerContext.getLogger("ROOT").getAppender("kafkaAppender");

            if (kafkaAppender != null) {
                kafkaAppender.stop();  // 先停止，保证可以重新设置

                // 重新添加配置，会覆盖原来的 bootstrap.servers
                kafkaAppender.addProducerConfig("bootstrap.servers=" + kafkaServerUrl);
                kafkaAppender.addProducerConfig("acks=0");
                kafkaAppender.addProducerConfig("linger.ms=1000");
                kafkaAppender.addProducerConfig("max.block.ms=0");

                kafkaAppender.start();

                logger.info("KafkaAppender bootstrap.servers 已更新: {}", kafkaServerUrl);
            } else {
                logger.warn("KafkaAppender未找到，请确认logback配置中是否有名为 'kafkaAppender' 的appender");
            }
        } catch (Exception e) {
            logger.error("更新KafkaAppender配置失败", e);
        }
    }
}
