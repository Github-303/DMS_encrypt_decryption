package com.document.web.controller;

import com.document.web.model.Document;
import com.document.web.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/viewer")
public class ViewerController {

    @Autowired
    private FileService fileService;

    @GetMapping("/document/{id}")
    public ResponseEntity<?> viewDocument(@PathVariable Long id) {
        try {
            Optional<Document> documentOptional = fileService.getDocument(id);

            if (!documentOptional.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Document document = documentOptional.get();
            String filePath = document.getPath(); // Đường dẫn từ Document

            System.out.println("Final file path: " + filePath); // Log đường dẫn cuối cùng

            String content = fileService.getDecryptedContent(filePath, document.getEncryptedKey());

            Map<String, Object> response = new HashMap<>();
            response.put("content", content);
            response.put("name", document.getName());
            response.put("preventScreenCapture", document.isPreventScreenCapture());
            response.put("hasWatermark", document.isHasWatermark());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace(); // Logging chi tiết lỗi
            return ResponseEntity.badRequest().body("Error viewing document: " + e.getMessage());
        }
    }

}