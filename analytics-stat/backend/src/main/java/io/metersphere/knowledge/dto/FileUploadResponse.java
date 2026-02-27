package io.metersphere.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private Long fileId;
    private String fileMd5;
    private String fileName;
    private String status;
    private String message;
}
