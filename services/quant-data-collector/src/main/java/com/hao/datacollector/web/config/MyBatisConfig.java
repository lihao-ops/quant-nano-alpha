package com.hao.datacollector.web.config;

import com.hao.datacollector.web.handler.MyBatisSlowSqlInterceptor;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置类
 *
 * <p>功能：
 * 1. 注册自定义 MyBatis 拦截器，用于 SQL 执行监控（慢 SQL、执行耗时）。
 * 2. 通过 ConfigurationCustomizer 将拦截器添加到 MyBatis 配置中。
 *
 * <p>使用说明：
 * - slowSqlInterceptor() 创建自定义拦截器 Bean，拦截所有 MyBatis 查询和更新 SQL。
 * - configurationCustomizer() 将拦截器注册到 MyBatis，全局生效。
 *
 * <p>扩展建议：
 * - 可添加多个拦截器，例如分页插件、性能分析插件等。
 * - 可以通过 Spring 配置文件动态修改慢 SQL 阈值。
 */
@Configuration
public class MyBatisConfig {

    /**
     * 创建 MyBatis 自定义拦截器 Bean
     *
     * @return MyBatisSlowSqlInterceptor 实例
     */
    @Bean
    public MyBatisSlowSqlInterceptor slowSqlInterceptor() {
        return new MyBatisSlowSqlInterceptor();
    }

    /**
     * 将自定义拦截器注册到 MyBatis 全局配置
     *
     * @param interceptor MyBatisSlowSqlInterceptor 拦截器实例
     * @return ConfigurationCustomizer 配置定制器
     */
    @Bean
    public ConfigurationCustomizer configurationCustomizer(MyBatisSlowSqlInterceptor interceptor) {
        return configuration -> configuration.addInterceptor(interceptor);
    }
}
