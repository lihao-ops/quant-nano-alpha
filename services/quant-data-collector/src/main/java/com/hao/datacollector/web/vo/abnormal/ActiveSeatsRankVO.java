package com.hao.datacollector.web.vo.abnormal;

import com.hao.datacollector.web.vo.limitup.BaseVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(description = "龙虎榜活跃榜VO对象")
public class ActiveSeatsRankVO extends BaseVO {
    @Schema(description = "席位机构名称", required = true)
    private String institution;

    @Schema(description = "席位机构id", required = true)
    private String institutionId;

    @Schema(description = "席位类型", required = true)
    private String seatType;

    @Schema(description = "上榜次数", required = true)
    private int listingCount;

    @Schema(description = "跟投胜率,单位：%，保留2位小数", required = true)
    private double winProportion;

    @Schema(description = "总成交额", required = true)
    private double amount;

    @Schema(description = "买入次数", required = true)
    private int buyCount;

    @Schema(description = "卖出次数", required = true)
    private int sellCount;
}