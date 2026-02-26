package io.metersphere.knowledge.dto;

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
    private String fileMd5;
    private String filePath;
    private String fileName;
    private String userId;
    private String workspaceId; // 映射 PaiSmart 的 orgTag
    private boolean isPublic;
}
