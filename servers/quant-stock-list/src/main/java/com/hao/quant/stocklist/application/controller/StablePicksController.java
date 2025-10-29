package com.hao.quant.stocklist.application.controller;

import com.hao.quant.stocklist.application.dto.StablePicksQueryDTO;
import com.hao.quant.stocklist.application.vo.StablePicksVO;
import com.hao.quant.stocklist.common.dto.PageResult;
import com.hao.quant.stocklist.common.dto.Result;
import com.hao.quant.stocklist.domain.service.StablePicksService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * API 接口层。
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stable-picks")
@Tag(name = "每日精选股票", description = "基于稳定策略的股票列表查询")
public class StablePicksController {

    private final StablePicksService stablePicksService;

    @GetMapping("/daily")
    @Operation(summary = "查询每日精选", description = "根据交易日期和策略ID查询股票列表")
    @RateLimiter(name = "stablePicksQuery", fallbackMethod = "rateLimitFallback")
    public Result<PageResult<StablePicksVO>> queryDailyPicks(
            @RequestParam @NotNull(message = "交易日期不能为空")
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradeDate,
            @RequestParam(required = false) String strategyId,
            @RequestParam(required = false) String industry,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        log.info("查询每日精选: tradeDate={}, strategyId={}, industry={}", tradeDate, strategyId, industry);

        StablePicksQueryDTO query = StablePicksQueryDTO.builder()
                .tradeDate(tradeDate)
                .strategyId(strategyId)
                .industry(industry)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();

        PageResult<StablePicksVO> result = stablePicksService.queryDailyPicks(query);
        return Result.success(result);
    }

    @GetMapping("/latest")
    @Operation(summary = "查询最新精选", description = "获取最近一个交易日的股票列表")
    @RateLimiter(name = "stablePicksLatest", fallbackMethod = "rateLimitListFallback")
    public Result<List<StablePicksVO>> queryLatestPicks(
            @RequestParam(required = false) String strategyId,
            @RequestParam(defaultValue = "50") Integer limit) {
        log.info("查询最新精选: strategyId={}, limit={}", strategyId, limit);
        return Result.success(stablePicksService.queryLatestPicks(strategyId, limit));
    }

    @GetMapping("/detail/{stockCode}")
    @Operation(summary = "查询股票详情", description = "查询指定股票在某日的策略详情")
    @RateLimiter(name = "stablePicksDetail", fallbackMethod = "rateLimitDetailFallback")
    public Result<StablePicksVO> queryStockDetail(
            @PathVariable String stockCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradeDate) {
        log.info("查询股票详情: stockCode={}, tradeDate={}", stockCode, tradeDate);
        return Result.success(stablePicksService.queryStockDetail(stockCode, tradeDate));
    }

    @PostMapping("/cache/refresh")
    @Operation(summary = "刷新缓存", description = "手动触发指定日期的缓存刷新")
    public Result<Void> refreshCache(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradeDate,
            @RequestParam(required = false) String strategyId) {
        log.info("手动刷新缓存: tradeDate={}, strategyId={}", tradeDate, strategyId);
        stablePicksService.manualRefreshCache(tradeDate, strategyId);
        return Result.success();
    }

    @PostMapping("/cache/warmup")
    @Operation(summary = "预热缓存", description = "交易日开盘前预热当日数据")
    public Result<Void> warmupCache(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradeDate) {
        log.info("预热缓存: tradeDate={}", tradeDate);
        stablePicksService.warmupCache(tradeDate);
        return Result.success();
    }

    public Result<PageResult<StablePicksVO>> rateLimitFallback(LocalDate tradeDate, String strategyId, String industry, Integer pageNum, Integer pageSize, Throwable ex) {
        log.warn("每日精选限流降级: {}", ex.getMessage());
        return Result.failure(429, "请求过于频繁,请稍后再试");
    }

    public Result<List<StablePicksVO>> rateLimitListFallback(String strategyId, Integer limit, Throwable ex) {
        log.warn("最新精选限流降级: {}", ex.getMessage());
        return Result.failure(429, "请求过于频繁,请稍后再试");
    }

    public Result<StablePicksVO> rateLimitDetailFallback(String stockCode, LocalDate tradeDate, Throwable ex) {
        log.warn("股票详情限流降级: {}", ex.getMessage());
        return Result.failure(429, "请求过于频繁,请稍后再试");
    }
}
