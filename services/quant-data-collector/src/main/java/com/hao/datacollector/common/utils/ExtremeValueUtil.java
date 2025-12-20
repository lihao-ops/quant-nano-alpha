package com.hao.datacollector.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 极值处理工具类
 *
 * 设计目的：
 * 1. 统一处理数值字段中的非法值与异常极值。
 * 2. 降低极端数据对后续计算的影响。
 *
 * 为什么需要该类：
 * - 数据源可能存在NaN、Infinity等异常值，需要统一兜底。
 *
 * 核心实现思路：
 * - 通过反射扫描对象字段，发现异常值后置为默认值。
 * - 对告警日志进行限流，避免刷屏。
 */
@Slf4j
public class ExtremeValueUtil {

    private static final Map<Class<?>, Number> DEFAULT_MAX_VALUES = new HashMap<>();
    private static final int MAX_WARN_LOGS = 100; // 限制日志输出数量
    private static final AtomicInteger warnLogCount = new AtomicInteger(0);

    static {
        DEFAULT_MAX_VALUES.put(Double.class, 1e9);  // 10亿
        DEFAULT_MAX_VALUES.put(double.class, 1e9);
        DEFAULT_MAX_VALUES.put(Float.class, 1e9f);
        DEFAULT_MAX_VALUES.put(float.class, 1e9f);
        DEFAULT_MAX_VALUES.put(Integer.class, 999999999);
        DEFAULT_MAX_VALUES.put(int.class, 999999999);
        DEFAULT_MAX_VALUES.put(Long.class, 999999999999L);
        DEFAULT_MAX_VALUES.put(long.class, 999999999999L);
    }

    /**
     * 处理列表对象中的异常极值
     *
     * 实现逻辑：
     * 1. 使用默认阈值处理列表对象。
     *
     * @param list 待处理对象列表
     * @param <T> 对象类型
     */
    public static <T> void handleExtremeValues(List<T> list) {
        // 实现思路：
        // 1. 委托到带自定义阈值的方法。
        handleExtremeValues(list, null);
    }

    /**
     * 处理列表对象中的异常极值（支持自定义阈值）
     *
     * 实现逻辑：
     * 1. 遍历列表并逐个处理对象字段。
     * 2. 对每轮处理重置告警计数。
     *
     * @param list 待处理对象列表
     * @param customMaxValues 自定义阈值
     * @param <T> 对象类型
     */
    public static <T> void handleExtremeValues(List<T> list, Map<String, Number> customMaxValues) {
        // 实现思路：
        // 1. 逐对象处理字段值。
        // 2. 每轮处理重置告警计数。
        if (list == null || list.isEmpty()) return;
        warnLogCount.set(0); // 每轮处理重置
        for (T obj : list) {
            handleSingleObject(obj, customMaxValues);
        }
    }

    /**
     * 处理单个对象字段中的异常极值
     *
     * 实现逻辑：
     * 1. 扫描字段并判断是否为数值类型。
     * 2. 对非法值与超阈值进行兜底处理。
     *
     * @param obj 目标对象
     * @param customMaxValues 自定义阈值
     * @param <T> 对象类型
     */
    private static <T> void handleSingleObject(T obj, Map<String, Number> customMaxValues) {
        // 实现思路：
        // 1. 反射读取字段值并做阈值判断。
        // 2. 异常值置为默认值并限流告警。
        if (obj == null) return;

        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value == null || !isNumericType(field.getType())) continue;

                Class<?> fieldType = field.getType();
                String fieldName = field.getName();
                Number numValue = (Number) value;
                Number maxValue = getMaxValue(fieldName, fieldType, customMaxValues);

                // 非法值（如 NaN, Infinity, MAX_VALUE）
                if (isIllegalValue(numValue, fieldType)) {
                    setDefaultValue(obj, field, fieldType);
                    logWarnOnce("字段非法值|Field_illegal_value,fieldName={},value={},action=reset_default",
                            fieldName, numValue);
                } else if (isExceedThreshold(numValue, maxValue, fieldType)) {
                    setDefaultValue(obj, field, fieldType);
                    logWarnOnce("字段超过阈值|Field_exceed_threshold,fieldName={},threshold={},value={},action=reset_default",
                            fieldName, maxValue, numValue);
                }

            } catch (Exception e) {
                log.error("字段处理异常|Field_handle_error,fieldName={}", field.getName(), e);
            }
        }
    }

    /**
     * 判断字段类型是否为数值类型
     *
     * 实现逻辑：
     * 1. 判断是否为常见基础数值类型。
     *
     * @param type 字段类型
     * @return 是否为数值类型
     */
    private static boolean isNumericType(Class<?> type) {
        // 实现思路：
        // 1. 仅允许基础数值类型进入处理。
        return type == double.class || type == Double.class ||
                type == float.class || type == Float.class ||
                type == int.class || type == Integer.class ||
                type == long.class || type == Long.class;
    }

    /**
     * 获取字段最大阈值
     *
     * 实现逻辑：
     * 1. 优先读取自定义阈值。
     * 2. 否则回退到默认阈值。
     *
     * @param fieldName 字段名
     * @param fieldType 字段类型
     * @param customMaxValues 自定义阈值
     * @return 最大阈值
     */
    private static Number getMaxValue(String fieldName, Class<?> fieldType, Map<String, Number> customMaxValues) {
        // 实现思路：
        // 1. 自定义阈值优先。
        // 2. 兜底使用默认阈值。
        if (customMaxValues != null && customMaxValues.containsKey(fieldName)) {
            return customMaxValues.get(fieldName);
        }
        return DEFAULT_MAX_VALUES.get(fieldType);
    }

    /**
     * 判断数值是否合法
     *
     * 实现逻辑：
     * 1. 检查NaN、Infinity与MAX_VALUE。
     *
     * @param value 数值
     * @param fieldType 字段类型
     * @return 是否为非法值
     */
    private static boolean isIllegalValue(Number value, Class<?> fieldType) {
        // 实现思路：
        // 1. 对浮点类型做NaN与Infinity判断。
        if (fieldType == double.class || fieldType == Double.class) {
            double v = value.doubleValue();
            return Double.isNaN(v) || Double.isInfinite(v) || v == Double.MAX_VALUE;
        }
        if (fieldType == float.class || fieldType == Float.class) {
            float v = value.floatValue();
            return Float.isNaN(v) || Float.isInfinite(v) || v == Float.MAX_VALUE;
        }
        return false;
    }

    private static boolean isExceedThreshold(Number value, Number maxValue, Class<?> fieldType) {
        // 实现思路：
        // 1. 根据类型对比阈值。
        if (maxValue == null) return false;
        if (fieldType == double.class || fieldType == Double.class) {
            return value.doubleValue() > maxValue.doubleValue();
        }
        if (fieldType == float.class || fieldType == Float.class) {
            return value.floatValue() > maxValue.floatValue();
        }
        return value.longValue() > maxValue.longValue();
    }

    private static void setDefaultValue(Object obj, Field field, Class<?> fieldType) throws IllegalAccessException {
        // 实现思路：
        // 1. 按字段类型设置默认值。
        if (fieldType == double.class || fieldType == Double.class) {
            field.set(obj, 0.0);
        } else if (fieldType == float.class || fieldType == Float.class) {
            field.set(obj, 0.0f);
        } else if (fieldType == int.class || fieldType == Integer.class) {
            field.set(obj, 0);
        } else if (fieldType == long.class || fieldType == Long.class) {
            field.set(obj, 0L);
        }
    }

    private static void logWarnOnce(String message, Object... args) {
        // 实现思路：
        // 1. 控制告警日志数量，避免刷屏。
        if (warnLogCount.getAndIncrement() < MAX_WARN_LOGS) {
            log.warn(message, args);
        } else if (warnLogCount.get() == MAX_WARN_LOGS + 1) {
            log.warn("告警日志达到上限|Warn_log_limit_reached,limit={}", MAX_WARN_LOGS);
        }
    }
}
