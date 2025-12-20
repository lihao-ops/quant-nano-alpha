package com.hao.quant.stocklist.application.controller;

/**
 * 类说明 / Class Description:
 * 中文：稳定精选股票的对外接口控制器，支持分页查询、最新列表、详情与缓存管理，并通过限流保护服务稳定性。
 * English: External API controller for stable picks; supports paged query, latest list, detail and cache management, protected by rate limiting.
 *
 * 使用场景 / Use Cases:
 * 中文：面向前端或第三方服务提供精选股票查询能力与缓存操作入口。
 * English: Provides query capabilities and cache operations for selected stocks to frontend or third-party services.
 *
 * 设计目的 / Design Purpose:
 * 中文：将应用服务能力以REST形式暴露，统一限流与兜底策略，提升稳定性与可维护性。
 * English: Expose application services via REST with unified rate-limiting and fallbacks to improve stability and maintainability.
 */

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
 * <p>
 * 对外暴露查询接口,并通过限流注解保护服务稳定性。
 * </p>
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stable-picks")
@Tag(name = "每日精选股票", description = "基于稳定策略的股票列表查询")
public class StablePicksController {

    private final StablePicksService stablePicksService;

    /**
     * 方法说明 / Method Description:
     * 中文：查询指定交易日的精选股票分页列表，支持按策略与行业过滤。
     * English: Query paged list of stable picks for a given trade date with optional strategy and industry filters.
     *
     * 参数 / Parameters:
     * @param tradeDate 中文：交易日期 / English: trade date
     * @param strategyId 中文：策略ID（可选） / English: strategy ID (optional)
     * @param industry 中文：行业（可选） / English: industry (optional)
     * @param pageNum 中文：页码（默认1） / English: page number (default 1)
     * @param pageSize 中文：每页数量（默认20） / English: page size (default 20)
     *
     * 返回值 / Return:
     * 中文：分页结果包装（含列表与分页元数据） / English: paged result wrapper containing list and pagination metadata
     *
     * 异常 / Exceptions:
     * 中文：参数校验异常（日期为空）、限流触发时走兜底方法 / English: validation exceptions (null date), rate-limit triggers fallback method
     */
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

        log.info("查询每日精选|Daily_picks_query,tradeDate={},strategyId={},industry={}", tradeDate, strategyId, industry);

        StablePicksQueryDTO query = StablePicksQueryDTO.builder()
                .tradeDate(tradeDate)
                .strategyId(strategyId)
                .industry(industry)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();

        // 中文：调用应用服务执行查询并封装分页结果
        // English: Invoke application service to execute query and wrap paged result
        PageResult<StablePicksVO> result = stablePicksService.queryDailyPicks(query);
        return Result.success(result);
    }

    /**
     * 方法说明 / Method Description:
     * 中文：查询最近一个交易日的精选股票列表，支持策略过滤与返回数量限制。
     * English: Query stable picks for the latest trade date with optional strategy filter and limit.
     *
     * 参数 / Parameters:
     * @param strategyId 中文：策略ID（可选） / English: strategy ID (optional)
     * @param limit 中文：返回数量上限（默认50） / English: max number of items to return (default 50)
     *
     * 返回值 / Return:
     * 中文：精选股票视图对象列表 / English: list of stable pick VOs
     *
     * 异常 / Exceptions:
     * 中文：限流触发时走兜底方法 / English: rate-limit triggers fallback method
     */
    @GetMapping("/latest")
    @Operation(summary = "查询最新精选", description = "获取最近一个交易日的股票列表")
    @RateLimiter(name = "stablePicksLatest", fallbackMethod = "rateLimitListFallback")
    public Result<List<StablePicksVO>> queryLatestPicks(
            @RequestParam(required = false) String strategyId,
            @RequestParam(defaultValue = "50") Integer limit) {
        log.info("查询最新精选|Latest_picks_query,strategyId={},limit={}", strategyId, limit);
        // 中文：拉取最新交易日数据并返回成功结果
        // English: Fetch latest trade day data and return success result
        return Result.success(stablePicksService.queryLatestPicks(strategyId, limit));
    }

    /**
     * 方法说明 / Method Description:
     * 中文：查询指定股票在给定交易日的策略详情。
     * English: Query strategy detail of a specific stock for a given trade date.
     *
     * 参数 / Parameters:
     * @param stockCode 中文：股票代码 / English: stock code
     * @param tradeDate 中文：交易日期 / English: trade date
     *
     * 返回值 / Return:
     * 中文：股票策略详情视图对象 / English: stock strategy detail VO
     *
     * 异常 / Exceptions:
     * 中文：限流触发时走兜底方法 / English: rate-limit triggers fallback method
     */
    @GetMapping("/detail/{stockCode}")
    @Operation(summary = "查询股票详情", description = "查询指定股票在某日的策略详情")
    @RateLimiter(name = "stablePicksDetail", fallbackMethod = "rateLimitDetailFallback")
    public Result<StablePicksVO> queryStockDetail(
            @PathVariable String stockCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradeDate) {
        log.info("查询股票详情|Stock_detail_query,stockCode={},tradeDate={}", stockCode, tradeDate);
        // 中文：调用应用服务拉取详情并返回
        // English: Call application service to fetch detail and return
        return Result.success(stablePicksService.queryStockDetail(stockCode, tradeDate));
    }

    /**
     * 方法说明 / Method Description:
     * 中文：手动刷新指定日期的缓存以保证读性能与数据新鲜度。
     * English: Manually refresh cache for given date to ensure read performance and freshness.
     *
     * 参数 / Parameters:
     * @param tradeDate 中文：交易日期 / English: trade date
     * @param strategyId 中文：策略ID（可选） / English: strategy ID (optional)
     *
     * 返回值 / Return:
     * 中文：操作结果（成功/失败） / English: operation result (success/failure)
     *
     * 异常 / Exceptions:
     * 中文：参数异常或下游服务异常需记录并返回失败信息 / English: parameter or downstream exceptions should be logged and failure returned
     */
    @PostMapping("/cache/refresh")
    @Operation(summary = "刷新缓存", description = "手动触发指定日期的缓存刷新")
    public Result<Void> refreshCache(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradeDate,
            @RequestParam(required = false) String strategyId) {
        log.info("手动刷新缓存|Manual_cache_refresh,tradeDate={},strategyId={}", tradeDate, strategyId);
        // 中文：调用应用服务刷新缓存
        // English: Invoke application service to refresh cache
        stablePicksService.manualRefreshCache(tradeDate, strategyId);
        return Result.success();
    }

    /**
     * 方法说明 / Method Description:
     * 中文：在开盘前预热当日数据以降低首次查询延迟。
     * English: Warm up the day's data before market open to reduce first query latency.
     *
     * 参数 / Parameters:
     * @param tradeDate 中文：交易日期 / English: trade date
     *
     * 返回值 / Return:
     * 中文：操作结果（成功/失败） / English: operation result (success/failure)
     *
     * 异常 / Exceptions:
     * 中文：下游服务异常需记录并返回失败信息 / English: downstream exceptions should be logged and failure returned
     */
    @PostMapping("/cache/warmup")
    @Operation(summary = "预热缓存", description = "交易日开盘前预热当日数据")
    public Result<Void> warmupCache(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradeDate) {
        log.info("预热缓存|Cache_warmup,tradeDate={}", tradeDate);
        // 中文：调用应用服务执行预热
        // English: Invoke application service to warm up
        stablePicksService.warmupCache(tradeDate);
        return Result.success();
    }

    /**
     * 方法说明 / Method Description:
     * 中文：每日精选接口限流后的兜底处理，返回统一失败结构。
     * English: Fallback for daily picks API after rate limit; returns a unified failure structure.
     *
     * 参数 / Parameters:
     * @param tradeDate 中文：交易日期 / English: trade date
     * @param strategyId 中文：策略ID（可选） / English: strategy ID (optional)
     * @param industry 中文：行业（可选） / English: industry (optional)
     * @param pageNum 中文：页码 / English: page number
     * @param pageSize 中文：页大小 / English: page size
     * @param ex 中文：触发的异常 / English: triggering exception
     *
     * 返回值 / Return:
     * 中文：统一失败响应（429） / English: unified failure response (429)
     *
     * 异常 / Exceptions:
     * 中文：无额外异常，记录告警日志 / English: none; logs a warning
     */
    public Result<PageResult<StablePicksVO>> rateLimitFallback(LocalDate tradeDate, String strategyId, String industry, Integer pageNum, Integer pageSize, Throwable ex) {
        log.warn("每日精选限流降级|Daily_picks_rate_limit_fallback,error={}", ex.getMessage());
        return Result.failure(429, "请求过于频繁,请稍后再试");
    }

    /**
     * 方法说明 / Method Description:
     * 中文：最新列表接口限流后的兜底处理，返回统一失败结构。
     * English: Fallback for latest list API after rate limit; returns a unified failure structure.
     *
     * 参数 / Parameters:
     * @param strategyId 中文：策略ID（可选） / English: strategy ID (optional)
     * @param limit 中文：返回数量限制 / English: result limit
     * @param ex 中文：触发的异常 / English: triggering exception
     *
     * 返回值 / Return:
     * 中文：统一失败响应（429） / English: unified failure response (429)
     *
     * 异常 / Exceptions:
     * 中文：无额外异常，记录告警日志 / English: none; logs a warning
     */
    public Result<List<StablePicksVO>> rateLimitListFallback(String strategyId, Integer limit, Throwable ex) {
        log.warn("最新精选限流降级|Latest_picks_rate_limit_fallback,error={}", ex.getMessage());
        return Result.failure(429, "请求过于频繁,请稍后再试");
    }

    /**
     * 方法说明 / Method Description:
     * 中文：股票详情接口限流后的兜底处理，返回统一失败结构。
     * English: Fallback for stock detail API after rate limit; returns a unified failure structure.
     *
     * 参数 / Parameters:
     * @param stockCode 中文：股票代码 / English: stock code
     * @param tradeDate 中文：交易日期 / English: trade date
     * @param ex 中文：触发的异常 / English: triggering exception
     *
     * 返回值 / Return:
     * 中文：统一失败响应（429） / English: unified failure response (429)
     *
     * 异常 / Exceptions:
     * 中文：无额外异常，记录告警日志 / English: none; logs a warning
     */
    public Result<StablePicksVO> rateLimitDetailFallback(String stockCode, LocalDate tradeDate, Throwable ex) {
        log.warn("股票详情限流降级|Stock_detail_rate_limit_fallback,error={}", ex.getMessage());
        return Result.failure(429, "请求过于频繁,请稍后再试");
    }
}
