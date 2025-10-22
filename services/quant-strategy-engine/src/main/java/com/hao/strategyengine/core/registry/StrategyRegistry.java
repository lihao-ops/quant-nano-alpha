package com.hao.strategyengine.core.registry;

import com.hao.strategyengine.strategy.QuantStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * StrategyRegistry
 *
 * <p>策略注册中心，用于管理系统中所有可用的策略实例。
 * 将策略Bean注入并按策略ID建立映射，方便通过ID快速获取策略。</p>
 *
 * <p>职责说明：</p>
 * <ul>
 *     <li>初始化策略映射表，将所有注入的 QuantStrategy Bean 以 ID 为 key 存储</li>
 *     <li>提供策略获取接口 get(String id)</li>
 *     <li>提供策略ID集合查询 ids()</li>
 * </ul>
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * QuantStrategy strategy = registry.get("strategy1");
 * Set<String> allIds = registry.ids();
 * }</pre>
 *
 * <p>注意事项：</p>
 * <ul>
 *     <li>要求每个策略Bean的 getId() 返回唯一值，否则初始化时会抛出异常</li>
 *     <li>ids() 返回不可变集合，避免外部修改内部映射</li>
 * </ul>
 *
 * @author hli
 * @date 2025-10-22
 */
@Component
public class StrategyRegistry {

    /** 存储策略ID -> 策略实例的映射 */
    private final Map<String, QuantStrategy> strategyMap;

    /**
     * 构造方法
     *
     * @param strategyBeans Spring 注入的所有 QuantStrategy Bean 列表
     */
    @Autowired
    public StrategyRegistry(List<QuantStrategy> strategyBeans) {
        this.strategyMap = strategyBeans.stream()
                .collect(Collectors.toMap(QuantStrategy::getId, Function.identity()));
    }

    /**
     * 根据策略ID获取策略实例
     *
     * @param id 策略ID
     * @return 对应的 QuantStrategy 实例，如果不存在则返回 null
     */
    public QuantStrategy get(String id) {
        return strategyMap.get(id);
    }

    /**
     * 获取所有策略ID
     *
     * @return 不可变的策略ID集合
     */
    public Set<String> ids() {
        return Collections.unmodifiableSet(strategyMap.keySet());
    }
}
