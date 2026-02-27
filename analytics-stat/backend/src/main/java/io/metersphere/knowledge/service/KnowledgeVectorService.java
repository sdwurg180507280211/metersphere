package io.metersphere.knowledge.service;

import io.metersphere.base.mapper.KbDocumentVectorMapper;
import io.metersphere.knowledge.client.EmbeddingClient;
import io.metersphere.knowledge.dto.EsDocument;
import io.metersphere.knowledge.dto.KbDocumentVector;
import io.metersphere.knowledge.dto.TextChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * 知识库向量化服务
 * 从 PaiSmart VectorizationService 迁移，适配 MeterSphere MyBatis 体系
 *
 * 流程：
 * 1. 从 kb_document_vectors 表读取文件的文本分块
 * 2. 调用 Embedding API 生成向量
 * 3. 构建 EsDocument 批量写入 ES
 */
@Service
public class KnowledgeVectorService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeVectorService.class);

    @Autowired
    private EmbeddingClient embeddingClient;

    @Autowired
    private KnowledgeEsService esService;

    @Autowired
    private KbDocumentVectorMapper documentVectorMapper;

    /**
     * 对指定文件执行向量化
     *
     * @param fileMd5     文件 MD5 指纹
     * @param userId      上传用户 ID
     * @param workspaceId 工作空间 ID（映射 PaiSmart 的 orgTag）
     * @param isPublic    是否公开
     */
    public void vectorize(String fileMd5, String userId, String workspaceId, boolean isPublic) {
        logger.info("开始向量化文件，fileMd5: {}, userId: {}, workspaceId: {}", fileMd5, userId, workspaceId);

        // 1. 从数据库获取文本分块
        List<KbDocumentVector> vectors = documentVectorMapper.selectByFileMd5(fileMd5);
        if (vectors == null || vectors.isEmpty()) {
            logger.warn("未找到分块内容，fileMd5: {}", fileMd5);
            return;
        }

        List<TextChunk> chunks = vectors.stream()
                .map(v -> new TextChunk(v.getChunkId(), v.getTextContent()))
                .toList();

        // 2. 提取文本内容，调用 Embedding API 生成向量
        List<String> texts = chunks.stream().map(TextChunk::getContent).toList();
        List<float[]> embeddingVectors = embeddingClient.embed(texts);

        if (embeddingVectors == null || embeddingVectors.size() != chunks.size()) {
            logger.error("向量生成结果数量不匹配，期望: {}, 实际: {}",
                    chunks.size(), embeddingVectors == null ? 0 : embeddingVectors.size());
            throw new RuntimeException("向量生成失败：结果数量不匹配");
        }

        // 3. 构建 ES 文档并批量索引
        List<EsDocument> esDocuments = IntStream.range(0, chunks.size())
                .mapToObj(i -> new EsDocument(
                        UUID.randomUUID().toString(),
                        fileMd5,
                        chunks.get(i).getChunkId(),
                        chunks.get(i).getContent(),
                        embeddingVectors.get(i),
                        "text-embedding-v4",
                        userId,
                        workspaceId,  // 映射为 ES 的 orgTag 字段
                        isPublic
                ))
                .toList();

        esService.bulkIndex(esDocuments);
        logger.info("向量化完成，fileMd5: {}, 文档数: {}", fileMd5, esDocuments.size());
    }
}
