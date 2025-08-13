package org.example.sdspringboot.service;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调用 DeepSeek 官方 Chat API 的业务层（Service）。
 *
 * 技术要点与流程说明：
 * 1) 这里使用 RestTemplate 进行同步 HTTP 调用（一次请求、一次响应）。
 *    - 与 WebFlux/WebClient 相比，RestTemplate 更简单直观，适合非流式、阻塞式的调用场景。
 * 2) DeepSeek /chat/completions 接口参数与 OpenAI 接口风格基本一致：
 *    - body 中包含 model、messages、stream 等字段；
 *    - messages 是一个数组，元素为 {"role":"system|user|assistant", "content":"..."}。
 * 3) 鉴权通过 HTTP Header：Authorization: Bearer {apiKey}。
 * 4) 响应结构中包含 choices[0].message.content 即回答文本（简化起见我们只取第一条）。
 * 5) 使用全局注入的 ObjectMapper 来解析 JSON 文本（JacksonConfig 已做时间模块的兼容配置）。
 * 6) 错误处理：
 *    - RestTemplate.exchange 返回非 2xx：直接返回状态码信息；
 *    - 其它异常：捕获异常并返回错误信息字符串（生产中建议抛业务异常并统一处理）。
 */
@Service
public class DeepSeekService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    public DeepSeekService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 调用 DeepSeek 对话 API
     * @param model 模型名称，如 "deepseek-chat" 或 "deepseek-reasoner"
     * @param messages 消息列表（每个元素包含 role 与 content）
     * @param stream 是否流式输出（此处仍以一次性响应 String 处理，仅占位）
     * @return 第一条回答文本
     */
    public String chat(String model, List<Map<String, String>> messages, boolean stream) {
        try {
            // 1) 构建请求 URL
            String url = baseUrl + "/chat/completions";

            // 2) 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("stream", stream);

            // 3) 设置请求头（JSON + Bearer 鉴权）
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            // 4) 发起请求
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            // 5) 处理响应
            if (response.getStatusCode().is2xxSuccessful()) {
                // 解析 JSON：取 choices[0].message.content
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("choices").get(0).get("message").get("content").asText();
            } else {
                return "API调用失败: " + response.getStatusCode();
            }
        } catch (Exception e) {
            // 将异常信息直接返回（生产环境建议使用统一异常处理）
            return "调用出错: " + e.getMessage();
        }
    }

    /**
     * 便捷方法：使用默认模型(deepseek-chat)发送消息。
     * - 此方法内部构造最小可用的 messages 数组（system + user），便于快速联调。
     */
    public String chatWithDefaultModel(String userMessage) {
        List<Map<String, String>> messages = new ArrayList<>();
        // 添加系统消息
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "You are a helpful assistant");
        messages.add(systemMsg);

        // 添加用户消息
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        return chat("deepseek-chat", messages, false);
    }
}
