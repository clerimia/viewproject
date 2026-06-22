package com.aicust.service;

import com.aicust.model.KnowledgeDocument;
import com.aicust.repository.KnowledgeDocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 知识文档管理服务 —— 本地维护文档索引，导入内容仍由 RAG Pipeline 处理。
 */
@Service
public class KnowledgeDocumentService {

    private final RagPipelineService pipelineService;
    private final KnowledgeDocumentRepository repository;

    public KnowledgeDocumentService(RagPipelineService pipelineService, KnowledgeDocumentRepository repository) {
        this.pipelineService = pipelineService;
        this.repository = repository;
    }

    public Map<String, Object> ingestText(String text, String title, String category,
                                           String sourceId, int chunkSize, int chunkOverlap) {
        Map<String, Object> result = pipelineService.ingestText(text, title, category, sourceId, chunkSize, chunkOverlap);
        if (Boolean.TRUE.equals(result.get("success"))) {
            KnowledgeDocument document = new KnowledgeDocument();
            document.setSourceId(sourceId != null ? sourceId : "manual");
            document.setRagDocumentId(asString(result.getOrDefault("documentId", result.get("id"))));
            document.setTitle(title != null && !title.isBlank() ? title : sourceId);
            document.setCategory(category != null ? category : "");
            document.setFileName("");
            document.setContentType("text");
            document.setChunkCount(extractChunkCount(result));
            document.setStatus("ACTIVE");
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            KnowledgeDocument saved = repository.save(document);
            return withDocument(result, saved);
        }
        return result;
    }

    public Map<String, Object> uploadFile(byte[] fileBytes, String fileName,
                                           String title, String category, String sourceId) {
        Map<String, Object> result = pipelineService.uploadFile(fileBytes, fileName, title, category, sourceId);
        if (Boolean.TRUE.equals(result.get("success"))) {
            KnowledgeDocument document = new KnowledgeDocument();
            document.setSourceId(sourceId);
            document.setRagDocumentId(asString(result.getOrDefault("documentId", result.get("id"))));
            document.setTitle(title != null && !title.isBlank() ? title : fileName);
            document.setCategory(category != null ? category : "");
            document.setFileName(fileName != null ? fileName : "");
            document.setContentType("upload");
            document.setChunkCount(extractChunkCount(result));
            document.setStatus("ACTIVE");
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            KnowledgeDocument saved = repository.save(document);
            return withDocument(result, saved);
        }
        return result;
    }

    public Map<String, Object> listDocuments(String keyword, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<KnowledgeDocument> docs = repository.searchActive(keyword == null ? "" : keyword.trim(), pageable);

        return Map.of(
                "success", true,
                "documents", docs.getContent(),
                "total", docs.getTotalElements(),
                "page", safePage,
                "size", safeSize
        );
    }

    public Map<String, Object> deleteDocument(Long id) {
        KnowledgeDocument document = repository.findById(id).orElse(null);
        if (document == null || !"ACTIVE".equals(document.getStatus())) {
            return Map.of("success", false, "message", "文档不存在或已删除");
        }

        String ragId = document.getRagDocumentId();
        if (ragId == null || ragId.isBlank()) {
            ragId = document.getSourceId();
        }
        Map<String, Object> ragDeleteResult = pipelineService.deleteDocument(ragId);

        document.setStatus("DELETED");
        document.setUpdatedAt(LocalDateTime.now());
        repository.save(document);

        String message = Boolean.TRUE.equals(ragDeleteResult.get("success"))
                ? "删除成功"
                : "已从本地文档列表移除；RAG 删除接口未确认成功，请联调 RAG 服务删除能力";
        return Map.of("success", true, "message", message, "ragResult", ragDeleteResult);
    }

    private Map<String, Object> withDocument(Map<String, Object> original, KnowledgeDocument document) {
        Map<String, Object> result = new LinkedHashMap<>(original);
        result.put("document", document);
        result.put("documentId", document.getId());
        return result;
    }

    private Integer extractChunkCount(Map<String, Object> result) {
        Object value = result.get("chunkCount");
        if (!(value instanceof Number)) value = result.get("chunks");
        if (!(value instanceof Number)) value = result.get("count");
        return value instanceof Number n ? n.intValue() : 0;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
