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
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanExpiredLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        Date cutoffDate = Date.from(cutoff.atZone(ZoneId.systemDefault()).toInstant());
        log.info("开始清理过期日志，截止时间: {}", cutoffDate);
        operationLogMapper.deleteByDateBefore(cutoffDate);
        systemLogMapper.deleteByDateBefore(cutoffDate);
        dataAuditRecordMapper.deleteByDateBefore(cutoffDate);
        log.info("过期日志清理完成");
    }
}
