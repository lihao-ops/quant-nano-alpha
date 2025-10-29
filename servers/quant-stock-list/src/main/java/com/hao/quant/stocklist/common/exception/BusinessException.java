package com.hao.quant.stocklist.common.exception;

/**
 * 业务异常。
 * <p>
 * 用于封装业务语义错误,在 Controller 层统一转换为友好提示。
 * </p>
 */
public class BusinessException extends RuntimeException {

    /**
     * 构造带消息的异常。
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * 构造带消息与原始异常的异常。
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
