package com.hao.datacollector.service.job;

import com.hao.datacollector.dto.param.abnormal.IndexSourceParam;
import com.hao.datacollector.service.AbnormalService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-06-23 18:49:19
 * @description: 龙虎榜相关转档
 */
@Slf4j
@Component
public class AbnormalJob {

    @Autowired
    private AbnormalService abnormalService;

    /**
     * 龙虎榜首页数据转档
     * 每日收盘后0 30 15 * * ?再执行
     */
    @XxlJob("abnormalIndexJob")
    public ReturnT<String> todayAbnormalIndexTransferJob() {
        String jobParam = XxlJobHelper.getJobParam();
        XxlJobHelper.log("todayLimitUpTransferJob_start,jobParam={}", jobParam);
        Boolean success = abnormalService.transferHomePage(new IndexSourceParam());
        return success ? ReturnT.SUCCESS : ReturnT.FAIL;
    }
}
