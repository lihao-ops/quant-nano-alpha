package com.hao.datacollector.service.job;

import com.hao.datacollector.service.NewsService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-06-22 00:43:48
 * @description: 新闻数据转档
 */
@Slf4j
@Component
public class NewsJob {

    @Autowired
    private NewsService newsService;

    //@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")
    @XxlJob("newsBaseTransferJob")
    public ReturnT<String> newsBaseTransferJob() {
        String jobParam = XxlJobHelper.getJobParam();
        XxlJobHelper.log("newsBaseTransferJob start,jobParam={}", jobParam);
        return ReturnT.SUCCESS;
//        return newsService.transferNewsStockData("000001.SZ") ? ReturnT.SUCCESS : ReturnT.FAIL;
    }
}
