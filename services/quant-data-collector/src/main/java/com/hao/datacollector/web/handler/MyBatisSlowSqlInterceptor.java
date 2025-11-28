package com.hao.datacollector.web.handler;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Statement;
import java.util.Properties;

/**
 * MyBatis SQL 监控拦截器
 *
 * <p>功能：
 * 1. 拦截 MyBatis 执行的所有 SQL，包括查询（query）和更新（update）。
 * 2. 记录每条 SQL 的执行耗时（毫秒）。
 * 3. 慢 SQL 监控：当 SQL 执行时间超过阈值时（默认 3000ms）输出 WARN 日志，方便性能分析和优化。
 *
 * <p>使用说明：
 * 1. 可通过 mybatis-config.xml 配置或在 Spring Boot 中注册该拦截器。
 * 2. 慢 SQL 阈值可通过 properties 属性 `slowSqlThresholdMs` 自定义。
 * 3. 日志采用 SLF4J 记录，可结合 Logback 或 Log4j 输出到文件、控制台或日志系统。
 *
 * <p>实现原理：
 * - 使用 MyBatis 插件机制（Interceptor），拦截 StatementHandler 的 query 与 update 方法。
 * - 执行 SQL 前记录时间戳，执行后计算耗时。
 * - 根据耗时打印普通日志或慢 SQL 日志。
 *
 * <p>扩展建议：
 * - 可打印 SQL 参数（statementHandler.getBoundSql().getParameterObject()）。
 * - 可异步发送慢 SQL 到 Kafka/ELK 做集中监控。
 * - 可增加调用方法或堆栈信息，便于追踪慢 SQL 来源。
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
        @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, org.apache.ibatis.session.ResultHandler.class})
})
public class MyBatisSlowSqlInterceptor implements Interceptor {

    private static final Logger logger = LoggerFactory.getLogger(MyBatisSlowSqlInterceptor.class);

    /** 慢 SQL 阈值，单位毫秒，默认 3000ms */
    //todo 核心测验，暂不打印慢sql日志
    private long slowSqlThresholdMs = 30000000;

    /**
     * 拦截 SQL 执行
     *
     * @param invocation MyBatis 执行方法的封装
     * @return SQL 执行结果
     * @throws Throwable 调用原始方法可能抛出的异常
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = invocation.proceed(); // 执行原始 SQL
        long end = System.currentTimeMillis();
        long cost = end - start; // SQL 执行耗时（ms）
        // 获取 SQL 内容并压缩空格
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        String sql = statementHandler.getBoundSql().getSql().replaceAll("\\s+", " ");
        // 打印所有 SQL 执行时间
//        logger.info("[SQL EXEC] 耗时={}ms, SQL: {}", cost, sql);
        // 超过阈值的慢 SQL
        if (cost >= slowSqlThresholdMs) {
            logger.warn("[SLOW SQL] 耗时={}ms, 超过阈值={}ms, SQL: {}", cost, slowSqlThresholdMs, sql);
        }

        return result;
    }
    /** 包装目标对象为 MyBatis 插件对象 */
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    /**
     * 设置属性
     *
     * @param properties 可配置参数
     *                   slowSqlThresholdMs - 慢 SQL 阈值（毫秒）
     */
    @Override
    public void setProperties(Properties properties) {
        String threshold = properties.getProperty("slowSqlThresholdMs");
        if (threshold != null) {
            slowSqlThresholdMs = Long.parseLong(threshold);
        }
    }
}