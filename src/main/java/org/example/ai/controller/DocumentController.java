package org.example.ai.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.ai.dto.ApiResponse;
import org.example.ai.service.rag.DocumentManagerService;
import org.example.ai.service.rag.VectorStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 文档管理控制器
 * 提供知识库文档的上传、管理和搜索功能
 */
@RestController
@RequestMapping("/api/documents")
@Slf4j
public class DocumentController {

    private final DocumentManagerService documentManagerService;
    private final VectorStoreService vectorStoreService;

    @Autowired
    public DocumentController(DocumentManagerService documentManagerService, 
                            VectorStoreService vectorStoreService) {
        this.documentManagerService = documentManagerService;
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * 上传单个农业知识文档
     * 
     * @param file 文档文件
     * @param category 文档类别（病虫害防治、栽培技术、用药安全等）
     * @param subject 具体主题（水稻、小麦、玉米等）
     * @param tags 标签（逗号分隔）
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "通用知识") String category,
            @RequestParam(value = "subject", defaultValue = "农业") String subject,
            @RequestParam(value = "tags", required = false) String tags) {
        
        log.info("收到文档上传请求：文件={}, 类别={}, 主题={}", file.getOriginalFilename(), category, subject);
        
        try {
            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("文件不能为空"));
            }
            
            if (!isValidFileType(file.getOriginalFilename())) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("不支持的文件类型，仅支持：.txt, .md, .doc, .docx"));
            }
            
            // 转换为Resource
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            
            // 解析标签
            List<String> tagList = null;
            if (tags != null && !tags.trim().isEmpty()) {
                tagList = Arrays.asList(tags.split(","));
            }
            
            // 添加文档到向量存储
            int chunks = documentManagerService.addAgriculturalDocument(resource, category, subject, tagList);
            
            // 构建响应数据
            Map<String, Object> result = new HashMap<>();
            result.put("filename", file.getOriginalFilename());
            result.put("category", category);
            result.put("subject", subject);
            result.put("chunks", chunks);
            result.put("fileSize", file.getSize());
            
            log.info("文档上传成功：{}，生成{}个文本块", file.getOriginalFilename(), chunks);
            
            return ResponseEntity.ok(ApiResponse.success("文档上传成功", result));
            
        } catch (Exception e) {
            log.error("文档上传失败：{}", file.getOriginalFilename(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("文档上传失败：" + e.getMessage()));
        }
    }

    /**
     * 上传病虫害防治专用文档
     * 
     * @param file 文档文件
     * @param diseaseName 病害名称
     * @param cropType 作物类型
     * @return 上传结果
     */
    @PostMapping("/upload/disease-control")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadDiseaseControlDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("diseaseName") String diseaseName,
            @RequestParam("cropType") String cropType) {
        
        log.info("收到病害防治文档上传：文件={}, 病害={}, 作物={}", 
                file.getOriginalFilename(), diseaseName, cropType);
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("文件不能为空"));
            }
            
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            
            int chunks = documentManagerService.addDiseaseControlDocument(resource, diseaseName, cropType);
            
            Map<String, Object> result = new HashMap<>();
            result.put("filename", file.getOriginalFilename());
            result.put("diseaseName", diseaseName);
            result.put("cropType", cropType);
            result.put("chunks", chunks);
            result.put("fileSize", file.getSize());
            
            return ResponseEntity.ok(ApiResponse.success("病害防治文档上传成功", result));
            
        } catch (Exception e) {
            log.error("病害防治文档上传失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("病害防治文档上传失败：" + e.getMessage()));
        }
    }

    /**
     * 搜索知识库内容
     * 
     * @param query 查询内容
     * @param category 类别过滤（可选）
     * @param subject 主题过滤（可选）
     * @param topK 返回结果数量（默认5）
     * @return 搜索结果
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchKnowledge(
            @RequestParam("query") String query,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {
        
        log.info("收到知识库搜索请求：查询={}, 类别={}, 主题={}, topK={}", 
                query, category, subject, topK);
        
        try {
            List<String> results = documentManagerService.searchAgriculturalKnowledge(
                query, category, subject, topK);
            
            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("category", category);
            response.put("subject", subject);
            response.put("resultCount", results.size());
            response.put("results", results);
            
            return ResponseEntity.ok(ApiResponse.success("搜索完成", response));
            
        } catch (Exception e) {
            log.error("知识库搜索失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("搜索失败：" + e.getMessage()));
        }
    }

    /**
     * 获取知识库统计信息
     * 
     * @return 统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        try {
            String statsInfo = documentManagerService.getKnowledgeBaseStats();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("status", "运行中");
            stats.put("info", statsInfo);
            stats.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success("统计信息获取成功", stats));
            
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("获取统计信息失败：" + e.getMessage()));
        }
    }

    /**
     * 重新初始化知识库（管理员操作）
     * 
     * @return 操作结果
     */
    @PostMapping("/reinitialize")
    public ResponseEntity<ApiResponse<String>> reinitializeKnowledgeBase() {
        log.warn("收到重新初始化知识库请求");
        
        try {
            // 清空现有知识库
            documentManagerService.clearKnowledgeBase();
            
            // 重新加载默认文档
            documentManagerService.initializeKnowledgeBase();
            
            return ResponseEntity.ok(ApiResponse.success("重新初始化完成", "知识库已重新初始化"));
            
        } catch (Exception e) {
            log.error("重新初始化知识库失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("重新初始化失败：" + e.getMessage()));
        }
    }

    /**
     * 清空知识库（危险操作，需要管理员权限）
     * 
     * @return 操作结果
     */
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<String>> clearKnowledgeBase() {
        log.warn("收到清空知识库请求");
        
        try {
            documentManagerService.clearKnowledgeBase();
            return ResponseEntity.ok(ApiResponse.success("清空完成", "知识库已清空"));
            
        } catch (Exception e) {
            log.error("清空知识库失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("清空失败：" + e.getMessage()));
        }
    }

    /**
     * 直接向向量存储查询（调试用）
     * 
     * @param query 查询文本
     * @param topK 返回结果数量
     * @param threshold 相似度阈值
     * @return 原始搜索结果
     */
    @GetMapping("/debug/vector-search")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> debugVectorSearch(
            @RequestParam("query") String query,
            @RequestParam(value = "topK", defaultValue = "3") int topK,
            @RequestParam(value = "threshold", defaultValue = "0.6") double threshold) {
        
        log.info("收到向量搜索调试请求：查询={}, topK={}, 阈值={}", query, topK, threshold);
        
        try {
            var documents = vectorStoreService.searchSimilar(query, topK, threshold);
            
            List<Map<String, Object>> results = documents.stream()
                .map(doc -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("content", doc.getText());
                    item.put("metadata", doc.getMetadata());
                    return item;
                })
                .toList();
            
            return ResponseEntity.ok(ApiResponse.success("向量搜索完成", results));
            
        } catch (Exception e) {
            log.error("向量搜索失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("向量搜索失败：" + e.getMessage()));
        }
    }

    /**
     * 验证文件类型
     */
    private boolean isValidFileType(String filename) {
        if (filename == null) return false;
        
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".txt") || 
               lowerName.endsWith(".md") || 
               lowerName.endsWith(".doc") || 
               lowerName.endsWith(".docx");
    }
}
