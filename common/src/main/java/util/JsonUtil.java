package util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具类
 * <p>
 * 类职责：
 * 提供对象与 JSON 字符串之间的相互转换能力。
 *
 * 设计目的：
 * 1. 封装 Jackson 细节，屏蔽受检异常，简化调用。
 * 2. 统一全局序列化配置（如日期格式、空值处理）。
 * 3. 提供静态方法，便于在非 Spring 管理的类中使用。
 *
 * 为什么需要该类：
 * 避免在业务代码中重复注入 ObjectMapper 和处理 try-catch。
 */
@Slf4j
public class JsonUtil {

    // 静态单例 ObjectMapper，保证全局配置一致且高性能
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        // 初始化配置
        // 1. 注册 Java 8 时间模块 (支持 LocalDateTime 等)
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        // 2. 禁用将日期写为时间戳 (使用 ISO-8601 格式或自定义格式)
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 3. 设置全局日期格式 (可选，这里为了兼容旧系统设为 yyyy-MM-dd HH:mm:ss)
        OBJECT_MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        // 4. 忽略空 Bean 转 JSON 的错误
        OBJECT_MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    // 私有构造防止实例化
    private JsonUtil() {}

    /**
     * 对象转 JSON 字符串
     *
     * @param obj 目标对象
     * @return JSON 字符串，转换失败返回 null
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON失败|Object_to_json_fail,class={}", obj.getClass().getName(), e);
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    /**
     * JSON 字符串转对象
     *
     * @param json JSON 字符串
     * @param clazz 目标类型
     * @return 目标对象，转换失败返回 null
     */
    public static <T> T toBean(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON转对象失败|Json_to_object_fail,json={},class={}", json, clazz.getName(), e);
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }

    /**
     * JSON 字符串转 List
     *
     * @param json JSON 字符串
     * @param clazz 集合元素类型
     * @return List集合
     */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            log.error("JSON转List失败|Json_to_list_fail,json={},class={}", json, clazz.getName(), e);
            throw new RuntimeException("JSON反序列化List失败", e);
        }
    }

    /**
     * JSON 字符串转 Map
     *
     * @param json JSON 字符串
     * @param keyClass Key 类型
     * @param valueClass Value 类型
     * @return Map集合
     */
    public static <K, V> Map<K, V> toMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, keyClass, valueClass));
        } catch (JsonProcessingException e) {
            log.error("JSON转Map失败|Json_to_map_fail,json={}", json, e);
            throw new RuntimeException("JSON反序列化Map失败", e);
        }
    }

    /**
     * 复杂泛型转换 (例如 Result<User>)
     *
     * @param json JSON 字符串
     * @param typeReference 类型引用
     * @return 目标对象
     */
    public static <T> T toType(String json, TypeReference<T> typeReference) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("JSON转复杂类型失败|Json_to_complex_type_fail,json={}", json, e);
            throw new RuntimeException("JSON反序列化复杂类型失败", e);
        }
    }
}
