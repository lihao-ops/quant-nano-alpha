package com.hao.datacollector.dto.table.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-07-26 17:15:56
 * @description: 股票基本信息DTO
 */
@Data
@Schema(description = "上市公司基础信息插入对象")
public class StockBaseDTO {
    @Schema(description = "证券代码", example = "600519.SH")
    private String windCode;

    @Schema(description = "证券简称", example = "贵州茅台")
    private String windName;
}
