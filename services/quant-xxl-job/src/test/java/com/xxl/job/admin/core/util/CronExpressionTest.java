package com.xxl.job.admin.core.util;

import com.xxl.job.admin.core.cron.CronExpression;
import com.xxl.job.core.util.DateUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;

/**
 * Cron表达式计算测试
 *
 * 测试目的：
 * 1. 验证Cron表达式下一次触发时间计算是否正确。
 * 2. 输出连续多次触发时间用于人工核对。
 *
 * 设计思路：
 * - 固定表达式并循环计算下一次触发时间。
 */
public class CronExpressionTest {
    private static final Logger LOG = LoggerFactory.getLogger(CronExpressionTest.class);

    /**
     * 验证连续触发时间计算
     *
     * 实现逻辑：
     * 1. 初始化CronExpression。
     * 2. 循环计算下一次触发时间并记录。
     *
     * @throws ParseException 解析异常
     */
    @Test
    public void shouldWriteValueAsString() throws ParseException {
        // 实现思路：连续计算并输出触发时间
        CronExpression cronExpression = new CronExpression("0 0 0 ? * 1");
        Date lastTriggerTime = new Date();
        for (int i = 0; i < 5; i++) {
            Date nextTriggerTime = cronExpression.getNextValidTimeAfter(lastTriggerTime);
            LOG.info("Cron触发时间|Cron_fire_time,time={}", DateUtil.formatDateTime(nextTriggerTime));

            lastTriggerTime = nextTriggerTime;
        }
    }
}
