package io.metersphere.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Elasticsearch 存储的文档实体类
 * 包含文档内容和权限信息，对应 ES 索引 knowledge_base 的文档结构
 *
 * 注意：isPublic 字段使用 @JsonProperty 显式指定 JSON 属性名为 "isPublic"，
 * 因为 Lombok @Data 生成的 getter 是 isPublic()，Jackson 默认会将其映射为 "public"，
 * 而 ES 中存储的字段名是 "isPublic"，导致反序列化时 UnrecognizedPropertyException。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EsDocument {

    private String id;             // 文档唯一标识（UUID）
    private String fileMd5;        // 文件指纹
    private Integer chunkId;       // 文本分块序号
    private String textContent;    // 文本内容
    private float[] vector;        // 向量数据（2048维）
    private String modelVersion;   // 向量生成模型版本
    private String userId;         // 上传用户ID（MeterSphere 用户ID）
    private String orgTag;         // 组织标签（映射为 workspaceId）
    @JsonProperty("isPublic")
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
