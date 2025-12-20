package com.quant.data.archive.service;

import com.quant.data.archive.mapper.DataAuditRecordMapper;
import com.quant.data.archive.mapper.OperationLogMapper;
import com.quant.data.archive.mapper.SystemLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 历史日志归档清理服务
 *
 * 设计目的：
 * 1. 定期清理超期日志，控制存储增长。
 * 2. 统一管理审计日志、系统日志与操作日志的生命周期。
 *
 * 为什么需要该类：
 * - 日志保留周期需要自动化治理，避免人工维护。
 *
 * 核心实现思路：
 * - 定时任务计算截止时间并批量清理三类日志表。
 */
@Slf4j
@Service
public class DataArchiveService {

    @Autowired
    private DataAuditRecordMapper dataAuditRecordMapper;
    @Autowired
    private OperationLogMapper operationLogMapper;
    @Autowired
    private SystemLogMapper systemLogMapper;

    @Value("${archive.retention-days:90}")
    private int retentionDays;

    /**
     * 每日凌晨执行历史日志清理
     *
     * 实现逻辑：
     * 1. 计算保留周期对应的截止时间。
     * 2. 按截止时间批量删除不同日志表记录。
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanExpiredLogs() {
        // 实现思路：
        // 1. 依据配置保留天数计算截止时间。
        // 2. 分表执行批量删除。
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        Date cutoffDate = Date.from(cutoff.atZone(ZoneId.systemDefault()).toInstant());
        log.info("开始清理过期日志|Start_cleanup_expired_logs,cutoffTime={}", cutoffDate);
        operationLogMapper.deleteByDateBefore(cutoffDate);
        systemLogMapper.deleteByDateBefore(cutoffDate);
        dataAuditRecordMapper.deleteByDateBefore(cutoffDate);
        log.info("过期日志清理完成|Expired_logs_cleanup_completed");
    }
}
