package com.document.web.dto;

import com.document.web.model.Document;
import lombok.Data;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class DocumentDTO {
    private Long id;
    private String name;
    private String path;
    private String contentType;
    private Long fileSize;
    private String currentVersion;
    private boolean hasWatermark;
    private boolean preventScreenCapture;
    private String description;
    private String status;
    private Date createdAt;
    private Date updatedAt;
    private List<FileVersionDTO> versions;

    public static DocumentDTO fromEntity(Document document) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(document.getId());
        dto.setName(document.getName());
        dto.setPath(document.getPath());
        dto.setContentType(document.getContentType());
        dto.setFileSize(document.getFileSize());
        dto.setCurrentVersion(document.getCurrentVersion());
        dto.setHasWatermark(document.isHasWatermark());
        dto.setPreventScreenCapture(document.isPreventScreenCapture());
        dto.setDescription(document.getDescription());
        dto.setStatus(document.getStatus());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setUpdatedAt(document.getUpdatedAt());

        if (document.getVersions() != null) {
            dto.setVersions(document.getVersions().stream()
                    .map(FileVersionDTO::fromEntity)
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}