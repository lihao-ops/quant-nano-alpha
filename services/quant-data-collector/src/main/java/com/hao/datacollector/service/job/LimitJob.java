package com.hao.datacollector.service.job;

import com.hao.datacollector.service.LimitUpService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-06-22 16:36:23
 * @description: 涨停相关job
 */
@Slf4j
@Component
public class LimitJob {
    @Autowired
    private LimitUpService limitUpService;

    /**
     * 涨停相关job
     * 每日收盘后0 30 15 * * ?再执行
     */
    @XxlJob("limitUpJob")
    public ReturnT<String> todayLimitUpTransferJob() {
        String jobParam = XxlJobHelper.getJobParam();
        XxlJobHelper.log("todayLimitUpTransferJob_start,jobParam={}", jobParam);
        Boolean success = limitUpService.transferLimitUpDataToDatabase(null);
        return success ? ReturnT.SUCCESS : ReturnT.FAIL;
    }
}
