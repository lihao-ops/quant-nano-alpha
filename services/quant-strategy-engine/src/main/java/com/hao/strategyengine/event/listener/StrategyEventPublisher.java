package com.hao.strategyengine.event.listener;

import com.hao.strategyengine.event.StrategyEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 事件发布器
 */
@Slf4j
@Component
public class StrategyEventPublisher {
    
    private List<StrategyEventListener> listeners = new ArrayList<>();
    private ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    /**
     * 注册监听器
     */
    public void registerListener(StrategyEventListener listener) {
        listeners.add(listener);
        log.info("注册策略事件监听器: {}", listener.getName());
    }

    /**
     * 注销监听器
     */
    public void unregisterListener(StrategyEventListener listener) {
        listeners.remove(listener);
        log.info("注销策略事件监听器: {}", listener.getName());
    }

    /**
     * 发布事件（同步）
     */
    public void publish(StrategyEvent event) {
        log.debug("发布策略事件: type={}, strategy={}, symbol={}",
                event.getType(), event.getStrategyName(), event.getSymbol());

        for (StrategyEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.error("监听器处理事件失败: listener={}, event={}",
                        listener.getName(), event.getType(), e);
            }
        }
    }

    /**
     * 发布事件（异步）
     */
    public void publishAsync(StrategyEvent event) {
        executorService.submit(() -> publish(event));
    }

    /**
     * 关闭发布器
     */
    public void shutdown() {
        executorService.shutdown();
        log.info("策略事件发布器已关闭");
    }
}
