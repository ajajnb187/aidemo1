package org.example.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
// import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
// import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AI配置类
 * 配置聊天记忆、向量存储和不同用途的ChatClient
 */
@Configuration
@Slf4j
public class AIConfig {

    /**
     * 病虫害诊断专用系统提示词（支持结构化输出）
     */
    private static final String DIAGNOSIS_SYSTEM_PROMPT = 
            "你是一位资深的水稻病虫害专家，拥有丰富的田间诊断和防治经验。你具备调用天气查询工具的能力，能够结合当地气候条件提供更精准的诊断分析。\n\n" +
            
            "诊断要求：\n" +
            "1. 如果用户提供了地址信息，请先调用天气工具查询当地近期天气情况\n" +
            "2. 结合天气数据（温度、湿度、降水等）分析病虫害发病原因\n" +
            "3. 根据天气趋势调整防治建议的紧急程度\n" +
            "4. 必须严格按照用户提供的JSON格式输出结构化诊断结果\n\n" +
            
            "输出内容要求：\n" +
            "• 病害信息：包含病害名称、严重程度（轻微/中等/严重）、描述和天气影响分析\n" +
            "• 症状特征：列出主要症状表现的要点\n" +
            "• 发病原因：分析导致病害的具体原因\n" +
            "• 治疗方案：包含紧急措施、推荐药剂（名称、剂量、频次）、用药时机和施药方法\n" +
            "• 预防措施：长期预防和管理建议\n" +
            "• 注意事项：安全提醒和重要事项\n\n" +
            
            "语言要求：专业但易懂，确保农民朋友能够理解和执行。";

    /**
     * 聊天助手系统提示词（支持结构化输出）
     */
    private static final String CHAT_SYSTEM_PROMPT = 
            "你是一位专业的水稻病虫害专家助手，能够基于之前的诊断结果回答用户的各种问题。\n\n" +
            
            "职责：\n" +
            "1. 基于聊天历史中的诊断结果，回答用户关于病虫害的进一步问题\n" +
            "2. 提供详细的用药指导、时机选择、注意事项等\n" +
            "3. 解答关于病害发展、传播、预防的疑问\n" +
            "4. 如果需要天气信息，可以调用天气工具\n" +
            "5. 利用你的专业知识库回答用户的专业问题\n" +
            "6. 必须严格按照用户提供的JSON格式输出结构化回答\n\n" +
            
            "回答原则：\n" +
            "- 基于对话历史提供针对性回答\n" +
            "- 语言简洁明了，便于农民理解\n" +
            "- 必要时提供具体的操作建议\n" +
            "- 如有安全风险，及时提醒注意事项\n" +
            "- 保持与之前诊断结果的一致性和连贯性\n\n" +
            
            "输出格式要求：\n" +
            "• answer: 主要回答内容\n" +
            "• suggestions: 建议或补充信息列表\n" +
            "• warnings: 警告或注意事项列表";

    /**
     * 配置聊天记忆
     * 使用消息窗口记忆，保留最近20条消息
     */
    @Bean
    @Primary
    public ChatMemory chatMemory() {
        log.info("配置ChatMemory - MessageWindowChatMemory，窗口大小：20");
        return MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository()) // 使用内存存储
            .maxMessages(20) // 保留最近20条消息
            .build();
    }

    /**
     * 病虫害诊断专用ChatClient
     * 不使用记忆功能，每次独立诊断
     */
    @Bean("diagnosisChatClient")
    public ChatClient diagnosisChatClient(ChatModel chatModel) {
        log.info("配置诊断专用ChatClient");
        return ChatClient.builder(chatModel)
                .defaultSystem(DIAGNOSIS_SYSTEM_PROMPT)
                .build();
    }

    /**
     * 聊天对话用ChatClient
     * 使用记忆功能和RAG检索增强功能
     */
    @Bean("conversationChatClient")
    public ChatClient conversationChatClient(ChatModel chatModel, ChatMemory chatMemory, VectorStore vectorStore) {
        log.info("配置对话专用ChatClient，启用聊天记忆和RAG功能");
        
        return ChatClient.builder(chatModel)
                .defaultSystem(CHAT_SYSTEM_PROMPT)
                .defaultAdvisors(
                    // 聊天记忆顾问 - 管理对话历史
                    MessageChatMemoryAdvisor.builder(chatMemory).build()
                    
                    // RAG检索增强顾问 - 暂时注释，等待版本兼容性解决
                    // 注意：RAG功能现在通过ManualRAGService手动实现
                    // QuestionAnswerAdvisor.builder()
                    //     .vectorStore(vectorStore)
                    //     .similarityThreshold(0.65) // 设置相似度阈值，过滤不相关内容
                    //     .topK(5) // 检索最相关的5个文档片段
                    //     .filterExpression(new FilterExpressionBuilder()
                    //         .eq("type", "agricultural_knowledge") // 只检索农业知识类文档
                    //         .build())
                    //     .build()
                )
                .build();
    }

    /**
     * 专用于病虫害诊断的RAG增强ChatClient
     * 针对诊断场景优化的RAG配置
     */
    @Bean("diagnosisRagChatClient")
    public ChatClient diagnosisRagChatClient(ChatModel chatModel, VectorStore vectorStore) {
        log.info("配置诊断专用RAG ChatClient，针对病虫害诊断优化");
        
        return ChatClient.builder(chatModel)
                .defaultSystem(DIAGNOSIS_SYSTEM_PROMPT + 
                    "\n\n补充说明：你可以参考提供的农业知识库内容，但请结合实际检测结果进行诊断，" +
                    "确保建议的准确性和实用性。优先使用经过验证的防治方法。")
                // 暂时不使用advisors，RAG功能通过ManualRAGService实现
                // .defaultAdvisors(
                //     // 病虫害诊断专用RAG顾问 - 暂时注释，使用手动实现
                //     // QuestionAnswerAdvisor.builder()
                //     //     .vectorStore(vectorStore)
                //     //     .similarityThreshold(0.7) // 诊断场景使用更高的相似度阈值
                //     //     .topK(8) // 获取更多相关文档以提供全面的诊断依据
                //     //     .filterExpression(new FilterExpressionBuilder()
                //     //         .in("category", "病虫害防治", "用药安全", "栽培技术") // 多类别过滤
                //     //         .build())
                //     //     .build()
                // )
                .build();
    }
}
