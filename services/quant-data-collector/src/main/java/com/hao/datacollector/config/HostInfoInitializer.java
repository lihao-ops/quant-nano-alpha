package com.hao.datacollector.config;

import integration.kafka.KafkaTopics;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;

@Configuration
public class HostInfoInitializer {

    @Bean
    public ApplicationRunner hostInfoInitializerRunner() {
        return args -> {
            try {
                InetAddress local = InetAddress.getLocalHost();
                System.setProperty("LOG_TOPIC", KafkaTopics.LOG_QUANT_DATA_COLLECTOR.code());
                System.setProperty("HOST_NAME", local.getHostName());
                System.setProperty("HOST_IP", local.getHostAddress());
            } catch (Exception e) {
                System.setProperty("HOST_NAME", "unknown");
                System.setProperty("HOST_IP", "unknown");
            }
        };
    }
}