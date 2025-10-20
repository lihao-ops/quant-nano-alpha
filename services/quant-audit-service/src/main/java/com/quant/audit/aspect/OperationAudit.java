package com.quant.audit.aspect;

import java.lang.annotation.*;

/**
 * 操作审计注解
 * 用于标记需要进行操作日志记录的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationAudit {
    
    /**
     * 服务名称
     */
    String serviceName() default "";
    
    /**
     * 操作类型
     */
    String operationType() default "";
    
    /**
     * 操作描述
     */
    String description() default "";
}