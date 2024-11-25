package com.document.web.dto;

import com.document.web.model.FileVersion;
import lombok.Data;
import java.util.Date;

@Data
public class FileVersionDTO {
    private Long id;
    private String version;
    private String path;
    private Long fileSize;
    private String changeLog;
    private Date createdAt;

    public static FileVersionDTO fromEntity(FileVersion version) {
        FileVersionDTO dto = new FileVersionDTO();
        dto.setId(version.getId());
        dto.setVersion(version.getVersion());
        dto.setPath(version.getPath());
        dto.setFileSize(version.getFileSize());
        dto.setChangeLog(version.getChangeLog());
        dto.setCreatedAt(version.getCreatedAt());
        return dto;
    }
}