package com.hao.quant.stocklist.infrastructure.task;

import com.hao.quant.stocklist.domain.repository.StablePicksRepository;
import com.hao.quant.stocklist.domain.service.StablePicksService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 预热任务,在开盘前预加载热点缓存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StablePicksWarmupTask {

    private final StablePicksRepository repository;
    private final StablePicksService stablePicksService;

    @Scheduled(cron = "0 0 8 * * MON-FRI")
    public void warmupToday() {
        List<LocalDate> tradeDates = repository.listRecentTradeDates(1);
        if (tradeDates.isEmpty()) {
            return;
        }
        LocalDate latest = tradeDates.getFirst();
        log.info("执行晨间预热任务,tradeDate={}", latest);
        stablePicksService.warmupCache(latest);
    }
}
