package io.metersphere.knowledge.dto;

import lombok.Data;

/**
 * 文件上传请求 DTO
 */
@Data
public class FileUploadRequest {
    private String fileName;
    private String workspaceId;
    private boolean isPublic;
}
