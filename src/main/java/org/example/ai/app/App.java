package org.example.ai.app;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.example.ai.dto.DiagnosisResponse;
import org.example.ai.service.rag.ManualRAGService;


/**
 * AI病虫害诊断应用类
 * 支持病虫害诊断和上下文聊天对话
 */
@Component
@Slf4j
public class App {

    // 诊断专用ChatClient（不带记忆）
    private final ChatClient diagnosisChatClient;
    
    // 对话专用ChatClient（带记忆和RAG）
    private final ChatClient conversationChatClient;
    
    // 病虫害诊断专用RAG ChatClient（带RAG增强）
    private final ChatClient diagnosisRagChatClient;
    
    // 聊天记忆管理
    private final ChatMemory chatMemory;
    
    // 结构化输出转换器
    private final BeanOutputConverter<DiagnosisResponse> diagnosisConverter;
    private final BeanOutputConverter<org.example.ai.dto.ChatResponse> chatConverter;

    // 手动RAG服务
    private final ManualRAGService manualRAGService;

    /**
     * 注入MCP服务的工具回调提供者
     * 
     * ToolCallbackProvider是Spring AI提供的接口，用于注册和管理AI可调用的工具
     * 
     * Spring Boot会自动扫描并注入所有配置的MCP服务，无需手动创建Bean
     * 它通过application.yml中的mcp.client配置找到并连接MCP服务
     */
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * 构造函数，注入配置的ChatClient实例
     */
    public App(@Qualifier("diagnosisChatClient") ChatClient diagnosisChatClient,
               @Qualifier("conversationChatClient") ChatClient conversationChatClient,
               @Qualifier("diagnosisRagChatClient") ChatClient diagnosisRagChatClient,
               ChatMemory chatMemory,
               ManualRAGService manualRAGService) {
        this.diagnosisChatClient = diagnosisChatClient;
        this.conversationChatClient = conversationChatClient;
        this.diagnosisRagChatClient = diagnosisRagChatClient;
        this.chatMemory = chatMemory;
        this.manualRAGService = manualRAGService;
        
        // 初始化结构化输出转换器
        this.diagnosisConverter = new BeanOutputConverter<>(DiagnosisResponse.class);
        this.chatConverter = new BeanOutputConverter<>(org.example.ai.dto.ChatResponse.class);
        
        log.info("AI应用初始化完成 - 诊断、对话和RAG增强ChatClient已配置，结构化输出转换器已初始化");
    }


    /**
     * 病虫害诊断和防治方案生成（带记忆模式，支持后续提问）
     * 
     * @param message 病虫害检测结果信息
     * @param conversationId 会话ID，用于保存诊断结果到记忆中
     * @return AI诊断分析和防治方案（结构化友好格式）
     */
    public String diagnosisAndTreatment(String message, String conversationId) {
        try {
            log.info("开始AI病虫害诊断（结构化输出），会话ID: {}, 输入信息: {}", conversationId, message);
            
            // 构建包含结构化输出格式的完整提示
            String formatInstruction = diagnosisConverter.getFormat();
            String enhancedMessage = message + "\n\n" + formatInstruction;
            
            // 使用对话专用ChatClient，带记忆功能
            ChatResponse chatResponse = conversationChatClient
                    .prompt()
                    .user(enhancedMessage)
                    .toolCallbacks(toolCallbackProvider)
                    // 设置会话ID以建立记忆上下文
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .chatResponse();
                    
            String rawResponse = chatResponse.getResult().getOutput().getText();
            log.info("AI原始响应长度: {} 字符", rawResponse.length());
            
            // 尝试解析为结构化输出
            try {
                DiagnosisResponse structuredResponse = diagnosisConverter.convert(rawResponse);
                String friendlyResponse = formatDiagnosisForDisplay(structuredResponse);
                
                log.info("AI病虫害诊断完成（结构化），会话ID: {}, 友好格式长度: {} 字符", conversationId, friendlyResponse.length());
                return friendlyResponse;
                
            } catch (Exception parseError) {
                log.warn("结构化解析失败，使用原始响应，会话ID: {}, 错误: {}", conversationId, parseError.getMessage());
                // 如果结构化解析失败，返回原始响应但去除Markdown符号
                return cleanMarkdownSymbols(rawResponse);
            }
            
        } catch (Exception e) {
            log.error("AI病虫害诊断过程中发生错误，会话ID: {}", conversationId, e);
            return "AI诊断服务遇到问题：" + e.getMessage() + 
                   "\n\n建议：\n1. 请稍后重试\n2. 如情况紧急，请联系当地农技专家\n3. 可尝试手动查阅相关病虫害防治手册";
        }
    }

    /**
     * 病虫害诊断和防治方案生成（无记忆模式，向后兼容）
     * 
     * @param message 病虫害检测结果信息
     * @return AI诊断分析和防治方案
     */
    public String diagnosisAndTreatment(String message) {
        try {
            log.info("开始AI病虫害诊断（RAG增强模式），输入信息: {}", message);
            
            // 1. 使用手动RAG服务增强消息
            String enhancedMessage = manualRAGService.enhanceDiagnosisMessageWithRAG(message);
            
            // 2. 使用增强后的消息进行诊断
            ChatResponse chatResponse = diagnosisChatClient
                    .prompt()
                    .user(enhancedMessage)
                    .toolCallbacks(toolCallbackProvider)
                    .call()
                    .chatResponse();
                    
            String content = chatResponse.getResult().getOutput().getText();
            
            log.info("AI病虫害诊断完成（RAG增强），回复长度: {} 字符", content.length());
            return cleanMarkdownSymbols(content);
            
        } catch (Exception e) {
            log.error("AI病虫害诊断过程中发生错误", e);
            return "AI诊断服务遇到问题：" + e.getMessage() + 
                   "\n\n建议：\n1. 请稍后重试\n2. 如情况紧急，请联系当地农技专家\n3. 可尝试手动查阅相关病虫害防治手册";
        }
    }

    /**
     * 病虫害诊断和防治方案生成（传统模式，不使用RAG）
     * 
     * @param message 病虫害检测结果信息
     * @return AI诊断分析和防治方案
     */
    public String diagnosisAndTreatmentBasic(String message) {
        try {
            log.info("开始AI病虫害诊断（传统模式），输入信息: {}", message);
            
            // 使用诊断专用ChatClient，不带记忆功能和RAG
            ChatResponse chatResponse = diagnosisChatClient
                    .prompt()
                    .user(message)
                    .toolCallbacks(toolCallbackProvider)
                    .call()
                    .chatResponse();
                    
            String content = chatResponse.getResult().getOutput().getText();
            
            log.info("AI病虫害诊断完成（传统模式），回复长度: {} 字符", content.length());
            return cleanMarkdownSymbols(content);
            
        } catch (Exception e) {
            log.error("AI病虫害诊断过程中发生错误", e);
            return "AI诊断服务遇到问题：" + e.getMessage() + 
                   "\n\n建议：\n1. 请稍后重试\n2. 如情况紧急，请联系当地农技专家\n3. 可尝试手动查阅相关病虫害防治手册";
        }
    }

    /**
     * 带记忆的聊天对话（用于后续提问）
     * 
     * @param message 用户提问内容
     * @param conversationId 会话ID，用于区分不同的对话线程
     * @return AI回复内容（结构化友好格式）
     */
    public String chatWithMemory(String message, String conversationId) {
        try {
            log.info("开始AI聊天对话（结构化输出），会话ID: {}, 消息: {}", conversationId, message);
            
            // 1. 使用手动RAG服务增强消息
            String ragEnhancedMessage = manualRAGService.enhanceChatMessageWithRAG(message);
            
            // 2. 构建包含结构化输出格式的完整提示
            String formatInstruction = chatConverter.getFormat();
            String enhancedMessage = ragEnhancedMessage + "\n\n" + formatInstruction;
            
            // 3. 使用对话专用ChatClient，带记忆功能
            ChatResponse chatResponse = conversationChatClient
                    .prompt()
                    .user(enhancedMessage)
                    .toolCallbacks(toolCallbackProvider)
                    // 设置会话ID以管理不同的对话上下文
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .chatResponse();
                    
            String rawResponse = chatResponse.getResult().getOutput().getText();
            log.info("AI聊天原始响应长度: {} 字符", rawResponse.length());
            
            // 尝试解析为结构化输出
            try {
                org.example.ai.dto.ChatResponse structuredResponse = chatConverter.convert(rawResponse);
                String friendlyResponse = formatChatForDisplay(structuredResponse);
                
                log.info("AI聊天对话完成（结构化），会话ID: {}, 友好格式长度: {} 字符", conversationId, friendlyResponse.length());
                return friendlyResponse;
                
            } catch (Exception parseError) {
                log.warn("聊天结构化解析失败，使用原始响应，会话ID: {}, 错误: {}", conversationId, parseError.getMessage());
                // 如果结构化解析失败，返回原始响应但去除Markdown符号
                return cleanMarkdownSymbols(rawResponse);
            }
            
        } catch (Exception e) {
            log.error("AI聊天对话过程中发生错误，会话ID: {}", conversationId, e);
            return "AI助手暂时无法回答您的问题：" + e.getMessage() + 
                   "\n\n请稍后重试，或重新描述您的问题。";
        }
    }

    
    /**
     * 通用工具调用方法（保留兼容性）
     * 
     * @param message 用户输入消息
     * @return AI回复
     */
    public String tools(String message) {
        try {
            // 使用诊断客户端进行通用工具调用
            ChatResponse chatResponse = diagnosisChatClient
                    .prompt()
                    .user(message)
                    .toolCallbacks(toolCallbackProvider)
                    .call()
                    .chatResponse();
                    
            String content = chatResponse.getResult().getOutput().getText();
            return cleanMarkdownSymbols(content);
        } catch (Exception e) {
            return "服务暂时不可用: " + e.getMessage();
        }
    }

    /**
     * 格式化诊断结果为友好显示格式
     * 
     * @param diagnosis 结构化诊断结果
     * @return 友好的显示文本
     */
    private String formatDiagnosisForDisplay(DiagnosisResponse diagnosis) {
        StringBuilder result = new StringBuilder();
        
        // 病害基本信息
        if (diagnosis.getDiseaseInfo() != null) {
            result.append("🔬 诊断结果\n\n");
            result.append("病害名称：").append(diagnosis.getDiseaseInfo().getName()).append("\n");
            result.append("严重程度：").append(diagnosis.getDiseaseInfo().getSeverity()).append("\n");
            if (diagnosis.getDiseaseInfo().getDescription() != null) {
                result.append("病害描述：").append(diagnosis.getDiseaseInfo().getDescription()).append("\n");
            }
            if (diagnosis.getDiseaseInfo().getWeatherImpact() != null) {
                result.append("天气影响：").append(diagnosis.getDiseaseInfo().getWeatherImpact()).append("\n");
            }
            result.append("\n");
        }
        
        // 症状特征
        if (diagnosis.getSymptoms() != null && !diagnosis.getSymptoms().isEmpty()) {
            result.append("📋 症状特征\n");
            for (int i = 0; i < diagnosis.getSymptoms().size(); i++) {
                result.append("• ").append(diagnosis.getSymptoms().get(i)).append("\n");
            }
            result.append("\n");
        }
        
        // 发病原因
        if (diagnosis.getCauses() != null && !diagnosis.getCauses().isEmpty()) {
            result.append("🔍 发病原因\n");
            for (String cause : diagnosis.getCauses()) {
                result.append("• ").append(cause).append("\n");
            }
            result.append("\n");
        }
        
        // 治疗方案
        if (diagnosis.getTreatment() != null) {
            result.append("💊 治疗方案\n");
            
            // 紧急措施
            if (diagnosis.getTreatment().getUrgentActions() != null && !diagnosis.getTreatment().getUrgentActions().isEmpty()) {
                result.append("\n紧急措施：\n");
                for (String action : diagnosis.getTreatment().getUrgentActions()) {
                    result.append("• ").append(action).append("\n");
                }
            }
            
            // 推荐药剂
            if (diagnosis.getTreatment().getMedications() != null && !diagnosis.getTreatment().getMedications().isEmpty()) {
                result.append("\n推荐药剂：\n");
                for (DiagnosisResponse.Medication med : diagnosis.getTreatment().getMedications()) {
                    result.append("• ").append(med.getName());
                    if (med.getDosage() != null) {
                        result.append("（").append(med.getDosage()).append("）");
                    }
                    if (med.getFrequency() != null) {
                        result.append(" - ").append(med.getFrequency());
                    }
                    result.append("\n");
                }
            }
            
            // 用药时机和方法
            if (diagnosis.getTreatment().getTiming() != null) {
                result.append("\n用药时机：").append(diagnosis.getTreatment().getTiming()).append("\n");
            }
            if (diagnosis.getTreatment().getApplication() != null) {
                result.append("施药方法：").append(diagnosis.getTreatment().getApplication()).append("\n");
            }
            result.append("\n");
        }
        
        // 预防措施
        if (diagnosis.getPreventions() != null && !diagnosis.getPreventions().isEmpty()) {
            result.append("🛡️ 预防措施\n");
            for (String prevention : diagnosis.getPreventions()) {
                result.append("• ").append(prevention).append("\n");
            }
            result.append("\n");
        }
        
        // 注意事项
        if (diagnosis.getNotes() != null && !diagnosis.getNotes().isEmpty()) {
            result.append("⚠️ 注意事项\n");
            for (String note : diagnosis.getNotes()) {
                result.append("• ").append(note).append("\n");
            }
        }
        
        return result.toString();
    }

    /**
     * 格式化聊天回复为友好显示格式
     * 
     * @param chatResponse 结构化聊天回复
     * @return 友好的显示文本
     */
    private String formatChatForDisplay(org.example.ai.dto.ChatResponse chatResponse) {
        StringBuilder result = new StringBuilder();
        
        // 主要回答
        if (chatResponse.getAnswer() != null) {
            result.append(chatResponse.getAnswer()).append("\n");
        }
        
        // 建议
        if (chatResponse.getSuggestions() != null && !chatResponse.getSuggestions().isEmpty()) {
            result.append("\n💡 建议：\n");
            for (String suggestion : chatResponse.getSuggestions()) {
                result.append("• ").append(suggestion).append("\n");
            }
        }
        
        // 警告
        if (chatResponse.getWarnings() != null && !chatResponse.getWarnings().isEmpty()) {
            result.append("\n⚠️ 注意：\n");
            for (String warning : chatResponse.getWarnings()) {
                result.append("• ").append(warning).append("\n");
            }
        }
        
        return result.toString().trim();
    }

    /**
     * 清理Markdown符号，使文本更友好
     * 
     * @param text 原始文本
     * @return 清理后的文本
     */
    private String cleanMarkdownSymbols(String text) {
        if (text == null) return "";
        
        return text
                // 移除多个#号（标题符号）
                .replaceAll("#{1,6}\\s*", "")
                // 移除**粗体符号
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                // 移除*斜体符号
                .replaceAll("\\*(.*?)\\*", "$1")
                // 移除多余的换行符
                .replaceAll("\n{3,}", "\n\n")
                // 移除开头结尾空白
                .trim();
    }
}
