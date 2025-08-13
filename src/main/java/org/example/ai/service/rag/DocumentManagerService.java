package org.example.ai.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档管理服务
 * 负责管理知识库文档的加载和更新
 */
@Service
@Slf4j
public class DocumentManagerService {

    private final VectorStoreService vectorStoreService;

    @Autowired
    public DocumentManagerService(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * 应用启动时初始化知识库
     * 可以根据实际需要启用或禁用
     */
    @PostConstruct
    public void initializeKnowledgeBase() {
        log.info("开始初始化RAG知识库...");
        
        try {
            // 加载默认的农业知识文档
            loadDefaultDocuments();
            
            log.info("RAG知识库初始化完成");
        } catch (Exception e) {
            log.warn("RAG知识库初始化失败，请手动添加文档", e);
        }
    }

    /**
     * 加载默认的农业知识文档
     */
    private void loadDefaultDocuments() {
        List<DocumentInfo> defaultDocs = getDefaultDocuments();
        
        for (DocumentInfo docInfo : defaultDocs) {
            try {
                Resource resource = new ClassPathResource(docInfo.getPath());
                if (resource.exists()) {
                    int chunks = vectorStoreService.addDocument(resource, docInfo.getMetadata());
                    log.info("已加载文档：{}，分割为 {} 个chunk", docInfo.getName(), chunks);
                } else {
                    log.warn("默认文档不存在：{}", docInfo.getPath());
                }
            } catch (Exception e) {
                log.warn("加载默认文档失败：{}，错误：{}", docInfo.getName(), e.getMessage());
            }
        }
    }

    /**
     * 添加农业知识文档
     * 
     * @param resource 文档资源
     * @param category 文档类别（如：病害防治、栽培技术等）
     * @param subject 具体主题（如：水稻、小麦等）
     * @param tags 标签列表
     * @return 添加的文档chunk数量
     */
    public int addAgriculturalDocument(Resource resource, String category, String subject, List<String> tags) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "agricultural_knowledge");
        metadata.put("category", category);
        metadata.put("subject", subject);
        metadata.put("source", resource.getFilename());
        metadata.put("upload_time", System.currentTimeMillis());
        
        if (tags != null && !tags.isEmpty()) {
            metadata.put("tags", String.join(",", tags));
        }
        
        return vectorStoreService.addDocument(resource, metadata);
    }

    /**
     * 添加病虫害防治文档
     * 
     * @param resource 文档资源
     * @param diseaseName 病害名称
     * @param cropType 作物类型
     * @return 添加的文档chunk数量
     */
    public int addDiseaseControlDocument(Resource resource, String diseaseName, String cropType) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "disease_control");
        metadata.put("disease_name", diseaseName);
        metadata.put("crop_type", cropType);
        metadata.put("category", "病虫害防治");
        metadata.put("source", resource.getFilename());
        metadata.put("upload_time", System.currentTimeMillis());
        
        return vectorStoreService.addDocument(resource, metadata);
    }

    /**
     * 批量添加文档
     * 
     * @param resources 文档资源列表
     * @param commonCategory 公共类别
     * @param commonSubject 公共主题
     * @return 添加的总chunk数量
     */
    public int addDocumentsBatch(List<Resource> resources, String commonCategory, String commonSubject) {
        Map<String, Object> commonMetadata = new HashMap<>();
        commonMetadata.put("type", "agricultural_knowledge");
        commonMetadata.put("category", commonCategory);
        commonMetadata.put("subject", commonSubject);
        commonMetadata.put("upload_time", System.currentTimeMillis());
        
        return vectorStoreService.addDocuments(resources, commonMetadata);
    }

    /**
     * 搜索农业知识
     * 
     * @param query 查询内容
     * @param category 文档类别过滤（可选）
     * @param subject 主题过滤（可选）
     * @param topK 返回结果数
     * @return 相关文档内容列表
     */
    public List<String> searchAgriculturalKnowledge(String query, String category, String subject, int topK) {
        // 构建过滤表达式
        StringBuilder filterBuilder = new StringBuilder("type == 'agricultural_knowledge'");
        
        if (category != null && !category.trim().isEmpty()) {
            filterBuilder.append(" && category == '").append(category).append("'");
        }
        
        if (subject != null && !subject.trim().isEmpty()) {
            filterBuilder.append(" && subject == '").append(subject).append("'");
        }
        
        String filterExpression = filterBuilder.toString();
        
        // 执行搜索
        var documents = vectorStoreService.searchWithFilter(query, topK, 0.6, filterExpression);
        
        // 提取文档内容
        List<String> results = new ArrayList<>();
        for (var doc : documents) {
            String content = doc.getText();
            String source = (String) doc.getMetadata().get("source");
            results.add(String.format("[来源：%s]\n%s", source != null ? source : "未知", content));
        }
        
        return results;
    }

    /**
     * 获取知识库统计信息
     * 
     * @return 统计信息
     */
    public String getKnowledgeBaseStats() {
        return vectorStoreService.getStats();
    }

    /**
     * 清空知识库（慎用）
     */
    public void clearKnowledgeBase() {
        vectorStoreService.deleteAll();
        log.warn("知识库已清空");
    }

    /**
     * 获取默认文档配置
     * 
     * @return 默认文档信息列表
     */
    private List<DocumentInfo> getDefaultDocuments() {
        List<DocumentInfo> docs = new ArrayList<>();
        
        // 这里添加您的默认农业知识文档
        // 注意：需要将文档文件放在 src/main/resources/knowledge/ 目录下
        
        docs.add(new DocumentInfo(
            "水稻病虫害防治手册",
            "knowledge/rice_disease_control.txt",
            createMetadata("病虫害防治", "水稻", "防治手册,病害识别,用药指南")
        ));
        
        docs.add(new DocumentInfo(
            "农药安全使用指南",
            "knowledge/pesticide_safety_guide.txt",
            createMetadata("用药安全", "通用", "农药安全,使用方法,注意事项")
        ));
        
        docs.add(new DocumentInfo(
            "水稻生长期管理",
            "knowledge/rice_growth_management.txt",
            createMetadata("栽培技术", "水稻", "生长管理,田间管理,营养管理")
        ));
        
        return docs;
    }

    /**
     * 创建元数据
     */
    private Map<String, Object> createMetadata(String category, String subject, String tags) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "agricultural_knowledge");
        metadata.put("category", category);
        metadata.put("subject", subject);
        metadata.put("tags", tags);
        metadata.put("upload_time", System.currentTimeMillis());
        return metadata;
    }

    /**
     * 文档信息内部类
     */
    private static class DocumentInfo {
        private final String name;
        private final String path;
        private final Map<String, Object> metadata;

        public DocumentInfo(String name, String path, Map<String, Object> metadata) {
            this.name = name;
            this.path = path;
            this.metadata = metadata;
        }

        public String getName() { return name; }
        public String getPath() { return path; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
}
