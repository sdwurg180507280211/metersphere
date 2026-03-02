package io.metersphere.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识问答响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChatAskResponse {
    private String answer;
    private List<KnowledgeChatSource> sources;
}
