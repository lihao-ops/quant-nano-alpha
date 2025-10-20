package com.quant.audit.aspect;

import com.alibaba.fastjson.JSON;
import com.quant.audit.model.OperationLog;
import com.quant.audit.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;

/**
 * 操作日志切面
 * 用于自动记录系统操作日志
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 定义切点 - 所有带有 @OperationAudit 注解的方法
     */
    @Pointcut("@annotation(com.quant.audit.aspect.OperationAudit)")
    public void operationLogPointcut() {
    }

    /**
     * 环绕通知，记录操作日志
     */
    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;

        try {
            // 执行方法
            result = point.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            // 记录操作日志
            saveOperationLog(point, result, beginTime, exception);
        }
    }

    /**
     * 保存操作日志
     */
    private void saveOperationLog(ProceedingJoinPoint joinPoint, Object result, long beginTime, Exception exception) {
        try {
            // 获取请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

            // 获取方法签名
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            
            // 获取注解信息
            OperationAudit operationAudit = method.getAnnotation(OperationAudit.class);
            
            // 构建操作日志对象
            OperationLog operationLog = OperationLog.builder()
                    .serviceName(operationAudit.serviceName())
                    .operationType(operationAudit.operationType())
                    .description(operationAudit.description())
                    .method(method.getName())
                    .params(JSON.toJSONString(joinPoint.getArgs()))
                    .result(result != null ? JSON.toJSONString(result) : null)
                    .executionTime(System.currentTimeMillis() - beginTime)
                    .createTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();
            
            // 设置请求信息
            if (request != null) {
                operationLog.setRequestUrl(request.getRequestURI());
                operationLog.setIp(getIpAddress(request));
                operationLog.setUsername(getUsernameFromRequest(request));
            }

            // 设置异常信息
            if (exception != null) {
                operationLog.setException(exception.getMessage());
            }

            // 保存操作日志
            operationLogService.saveOperationLog(operationLog);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }
    
    /**
     * 获取请求IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    /**
     * 从请求中获取用户名
     */
    private String getUsernameFromRequest(HttpServletRequest request) {
        // 实际项目中根据认证方式获取用户名
        return "system";
    }
}