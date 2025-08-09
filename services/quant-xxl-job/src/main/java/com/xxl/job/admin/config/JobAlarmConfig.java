package com.xxl.job.admin.config;

import com.xxl.job.admin.core.alarm.JobAlarm;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import org.springframework.stereotype.Component;

//默认提供邮件失败告警，可扩展短信、钉钉等方式。如果需要新增一种告警方式，只需要新增一个实现 “com.xxl.job.admin.core.alarm.JobAlarm” 接口的告警实现即可。可以参考默认提供邮箱告警实现 “EmailJobAlarm”。

/**
 * 首选：钉钉 / 飞书 / 企业微信机器人
 * 👍 优点：
 * 秒级推送，不丢消息
 *
 * 支持图文格式、链接跳转，适合展示“股票名 + 买/卖建议 + K线链接”
 *
 * 多终端同步（手机+PC）
 *
 * 免费，无限条
 *
 * 示例消息：
 * 🛎️ 股票提醒
 * 股票：$贵州茅台(SH600519)$
 * 当前价格：1730.50
 * 推荐操作：买入
 * 技术信号：5日均线上穿10日均线
 */
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
        System.out.println("告警触发！");
        System.out.println("任务名称: " + info.getJobDesc());
        System.out.println("失败信息: " + jobLog.getHandleMsg());
        return true; // 告警成功
    }
}
