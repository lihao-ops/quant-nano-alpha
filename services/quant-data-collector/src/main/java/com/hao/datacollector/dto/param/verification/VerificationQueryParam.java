package com.hao.datacollector.dto.param.verification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author hli
 * @date 2025-06-05
 * @description 数据一致性校验请求参数
 */
@Data
@Schema(name = "数据一致性校验请求参数")
public class VerificationQueryParam {

    @Schema(description = "待校验的年份列表", example = "[\"2020\", \"2021\"]", required = true)
    private List<String> years;

    @Schema(description = "源标准数据表名", example = "tb_quotation_history_warm", required = true)
    private String sourceTableName;

    @Schema(description = "目标验证表名", example = "tb_quotation_history_warm", required = true)
    private String targetTableName;
}