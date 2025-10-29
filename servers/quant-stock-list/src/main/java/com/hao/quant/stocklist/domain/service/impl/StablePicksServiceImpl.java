package com.hao.quant.stocklist.domain.service.impl;

import com.hao.quant.stocklist.application.assembler.StablePicksAssembler;
import com.hao.quant.stocklist.application.dto.StablePicksQueryDTO;
import com.hao.quant.stocklist.application.vo.StablePicksVO;
import com.hao.quant.stocklist.common.dto.PageResult;
import com.hao.quant.stocklist.common.exception.BusinessException;
import com.hao.quant.stocklist.domain.bloom.StablePicksBloomFilter;
import com.hao.quant.stocklist.domain.lock.StablePicksLockManager;
import com.hao.quant.stocklist.domain.model.StablePick;
import com.hao.quant.stocklist.domain.repository.StablePicksRepository;
import com.hao.quant.stocklist.domain.service.StablePicksService;
import com.hao.quant.stocklist.infrastructure.cache.CacheWrapper;
import com.hao.quant.stocklist.infrastructure.cache.StablePicksCacheKeyBuilder;
import com.hao.quant.stocklist.infrastructure.cache.StablePicksCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 每日精选股票领域服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StablePicksServiceImpl implements StablePicksService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_DATE;

    private final StablePicksRepository repository;
    private final StablePicksAssembler assembler;
    private final StablePicksCacheRepository cacheRepository;
    private final StablePicksCacheKeyBuilder cacheKeyBuilder;
    private final StablePicksBloomFilter bloomFilter;
    private final StablePicksLockManager lockManager;
    @Qualifier("stablePicksScheduler")
    private final TaskScheduler stablePicksScheduler;

    @Value("${stable-picks.cache.redis-ttl:PT10M}")
    private Duration redisTtl;

    @Value("${stable-picks.cache.lock-wait:PT1S}")
    private Duration lockWait;

    @Value("${stable-picks.cache.lock-lease:PT5S}")
    private Duration lockLease;

    @Override
    public PageResult<StablePicksVO> queryDailyPicks(StablePicksQueryDTO query) {
        LocalDate tradeDate = query.getTradeDate();
        if (tradeDate == null) {
            throw new BusinessException("交易日期不能为空");
        }
        int pageNum = query.getPageNum() == null || query.getPageNum() <= 0 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 20 : Math.min(query.getPageSize(), 200);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);

        if (!bloomFilter.mightContainTradeDate(tradeDate)) {
            log.warn("布隆过滤器拦截非法交易日: {}", tradeDate);
            return PageResult.empty(pageNum, pageSize);
        }

        String cacheKey = cacheKeyBuilder.buildDailyKey(query);
        Optional<CacheWrapper<PageResult<StablePicksVO>>> local = cacheRepository.getLocal(cacheKey);
        if (local.isPresent() && !local.get().isExpired()) {
            triggerAsyncRefreshIfNecessary(() -> refreshDailyCache(query, cacheKey), local.get());
            return local.get().getData();
        }

        Optional<CacheWrapper<PageResult<StablePicksVO>>> distributed = cacheRepository.getDistributed(cacheKey);
        if (distributed.isPresent() && !distributed.get().isExpired()) {
            cacheRepository.putLocal(cacheKey, distributed.get());
            triggerAsyncRefreshIfNecessary(() -> refreshDailyCache(query, cacheKey), distributed.get());
            return distributed.get().getData();
        }

        return lockManager.executeWithLock(
                "lock:" + cacheKey,
                lockWait,
                lockLease,
                () -> refreshDailyCache(query, cacheKey),
                () -> distributed.filter(wrapper -> !wrapper.isExpired())
                        .map(CacheWrapper::getData)
                        .orElse(PageResult.empty(pageNum, pageSize))
        );
    }

    @Override
    public List<StablePicksVO> queryLatestPicks(String strategyId, Integer limit) {
        int queryLimit = (limit == null || limit <= 0) ? 50 : Math.min(limit, 200);
        String cacheKey = cacheKeyBuilder.buildLatestKey(strategyId, queryLimit);

        Optional<CacheWrapper<List<StablePicksVO>>> local = cacheRepository.getLocal(cacheKey);
        if (local.isPresent() && !local.get().isExpired()) {
            triggerAsyncRefreshIfNecessary(() -> refreshLatestCache(strategyId, queryLimit, cacheKey), local.get());
            return local.get().getData();
        }

        Optional<CacheWrapper<List<StablePicksVO>>> distributed = cacheRepository.getDistributed(cacheKey);
        if (distributed.isPresent() && !distributed.get().isExpired()) {
            cacheRepository.putLocal(cacheKey, distributed.get());
            triggerAsyncRefreshIfNecessary(() -> refreshLatestCache(strategyId, queryLimit, cacheKey), distributed.get());
            return distributed.get().getData();
        }

        return lockManager.executeWithLock(
                "lock:" + cacheKey,
                lockWait,
                lockLease,
                () -> refreshLatestCache(strategyId, queryLimit, cacheKey),
                () -> distributed.filter(wrapper -> !wrapper.isExpired())
                        .map(CacheWrapper::getData)
                        .orElse(Collections.emptyList())
        );
    }

    @Override
    public StablePicksVO queryStockDetail(String stockCode, LocalDate tradeDate) {
        if (tradeDate == null) {
            throw new BusinessException("交易日期不能为空");
        }
        if (!bloomFilter.mightContainTradeDate(tradeDate)) {
            throw new BusinessException("非法交易日,请检查日期是否正确");
        }
        String cacheKey = cacheKeyBuilder.buildDetailKey(stockCode, DATE_FORMAT.format(tradeDate));
        Optional<CacheWrapper<StablePicksVO>> local = cacheRepository.getLocal(cacheKey);
        if (local.isPresent() && !local.get().isExpired()) {
            triggerAsyncRefreshIfNecessary(() -> refreshDetailCache(stockCode, tradeDate, cacheKey), local.get());
            return local.get().getData();
        }
        Optional<CacheWrapper<StablePicksVO>> distributed = cacheRepository.getDistributed(cacheKey);
        if (distributed.isPresent() && !distributed.get().isExpired()) {
            cacheRepository.putLocal(cacheKey, distributed.get());
            triggerAsyncRefreshIfNecessary(() -> refreshDetailCache(stockCode, tradeDate, cacheKey), distributed.get());
            return distributed.get().getData();
        }
        return lockManager.executeWithLock(
                "lock:" + cacheKey,
                lockWait,
                lockLease,
                () -> refreshDetailCache(stockCode, tradeDate, cacheKey),
                () -> distributed.filter(wrapper -> !wrapper.isExpired())
                        .map(CacheWrapper::getData)
                        .orElse(null)
        );
    }

    @Override
    public void manualRefreshCache(LocalDate tradeDate, String strategyId) {
        log.info("手动刷新缓存: tradeDate={}, strategyId={}", tradeDate, strategyId);
        if (tradeDate == null) {
            throw new BusinessException("交易日期不能为空");
        }
        StablePicksQueryDTO query = StablePicksQueryDTO.builder()
                .tradeDate(tradeDate)
                .strategyId(strategyId)
                .pageNum(1)
                .pageSize(50)
                .build();
        refreshDailyCache(query, cacheKeyBuilder.buildDailyKey(query));
        refreshLatestCache(strategyId, 50, cacheKeyBuilder.buildLatestKey(strategyId, 50));
    }

    @Override
    public void warmupCache(LocalDate tradeDate) {
        if (tradeDate == null) {
            throw new BusinessException("交易日期不能为空");
        }
        List<StablePick> picks = repository.queryDaily(tradeDate, null, null, 0, 200);
        if (CollectionUtils.isEmpty(picks)) {
            return;
        }
        bloomFilter.addTradeDate(tradeDate);
        StablePicksQueryDTO query = StablePicksQueryDTO.builder()
                .tradeDate(tradeDate)
                .pageNum(1)
                .pageSize(50)
                .build();
        refreshDailyCache(query, cacheKeyBuilder.buildDailyKey(query));
    }

    private PageResult<StablePicksVO> refreshDailyCache(StablePicksQueryDTO query, String cacheKey) {
        int offset = (query.getPageNum() - 1) * query.getPageSize();
        List<StablePick> picks = repository.queryDaily(query.getTradeDate(), query.getStrategyId(), query.getIndustry(), offset, query.getPageSize());
        long total = repository.countDaily(query.getTradeDate(), query.getStrategyId(), query.getIndustry());
        if (total == 0) {
            CacheWrapper<PageResult<StablePicksVO>> wrapper = CacheWrapper.of(PageResult.empty(query.getPageNum(), query.getPageSize()), redisTtl);
            cacheRepository.put(cacheKey, wrapper, redisTtl.plusSeconds(randomTtlOffset()));
            return wrapper.getData();
        }
        PageResult<StablePicksVO> page = PageResult.<StablePicksVO>builder()
                .total(total)
                .pageNum(query.getPageNum())
                .pageSize(query.getPageSize())
                .records(assembler.toView(picks))
                .build();
        CacheWrapper<PageResult<StablePicksVO>> wrapper = CacheWrapper.of(page, redisTtl);
        cacheRepository.put(cacheKey, wrapper, redisTtl.plusSeconds(randomTtlOffset()));
        return page;
    }

    private List<StablePicksVO> refreshLatestCache(String strategyId, int limit, String cacheKey) {
        List<StablePick> picks = repository.queryLatest(strategyId, limit);
        List<StablePicksVO> result = assembler.toView(picks);
        CacheWrapper<List<StablePicksVO>> wrapper = CacheWrapper.of(result, redisTtl);
        cacheRepository.put(cacheKey, wrapper, redisTtl.plusSeconds(randomTtlOffset()));
        bloomFilter.addTradeDates(picks.stream().map(StablePick::tradeDate).toList());
        return result;
    }

    private StablePicksVO refreshDetailCache(String stockCode, LocalDate tradeDate, String cacheKey) {
        Optional<StablePick> pickOpt = repository.findDetail(stockCode, tradeDate);
        StablePicksVO vo = pickOpt.map(assembler::toView).orElse(null);
        CacheWrapper<StablePicksVO> wrapper = CacheWrapper.of(vo, redisTtl);
        cacheRepository.put(cacheKey, wrapper, redisTtl.plusSeconds(randomTtlOffset()));
        return vo;
    }

    private void triggerAsyncRefreshIfNecessary(Runnable runnable, CacheWrapper<?> wrapper) {
        if (!wrapper.shouldRefreshAsync()) {
            return;
        }
        stablePicksScheduler.schedule(runnable, java.time.Instant.now());
    }

    private long randomTtlOffset() {
        return (long) (Math.random() * 120);
    }
}
