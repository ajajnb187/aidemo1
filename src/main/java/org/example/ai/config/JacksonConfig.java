package org.example.ai.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.nio.charset.StandardCharsets;

/**
 * 全局 Jackson 配置。
 *
 * 背景说明（关键点，底层行为）：
 * 1) WebFlux 的 JSON 编解码器为 Jackson2JsonDecoder/Jackson2JsonEncoder，
 *    它们会复用 Spring 容器中的全局 ObjectMapper。
 * 2) Spring AI 在调用 Ollama 或 DashScope 等模型供应商时，
 *    使用 WebClient（基于 WebFlux）去解析 HTTP 响应为 Java 对象。
 * 3) API 响应里存在时间字段（如 created_at），对应 Java 类型为 java.time.Instant。
 *    若未给 ObjectMapper 注册 JavaTimeModule，Jackson 默认不支持 Java 8 时间类，
 *    会抛出 InvalidDefinitionException/CodecException。
 * 4) DashScope API 可能返回一些未知字段，需要配置忽略未知属性避免反序列化失败。
 * 5) 配置容错性更强的反序列化选项，提高与各种AI API的兼容性。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public StringHttpMessageConverter stringHttpMessageConverter() {
        return new StringHttpMessageConverter(StandardCharsets.UTF_8);
    }

    /**
     * 提供一个被 Spring 复用的全局 ObjectMapper：
     * - 注册 JavaTimeModule 以支持 Instant/LocalDateTime 等 Java 8 时间类型；
     * - 关闭时间戳写出，保持与大多数 HTTP API 兼容的 ISO-8601 字符串格式；
     * - 忽略未知属性，提高与各种AI API的兼容性；
     * - 配置容错的反序列化选项，避免因格式差异导致的失败；
     * - WebClient(WebFlux) 与 RestTemplate 的 Jackson 消息转换都会用到它。
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 注册Java 8时间模块
        mapper.registerModule(new JavaTimeModule());
        
        // 序列化配置
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
        // 反序列化配置 - 提高容错性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        
        return mapper;
    }
}