package com.hao.quant.stocklist.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 每日精选股票查询请求 DTO。
 * <p>
 * 封装了 Controller 层接受的查询参数,便于在领域服务中统一校验与处理。
 * </p>
 */
@Data
@Builder
public class StablePicksQueryDTO {

    /** 交易日期 */
    private LocalDate tradeDate;
    /** 策略标识 */
    private String strategyId;
    /** 行业过滤条件 */
    private String industry;
    /** 当前页码 */
    private Integer pageNum;
    /** 每页数量 */
    private Integer pageSize;
}
