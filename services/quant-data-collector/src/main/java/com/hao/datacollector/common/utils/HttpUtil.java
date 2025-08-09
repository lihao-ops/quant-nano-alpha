package com.hao.datacollector.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP请求工具类
 * 提供GET、POST等HTTP请求的封装方法
 * 支持超时设置、请求头配置、JSON处理等功能
 * 使用Jackson替代FastJSON，提供更好的性能和安全性
 *
 * @author LiHao
 * @version 2.0
 * @since 2022-07-25
 */
@Slf4j
public final class HttpUtil {

    // 默认超时时间配置（毫秒）
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;  // 5秒连接超时
    private static final int DEFAULT_READ_TIMEOUT = 30000;    // 30秒读取超时

    // 默认User-Agent
    private static final String DEFAULT_USER_AGENT = "HttpUtil/2.0 (Java)";

    // Jackson对象映射器，线程安全
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 私有构造函数，防止实例化
    private HttpUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== POST请求方法 ====================

    /**
     * 发送POST请求（JSON格式）
     * 使用默认超时时间和Content-Type
     *
     * @param url         请求URL
     * @param requestBody 请求体内容（JSON字符串或对象）
     * @return 响应体字符串
     * @throws HttpRequestException 请求失败时抛出
     */
    public static String sendPostRequest(String url, Object requestBody) {
        return sendPostRequest(url, requestBody, null, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     * 发送POST请求（JSON格式）带自定义请求头
     *
     * @param url         请求URL
     * @param requestBody 请求体内容
     * @param headers     自定义请求头
     * @return 响应体字符串
     * @throws HttpRequestException 请求失败时抛出
     */
    public static String sendPostRequest(String url, Object requestBody, HttpHeaders headers) {
        return sendPostRequest(url, requestBody, headers, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    /**
     * 发送POST请求（JSON格式）完整参数版本
     *
     * @param url            请求URL，不能为空
     * @param requestBody    请求体内容，支持字符串、Map、自定义对象等
     * @param headers        自定义请求头，可以为null
     * @param connectTimeout 连接超时时间（毫秒），必须大于0
     * @param readTimeout    读取超时时间（毫秒），必须大于0
     * @return 响应体字符串
     * @throws HttpRequestException 请求失败时抛出
     */
    public static String sendPostRequest(String url, Object requestBody, HttpHeaders headers,
                                         int connectTimeout, int readTimeout) {
        // 参数验证
        validateUrl(url);
        validateTimeouts(connectTimeout, readTimeout);

        try {
            // 创建RestTemplate并配置超时
            RestTemplate restTemplate = createRestTemplate(connectTimeout, readTimeout);

            // 准备请求头
            HttpHeaders requestHeaders = prepareHeaders(headers);
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);

            // 转换请求体为JSON字符串
            String jsonBody = convertToJson(requestBody);

            // 创建请求实体
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, requestHeaders);

            // 发送请求并记录日志
            log.debug("Sending POST request to: {}, body length: {}", url,
                    jsonBody != null ? jsonBody.length() : 0);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, String.class);

            log.debug("Received response with status: {}, body length: {}",
                    response.getStatusCode(),
                    response.getBody() != null ? response.getBody().length() : 0);

            return response.getBody();

        } catch (RestClientException e) {
            log.error("POST request failed for URL: {}", url, e);
            throw new HttpRequestException("POST request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during POST request to: {}", url, e);
            throw new HttpRequestException("Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * 发送表单POST请求
     *
     * @param url            请求URL
     * @param formData       表单数据
     * @param headers        自定义请求头
     * @param connectTimeout 连接超时时间（毫秒）
     * @param readTimeout    读取超时时间（毫秒）
     * @return 完整的响应实体
     * @throws HttpRequestException 请求失败时抛出
     */
    public static ResponseEntity<String> sendFormPost(String url,
                                                      MultiValueMap<String, String> formData,
                                                      HttpHeaders headers,
                                                      int connectTimeout,
                                                      int readTimeout) {
        // 参数验证
        validateUrl(url);
        validateTimeouts(connectTimeout, readTimeout);

        if (formData == null) {
            throw new IllegalArgumentException("Form data cannot be null");
        }

        try {
            // 创建RestTemplate并配置超时
            RestTemplate restTemplate = createRestTemplate(connectTimeout, readTimeout);

            // 准备请求头
            HttpHeaders requestHeaders = prepareHeaders(headers);
            requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // 创建请求实体
            HttpEntity<MultiValueMap<String, String>> requestEntity =
                    new HttpEntity<>(formData, requestHeaders);

            // 发送请求
            log.debug("Sending form POST request to: {}, form data size: {}", url, formData.size());

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, String.class);

            log.debug("Received form POST response with status: {}", response.getStatusCode());

            return response;

        } catch (RestClientException e) {
            log.error("Form POST request failed for URL: {}", url, e);
            throw new HttpRequestException("Form POST request failed: " + e.getMessage(), e);
        }
    }

    // ==================== GET请求方法 ====================

    /**
     * 发送GET请求（简单版本）
     *
     * @param url 请求URL
     * @return 响应体字符串
     * @throws HttpRequestException 请求失败时抛出
     */
    public static String sendGetRequest(String url) {
        return sendGetRequest(url, null, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT).getBody();
    }

    /**
     * 发送GET请求（完整版本）
     *
     * @param url            请求URL，不能为空
     * @param headers        自定义请求头，可以为null
     * @param connectTimeout 连接超时时间（毫秒），必须大于0
     * @param readTimeout    读取超时时间（毫秒），必须大于0
     * @return 完整的响应实体
     * @throws HttpRequestException 请求失败时抛出
     */
    public static ResponseEntity<String> sendGetRequest(String url, HttpHeaders headers,
                                                        int connectTimeout, int readTimeout) {
        // 参数验证
        validateUrl(url);
        validateTimeouts(connectTimeout, readTimeout);

        try {
            // 创建RestTemplate并配置超时
            RestTemplate restTemplate = createRestTemplate(connectTimeout, readTimeout);

            // 准备请求头
            HttpHeaders requestHeaders = prepareHeaders(headers);

            // 创建请求实体
            HttpEntity<Void> requestEntity = new HttpEntity<>(requestHeaders);

            // 发送请求
            log.debug("Sending GET request to: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, String.class);

            log.debug("Received GET response with status: {}, body length: {}",
                    response.getStatusCode(),
                    response.getBody() != null ? response.getBody().length() : 0);

            return response;

        } catch (RestClientException e) {
            log.error("GET request failed for URL: {}", url, e);
            throw new HttpRequestException("GET request failed: " + e.getMessage(), e);
        }
    }

    /**
     * 发送Get请求，带查询参数和超时时间
     *
     * @param url            请求url
     * @param queryParams    查询参数
     * @param httpHeader     http header头信息
     * @param connectTimeOut 连接超时时间，单位为毫秒
     * @param readTimeOut    读取缓存区数据超时时间，单位为毫秒
     * @return
     */
    public static ResponseEntity<String> sendGetWithParams(String url, MultiValueMap<String, String> queryParams, HttpHeaders httpHeader, int connectTimeOut, int readTimeOut) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeOut);
        //获取数据超时时间
        requestFactory.setReadTimeout(readTimeOut);
        RestTemplate client = new RestTemplate(requestFactory);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(httpHeader);
        // 构建带查询参数的URI
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (queryParams != null && !queryParams.isEmpty()) {
            builder.queryParams(queryParams);
        }
        String finalUrl = builder.toUriString();
        ResponseEntity<String> response = client.exchange(finalUrl, HttpMethod.GET, requestEntity, String.class);
        return response;
    }

    // ==================== JSON处理方法 ====================

    /**
     * 将对象转换为JSON字符串
     * 支持字符串、Map、自定义对象等类型
     *
     * @param object 待转换的对象
     * @return JSON字符串
     * @throws HttpRequestException 转换失败时抛出
     */
    public static String convertToJson(Object object) {
        if (object == null) {
            return null;
        }

        // 如果已经是字符串，直接返回
        if (object instanceof String) {
            return (String) object;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert object to JSON: {}", object.getClass().getSimpleName(), e);
            throw new HttpRequestException("JSON conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * 将JSON字符串解析为指定类型的对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 解析后的对象
     * @throws HttpRequestException 解析失败时抛出
     */
    public static <T> T parseJson(String json, Class<T> clazz) {
        if (!StringUtils.hasText(json)) {
            return null;
        }

        if (clazz == null) {
            throw new IllegalArgumentException("Target class cannot be null");
        }

        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON to {}: {}", clazz.getSimpleName(), e.getMessage());
            throw new HttpRequestException("JSON parsing failed: " + e.getMessage(), e);
        }
    }

    /**
     * 将JSON字符串解析为JsonNode对象
     * 便于动态访问JSON数据
     *
     * @param json JSON字符串
     * @return JsonNode对象
     * @throws HttpRequestException 解析失败时抛出
     */
    public static JsonNode parseJsonNode(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON to JsonNode: {}", e.getMessage());
            throw new HttpRequestException("JSON parsing failed: " + e.getMessage(), e);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 创建配置了超时时间的RestTemplate
     *
     * @param connectTimeout 连接超时时间（毫秒）
     * @param readTimeout    读取超时时间（毫秒）
     * @return 配置好的RestTemplate实例
     */
    private static RestTemplate createRestTemplate(int connectTimeout, int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        // 移除已过时的setBufferRequestBody方法调用
        // factory.setBufferRequestBody(true); // 此方法在Spring 6.1中已过时

        return new RestTemplate(factory);
    }

    /**
     * 准备HTTP请求头
     * 如果传入的headers为null，则创建新的HttpHeaders
     * 并设置默认的User-Agent和Accept头
     *
     * @param headers 原始请求头，可以为null
     * @return 准备好的请求头
     */
    private static HttpHeaders prepareHeaders(HttpHeaders headers) {
        HttpHeaders requestHeaders = headers != null ? new HttpHeaders(headers) : new HttpHeaders();

        // 设置默认User-Agent（如果没有设置）
        if (!requestHeaders.containsKey(HttpHeaders.USER_AGENT)) {
            requestHeaders.set(HttpHeaders.USER_AGENT, DEFAULT_USER_AGENT);
        }

        // 设置默认Accept头（如果没有设置）
        if (!requestHeaders.containsKey(HttpHeaders.ACCEPT)) {
            requestHeaders.setAccept(java.util.List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));
        }

        // 设置字符编码
        requestHeaders.setAcceptCharset(java.util.List.of(StandardCharsets.UTF_8));

        return requestHeaders;
    }

    /**
     * 验证URL参数
     *
     * @param url 待验证的URL
     * @throws IllegalArgumentException URL无效时抛出
     */
    private static void validateUrl(String url) {
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        try {
            URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URL format: " + url, e);
        }
    }

    /**
     * 验证超时时间参数
     *
     * @param connectTimeout 连接超时时间
     * @param readTimeout    读取超时时间
     * @throws IllegalArgumentException 超时时间无效时抛出
     */
    private static void validateTimeouts(int connectTimeout, int readTimeout) {
        if (connectTimeout <= 0) {
            throw new IllegalArgumentException("Connect timeout must be positive: " + connectTimeout);
        }
        if (readTimeout <= 0) {
            throw new IllegalArgumentException("Read timeout must be positive: " + readTimeout);
        }
    }

    // ==================== 兼容性方法（保持向后兼容） ====================

    /**
     * @deprecated 使用 {@link #sendPostRequest(String, Object, HttpHeaders, int, int)} 替代
     */
    @Deprecated
    public static String sendPostRequestTimeOut(String url, String bodyContent, int timeOut,
                                                Map<String, String> httpHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (httpHeader != null) {
            headers.setAll(httpHeader);
        }
        return sendPostRequest(url, bodyContent, headers, timeOut, timeOut);
    }

    /**
     * 发送 POST 表单请求
     */
    public static ResponseEntity<String> sendRequestFormPost(String url,
                                                             MultiValueMap<String, String> bodyContent,
                                                             HttpHeaders httpHeader,
                                                             int connectTimeOut,
                                                             int readTimeOut) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeOut);
        factory.setReadTimeout(readTimeOut);

        RestTemplate restTemplate = new RestTemplate(factory);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(bodyContent, httpHeader);
        return restTemplate.postForEntity(url, entity, String.class);
    }

    /**
     * @deprecated 使用 {@link #sendFormPost(String, MultiValueMap, HttpHeaders, int, int)} 替代
     * 重命名方法以避免与正式方法签名冲突
     */
    @Deprecated
    public static ResponseEntity<String> sendFormPostLegacy(String url,
                                                            MultiValueMap<String, String> bodyContent,
                                                            HttpHeaders httpHeader,
                                                            int connectTimeOut,
                                                            int readTimeOut) {
        return sendFormPost(url, bodyContent, httpHeader, connectTimeOut, readTimeOut);
    }

    /**
     * @deprecated 使用 {@link #sendGetRequest(String, HttpHeaders, int, int)} 替代
     */
    @Deprecated
    public static ResponseEntity<String> sendGet(String url, HttpHeaders httpHeader,
                                                 int connectTimeOut, int readTimeOut) {
        return sendGetRequest(url, httpHeader, connectTimeOut, readTimeOut);
    }

    // ==================== 自定义异常类 ====================

    /**
     * HTTP请求异常类
     * 用于封装HTTP请求过程中的各种异常
     */
    public static class HttpRequestException extends RuntimeException {

        public HttpRequestException(String message) {
            super(message);
        }

        public HttpRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}