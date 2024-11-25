package com.document.web.service;

import com.document.web.exception.FileStorageException;
import com.document.web.model.Document;
import com.document.web.model.FileVersion;
import com.document.web.repository.DocumentRepository;
import com.document.web.repository.FileVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private Path fileStoragePath;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private FileVersionRepository fileVersionRepository;

    @Autowired
    private TextWatermarkService watermarkService;

    @Autowired
    private EncryptionService encryptionService;

    private static final long MAX_FILE_SIZE = 200 * 1024 * 1024; // 200MB

    @PostConstruct
    public void init() {
        try {
            if (uploadDir == null || uploadDir.isEmpty()) {
                throw new FileStorageException("Upload directory path cannot be empty");
            }
            fileStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(fileStoragePath)) {
                Files.createDirectories(fileStoragePath);
            }
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory!", e);
        }
    }

    @PostConstruct
    public void cleanupTempFiles() {
        try {
            if (fileStoragePath != null && Files.exists(fileStoragePath)) {
                Files.list(fileStoragePath)
                        .filter(path -> path.getFileName().toString().startsWith("temp_"))
                        .filter(path -> {
                            try {
                                return Files.getLastModifiedTime(path).toMillis() <
                                        System.currentTimeMillis() - 3600000;
                            } catch (IOException e) {
                                return false;
                            }
                        })
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                System.err.println("Failed to delete temporary file: " + e.getMessage());
                            }
                        });
            }
        } catch (IOException e) {
            System.err.println("Failed to cleanup temporary files: " + e.getMessage());
        }
    }

    @Transactional
    public Document saveFileWithVersion(MultipartFile file, String description,
                                        boolean addWatermark, boolean preventScreenCapture) throws Exception {
        validateFile(file);

        // Create new document
        Document document = new Document();
        document.setName(StringUtils.cleanPath(file.getOriginalFilename()));
        document.setContentType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setDescription(description);
        document.setHasWatermark(addWatermark);
        document.setPreventScreenCapture(preventScreenCapture);
        document.setStatus("Active");
        document.setCurrentVersion("1.0");

        // Generate encryption key
        var key = encryptionService.generateKey();
        document.setEncryptedKey(encryptionService.keyToString(key));

        // Process and save file
        byte[] content = file.getBytes();
        if (addWatermark && isTextFile(file)) {
            content = watermarkService.createWatermarkedFile(content, "CONFIDENTIAL");
        }

        // Save file and create initial version
        String filePath = saveFile(content, file.getOriginalFilename());
        document.setPath(filePath);

        FileVersion version = new FileVersion();
        version.setDocument(document);
        version.setVersion("1.0");
        version.setPath(filePath);
        version.setFileSize(file.getSize());
        version.setEncryptedKey(document.getEncryptedKey());

        document.getVersions().add(version);
        return documentRepository.save(document);
    }

    @Transactional
    public FileVersion createNewVersion(Document document, MultipartFile file, String changeLog) throws Exception {
        validateFile(file);

        // Increment version number
        String newVersion = incrementVersion(document.getCurrentVersion());

        // Generate new encryption key
        var key = encryptionService.generateKey();
        String encryptedKey = encryptionService.keyToString(key);

        // Process file content
        byte[] content = file.getBytes();
        if (document.isHasWatermark() && isTextFile(file)) {
            content = watermarkService.createWatermarkedFile(content, "CONFIDENTIAL");
        }

        // Save new version file
        String filePath = saveFile(content, file.getOriginalFilename());

        // Update document
        document.setCurrentVersion(newVersion);
        document.setPath(filePath);
        document.setEncryptedKey(encryptedKey);
        document.setFileSize(file.getSize());

        // Create new version
        FileVersion version = new FileVersion();
        version.setDocument(document);
        version.setVersion(newVersion);
        version.setPath(filePath);
        version.setFileSize(file.getSize());
        version.setEncryptedKey(encryptedKey);
        version.setChangeLog(changeLog);

        documentRepository.save(document);
        return fileVersionRepository.save(version);
    }

    public String saveFile(byte[] content, String originalFilename) throws Exception {
        String uniqueFilename = generateUniqueFilename(originalFilename);
        Path targetLocation = fileStoragePath.resolve(uniqueFilename);

        // For text files, encrypt the content
        if (originalFilename.toLowerCase().endsWith(".txt")) {
            var key = encryptionService.generateKey();
            String encryptedContent = encryptionService.encryptToBase64(content, key);
            Files.write(targetLocation, encryptedContent.getBytes());
            return targetLocation.toString() + ".encrypted";
        } else {
            Files.write(targetLocation, content);
            return targetLocation.toString();
        }
    }

    public void decryptFile(String encryptedPath, String outputPath, String keyString) throws Exception {
        Path inputPath = Paths.get(encryptedPath);
        if (!Files.exists(inputPath)) {
            throw new FileStorageException("Encrypted file not found: " + encryptedPath);
        }

        String encryptedContent = new String(Files.readAllBytes(inputPath));
        var key = encryptionService.stringToKey(keyString);
        byte[] decryptedBytes = encryptionService.decryptFromBase64(encryptedContent, key);

        Path outputFilePath = Paths.get(outputPath);
        Files.write(outputFilePath, decryptedBytes);

        // Schedule temporary file deletion
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.deleteIfExists(outputFilePath);
            } catch (IOException e) {
                System.err.println("Failed to delete temporary file: " + e.getMessage());
            }
        }));
    }

    public String getDecryptedContent(String encryptedPath, String encryptedKey) throws Exception {
        if (encryptedPath == null || encryptedKey == null) {
            throw new IllegalArgumentException("Path and key cannot be null");
        }

        Path inputPath = Paths.get(encryptedPath);
        if (!Files.exists(inputPath)) {
            throw new FileNotFoundException("File not found: " + encryptedPath);
        }

        Path tempPath = null;
        try {
            // Create temporary file
            tempPath = Files.createTempFile("decrypted_", ".txt");

            // Decrypt file
            decryptFile(encryptedPath, tempPath.toString(), encryptedKey);

            // Read content
            String content = new String(Files.readAllBytes(tempPath));

            return content;

        } finally {
            // Clean up temp file
            if (tempPath != null) {
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException e) {
                    System.err.println("Failed to delete temp file: " + e.getMessage());
                }
            }
        }
    }
    
    // Helper methods
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Cannot store empty file");
        }

        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        if (filename.contains("..")) {
            throw new FileStorageException("Invalid file path: " + filename);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("File size exceeds maximum limit of 200MB");
        }
    }

    private String generateUniqueFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + extension;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private boolean isTextFile(MultipartFile file) {
        return file.getContentType() != null &&
                file.getContentType().equals("text/plain");
    }

    private String incrementVersion(String currentVersion) {
        String[] parts = currentVersion.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        return major + "." + (minor + 1);
    }

    // Public methods for document operations
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    public Optional<Document> getDocument(Long id) {
        return documentRepository.findById(id);
    }

    public List<FileVersion> getDocumentVersions(Long documentId) {
        return fileVersionRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
    }

    public void deleteDocument(Long id) throws IOException {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new FileStorageException("Document not found"));

        // Delete all version files
        for (FileVersion version : document.getVersions()) {
            Files.deleteIfExists(Paths.get(version.getPath()));
        }

        // Delete main file
        Files.deleteIfExists(Paths.get(document.getPath()));

        // Delete from database
        documentRepository.delete(document);
    }

    public List<Document> searchDocuments(String searchTerm) {
        return documentRepository.findByNameContainingIgnoreCase(searchTerm);
    }
}