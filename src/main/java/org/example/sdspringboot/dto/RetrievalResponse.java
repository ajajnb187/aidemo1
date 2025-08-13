package org.example.sdspringboot.dto;

import lombok.Data;

import java.util.List;

/**
 * 向量检索/语义检索的响应对象（预留示例）。
 *
 * - code：200 成功，其它失败；
 * - records：返回召回的语义片段（包含内容、相似度、来源元数据等）。
 */
@Data
public class RetrievalResponse {
    /**
     * 状态码：200成功，其他失败
     */
    private int code;
    
    /**
     * 状态描述
     */
    private String message;

    /**
     * 检索结果列表
     */
    private List<Chunk> records;

    @Data
    public static class Chunk {
        /**
         * 检索到的内容
         */
        private String content;
        
        /**
         * 相似度分数
         */
        private Double score;
        
        /**
         * 文档标题
         */
        private String title;
        
        /**
         * 文档ID（可选）
         */
        private String documentId;
    }
}
