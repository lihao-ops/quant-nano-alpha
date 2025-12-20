package com.quant.data.archive.service;

import com.quant.data.archive.mapper.OperationLogMapper;
import com.quant.data.archive.model.OperationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务
 *
 * 设计目的：
 * 1. 统一封装操作日志的持久化入口。
 * 2. 降低上层调用对数据访问层的直接依赖。
 *
 * 为什么需要该类：
 * - 操作日志属于统一审计能力，需要集中处理与扩展。
 *
 * 核心实现思路：
 * - 由服务层接收日志实体并调用Mapper入库。
 */
@Slf4j
@Service
public class OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    /**
     * 保存操作日志
     *
     * 实现逻辑：
     * 1. 输出关键日志，便于追踪审计入库。
     * 2. 调用Mapper完成持久化。
     *
     * @param operationLog 操作日志实体
     */
    public void saveOperationLog(OperationLog operationLog) {
        // 实现思路：
        // 1. 记录入库请求。
        // 2. 调用Mapper完成落库。
        log.info("保存操作日志|Save_operation_log,operationLog={}", operationLog);
        operationLogMapper.insert(operationLog);
    }
}
