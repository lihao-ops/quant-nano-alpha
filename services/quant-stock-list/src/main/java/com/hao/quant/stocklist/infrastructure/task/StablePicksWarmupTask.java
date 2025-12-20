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
 * <p>
 * 结合领域服务的 warmup 能力,定时加载最近交易日的数据到缓存,减少首波请求延迟。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StablePicksWarmupTask {

    private final StablePicksRepository repository;
    private final StablePicksService stablePicksService;

    /**
     * 在工作日早上 8 点执行缓存预热。
     */
    @Scheduled(cron = "0 0 8 * * MON-FRI")
    public void warmupToday() {
        List<LocalDate> tradeDates = repository.listRecentTradeDates(1);
        if (tradeDates.isEmpty()) {
            return;
        }
        LocalDate latest = tradeDates.getFirst();
        log.info("晨间预热任务执行|Warmup_task_run,tradeDate={}", latest);
        // 调用领域服务预热缓存
        stablePicksService.warmupCache(latest);
    }
}
