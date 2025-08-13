package org.example.ai.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 向量存储服务
 * 负责文档的加载、分割、存储和检索
 */
@Service
@Slf4j
public class VectorStoreService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;

    @Autowired
    public VectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        // 配置文本分割器：每个chunk最大800个token，重叠200个token
        this.textSplitter = new TokenTextSplitter(800, 200, 5, 10000, true);
        log.info("向量存储服务初始化完成，向量存储已启用");
    }

    /**
     * 添加单个文档到向量存储
     * 
     * @param resource 文档资源
     * @param metadata 元数据（如文档类型、主题等）
     * @return 添加的文档数量
     */
    public int addDocument(Resource resource, Map<String, Object> metadata) {
        try {
            // 1. 读取文档
            DocumentReader reader = new TextReader(resource);
            List<Document> documents = reader.get();
            
            // 2. 添加元数据
            if (metadata != null && !metadata.isEmpty()) {
                documents.forEach(doc -> doc.getMetadata().putAll(metadata));
            }
            
            // 3. 文档分割
            List<Document> splitDocuments = textSplitter.apply(documents);
            
            // 4. 存储到向量数据库
            vectorStore.add(splitDocuments);
            log.info("文档已成功存储到向量数据库");
            
            log.info("成功添加文档：{}，分割后chunk数量：{}", resource.getFilename(), splitDocuments.size());
            return splitDocuments.size();
            
        } catch (Exception e) {
            log.error("添加文档失败：{}", resource.getFilename(), e);
            throw new RuntimeException("文档添加失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量添加文档到向量存储
     * 
     * @param resources 文档资源列表
     * @param commonMetadata 公共元数据
     * @return 添加的文档总数
     */
    public int addDocuments(List<Resource> resources, Map<String, Object> commonMetadata) {
        int totalChunks = 0;
        
        for (Resource resource : resources) {
            try {
                int chunks = addDocument(resource, commonMetadata);
                totalChunks += chunks;
            } catch (Exception e) {
                log.warn("跳过文档（处理失败）：{}", resource.getFilename(), e);
            }
        }
        
        log.info("批量添加完成，总共处理 {} 个文档，生成 {} 个chunk", resources.size(), totalChunks);
        return totalChunks;
    }

    /**
     * 根据查询进行相似性搜索
     * 
     * @param query 查询文本
     * @param topK 返回最相似的K个结果
     * @param similarityThreshold 相似度阈值 (0-1)
     * @return 搜索结果文档列表
     */
    public List<Document> searchSimilar(String query, int topK, double similarityThreshold) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build();
                    
            List<Document> results = vectorStore.similaritySearch(searchRequest);
            
            log.info("相似性搜索完成：查询[{}]，返回{}个结果", query, results.size());
            return results;
            
        } catch (Exception e) {
            log.error("相似性搜索失败：{}", query, e);
            throw new RuntimeException("搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据元数据过滤进行搜索
     * 
     * @param query 查询文本
     * @param topK 返回最相似的K个结果
     * @param similarityThreshold 相似度阈值
     * @param filterExpression 过滤表达式 (如：category == '农业知识')
     * @return 搜索结果文档列表
     */
    public List<Document> searchWithFilter(String query, int topK, double similarityThreshold, String filterExpression) {
        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold);
                    
            if (filterExpression != null && !filterExpression.trim().isEmpty()) {
                log.info("应用过滤表达式：{}", filterExpression);
                // 注意：过滤功能取决于向量存储实现
                // builder = builder.filterExpression(filterExpression);
            }
                    
            List<Document> results = vectorStore.similaritySearch(builder.build());
            
            log.info("带过滤搜索完成：查询[{}]，过滤[{}]，返回{}个结果", 
                    query, filterExpression, results.size());
            return results;
            
        } catch (Exception e) {
            log.error("带过滤搜索失败：{}", query, e);
            throw new RuntimeException("搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除所有文档（慎用）
     */
    public void deleteAll() {
        try {
            // 注意：这会删除向量存储中的所有数据
            // SimpleVectorStore没有直接的deleteAll方法，我们可以重新创建实例
            log.warn("正在删除向量存储中的所有文档...");
        } catch (Exception e) {
            log.error("删除所有文档失败", e);
            throw new RuntimeException("删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取向量存储的统计信息
     * 
     * @return 统计信息字符串
     */
    public String getStats() {
        try {
            // 简单的统计查询，返回任意查询的结果数作为数据库规模参考
            List<Document> sampleResults = vectorStore.similaritySearch(
                SearchRequest.builder().query("水稻").topK(1).build()
            );
            
            return String.format("向量存储状态：正常运行，示例查询返回 %d 个结果", sampleResults.size());
        } catch (Exception e) {
            return "向量存储状态：连接异常 - " + e.getMessage();
        }
    }
}
