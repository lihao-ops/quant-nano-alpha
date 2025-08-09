package com.hao.datacollector.web.vo.abnormal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Data
@Schema(description = "活跃席位VO对象")
public class ActiveRankRecordVO {
    @Schema(description = "股票代码", required = true)
    private String windCode;
    @Schema(description = "股票名称", required = true)
    private String stockName;
    @Schema(description = "现价", required = true)
    private Double price;
    @Schema(description = "涨跌幅", required = true)
    private Double priceChange;
    @Schema(description = "上榜次数", required = true)
    private Integer onListTimes;
    @Schema(description = "累计买入", required = true)
    private Double totalBuy;
    @Schema(description = "累计卖出", required = true)
    private Double totalSold;
    @Schema(description = "累计净买入", required = true)
    private Double netbuy;
    @Schema(description = "买卖比", required = true)
    private Double buySoldRate;

    public static Set<String> windCodeSet(List<ActiveRankRecordVO> activeRankRecordVOList) {
        Set<String> windCodeSet = new HashSet<>();
        activeRankRecordVOList.forEach(activeRankRecordVO -> windCodeSet.add(activeRankRecordVO.getWindCode()));
        return windCodeSet;
    }
}