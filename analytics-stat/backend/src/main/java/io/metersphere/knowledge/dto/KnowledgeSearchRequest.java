package io.metersphere.knowledge.dto;

import lombok.Data;

/**
 * 知识库检索请求 DTO
 */
@Data
public class KnowledgeSearchRequest {
    /** 搜索关键词 */
    private String query;
    /** 返回的前 K 个结果，默认10 */
    private int topK = 10;
}
