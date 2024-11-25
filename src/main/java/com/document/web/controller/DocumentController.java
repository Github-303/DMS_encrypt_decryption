// DocumentController.java
package com.document.web.controller;

import com.document.web.dto.DocumentDTO;
import com.document.web.dto.FileVersionDTO;
import com.document.web.model.Document;
import com.document.web.model.FileVersion;
import com.document.web.service.FileService;
import com.document.web.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private FileService fileService;

    @Autowired
    private DocumentRepository documentRepository;

    @GetMapping
    public ResponseEntity<List<DocumentDTO>> getAllDocuments() {
        List<Document> documents = documentRepository.findAll();
        List<DocumentDTO> dtos = documents.stream()
                .map(DocumentDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getDocument(@PathVariable Long id) {
        return documentRepository.findById(id)
                .map(DocumentDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(defaultValue = "false") boolean addWatermark,
            @RequestParam(defaultValue = "false") boolean preventScreenCapture) {
        try {
            Document document = fileService.saveFileWithVersion(file, description, addWatermark, preventScreenCapture);
            return ResponseEntity.ok(DocumentDTO.fromEntity(document));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{documentId}/versions")
    public ResponseEntity<?> createNewVersion(
            @PathVariable Long documentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String changeLog) {
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            FileVersion version = fileService.createNewVersion(document, file, changeLog);
            return ResponseEntity.ok(FileVersionDTO.fromEntity(version));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{documentId}/versions")
    public ResponseEntity<?> getDocumentVersions(@PathVariable Long documentId) {
        try {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            List<FileVersionDTO> versions = document.getVersions().stream()
                    .map(FileVersionDTO::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(versions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        try {
            fileService.deleteDocument(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<DocumentDTO>> searchDocuments(
            @RequestParam(required = false) String term) {
        try {
            List<Document> documents;
            if (term == null || term.trim().isEmpty()) {
                documents = documentRepository.findAll();
            } else {
                documents = documentRepository.findByNameContainingIgnoreCase(term.trim());
            }

            List<DocumentDTO> dtos = documents.stream()
                    .map(DocumentDTO::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}