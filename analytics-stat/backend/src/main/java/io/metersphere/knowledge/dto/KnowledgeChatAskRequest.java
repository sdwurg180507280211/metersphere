package io.metersphere.knowledge.dto;

import lombok.Data;

/**
 * 知识问答请求 DTO
 */
@Data
public class KnowledgeChatAskRequest {
    /** 用户问题 */
    private String question;

    /** 召回条数，默认 5 */
    private Integer topK = 5;
}
