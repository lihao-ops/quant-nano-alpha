package com.hao.strategyengine.common.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

/**
 * StrategyResultBundle
 *
 * <p>策略结果集合对象，用于封装多个策略执行结果的打包信息。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *     <li>comboKey - 策略组合唯一标识，用于缓存或快速查找策略结果</li>
 *     <li>results - 多个策略执行结果的列表，每个元素为 {@link StrategyResult}</li>
 * </ul>
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * List<StrategyResult> resultList = List.of(result1, result2);
 * StrategyResultBundle bundle = new StrategyResultBundle("comboKey123", resultList);
 * }</pre>
 *
 * <p>注意事项：</p>
 * <ul>
 *     <li>comboKey 应保证唯一性，可通过策略ID列表生成 MD5 或其他哈希值</li>
 *     <li>results 列表的顺序可根据策略执行顺序或优先级排列</li>
 * </ul>
 *
 * Lombok 注解说明：
 * - @Data 提供 getter、setter、toString、equals、hashCode 方法
 * - @AllArgsConstructor 提供全参数构造器，方便快速初始化实例
 * </p>
 *
 * @author hli
 * @date 2025-10-22
 */
@Data
@AllArgsConstructor
public class StrategyResultBundle {

    /** 策略组合唯一标识，用于缓存或快速查找 */
    private String comboKey;

    /** 多个策略执行结果列表 */
    private List<StrategyResult> results;
}
