package com.hao.strategyengine.event.listener;

import com.hao.strategyengine.event.StrategyEvent;

/**
 * 策略事件监听器接口
 */
public interface StrategyEventListener {
    
    /**
     * 处理策略事件
     */
    void onEvent(StrategyEvent event);
    
    /**
     * 监听器名称
     */
    String getName();
}
