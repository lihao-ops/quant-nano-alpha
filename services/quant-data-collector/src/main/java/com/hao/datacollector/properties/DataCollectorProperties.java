package com.hao.datacollector.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author hli
 * @program: data-collector
 * @Date 2025-08-10 15:54:51
 * @description: 配置类
 */
@Data
//配置批量绑定在nacos下，可以无需@RefreshScope注解就能实现自动刷新
@ConfigurationProperties(prefix = "data-collector")
@Component
public class DataCollectorProperties {
    private String timeout;

    /**
     * 注:在配置文件中配置的是-,则在此使用小驼峰形式对应首字母大写！
     * order:
     * auto-confirm: 71d
     */
    private String autoConfirm;

    private String dbUrl;
}
