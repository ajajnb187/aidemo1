package org.example.ai.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.ai.app.App;
import org.example.ai.dto.UserVO;
import org.example.ai.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * AI病虫害诊断控制器
 * 提供AI智能诊断和防治方案接口
 */
@RestController
@RequestMapping("/api/ai")
@Slf4j
public class ChatController {

    @Resource
    private App app;
    
    @Resource
    private UserService userService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构建包含用户地址信息的增强诊断消息
     * 
     * @param originalMessage 原始病虫害检测消息
     * @return 包含用户地址信息的增强消息
     */
    private String buildEnhancedMessage(String originalMessage) {
        try {
            // 获取当前登录用户信息
            UserVO currentUser = userService.getCurrentUser();
            
            if (currentUser != null && currentUser.getAddress() != null && !currentUser.getAddress().trim().isEmpty()) {
                log.info("为用户 {} 的AI诊断添加地址信息: {}", currentUser.getUsername(), currentUser.getAddress());
                
                return originalMessage + "\n\n📍 用户所在地区：" + currentUser.getAddress() + 
                       "\n请结合当地气候条件进行分析，如需要请查询该地区的天气信息。";
            } else {
                log.info("当前用户未设置地址或未登录，使用原始诊断消息");
                return originalMessage;
            }
        } catch (Exception e) {
            log.warn("获取用户地址信息失败，使用原始诊断消息: {}", e.getMessage());
            return originalMessage;
        }
    }

    /**
     * AI病虫害诊断和防治方案（同步版本 - 备用）
     * 
     * @param message 病虫害检测结果信息，包含检测到的病种和检测ID
     * @return AI诊断分析和防治方案
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        log.info("收到AI诊断请求（同步）: {}", message);
        
        try {
            // 构建包含用户地址信息的完整诊断消息
            String enhancedMessage = buildEnhancedMessage(message);
            
            String result = app.diagnosisAndTreatment(enhancedMessage);
            log.info("AI诊断完成，返回结果长度: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("AI诊断失败", e);
            return "AI诊断服务暂时不可用，请稍后重试。如需紧急处理，请咨询当地农技专家。";
        }
    }

    /**
     * AI病虫害诊断和防治方案（流式版本 - 推荐）
     * 
     * @param message 病虫害检测结果信息
     * @return Server-Sent Events流式响应
     */
    @GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam String message) {
        log.info("收到AI诊断请求（流式）: {}", message);
        
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
        
        // 异步处理AI诊断
        CompletableFuture.runAsync(() -> {
            try {
                // 发送开始信号
                emitter.send(SseEmitter.event()
                    .name("start")
                    .data("🤖 AI专家开始分析..."));
                
                // 模拟流式返回 - 分阶段发送
                emitter.send(SseEmitter.event()
                    .name("progress")
                    .data("📋 正在分析病虫害特征..."));
                
                Thread.sleep(1000); // 模拟处理时间
                
                emitter.send(SseEmitter.event()
                    .name("progress")
                    .data("🔬 正在生成诊断分析..."));
                
                // 调用AI诊断（使用增强消息）
                String enhancedMessage = buildEnhancedMessage(message);
                String result = app.diagnosisAndTreatment(enhancedMessage);
                
                emitter.send(SseEmitter.event()
                    .name("progress")
                    .data("💊 正在制定防治方案..."));
                
                Thread.sleep(500);
                
                // 发送完整结果
                emitter.send(SseEmitter.event()
                    .name("result")
                    .data(result));
                
                emitter.send(SseEmitter.event()
                    .name("complete")
                    .data("✅ AI诊断完成"));
                
                emitter.complete();
                log.info("流式AI诊断完成，结果长度: {}", result.length());
                
            } catch (Exception e) {
                log.error("流式AI诊断失败", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("❌ AI诊断服务异常: " + e.getMessage()));
                    emitter.completeWithError(e);
                } catch (IOException ioException) {
                    log.error("发送错误消息失败", ioException);
                }
            }
        });
        
        return emitter;
    }

    /**
     * AI病虫害诊断和防治方案（WebFlux流式版本 - 最优）
     *
     * @param message 病虫害检测结果信息或用户问题
     * @param conversationId 会话ID（可选，用于区分不同的对话）
     * @param isInitialDiagnosis 是否为初始诊断（true=诊断模式，false=聊天模式）
     * @return Reactor Flux流式响应
     */
    @GetMapping(value = "/chat-flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatFlux(@RequestParam String message,
                                 @RequestParam(required = false) String conversationId,
                                 @RequestParam(defaultValue = "true") boolean isInitialDiagnosis) {
        
        log.info("收到AI请求（WebFlux流式） - 消息: {}, 会话ID: {}, 初始诊断: {}", 
                 message, conversationId, isInitialDiagnosis);
        
        return Flux.<String>create(fluxSink -> {
            try {
                // 1. 发送开始消息
                String startMessage = isInitialDiagnosis ? "🤖 AI专家开始分析..." : "🤖 AI助手正在思考...";
                fluxSink.next("data: {\"type\":\"start\",\"content\":\"" + startMessage + "\"}\n\n");
                
                if (isInitialDiagnosis) {
                    // 2. 诊断模式：发送诊断相关的进度消息
                    fluxSink.next("data: {\"type\":\"progress\",\"content\":\"📋 正在分析病虫害特征...\"}\n\n");
                    Thread.sleep(1000);
                    fluxSink.next("data: {\"type\":\"progress\",\"content\":\"🔬 正在生成诊断分析...\"}\n\n");
                    
                    // 调用诊断方法（带记忆，支持后续提问）
                    String enhancedMessage = buildEnhancedMessage(message);
                    
                    if (conversationId != null && !conversationId.trim().isEmpty()) {
                        // 使用带记忆的诊断方法
                        Mono.fromCallable(() -> app.diagnosisAndTreatment(enhancedMessage, conversationId))
                            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                            .subscribe(
                                result -> {
                                    try {
                                        log.info("AI诊断完成（记忆模式），会话ID: {}, 结果长度: {}", conversationId, result.length());
                                        sendSuccessResponse(fluxSink, result, "✅ AI诊断完成，可继续提问");
                                    } catch (Exception e) {
                                        log.error("处理诊断结果失败", e);
                                        sendErrorResponse(fluxSink, e);
                                    }
                                },
                                error -> {
                                    log.error("AI诊断失败", error);
                                    sendErrorResponse(fluxSink, error);
                                }
                            );
                    } else {
                        // 无会话ID时使用无记忆诊断方法
                        Mono.fromCallable(() -> app.diagnosisAndTreatment(enhancedMessage))
                            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                            .subscribe(
                                result -> {
                                    try {
                                        log.info("AI诊断完成（无记忆模式），结果长度: {}", result.length());
                                        sendSuccessResponse(fluxSink, result, "✅ AI诊断完成");
                                    } catch (Exception e) {
                                        log.error("处理诊断结果失败", e);
                                        sendErrorResponse(fluxSink, e);
                                    }
                                },
                                error -> {
                                    log.error("AI诊断失败", error);
                                    sendErrorResponse(fluxSink, error);
                                }
                            );
                    }
                } else {
                    // 3. 聊天模式：发送聊天相关的进度消息
                    fluxSink.next("data: {\"type\":\"progress\",\"content\":\"💭 正在查阅聊天记录...\"}\n\n");
                    Thread.sleep(500);
                    fluxSink.next("data: {\"type\":\"progress\",\"content\":\"📚 正在检索相关知识...\"}\n\n");
                    
                    // 验证会话ID
                    if (conversationId == null || conversationId.trim().isEmpty()) {
                        sendErrorResponse(fluxSink, new IllegalArgumentException("聊天模式需要提供会话ID"));
                        return;
                    }
                    
                    // 调用聊天方法（带记忆）
                    String enhancedMessage = buildEnhancedMessage(message);
                    Mono.fromCallable(() -> app.chatWithMemory(enhancedMessage, conversationId))
                        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                        .subscribe(
                            result -> {
                                try {
                                    log.info("AI聊天完成，会话ID: {}, 结果长度: {}", conversationId, result.length());
                                    sendSuccessResponse(fluxSink, result, "✅ AI回复完成");
                                } catch (Exception e) {
                                    log.error("处理聊天结果失败", e);
                                    sendErrorResponse(fluxSink, e);
                                }
                            },
                            error -> {
                                log.error("AI聊天失败，会话ID: {}", conversationId, error);
                                sendErrorResponse(fluxSink, error);
                            }
                        );
                }
                
            } catch (Exception e) {
                log.error("WebFlux流式处理异常", e);
                sendErrorResponse(fluxSink, e);
            }
        })
        .doOnComplete(() -> log.info("WebFlux流式响应完成"))
        .doOnError(error -> log.error("WebFlux流式响应异常", error))
        .onErrorResume(error -> Flux.<String>just("data: {\"type\":\"error\",\"content\":\"❌ 服务异常，请重试\"}\n\n"));
    }

    /**
     * 发送成功响应
     */
    private void sendSuccessResponse(reactor.core.publisher.FluxSink<String> fluxSink, String result, String completeMessage) {
        try {
            // 使用ObjectMapper安全构建JSON响应
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("type", "result");
            resultData.put("content", result);
            
            String jsonResult = objectMapper.writeValueAsString(resultData);
            String sseData = "data: " + jsonResult + "\n\n";
            
            fluxSink.next(sseData);
            
            // 发送完成消息
            fluxSink.next("data: {\"type\":\"complete\",\"content\":\"" + completeMessage + "\"}\n\n");
            
            fluxSink.complete();
        } catch (Exception e) {
            log.error("发送成功响应失败", e);
            fluxSink.error(e);
        }
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(reactor.core.publisher.FluxSink<String> fluxSink, Throwable error) {
        try {
            String errorJson = String.format("{\"type\":\"error\",\"content\":\"❌ 服务异常: %s\"}", 
                error.getMessage());
            fluxSink.next("data: " + errorJson + "\n\n");
            fluxSink.error(error);
        } catch (Exception e) {
            log.error("发送错误响应失败", e);
            fluxSink.error(e);
        }
    }
}
