package org.example.sdspringboot.dto;

import lombok.Data;

/**
 * 向量检索/语义检索的请求对象（预留示例）。
 *
 * 典型流程：
 * - 前端发送查询 query（用户问题/关键字），可可选指定 topK 与相似度阈值；
 * - 服务端据此从向量库/索引中召回最相关的片段，作为 RAG 上下文；
 * - 再将上下文与用户问题交给大模型生成回答。
 */
@Data
public class RetrievalRequest {
    /**
     * 检索的问题（必填）
     */
    private String query;

    /**
     * 可选：覆盖默认的 topK（不填则使用配置文件中的值）
     */
    private Integer topK;
    
    /**
     * 可选：覆盖默认的相似度阈值（不填则使用配置文件中的值）
     */
    private Double similarityThreshold;
}
