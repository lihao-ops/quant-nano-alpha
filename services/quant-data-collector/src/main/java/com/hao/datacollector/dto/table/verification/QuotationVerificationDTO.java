package com.hao.datacollector.dto.table.verification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * @author hli
 * @date 2025-06-05
 * @description 行情数据一致性校验实体DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "行情数据校验实体对象")
public class QuotationVerificationDTO {

    @Schema(description = "股票代码")
    private String windCode;

    @Schema(description = "交易时间")
    private Timestamp tradeDate;

    @Schema(description = "最新价")
    private BigDecimal latestPrice;

    @Schema(description = "总成交量")
    private BigDecimal totalVolume;

    @Schema(description = "均价")
    private BigDecimal averagePrice;

    @Schema(description = "数据状态")
    private Integer status;
}