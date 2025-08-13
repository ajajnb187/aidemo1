package org.example.sdspringboot.controller;

import org.example.sdspringboot.dto.RagflowDtos;
import org.example.sdspringboot.service.RagflowService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RagFlow 对话接口的 Web 层。
 *
 * 说明：
 * - Controller 只做“参数接收 + 结果返回”，不写业务细节，便于维护与测试。
 * - 具体调用 RagFlow API 的逻辑在 {@link org.example.sdspringboot.service.RagflowService} 中。
 * - 这里提供了一个普通对话接口；若需要流式接口，可参考 Service 的 chatStream 方法新增一个 SSE 端点。
 */
@RestController
@RequestMapping("/api/ragflow")
public class RagflowController {

    private final RagflowService ragflowService;

    public RagflowController(RagflowService ragflowService) {
        this.ragflowService = ragflowService;
    }

    /**
     * 调用 RagFlow 聊天接口（普通非流式）。
     * - 入参为结构化的请求对象 {@link RagflowDtos.RagflowChatRequest}
     * - 返回值这里简化为字符串（实际为 JSON 文本经过反序列化再 toString），生产可直接返回对象。
     */
    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> chat(@RequestBody RagflowDtos.RagflowChatRequest request) {
        String result = String.valueOf(ragflowService.chat(request));
        return ResponseEntity.ok(result);
    }
}