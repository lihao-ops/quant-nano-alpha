package com.hao.datacollector.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理配置类
 *
 * 设计目的：
 * 1. 统一处理系统异常并保持接口返回格式一致。
 * 2. 记录异常信息便于排查与告警。
 *
 * 为什么需要该类：
 * - 分散的异常处理会导致返回格式不统一且难以维护。
 *
 * 核心实现思路：
 * - 按异常类型分类处理并构建统一响应结构。
 *
 * @author hli
 * @program datacollector
 * @date 2025-01-15
 * @description 量化交易数据收集器全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandlerConfig {

    /**
     * 处理参数校验异常
     *
     * 实现逻辑：
     * 1. 收集字段错误并构建响应体。
     * 2. 返回统一的错误结构。
     *
     * @param ex 参数校验异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        // 实现思路：
        // 1. 提取字段错误并返回标准结构。
        log.warn("参数校验异常|Validation_error,error={}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "参数校验失败");
        errorResponse.put("message", "请求参数不符合要求");
        errorResponse.put("path", request.getRequestURI());
        errorResponse.put("details", fieldErrors);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 处理绑定异常
     *
     * 实现逻辑：
     * 1. 收集绑定错误并构建响应体。
     * 2. 返回统一的错误结构。
     *
     * @param ex 绑定异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(
            BindException ex, HttpServletRequest request) {
        // 实现思路：
        // 1. 提取绑定错误并返回标准结构。
        log.warn("参数绑定异常|Bind_error,error={}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "参数绑定失败");
        errorResponse.put("message", "请求参数格式错误");
        errorResponse.put("path", request.getRequestURI());
        errorResponse.put("details", fieldErrors);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 处理缺少请求参数异常
     *
     * 实现逻辑：
     * 1. 构建缺参错误响应。
     * 2. 返回统一的错误结构。
     *
     * @param ex 缺少请求参数异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        // 实现思路：
        // 1. 返回缺参错误与请求路径。
        log.warn("缺少请求参数异常|Missing_param_error,error={}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "缺少必需参数");
        errorResponse.put("message", String.format("缺少必需的请求参数: %s", ex.getParameterName()));
        errorResponse.put("path", request.getRequestURI());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 处理参数类型不匹配异常
     *
     * 实现逻辑：
     * 1. 构建类型错误提示信息。
     * 2. 返回统一的错误结构。
     *
     * @param ex 参数类型不匹配异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        // 实现思路：
        // 1. 构建类型不匹配提示信息。
        log.warn("参数类型不匹配异常|Param_type_mismatch,error={}", ex.getMessage());

        String message = String.format("参数 '%s' 的值 '%s' 无法转换为 %s 类型",
                ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "参数类型错误");
        errorResponse.put("message", message);
        errorResponse.put("path", request.getRequestURI());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 处理业务异常
     *
     * 实现逻辑：
     * 1. 读取业务异常码与信息。
     * 2. 返回统一的错误结构。
     *
     * @param ex 业务异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        // 实现思路：
        // 1. 使用业务异常码构建响应。
        log.warn("业务异常|Business_error,error={}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", ex.getCode());
        errorResponse.put("error", "业务处理失败");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("path", request.getRequestURI());

        return ResponseEntity.status(ex.getCode()).body(errorResponse);
    }

    /**
     * 处理数据库异常
     *
     * 实现逻辑：
     * 1. 记录数据库异常信息。
     * 2. 返回统一的错误结构。
     *
     * @param ex 数据库异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler({org.springframework.dao.DataAccessException.class})
    public ResponseEntity<Map<String, Object>> handleDataAccessException(
            Exception ex, HttpServletRequest request) {
        // 实现思路：
        // 1. 记录异常并返回统一错误响应。
        log.error("数据库访问异常|Database_access_error", ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "数据库访问失败");
        errorResponse.put("message", "数据操作异常，请稍后重试");
        errorResponse.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 处理空指针异常
     *
     * 实现逻辑：
     * 1. 记录空指针异常。
     * 2. 返回统一的错误结构。
     *
     * @param ex 空指针异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointerException(
            NullPointerException ex, HttpServletRequest request) {
        // 实现思路：
        // 1. 记录异常并返回统一错误响应。
        log.error("空指针异常|Null_pointer_error", ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "系统内部错误");
        errorResponse.put("message", "系统处理异常，请联系管理员");
        errorResponse.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 处理其他未捕获的异常
     *
     * 实现逻辑：
     * 1. 记录未知异常信息。
     * 2. 返回统一的错误结构。
     *
     * @param ex 未知异常
     * @param request HTTP请求
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        // 实现思路：
        // 1. 记录异常并返回统一错误响应。
        log.error("未知异常|Unknown_error", ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "系统异常");
        errorResponse.put("message", "系统处理异常，请稍后重试");
        errorResponse.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 业务异常类
     *
     * 设计目的：
     * 1. 封装业务异常码与异常信息。
     *
     * 为什么需要该类：
     * - 业务异常需要统一的错误码与消息承载。
     *
     * 核心实现思路：
     * - 通过构造函数设置错误码并继承RuntimeException。
     */
    public static class BusinessException extends RuntimeException {
        private final int code;

        /**
         * 构造业务异常（默认400）
         *
         * 实现逻辑：
         * 1. 设置默认错误码并传递消息。
         *
         * @param message 异常消息
         */
        public BusinessException(String message) {
            super(message);
            this.code = HttpStatus.BAD_REQUEST.value();
        }

        /**
         * 构造业务异常（指定错误码）
         *
         * 实现逻辑：
         * 1. 设置指定错误码并传递消息。
         *
         * @param code 错误码
         * @param message 异常消息
         */
        public BusinessException(int code, String message) {
            super(message);
            this.code = code;
        }

        /**
         * 构造业务异常（携带异常原因）
         *
         * 实现逻辑：
         * 1. 设置500错误码并传递异常原因。
         *
         * @param message 异常消息
         * @param cause 异常原因
         */
        public BusinessException(String message, Throwable cause) {
            super(message, cause);
            this.code = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }

        /**
         * 获取业务错误码
         *
         * 实现逻辑：
         * 1. 返回当前异常携带的错误码。
         *
         * @return 错误码
         */
        public int getCode() {
            return code;
        }
    }
}
