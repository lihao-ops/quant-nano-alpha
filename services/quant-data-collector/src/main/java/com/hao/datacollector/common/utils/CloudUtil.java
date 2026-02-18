package com.hao.datacollector.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 云数据处理工具类
 *
 * @author antigravity
 * @description 用于将云端返回的 List<List<Object>> 映射为 DTO 对象。
 */
@Slf4j
public class CloudUtil {

    /**
     * 将多个报告的结果映射到单个 DTO 类中
     * 简单的实现转换逻辑：按顺序将结果列表中的值赋给 DTO 的字段
     *
     * @param clazz      目标 DTO 类
     * @param resultList 云端返回的结果列表集（每个元素代表一个报告的结果）
     * @param <T>        目标类型
     * @return 映射后的 DTO 对象
     */
    public static <T> T mapToDTO(Class<T> clazz, List<List<List<Object>>> resultList) {
        try {
            T target = clazz.getDeclaredConstructor().newInstance();
            Field[] fields = clazz.getDeclaredFields();
            int fieldIndex = 0;

            for (List<List<Object>> report : resultList) {
                if (report == null || report.isEmpty()) {
                    continue;
                }
                // 通常一个报告对应一条记录（第一项）
                List<Object> row = report.get(0);
                for (Object value : row) {
                    if (fieldIndex < fields.length) {
                        Field field = fields[fieldIndex++];
                        field.setAccessible(true);
                        setFieldValue(target, field, value);
                    }
                }
            }
            return target;
        } catch (Exception e) {
            log.error("CloudUtil mapping error: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 将单个报告的结果映射为 DTO 列表
     *
     * @param clazz  目标 DTO 类
     * @param report 报告结果 (List<List<Object>>)
     * @param <T>    目标类型
     * @return 映射后的 DTO 列表
     */
    public static <T> List<T> mapToList(Class<T> clazz, List<List<Object>> report) {
        List<T> result = new ArrayList<>();
        if (report == null || report.isEmpty()) {
            return result;
        }
        Field[] fields = clazz.getDeclaredFields();
        for (List<Object> row : report) {
            try {
                T target = clazz.getDeclaredConstructor().newInstance();
                for (int i = 0; i < Math.min(fields.length, row.size()); i++) {
                    Field field = fields[i];
                    field.setAccessible(true);
                    setFieldValue(target, field, row.get(i));
                }
                result.add(target);
            } catch (Exception e) {
                log.error("CloudUtil list mapping error: {}", e.getMessage(), e);
            }
        }
        return result;
    }

    private static void setFieldValue(Object target, Field field, Object value) throws IllegalAccessException {
        if (value == null || "null".equals(value.toString())) {
            return;
        }
        Class<?> type = field.getType();
        String strValue = value.toString();

        if (type == String.class) {
            field.set(target, strValue);
        } else if (type == Double.class || type == double.class) {
            field.set(target, Double.valueOf(strValue));
        } else if (type == Integer.class || type == int.class) {
            field.set(target, (int) Double.parseDouble(strValue));
        } else if (type == Long.class || type == long.class) {
            field.set(target, (long) Double.parseDouble(strValue));
        } else if (type == Boolean.class || type == boolean.class) {
            field.set(target, Boolean.valueOf(strValue));
        }
    }
}
