package com.quant.data.archive.service;

import com.quant.data.archive.mapper.SystemLogMapper;
import com.quant.data.archive.model.SystemLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogCollectorService {

    @Autowired
    private SystemLogMapper systemLogMapper;

    /**
     * 收集系统日志并写入数据库
     */
    public void collectSystemLog(SystemLog systemLog) {
        log.info("收集系统日志: {}", systemLog);
        systemLogMapper.insert(systemLog);
    }
}
