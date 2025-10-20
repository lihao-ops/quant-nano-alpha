package com.quant.audit.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * 操作日志实体类
 */
@Builder
@Data
@Schema(description = "操作日志")
public class OperationLog {
    
    @Schema(description = "主键ID")
    private Long id;
    
    @Schema(description = "服务名称")
    private String serviceName;
    
    @Schema(description = "操作类型")
    private String operationType;
    
    @Schema(description = "操作描述")
    private String description;
    
    @Schema(description = "操作用户")
    private String username;
    
    @Schema(description = "IP地址")
    private String ip;
    
    @Schema(description = "请求URL")
    private String requestUrl;
    
    @Schema(description = "请求方法")
    private String method;
    
    @Schema(description = "请求参数")
    private String params;
    
    @Schema(description = "执行结果")
    private String result;
    
    @Schema(description = "执行时间(毫秒)")
    private Long executionTime;
    
    @Schema(description = "异常信息")
    private String exception;
    
    @Schema(description = "创建时间")
    private String createTime;
}