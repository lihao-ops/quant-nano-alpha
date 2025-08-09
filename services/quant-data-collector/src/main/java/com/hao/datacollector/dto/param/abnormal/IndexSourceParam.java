package com.hao.datacollector.dto.param.abnormal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "首页数据源参数对象")
public class IndexSourceParam {

    @Schema(description = "交易日期，格式：YYYYMMDD，例如：20250620", required = true)
    private String tradeDate;

    @Schema(description = "排序字段：1.windCode，2.priceChange，3.onListTime，4.seats", required = false)
    private Integer sortCol = 1;

    @Schema(description = "排序方式：-1.降序，1.升序，0.不排序", required = false)
    private Integer orderType = 1;
}