package com.hao.datacollector.dto.param.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 云数据请求参数
 *
 * @author hli
 * @program: datacollector
 * @Date 2025-06-20 17:10:56
 * @description: 云数据请求参数
 */
@Data
@Schema(description = "云数据请求参数")
public class CloudDataParams {

    @Schema(description = "会话ID", example = "a37fbf77fef444f68d9efb6e1387d259")
    private String sessionId;

    @Schema(description = "命令", example = "DEV_COMMON_REPORT")
    private String command = "DEV_COMMON_REPORT";

    @Schema(description = "云类型", example = "0")
    private Integer cloudType = 0;

    @Schema(description = "云参数", example = "{\"reportBody\": \"WSS('macro=a001010100000000','s_info_name','tradeDate=s_trade_date(windcode,now(), 0)')\"}")
    private Map<String, String> cloudParams;

    @Schema(description = "语言", example = "zh")
    private String lan = "zh";
}
