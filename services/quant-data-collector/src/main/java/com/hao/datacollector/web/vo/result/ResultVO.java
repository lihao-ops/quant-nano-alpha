package com.hao.datacollector.web.vo.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "通用响应结果包装类")
public class ResultVO<T> {
    @Schema(description = "状态码 200：OK,401：Unauthorized,403: Forbidden,404：Not Found", required = true)
    private Integer code = 200;

    @Schema(description = "返回请求信息", required = true)
    protected String message = "OK";

    @Schema(description = "返回结果对象", required = true)
    private T data;
}