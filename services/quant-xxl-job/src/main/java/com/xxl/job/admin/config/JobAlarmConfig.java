package com.xxl.job.admin.config;

import com.xxl.job.admin.core.alarm.JobAlarm;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 任务告警适配器，默认承接调度失败事件，可扩展短信、邮件与机器人通知。
 *
 * 设计目的：
 * 1. 统一调度失败告警入口，屏蔽不同通知渠道差异。
 * 2. 保留扩展点，便于引入钉钉/飞书/企业微信机器人推送。
 *
 * 为什么需要该类：
 * - 调度失败涉及多种通知渠道，需要统一接口与落地入口。
 *
 * 实现思路：
 * - 通过实现 JobAlarm 接口暴露统一告警方法。
 * - 在告警方法中汇总任务描述与失败信息，再交由具体渠道发送。
 *
 * 机器人告警建议：
 * 1. 支持秒级推送与多终端同步，降低消息丢失概率。
 * 2. 支持图文格式与链接跳转，适合展示股票名、买卖建议与K线链接。
 *
 * 示例消息：
 * 股票提醒
 * 股票：$贵州茅台(SH600519)$
 * 当前价格：1730.50
 * 推荐操作：买入
 * 技术信号：5日均线上穿10日均线
 */
@Slf4j
@Component  // 确保能被 Spring 扫描
public class JobAlarmConfig implements JobAlarm {

    /**
     * 告警方法
     *
     * @param info   执行信息
     * @param jobLog 调度日志
     * @return true为告警成功，false为告警失败
     */
    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {
        log.info("告警触发|Alarm_triggered,jobId={},jobDesc={}", info.getId(), info.getJobDesc());
        log.warn("告警失败信息|Alarm_fail_msg,jobId={},handleMsg={}", info.getId(), jobLog.getHandleMsg());
        return true; // 告警成功
    }
}

