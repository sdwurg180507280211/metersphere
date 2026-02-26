package io.metersphere.knowledge.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库文件上传记录 POJO
 * 对应数据库表 kb_file_upload
 * 注意：MeterSphere 使用 MyBatis，不使用 JPA 注解
 */
@Data
public class KbFileUpload {
    private Long id;
    private String fileMd5;
    private String fileName;
    private long totalSize;
    private int status;          // 0-上传中 1-已完成 2-解析中 3-已入库
    private String userId;
    private String workspaceId;  // 映射 PaiSmart 的 orgTag
    private boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
