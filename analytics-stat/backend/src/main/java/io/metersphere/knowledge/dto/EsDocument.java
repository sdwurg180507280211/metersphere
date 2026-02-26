package io.metersphere.knowledge.dto;

import lombok.Data;

/**
 * Elasticsearch 存储的文档实体类
 * 包含文档内容和权限信息，对应 ES 索引 knowledge_base 的文档结构
 */
@Data
public class EsDocument {

    private String id;             // 文档唯一标识（UUID）
    private String fileMd5;        // 文件指纹
    private Integer chunkId;       // 文本分块序号
    private String textContent;    // 文本内容
    private float[] vector;        // 向量数据（2048维）
    private String modelVersion;   // 向量生成模型版本
    private String userId;         // 上传用户ID（MeterSphere 用户ID）
    private String orgTag;         // 组织标签（映射为 workspaceId）
    private boolean isPublic;      // 是否公开

    public EsDocument() {
    }

    /**
     * 完整构造函数
     */
    public EsDocument(String id, String fileMd5, int chunkId, String content,
                      float[] vector, String modelVersion,
                      String userId, String orgTag, boolean isPublic) {
        this.id = id;
        this.fileMd5 = fileMd5;
        this.chunkId = chunkId;
        this.textContent = content;
        this.vector = vector;
        this.modelVersion = modelVersion;
        this.userId = userId;
        this.orgTag = orgTag;
        this.isPublic = isPublic;
    }
}
