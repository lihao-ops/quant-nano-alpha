package com.hao.datacollector.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

    public static <T> void handleExtremeValues(List<T> list) {
        handleExtremeValues(list, null);
    }

    public static <T> void handleExtremeValues(List<T> list, Map<String, Number> customMaxValues) {
        if (list == null || list.isEmpty()) return;
        warnLogCount.set(0); // 每轮处理重置
        for (T obj : list) {
            handleSingleObject(obj, customMaxValues);
        }
    }

    private static <T> void handleSingleObject(T obj, Map<String, Number> customMaxValues) {
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
                    logWarnOnce("字段 {} 是非法值 {}（如 NaN / Infinity），已设置为默认值", fieldName, numValue);
                } else if (isExceedThreshold(numValue, maxValue, fieldType)) {
                    setDefaultValue(obj, field, fieldType);
                    logWarnOnce("字段 {} 超过阈值 {}，实际值为 {}，已设置为默认值", fieldName, maxValue, numValue);
                }

            } catch (Exception e) {
                log.error("处理字段 {} 时发生异常", field.getName(), e);
            }
        }
    }

    private static boolean isNumericType(Class<?> type) {
        return type == double.class || type == Double.class ||
                type == float.class || type == Float.class ||
                type == int.class || type == Integer.class ||
                type == long.class || type == Long.class;
    }

    private static Number getMaxValue(String fieldName, Class<?> fieldType, Map<String, Number> customMaxValues) {
        if (customMaxValues != null && customMaxValues.containsKey(fieldName)) {
            return customMaxValues.get(fieldName);
        }
        return DEFAULT_MAX_VALUES.get(fieldType);
    }

    private static boolean isIllegalValue(Number value, Class<?> fieldType) {
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
        if (warnLogCount.getAndIncrement() < MAX_WARN_LOGS) {
            log.warn(message, args);
        } else if (warnLogCount.get() == MAX_WARN_LOGS + 1) {
            log.warn("日志输出已达到上限（{} 条），后续将不再打印...", MAX_WARN_LOGS);
        }
    }
}
