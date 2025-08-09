package com.hao.datacollector.web.controller;


import com.hao.datacollector.dto.param.abnormal.IndexSourceParam;
import com.hao.datacollector.service.AbnormalService;
import com.hao.datacollector.web.vo.abnormal.AbnormalIndexVO;
import com.hao.datacollector.web.vo.abnormal.ActiveRankRecordVO;
import com.hao.datacollector.web.vo.abnormal.ActiveSeatsRankVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author hli
 * @Date 2025-06-23 14:07:23
 * @description: 龙虎榜相关Controller
 */
@Slf4j
@Tag(name = "龙虎榜")
@RequestMapping("abnormal")
@RestController
public class AbnormalController {

    @Autowired
    private AbnormalService abnormalService;

    @Operation(summary = "首页数据源", method = "GET")
    @Parameter(name = "tradeDate", description = "交易日期，格式：YYYYMMDD，例如：20250620", required = true)
    @Parameter(name = "sortCol", description = "排序字段：1.windCode，2.priceChange，3.onListTime，4.seats", required = false)
    @Parameter(name = "orderType", description = "排序方式：-1.降序，1.升序，0.不排序", required = false)
    @GetMapping("index_source")
    public List<AbnormalIndexVO> getSourceHomePage(@RequestParam("tradeDate") String tradeDate,
                                                   @RequestParam(value = "sortCol", required = false, defaultValue = "1") Integer sortCol,
                                                   @RequestParam(value = "orderType", required = false, defaultValue = "1") Integer orderType) {
        return abnormalService.getSourceHomePage(tradeDate, sortCol, orderType);
    }

    @Operation(summary = "转档首页数据", method = "POST")
    @PostMapping("transfer_index")
    public Boolean transferHomeData(@RequestBody IndexSourceParam indexSourceParam) {
        return abnormalService.transferHomePage(indexSourceParam);
    }

    @Operation(summary = "席位榜数据源", method = "GET")
    @Parameters({
            @Parameter(name = "period", description = "时间周期类型,0-近1月，1-近3月，2-近半年，3-今年以来", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "integer")),
            @Parameter(name = "seatType", description = "席位类型,全部席位：0，高胜率席位介入：1，机构介入：2，游资介入：3，普通介入：4", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "integer")),
            @Parameter(name = "pageNo", description = "当前页码", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "integer")),
            @Parameter(name = "pageSize", description = "每页记录数:默认显示20条", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "integer")),
            @Parameter(name = "sortCol", description = "排序字段,0-上榜次数、1-跟投胜率、2-总成交额、3-买入次数、4-卖出次数、5-席位类型", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "integer")),
            @Parameter(name = "sortFlag", description = "排序标记,0-不排序，1-排升序，-1排降序", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "integer"))
    })
    @GetMapping("/seats_source")
    public List<ActiveSeatsRankVO> getSourceListOfSeats(@RequestParam(required = false, defaultValue = "3") Integer period,
                                                        @RequestParam(required = false, defaultValue = "0") Integer seatType,
                                                        @RequestParam(required = false, defaultValue = "1") Integer pageNo,
                                                        @RequestParam(required = false, defaultValue = "10000") Integer pageSize,
                                                        @RequestParam(required = false, defaultValue = "1") Integer sortCol,
                                                        @RequestParam(required = false, defaultValue = "-1") Integer sortFlag) {
        return abnormalService.getSourceListOfSeats(period, seatType, pageNo, pageSize, sortCol, sortFlag);
    }

    @Operation(summary = "活跃榜数据源", method = "GET")
    @Parameters({
            @Parameter(name = "period", description = "时间周期类型,0-近1月，1-近3月，2-近半年，3-今年以来", required = true, in = ParameterIn.QUERY, schema = @Schema(type = "integer")),
            @Parameter(name = "pageNo", description = "当前页码", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "integer")),
            @Parameter(name = "pageSize", description = "每页记录数,默认20条", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "integer")),
            @Parameter(name = "sortCol", description = "排序字段,0-上榜次数、1-跟投胜率、2-总成交额、3-买入次数、4-卖出次数、5-席位类型、6-涨跌幅", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "integer")),
            @Parameter(name = "sortFlag", description = "排序标记,0-不排序，1-排升序，-1排降序", required = false, in = ParameterIn.QUERY, schema = @Schema(type = "integer"))
    })
    @GetMapping("/active_source")
    public List<ActiveRankRecordVO> getSourceActiveRank(@RequestParam(required = false, defaultValue = "3") Integer period,
                                                        @RequestParam(required = false, defaultValue = "1") Integer pageNo,
                                                        @RequestParam(required = false, defaultValue = "10000") Integer pageSize,
                                                        @RequestParam(required = false, defaultValue = "1") Integer sortCol,
                                                        @RequestParam(required = false, defaultValue = "-1") Integer sortFlag) {
        return abnormalService.getSourceActiveRank(period, pageNo, pageSize, sortCol, sortFlag);
    }
}