package org.example.sdspringboot.controller;

import org.example.sdspringboot.service.DeepSeekService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * DeepSeek 对话接口的 Web 层（Controller）。
 *
 * 设计说明：
 * - Controller 负责 HTTP 参数接收与简单校验，将业务委托给 Service。
 * - 真正的 HTTP 调用 DeepSeek API 的逻辑在 {@link org.example.sdspringboot.service.DeepSeekService} 中实现。
 * - 这里演示了两种用法：
 *   1) /api/deepseek/chat：通用接口，接收完整的 messages 数组（包含 role/content），更灵活；
 *   2) /api/deepseek/chat/simple：简化接口，只传入用户一句话，内部会拼装系统/用户消息示例。
 *
 * 关于 messages 参数：
 * - 结构为 List<Map<String,String>>，每个元素是一个消息对象，至少包含：
 *   { "role": "system|user|assistant", "content": "消息内容" }
 * - DeepSeek 的 /chat/completions 与 OpenAI 风格一致，需按此格式传入。
 */
@RestController
@RequestMapping("/api/deepseek")
public class DeepSeekController {

    private final DeepSeekService deepSeekService;

    public DeepSeekController(DeepSeekService deepSeekService) {
        this.deepSeekService = deepSeekService;
    }

    /**
     * 通用调用接口：可自定义模型、是否流式、以及完整消息列表。
     *
     * @param model   模型名称，例如 deepseek-chat / deepseek-reasoner
     * @param stream  是否流式（当前示例服务端仍按一次性返回 string 处理）
     * @param messages 消息列表，元素包含 role/content
     * @return AI 回复的文本
     */
    @PostMapping("/chat")
    public ResponseEntity<String> chat(
            @RequestParam(defaultValue = "deepseek-chat") String model,
            @RequestParam(defaultValue = "false") boolean stream,
            @RequestBody List<Map<String, String>> messages) {
        String response = deepSeekService.chat(model, messages, stream);
        return ResponseEntity.ok(response);
    }

    /**
     * 简单对话接口：仅传入用户一句话。
     * - 内部会构造一个系统提示 + 用户消息的最小可用消息数组。
     */
    @GetMapping("/chat/simple")
    public ResponseEntity<String> simpleChat(@RequestParam String message) {
        String response = deepSeekService.chatWithDefaultModel(message);
        return ResponseEntity.ok(response);
    }
}
