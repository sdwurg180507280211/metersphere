package io.metersphere.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识问答状态响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChatStatusResponse {
    private boolean llmEnabled;
}
