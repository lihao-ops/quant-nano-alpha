package com.hao.datacollector.service;

import com.hao.datacollector.dto.param.verification.VerificationQueryParam;

import java.util.concurrent.CompletableFuture;

/**
 * @author hli
 * @description 数据校验服务接口
 */
public interface DataVerificationService {

    /**
     * 启动全量校验任务
     *
     * @param param 包含年份列表和目标表名的参数对象
     */
    void startVerification(VerificationQueryParam param);

    // 🔥 必须增加这个方法定义，否则 self.verifyMonthTableAsync 会报错
    CompletableFuture<String> verifyMonthTableAsync(String yearMonth, String getSourceTableName, String targetTable);
}