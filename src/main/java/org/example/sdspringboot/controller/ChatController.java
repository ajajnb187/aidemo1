package org.example.sdspringboot.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class ChatController {
    private final ChatClient chatClient;

    // 构造方法注入 ChatClient.Builder，用于构建 ChatClient 实例
    public ChatController(ChatClient.Builder chatClient) {
        this.chatClient = chatClient.build();
    }

    @GetMapping(value = "/chat",produces = "text/plain;charset=UTF-8")
    public ResponseEntity<Flux<String>> chat(@RequestParam(value = "message") String message) {
        try {
            // ChatClient 使用 WebFlux WebClient 与模型服务交互；
            // 流式响应（SSE/分段文本）的 JSON 解析依赖全局 ObjectMapper（见 JacksonConfig），
            // 因此已注册的 JavaTimeModule 能保证包含 Instant 的响应字段正确反序列化。
            Flux<String> response = chatClient.prompt(message).stream().content();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
