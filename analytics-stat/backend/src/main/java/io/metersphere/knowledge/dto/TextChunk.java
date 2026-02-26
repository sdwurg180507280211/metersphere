package io.metersphere.knowledge.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 文件分块内容实体类
 */
@Setter
@Getter
public class TextChunk {
    private int chunkId;    // 分块序号
    private String content; // 分块内容

    public TextChunk(int chunkId, String content) {
        this.chunkId = chunkId;
        this.content = content;
    }
}
