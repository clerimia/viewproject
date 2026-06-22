package com.aicust.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 本地知识文档索引 —— 记录通过管理后台导入到 RAG 的文档元数据。
 */
@Entity
@Table(name = "knowledge_document", indexes = {
        @Index(name = "idx_kdoc_source", columnList = "source_id"),
        @Index(name = "idx_kdoc_status", columnList = "status"),
        @Index(name = "idx_kdoc_category", columnList = "category"),
        @Index(name = "idx_kdoc_created", columnList = "created_at")
})
@Data
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id", nullable = false, length = 128)
    private String sourceId;

    @Column(name = "rag_document_id", length = 128)
    private String ragDocumentId;

    @Column(name = "title", length = 256)
    private String title;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "file_name", length = 256)
    private String fileName;

    @Column(name = "content_type", length = 32)
    private String contentType;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
