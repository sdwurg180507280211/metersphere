package io.metersphere.knowledge.dto;

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
    private boolean isPublic;
}
