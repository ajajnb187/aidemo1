package org.example.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI聊天结构化响应
 * 用于友好展示AI助手的回答
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"answer", "suggestions", "warnings"})
public class ChatResponse {
    
    /**
     * 主要回答内容
     */
    private String answer;
    
    /**
     * 建议或补充信息
     */
    private List<String> suggestions;
    
    /**
     * 警告或注意事项
     */
    private List<String> warnings;
}

