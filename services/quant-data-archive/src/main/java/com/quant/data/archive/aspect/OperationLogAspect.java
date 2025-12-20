package com.quant.data.archive.aspect;

import com.alibaba.fastjson.JSON;
import com.quant.data.archive.model.OperationLog;
import com.quant.data.archive.service.OperationLogService;
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
/**
 * 操作日志切面
 *
 * 设计目的：
 * 1. 通过AOP统一采集操作日志，减少业务代码侵入。
 * 2. 自动补齐请求信息与执行耗时，形成完整审计链路。
 *
 * 为什么需要该类：
 * - 操作审计属于横切关注点，需要集中治理与复用。
 *
 * 核心实现思路：
 * - 环绕通知捕获请求与执行结果，构建日志实体后入库。
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 定义切点，匹配所有带有@OperationAudit注解的方法
     *
     * 实现逻辑：
     * 1. 使用注解切点拦截操作审计场景。
     */
    @Pointcut("@annotation(com.quant.data.archive.aspect.OperationAudit)")
    public void operationLogPointcut() {
    }

    /**
     * 环绕通知，记录操作日志
     *
     * 实现逻辑：
     * 1. 记录开始时间并执行业务方法。
     * 2. 捕获异常并在finally中统一记录日志。
     *
     * @param point 切点信息
     * @return 方法执行结果
     * @throws Throwable 业务异常向上抛出
     */
    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // 实现思路：
        // 1. 执行目标方法并记录耗时。
        // 2. 捕获异常后交由保存日志处理。
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
     *
     * 实现逻辑：
     * 1. 解析请求与方法信息构造日志实体。
     * 2. 补齐请求来源与异常信息。
     * 3. 调用服务层完成持久化。
     *
     * @param joinPoint 切点信息
     * @param result 方法返回结果
     * @param beginTime 执行开始时间
     * @param exception 执行异常
     */
    private void saveOperationLog(ProceedingJoinPoint joinPoint, Object result, long beginTime, Exception exception) {
        // 实现思路：
        // 1. 构建日志实体并填充请求信息。
        // 2. 写入异常字段并调用服务保存。
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
            log.error("记录操作日志失败|Record_operation_log_failed,error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取请求IP地址
     *
     * 实现逻辑：
     * 1. 依次读取代理头部。
     * 2. 回退到请求源IP。
     *
     * @param request Http请求
     * @return IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        // 实现思路：
        // 1. 优先读取代理链IP。
        // 2. 兜底使用远端地址。
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
     *
     * 实现逻辑：
     * 1. 预留认证信息解析入口。
     *
     * @param request Http请求
     * @return 用户名
     */
    private String getUsernameFromRequest(HttpServletRequest request) {
        // 实现思路：
        // 1. 根据认证上下文提取用户名。
        // 实际项目中根据认证方式获取用户名
        return "system";
    }
}
