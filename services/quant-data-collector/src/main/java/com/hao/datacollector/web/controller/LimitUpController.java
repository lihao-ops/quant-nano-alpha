package com.hao.datacollector.web.controller;

import com.hao.datacollector.dto.param.limitup.LimitUpStockQueryParam;
import com.hao.datacollector.service.LimitUpService;
import com.hao.datacollector.web.vo.limitup.ApiResponse;
import com.hao.datacollector.web.vo.limitup.LimitUpStockQueryResultVO;
import com.hao.datacollector.web.vo.limitup.ResultObjectVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author hli
 * @program: datacollector
 * @Date 2025-06-12 19:34:31
 * @description: 涨停相关接口
 */
@Slf4j
@RestController
@RequestMapping("limit_up")
@Tag(name = "涨停相关", description = "系统健康检查和状态监控接口")
public class LimitUpController {

    @Autowired
    private LimitUpService limitUpService;

    @PostMapping("transfer_list_data")
    @Operation(summary = "转档涨停股票列表", description = "检查服务运行状态")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "服务正常运行"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务异常")
    })
    public ResponseEntity<ApiResponse<ResultObjectVO>> getLimitUpData(@RequestParam(required = false) String tradeTime) {
        ApiResponse<ResultObjectVO> result = limitUpService.getLimitUpData(tradeTime);
        if (result == null || !"200".equals(result.getResultCode())) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "将涨停数据转档到数据库", description = "检查服务运行状态")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "服务正常运行"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服务异常")
    })
    @PostMapping("/transfer")
    public ResponseEntity<String> transferLimitUpData(@RequestParam(required = false) String tradeTime) {
        Boolean success = limitUpService.transferLimitUpDataToDatabase(tradeTime);
        if (!success) {
            return ResponseEntity.badRequest().body("数据转档失败");
        }
        return ResponseEntity.ok("数据转档成功");
    }

    @GetMapping("/stock_info_list")
    @Operation(summary = "涨停股票信息列表", description = "根据查询条件获取涨停股票信息列表")
    public List<LimitUpStockQueryResultVO> queryLimitUpStockList(
            @Parameter(description = "股票代码") @RequestParam(required = false) String windCode,
            @Parameter(description = "交易日期开始") @RequestParam(required = false) String tradeDateStart,
            @Parameter(description = "交易日期结束") @RequestParam(required = false) String tradeDateEnd,
            @Parameter(description = "股票名称") @RequestParam(required = false) String windName,
            @Parameter(description = "首次涨停时间开始") @RequestParam(required = false) String firstTimeStart,
            @Parameter(description = "首次涨停时间结束") @RequestParam(required = false) String firstTimeEnd,
            @Parameter(description = "连板情况") @RequestParam(required = false) String limitStatus,
            @Parameter(description = "流通市值最小值") @RequestParam(required = false) Double listedStockMin,
            @Parameter(description = "流通市值最大值") @RequestParam(required = false) Double listedStockMax,
            @Parameter(description = "涨停封单额最小值") @RequestParam(required = false) Double orderTotalMin,
            @Parameter(description = "涨停封单额最大值") @RequestParam(required = false) Double orderTotalMax,
            @Parameter(description = "主力流入最小值") @RequestParam(required = false) Double volumeNetInMin,
            @Parameter(description = "主力流入最大值") @RequestParam(required = false) Double volumeNetInMax,
            @Parameter(description = "当前价格最小值") @RequestParam(required = false) Double priceMin,
            @Parameter(description = "当前价格最大值") @RequestParam(required = false) Double priceMax,
            @Parameter(description = "连板情况X") @RequestParam(required = false) Integer limitUpX,
            @Parameter(description = "连板情况N最小值") @RequestParam(required = false) Integer limitUpNMin,
            @Parameter(description = "连板情况N最大值") @RequestParam(required = false) Integer limitUpNMax,
            @Parameter(description = "连板情况M最小值") @RequestParam(required = false) Integer limitUpMMin,
            @Parameter(description = "连板情况M最大值") @RequestParam(required = false) Integer limitUpMMax,
            @Parameter(description = "主力强度最小值") @RequestParam(required = false) Double mainForcesMin,
            @Parameter(description = "主力强度最大值") @RequestParam(required = false) Double mainForcesMax,
            @Parameter(description = "主力成本最小值") @RequestParam(required = false) Double costMin,
            @Parameter(description = "主力成本最大值") @RequestParam(required = false) Double costMax,
            @Parameter(description = "主力浮盈最小值") @RequestParam(required = false) Double profitMin,
            @Parameter(description = "主力浮盈最大值") @RequestParam(required = false) Double profitMax,
            @Parameter(description = "主力流入最小值") @RequestParam(required = false) Double mainForcesInMin,
            @Parameter(description = "主力流入最大值") @RequestParam(required = false) Double mainForcesInMax,
            @Parameter(description = "主力分歧最小值") @RequestParam(required = false) Double divergencyMin,
            @Parameter(description = "主力分歧最大值") @RequestParam(required = false) Double divergencyMax,
            @Parameter(description = "主力撤买最小值") @RequestParam(required = false) Double mainForcesCBMin,
            @Parameter(description = "主力撤买最大值") @RequestParam(required = false) Double mainForcesCBMax,
            @Parameter(description = "主力撤卖最小值") @RequestParam(required = false) Double mainForcesCSMin,
            @Parameter(description = "主力撤卖最大值") @RequestParam(required = false) Double mainForcesCSMax,
            @Parameter(description = "主力买入次数最小值") @RequestParam(required = false) Integer mainForcesNtimesMin,
            @Parameter(description = "主力买入次数最大值") @RequestParam(required = false) Integer mainForcesNtimesMax,
            @Parameter(description = "主力卖出次数最小值") @RequestParam(required = false) Integer mainForcesStimesMin,
            @Parameter(description = "主力卖出次数最大值") @RequestParam(required = false) Integer mainForcesStimesMax,
            @Parameter(description = "买入体量最小值") @RequestParam(required = false) Double buyAvgAmountMin,
            @Parameter(description = "买入体量最大值") @RequestParam(required = false) Double buyAvgAmountMax,
            @Parameter(description = "卖出体量最小值") @RequestParam(required = false) Double sellAvgAmountMin,
            @Parameter(description = "卖出体量最大值") @RequestParam(required = false) Double sellAvgAmountMax,
            @Parameter(description = "标签ID") @RequestParam(required = false) Integer topicId,
            @Parameter(description = "标签名称") @RequestParam(required = false) String topicName,
            @Parameter(description = "标签颜色") @RequestParam(required = false) String color,
            @Parameter(description = "标签股票数量最小值") @RequestParam(required = false) Integer stockNumMin,
            @Parameter(description = "标签股票数量最大值") @RequestParam(required = false) Integer stockNumMax,
            @Parameter(description = "标签热度最小值") @RequestParam(required = false) Double topicHotMin,
            @Parameter(description = "标签热度最大值") @RequestParam(required = false) Double topicHotMax,
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @Parameter(description = "每页大小") @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        // 构建查询参数对象
        LimitUpStockQueryParam queryParam = new LimitUpStockQueryParam();
        queryParam.setWindCode(windCode);
        // 日期参数解析
        if (tradeDateStart != null && !tradeDateStart.isEmpty()) {
            queryParam.setTradeDateStart(java.time.LocalDate.parse(tradeDateStart));
        }
        if (tradeDateEnd != null && !tradeDateEnd.isEmpty()) {
            queryParam.setTradeDateEnd(java.time.LocalDate.parse(tradeDateEnd));
        }
        queryParam.setWindName(windName);
        queryParam.setFirstTimeStart(firstTimeStart);
        queryParam.setFirstTimeEnd(firstTimeEnd);
        queryParam.setLimitStatus(limitStatus);
        queryParam.setListedStockMin(listedStockMin);
        queryParam.setListedStockMax(listedStockMax);
        queryParam.setOrderTotalMin(orderTotalMin);
        queryParam.setOrderTotalMax(orderTotalMax);
        queryParam.setVolumeNetInMin(volumeNetInMin);
        queryParam.setVolumeNetInMax(volumeNetInMax);
        queryParam.setPriceMin(priceMin);
        queryParam.setPriceMax(priceMax);
        queryParam.setLimitUpX(limitUpX);
        queryParam.setLimitUpNMin(limitUpNMin);
        queryParam.setLimitUpNMax(limitUpNMax);
        queryParam.setLimitUpMMin(limitUpMMin);
        queryParam.setLimitUpMMax(limitUpMMax);
        queryParam.setMainForcesMin(mainForcesMin);
        queryParam.setMainForcesMax(mainForcesMax);
        queryParam.setCostMin(costMin);
        queryParam.setCostMax(costMax);
        queryParam.setProfitMin(profitMin);
        queryParam.setProfitMax(profitMax);
        queryParam.setMainForcesInMin(mainForcesInMin);
        queryParam.setMainForcesInMax(mainForcesInMax);
        queryParam.setDivergencyMin(divergencyMin);
        queryParam.setDivergencyMax(divergencyMax);
        queryParam.setMainForcesCBMin(mainForcesCBMin);
        queryParam.setMainForcesCBMax(mainForcesCBMax);
        queryParam.setMainForcesCSMin(mainForcesCSMin);
        queryParam.setMainForcesCSMax(mainForcesCSMax);
        queryParam.setMainForcesNtimesMin(mainForcesNtimesMin);
        queryParam.setMainForcesNtimesMax(mainForcesNtimesMax);
        queryParam.setMainForcesStimesMin(mainForcesStimesMin);
        queryParam.setMainForcesStimesMax(mainForcesStimesMax);
        queryParam.setBuyAvgAmountMin(buyAvgAmountMin);
        queryParam.setBuyAvgAmountMax(buyAvgAmountMax);
        queryParam.setSellAvgAmountMin(sellAvgAmountMin);
        queryParam.setSellAvgAmountMax(sellAvgAmountMax);
        queryParam.setTopicId(topicId);
        queryParam.setTopicName(topicName);
        queryParam.setColor(color);
        queryParam.setStockNumMin(stockNumMin);
        queryParam.setStockNumMax(stockNumMax);
        queryParam.setTopicHotMin(topicHotMin);
        queryParam.setTopicHotMax(topicHotMax);
        queryParam.setPageNo(pageNo);
        queryParam.setPageSize(pageSize);
        log.info("queryLimitUpStockList_start=queryParam={}", queryParam);
        List<LimitUpStockQueryResultVO> resultList = limitUpService.queryLimitUpStockList(queryParam);
        log.info("queryLimitUpStockList_success=result_count={}", resultList.size());
        return resultList;
    }

    @GetMapping("trade_limit_list")
    @Operation(summary = "交易日涨停映射列表", description = "根据查询条件获取交易日涨停股票代码列表")
    public Map<String, Set<String>> getLimitUpTradeDateMap(@Parameter(description = "交易日期开始") @RequestParam(required = false) String tradeDateStart,
                                                           @Parameter(description = "交易日期结束") @RequestParam(required = false) String tradeDateEnd) {
        return limitUpService.getLimitUpTradeDateMap(tradeDateStart, tradeDateEnd);
    }
}
