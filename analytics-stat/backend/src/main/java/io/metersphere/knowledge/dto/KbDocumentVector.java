package io.metersphere.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 知识库文本分块 POJO
 * 对应数据库表 kb_document_vectors
 */
@Data
public class KbDocumentVector {
    private Long vectorId;
    private String fileMd5;
    private Integer chunkId;
    private String textContent;
    private String modelVersion;
    private String userId;
    private String workspaceId;
    
    /**
     * 是否公开
     * 使用 @JsonProperty 显式指定 JSON 属性名，避免 Lombok + Jackson 命名陷阱
     */
    @JsonProperty("isPublic")
    private boolean isPublic;
}
