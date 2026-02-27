package io.metersphere.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件处理任务，用于 Kafka 消息传递
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileProcessingTask {
    private Long fileId;
    private String fileMd5;
    private String storagePath;  // MinIO存储路径
    private String fileName;
    private String userId;
    private String workspaceId; // 映射 PaiSmart 的 orgTag
    
    /**
     * 是否公开
     * Kafka 消息会被 JSON 序列化，需要 @JsonProperty 避免字段名不一致
     */
    @JsonProperty("isPublic")
    private boolean isPublic;
}
