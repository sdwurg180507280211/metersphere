package io.metersphere.knowledge.dto;

import lombok.Data;

/**
 * 知识库文件分片元数据 POJO
 * 对应数据库表 kb_chunk_info
 */
@Data
public class KbChunkInfo {
    private Long id;
    private String fileMd5;
    private int chunkIndex;
    private String chunkMd5;
    private String storagePath;
    private String userId;
}
