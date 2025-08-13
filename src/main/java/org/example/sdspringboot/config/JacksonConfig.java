package org.example.sdspringboot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.nio.charset.StandardCharsets;

/**
 * 全局 Jackson 配置。
 *
 * 背景说明（关键点，底层行为）：
 * 1) WebFlux 的 JSON 编解码器为 Jackson2JsonDecoder/Jackson2JsonEncoder，
 *    它们会复用 Spring 容器中的全局 ObjectMapper。
 * 2) Spring AI 在调用本地 Ollama（例如 /api/chat）或其它模型供应商时，
 *    使用 WebClient（基于 WebFlux）去解析 HTTP 响应为 Java 对象。
 * 3) Ollama Chat API 响应里存在 created_at 字段，对应 Java 类型为 java.time.Instant。
 *    若未给 ObjectMapper 注册 JavaTimeModule，Jackson 默认不支持 Java 8 时间类，
 *    会抛出 InvalidDefinitionException/CodecException（你之前日志中看到的异常）。
 * 4) 因此这里显式注册 JavaTimeModule，并关闭 WRITE_DATES_AS_TIMESTAMPS，
 *    以便时间类型按 ISO-8601 格式序列化；反序列化也能正确处理 JSON 字符串时间。
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
     * - WebClient(WebFlux) 与 RestTemplate 的 Jackson 消息转换都会用到它。
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}