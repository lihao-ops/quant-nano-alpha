package com.quant.data.archive.service;

import com.quant.data.archive.mapper.OperationLogMapper;
import com.quant.data.archive.model.OperationLog;
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
