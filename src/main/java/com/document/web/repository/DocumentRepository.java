package com.document.web.repository;

import com.document.web.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByNameContainingIgnoreCase(String name);
    List<Document> findByContentType(String contentType);
    boolean existsByPath(String path);
    List<Document> findByStatusOrderByCreatedAtDesc(String status);
    List<Document> findByHasWatermark(boolean hasWatermark);

}