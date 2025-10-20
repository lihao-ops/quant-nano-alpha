package com.quant.audit.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 数据审计记录实体类
 */
@Data
@Schema(description = "数据审计记录")
public class DataAuditRecord {
    
    @Schema(description = "主键ID")
    private Long id;
    
    @Schema(description = "服务名称")
    private String serviceName;
    
    @Schema(description = "表名")
    private String tableName;
    
    @Schema(description = "操作类型")
    private String operationType;
    
    @Schema(description = "记录ID")
    private String recordId;
    
    @Schema(description = "修改前数据")
    private String beforeData;
    
    @Schema(description = "修改后数据")
    private String afterData;
    
    @Schema(description = "操作用户")
    private String username;
    
    @Schema(description = "操作时间")
    private Date operationTime;
    
    @Schema(description = "创建时间")
    private Date createTime;
}