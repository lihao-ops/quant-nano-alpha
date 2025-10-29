package com.hao.quant.stocklist.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 每日精选股票查询请求 DTO。
 */
@Data
@Builder
public class StablePicksQueryDTO {

    private LocalDate tradeDate;
    private String strategyId;
    private String industry;
    private Integer pageNum;
    private Integer pageSize;
}
