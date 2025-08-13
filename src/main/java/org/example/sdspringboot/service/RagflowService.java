package org.example.sdspringboot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.sdspringboot.dto.RagflowDtos.RagflowApiResponse;
import org.example.sdspringboot.dto.RagflowDtos.RagflowChatRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

/**
 * 调用 RagFlow API 的业务层。
 *
 * 关键点：
 * - 同时演示了两种 HTTP 客户端：
 *   1) RestTemplate：阻塞式、简单易用，适合普通非流式调用；
 *   2) WebClient（WebFlux）：响应式，支持流式（SSE）处理。
 * - 鉴权统一采用 Bearer Token：Authorization: Bearer ${ragflow.api-key}
 * - ObjectMapper 来将 JSON 文本与 Java 对象互转（全局配置见 JacksonConfig）。
 */
@Service
public class RagflowService {

    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * RagFlow 聊天接口完整 URL。
     * - 通常包含形如 /api/v1/chats/{chatId}/completions 路径。
     */
    @Value("${ragflow.api-url}")
    private String ragflowApiUrl;

    /**
     * RagFlow 接口的 API Key。
     */
    @Value("${ragflow.api-key}")
    private String ragflowApiKey;

    public RagflowService(RestTemplate restTemplate, WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * 调用 RagFlow 聊天接口 - 普通对话模式（非流式）
     *
     * 流程：构建请求头（JSON + Bearer）→ RestTemplate.postForEntity → 解析响应体 → 返回结构化对象
     */
    public RagflowApiResponse chat(RagflowChatRequest request) {
        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(ragflowApiKey);
            var entity = new org.springframework.http.HttpEntity<>(request, headers);
            var response = restTemplate.postForEntity(ragflowApiUrl, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readValue(response.getBody(), RagflowApiResponse.class);
            }
            RagflowApiResponse error = new RagflowApiResponse();
            error.setCode(response.getStatusCode().value());
            error.setMessage("HTTP " + response.getStatusCode());
            return error;
        } catch (HttpStatusCodeException e) {
            // 捕获 4xx/5xx 并返回原始响应文本作为错误信息，便于排查
            RagflowApiResponse error = new RagflowApiResponse();
            error.setCode(e.getStatusCode().value());
            error.setMessage(e.getResponseBodyAsString(StandardCharsets.UTF_8));
            return error;
        } catch (Exception e) {
            RagflowApiResponse error = new RagflowApiResponse();
            error.setCode(500);
            error.setMessage(e.getMessage());
            return error;
        }
    }

    /**
     * 调用 RagFlow 聊天接口 - 流式对话模式（SSE）
     *
     * 说明：
     * - 将 request.stream 置为 true，告知服务端以流式返回。
     * - WebClient 设置 accept 为 text/event-stream 与 application/json：
     *   部分实现可能返回纯 JSON 分片或标准 SSE 事件。
     * - 这里采用 bodyToFlux(String.class) 接收原始字符串流，按需处理（trim + 过滤空行）。
     */
    public Flux<String> chatStream(RagflowChatRequest request) {
        request.setStream(Boolean.TRUE);
        return webClient
                .post()
                .uri(ragflowApiUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ragflowApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .map(String::trim)
                .filter(s -> !s.isEmpty());
    }
}