package com.hao.datacollector.web.vo.abnormal;

import com.hao.datacollector.web.vo.limitup.BaseVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "新龙虎榜首页VO对象")
public class AbnormalIndexVO extends BaseVO {

    private static final long serialVersionUID = -5484547909986859218L;

    /**
     * 股票代码
     */
    @Schema(description = "股票代码", required = true)
    private String windCode;
    /**
     * 股票名称
     */
    @Schema(description = "股票名称", required = true)
    private String stockName;
    /**
     * 涨跌幅
     */
    @Schema(description = "涨跌幅", required = true)
    private Double priceChange;
    /**
     * 最近一个月的上榜次数
     */
    @Schema(description = "最近一个月的上榜次数", required = true)
    private Integer onListTime;
    /**
     * 席位（机构介入、游资介入、机构撤出、游资撤出）
     */
    @Schema(description = "席位（机构介入、游资介入、机构撤出、游资撤出）", required = true)
    private String seats;
}