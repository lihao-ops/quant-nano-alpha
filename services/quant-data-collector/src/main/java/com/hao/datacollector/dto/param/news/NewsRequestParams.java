package com.hao.datacollector.dto.param.news;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author LiHao
 * @program:
 * @description: 新闻请求参数实体类
 * @Date 2025-06-18
 */
@Data
@Schema(name = "新闻请求参数实体类")
public class NewsRequestParams {
    @Schema(description = "Wind代码", required = true)
    private String windCode;
    
    @Schema(description = "语言:默认cn", required = true)
    private String lan = "cn";
} 