package com.aicust.repository;

import com.aicust.model.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    @Query("SELECT d FROM KnowledgeDocument d " +
           "WHERE d.status = 'ACTIVE' " +
           "AND (:keyword IS NULL OR :keyword = '' " +
           "OR LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(d.category) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(d.sourceId) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY d.createdAt DESC")
    Page<KnowledgeDocument> searchActive(@Param("keyword") String keyword, Pageable pageable);
}
