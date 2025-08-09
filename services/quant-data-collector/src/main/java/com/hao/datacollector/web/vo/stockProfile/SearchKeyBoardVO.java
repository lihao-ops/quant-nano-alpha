package com.hao.datacollector.web.vo.stockProfile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description ="键盘精灵VO对象")
public class SearchKeyBoardVO {
    public SearchKeyBoardVO(String windCode, String name, String priority){
        this.windCode = windCode;
        this.name = name;
        this.priority = priority;
    }

    @Schema(description ="万得代码", required = true)
    private String windCode;

    @Schema(description ="名称", required = true)
    private String name;

    @Schema(description ="根据此值获取品种", required = true)
    private String priority;
}