package com.hao.datacollector.common.exception;

import lombok.Data;

@Data
public class BusinessException extends RuntimeException {
    /**
     * 错误码
     */
    private Integer errorCode;

    /**
     * 消息内容
     */
    private String message;

    public BusinessException(Integer errorCode, String message) {
        this.message = message;
        this.errorCode = errorCode;
    }
}