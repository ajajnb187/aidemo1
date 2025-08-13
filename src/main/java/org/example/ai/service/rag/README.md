# RAG功能使用指南

## 功能概述

此RAG（检索增强生成）系统为您的水稻病虫害诊断应用提供了知识库检索功能，可以基于上传的农业知识文档为AI诊断提供更准确的背景信息和专业知识。

## 主要组件

### 1. VectorStoreService
向量存储服务，负责：
- 文档的向量化存储
- 相似性搜索
- 元数据过滤

### 2. DocumentManagerService  
文档管理服务，负责：
- 知识库文档的加载和管理
- 文档分类和标签管理
- 知识库初始化

### 3. DocumentController
文档管理API接口，提供：
- 文档上传接口
- 知识库搜索接口
- 统计信息接口

## API使用说明

### 上传农业知识文档
```http
POST /api/documents/upload
Content-Type: multipart/form-data

file: 文档文件 (.txt, .md, .doc, .docx)
category: 文档类别 (病虫害防治、栽培技术、用药安全等)
subject: 具体主题 (水稻、小麦、玉米等)  
tags: 标签 (逗号分隔)
```

### 上传病虫害防治专用文档
```http
POST /api/documents/upload/disease-control
Content-Type: multipart/form-data

file: 文档文件
diseaseName: 病害名称
cropType: 作物类型
```

### 搜索知识库
```http
GET /api/documents/search?query=稻瘟病&category=病虫害防治&topK=5
```

### 获取知识库状态
```http
GET /api/documents/stats
```

### 调试向量搜索
```http
GET /api/documents/debug/vector-search?query=稻瘟病&topK=3&threshold=0.6
```

## 配置说明

### 1. 数据库配置
确保application.yml中配置了数据库连接：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aidemo1
    username: root
    password: 123456
```

### 2. 向量存储配置
```yaml
spring:
  ai:
    vectorstore:
      pgvector:
        table-name: ai_knowledge_vectors
        dimensions: 1536
        initialize-schema: true
```

### 3. 嵌入模型配置
目前使用阿里云DashScope的嵌入模型，确保API密钥正确配置：
```yaml
spring:
  ai:
    dashscope:
      api-key: your-api-key
      embedding:
        options:
          model: text-embedding-v1
```

## 知识库管理

### 默认文档
系统启动时会自动加载以下默认文档：
- `rice_disease_control.txt` - 水稻病虫害防治手册
- `pesticide_safety_guide.txt` - 农药安全使用指南  
- `rice_growth_management.txt` - 水稻生长期管理技术

### 文档格式要求
- 支持格式：.txt, .md, .doc, .docx
- 建议使用UTF-8编码
- 内容应包含明确的主题和关键信息

### 元数据标签
为了提高检索精度，建议为文档添加合适的元数据：
- `type`: 文档类型 (agricultural_knowledge, disease_control等)
- `category`: 类别 (病虫害防治、栽培技术、用药安全等)
- `subject`: 主题 (水稻、小麦等)
- `tags`: 标签 (逗号分隔)

## 使用场景

### 1. 病虫害诊断增强
AI诊断时会自动从知识库检索相关内容，提供：
- 相似病害的详细信息
- 防治方案参考
- 用药安全指导

### 2. 专家咨询支持
用户提问时，系统会：
- 检索相关专业知识
- 提供权威的参考信息
- 减少AI幻觉现象

### 3. 知识管理
支持：
- 批量上传专业文档
- 分类管理知识内容
- 定期更新知识库

## 注意事项

1. **首次启动**：系统首次启动时会初始化数据库表，请确保数据库连接正常
2. **文档质量**：上传高质量、准确的专业文档可以显著提高RAG效果
3. **相似度阈值**：可根据实际效果调整搜索的相似度阈值
4. **性能优化**：大量文档时建议定期清理无用数据，优化检索性能

## 故障排查

### 常见问题

1. **向量存储连接失败**
   - 检查数据库连接配置
   - 确认数据库服务运行正常
   - 检查用户权限

2. **嵌入模型错误**  
   - 确认DashScope API密钥正确
   - 检查网络连接
   - 验证嵌入模型配置

3. **文档上传失败**
   - 检查文件格式是否支持
   - 确认文件大小合理
   - 查看服务器日志

4. **搜索结果质量差**
   - 调整相似度阈值
   - 优化文档内容质量
   - 检查元数据标签准确性

## 性能监控

建议监控以下指标：
- 向量搜索响应时间
- 文档检索成功率
- 系统内存使用情况
- 数据库查询性能

## 扩展建议

1. **多语言支持**：可扩展支持多种语言的文档和搜索
2. **文档版本管理**：支持文档的版本控制和历史记录
3. **智能推荐**：基于用户行为推荐相关文档
4. **批量处理**：支持大批量文档的自动化处理
