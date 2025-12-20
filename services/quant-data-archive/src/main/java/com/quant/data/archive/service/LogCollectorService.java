package com.quant.data.archive.service;

import com.quant.data.archive.mapper.SystemLogMapper;
import com.quant.data.archive.model.SystemLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 系统日志采集服务
 *
 * 设计目的：
 * 1. 提供统一的系统日志采集入口。
 * 2. 将采集与持久化逻辑封装在服务层。
 *
 * 为什么需要该类：
 * - 系统日志需要集中治理，便于审计与追溯。
 *
 * 核心实现思路：
 * - 接收日志实体后直接入库，并记录关键信息。
 */
@Slf4j
@Service
public class LogCollectorService {

    @Autowired
    private SystemLogMapper systemLogMapper;

    /**
     * 收集系统日志并写入数据库
     *
     * 实现逻辑：
     * 1. 记录采集日志，便于追踪处理情况。
     * 2. 调用Mapper完成持久化。
     *
     * @param systemLog 系统日志实体
     */
    public void collectSystemLog(SystemLog systemLog) {
        // 实现思路：
        // 1. 输出采集信息。
        // 2. 入库持久化。
        log.info("收集系统日志|Collect_system_log,systemLog={}", systemLog);
        systemLogMapper.insert(systemLog);
    }
}
