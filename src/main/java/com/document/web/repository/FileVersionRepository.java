package com.document.web.repository;

import com.document.web.model.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    List<FileVersion> findByDocumentId(Long documentId);
    List<FileVersion> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
    Optional<FileVersion> findTopByDocumentIdOrderByVersionDesc(Long documentId);
    List<FileVersion> findByDocumentIdAndVersionContaining(Long documentId, String version);
}