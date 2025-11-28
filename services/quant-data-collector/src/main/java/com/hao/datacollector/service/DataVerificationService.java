package com.hao.datacollector.service;

import com.hao.datacollector.dto.param.verification.VerificationQueryParam;

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
}