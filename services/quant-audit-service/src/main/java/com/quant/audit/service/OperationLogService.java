package com.quant.audit.service;

import com.quant.audit.mapper.OperationLogMapper;
import com.quant.audit.model.OperationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    /**
     * 保存操作日志
     */
    public void saveOperationLog(OperationLog operationLog) {
        log.info("保存操作日志: {}", operationLog);
        operationLogMapper.insert(operationLog);
    }
}