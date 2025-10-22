package com.hao.strategyengine.model.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * StrategyResultDto
 *
 * <p>策略结果传输对象（DTO），用于向外部系统或前端返回策略组合的执行结果。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *     <li>comboKey - 策略组合唯一标识，可用于缓存或快速查找</li>
 *     <li>results - 策略执行结果列表，可为任意类型，通常为 {@link StrategyResult} 或序列化后的数据</li>
 * </ul>
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * StrategyResultDto dto = StrategyResultDto.builder()
 *     .comboKey("comboKey123")
 *     .results(List.of(result1, result2))
 *     .build();
 * }</pre>
 *
 * <p>注意事项：</p>
 * <ul>
 *     <li>results 类型为 Object，建议在 DTO 层统一格式，例如全部序列化为 Map 或自定义 DTO，避免类型转换问题</li>
 *     <li>comboKey 应与策略执行的组合逻辑保持一致，保证缓存命中和查询正确</li>
 * </ul>
 *
 * Lombok 注解说明：
 * - @Data 提供 getter、setter、toString、equals、hashCode 方法
 * - @Builder 提供建造者模式，方便构造实例
 * </p>
 *
 * @author hli
 * @date 2025-10-22
 */
@Data
@Builder
public class StrategyResultDto {

    /** 策略组合唯一标识，用于缓存或快速查找 */
    private String comboKey;

    /** 策略执行结果列表，可为任意类型，建议统一格式 */
    private List<Object> results;
}
