package com.hao.quant.stocklist.infrastructure.messaging;

import com.hao.quant.stocklist.domain.bloom.StablePicksBloomFilter;
import com.hao.quant.stocklist.domain.event.StrategyResultEvent;
import com.hao.quant.stocklist.infrastructure.cache.StablePicksCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Kafka 事件监听器,负责缓存失效和布隆过滤器刷新。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StrategyResultEventListener {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_DATE;

    private final StablePicksCacheRepository cacheRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StablePicksBloomFilter bloomFilter;

    @KafkaListener(topics = "strategy.result.completed", groupId = "stable-picks-query")
    public void onMessage(StrategyResultEvent event, Acknowledgment ack) {
        if (event == null || StringUtils.isBlank(event.getEventId())) {
            ack.acknowledge();
            return;
        }
        try {
            log.info("接收策略结果事件: {}", event);
            bloomFilter.addTradeDate(event.getTradeDate());
            String dateKey = DATE_FORMAT.format(event.getTradeDate());
            String dailyPattern = "stable:picks:daily:" + dateKey + "*";
            evictPattern(dailyPattern);
            String latestPattern = "stable:picks:latest:*";
            evictPattern(latestPattern);
            String detailPattern = "stable:picks:detail:*:" + dateKey;
            evictPattern(detailPattern);
        } finally {
            ack.acknowledge();
        }
    }

    private void evictPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        keys.forEach(cacheRepository::evict);
        log.info("批量清理缓存,pattern={}, size={}", pattern, keys.size());
    }
}
