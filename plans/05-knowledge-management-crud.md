# 05 知识库管理列表/搜索/删除真实功能

## 目标

将当前管理后台知识库“文档列表/搜索/删除”的占位功能改成真实可用功能。

## 当前情况

`admin.html` 已有文档列表页面，但显示提示：RAG 服务暂不支持文档列表和删除功能。

后端 `RagPipelineService` 中：

- `listDocuments()` 是桩方法。
- `deleteDocument()` 是桩方法。

## 涉及文件

- `src/main/java/com/aicust/service/RagPipelineService.java`
- `src/main/java/com/aicust/controller/AdminKnowledgeController.java`
- `src/main/resources/static/admin.html`
- 可选新增：
  - `src/main/java/com/aicust/model/KnowledgeDocument.java`
  - `src/main/java/com/aicust/repository/KnowledgeDocumentRepository.java`

## 推荐方案

优先采用“本地文档索引表 + RAG 导入接口”的方式，避免依赖 RAG 服务必须提供完整文档管理 API。

## 数据表设计

新增表：`knowledge_document`

字段建议：

- `id`
- `source_id`
- `title`
- `category`
- `file_name`
- `chunk_count`
- `status`：ACTIVE / DELETED
- `created_at`
- `updated_at`

## 实施步骤

### 步骤 1：新增 Entity

新增 `KnowledgeDocument`，对应 `knowledge_document` 表。

### 步骤 2：新增 Repository

新增 `KnowledgeDocumentRepository`，支持：

- 按状态查询
- 按标题/分类/sourceId 关键词搜索
- 分页查询

### 步骤 3：导入成功后写入本地表

在 `RagPipelineService.ingestText()` 和 `uploadFile()` 成功后，记录文档元数据。

注意：如果 `RagPipelineService` 不适合直接依赖 Repository，也可以新增 `KnowledgeDocumentService` 协调。

### 步骤 4：实现真实列表

`listDocuments(keyword, page, size)` 返回本地表数据。

返回格式保持前端兼容：

```json
{
  "success": true,
  "documents": [],
  "total": 0,
  "page": 0,
  "size": 20
}
```

### 步骤 5：实现删除

删除有两种策略：

1. 软删除：将本地状态改为 `DELETED`。
2. 如果 RAG 服务支持删除，则同步调用 RAG 删除接口。

短期建议先实现软删除，并在页面提示“已从管理列表移除”。

长期需要确保被删除文档不再被 RAG 召回，这要求 RAG 侧支持按 `sourceId` 删除或过滤状态。

### 步骤 6：前端去掉占位提示

修改 `admin.html`：

- 删除“RAG 服务暂不支持文档列表和删除功能”提示。
- 显示真实表格。
- 删除按钮调用后刷新列表。

## 验收标准

1. 上传/文本导入成功后，文档列表能看到记录。
2. 可按关键词搜索标题或分类。
3. 删除后文档从列表消失或状态变更。
4. 页面不再显示占位提示。

## 风险点

- 软删除不能保证 Qdrant 中数据消失，只能保证管理端列表消失。
- 如果要求删除后不再召回，必须增加 RAG 侧删除能力。
- 本地表记录和 RAG 导入失败/成功状态要保持一致。
