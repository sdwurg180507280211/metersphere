package io.metersphere.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识问答引用来源 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChatSource {
    private String fileName;
    private Integer chunkId;
    private String snippet;
    private Double score;
}
