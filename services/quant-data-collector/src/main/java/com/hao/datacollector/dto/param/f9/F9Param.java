package com.hao.datacollector.dto.param.f9;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-07-30 18:52:35
 * @description: 简版F9请求参数
 */
@Data
@Schema(description = "简版F9请求参数")
public class F9Param {
    @Schema(description = "股票代码", required = true)
    private String windCode;

    @Schema(description = "多语言:cn.中文(默认),en.英文", required = false)
    private String lan = "cn";
}
