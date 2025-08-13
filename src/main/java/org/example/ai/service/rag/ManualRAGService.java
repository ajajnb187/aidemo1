package org.example.ai.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 手动RAG服务
 * 由于版本兼容性问题，暂时使用手动实现RAG功能
 */
@Service
@Slf4j
public class ManualRAGService {

    private final VectorStoreService vectorStoreService;

    @Autowired
    public ManualRAGService(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * 为诊断消息添加RAG上下文
     * 
     * @param originalMessage 原始诊断消息
     * @param topK 检索文档数量
     * @param threshold 相似度阈值
     * @return 增强后的消息（包含相关知识上下文）
     */
    public String enhanceMessageWithRAG(String originalMessage, int topK, double threshold) {
        try {
            log.info("开始RAG增强，查询：{}，topK：{}，阈值：{}", originalMessage, topK, threshold);
            
            // 1. 从向量存储中检索相关文档
            List<Document> relevantDocs = vectorStoreService.searchSimilar(originalMessage, topK, threshold);
            
            if (relevantDocs.isEmpty()) {
                log.info("未找到相关文档，使用原始消息");
                return originalMessage;
            }
            
            // 2. 构建上下文信息
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("参考知识库信息：\n\n");
            
            for (int i = 0; i < relevantDocs.size(); i++) {
                Document doc = relevantDocs.get(i);
                String source = (String) doc.getMetadata().get("source");
                String category = (String) doc.getMetadata().get("category");
                
                contextBuilder.append(String.format("【参考资料%d】", i + 1));
                if (source != null) {
                    contextBuilder.append(" 来源：").append(source);
                }
                if (category != null) {
                    contextBuilder.append(" 类别：").append(category);
                }
                contextBuilder.append("\n");
                contextBuilder.append(doc.getText());
                contextBuilder.append("\n\n");
            }
            
            // 3. 组合原始消息和上下文
            String enhancedMessage = contextBuilder.toString() + 
                "---\n\n" +
                "用户问题：\n" + originalMessage + "\n\n" +
                "请基于以上参考资料和你的专业知识，提供准确、实用的诊断建议。如果参考资料与用户描述的情况不匹配，请以实际检测结果为准。";
            
            log.info("RAG增强完成，添加了{}个参考文档，增强消息长度：{}", relevantDocs.size(), enhancedMessage.length());
            return enhancedMessage;
            
        } catch (Exception e) {
            log.warn("RAG增强失败，使用原始消息：{}", e.getMessage());
            return originalMessage;
        }
    }

    /**
     * 为对话消息添加RAG上下文（针对聊天场景优化）
     * 
     * @param originalMessage 原始聊天消息
     * @return 增强后的消息
     */
    public String enhanceChatMessageWithRAG(String originalMessage) {
        return enhanceMessageWithRAG(originalMessage, 5, 0.65);
    }

    /**
     * 为诊断消息添加RAG上下文（针对诊断场景优化）
     * 
     * @param originalMessage 原始诊断消息
     * @return 增强后的消息
     */
    public String enhanceDiagnosisMessageWithRAG(String originalMessage) {
        return enhanceMessageWithRAG(originalMessage, 8, 0.7);
    }

    /**
     * 搜索相关农业知识
     * 
     * @param query 查询内容
     * @param maxResults 最大结果数
     * @return 相关知识列表
     */
    public List<String> searchRelevantKnowledge(String query, int maxResults) {
        try {
            List<Document> docs = vectorStoreService.searchSimilar(query, maxResults, 0.6);
            
            return docs.stream()
                .map(doc -> {
                    String source = (String) doc.getMetadata().get("source");
                    String category = (String) doc.getMetadata().get("category");
                    
                    StringBuilder result = new StringBuilder();
                    if (source != null || category != null) {
                        result.append("[");
                        if (category != null) result.append(category);
                        if (source != null) {
                            if (category != null) result.append(" - ");
                            result.append(source);
                        }
                        result.append("]\n");
                    }
                    result.append(doc.getText());
                    return result.toString();
                })
                .toList();
                
        } catch (Exception e) {
            log.error("搜索相关知识失败", e);
            return List.of();
        }
    }
}
