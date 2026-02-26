package io.metersphere.knowledge.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import io.metersphere.knowledge.client.EmbeddingClient;
import io.metersphere.knowledge.dto.EsDocument;
import io.metersphere.knowledge.dto.KbFileUpload;
import io.metersphere.knowledge.dto.KnowledgeSearchResult;
import io.metersphere.base.mapper.KbFileUploadMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库混合检索服务
 * 实现两阶段混合检索：KNN 向量召回 + BM25 Rescore
 * 从 PaiSmart HybridSearchService 迁移，适配 MeterSphere 认证体系
 *
 * 关键适配：
 * - 去掉 UserRepository/OrgTagCacheService，用户信息由 Controller 层通过 SessionUtils 获取后传入
 * - orgTag 映射为 workspaceId（MeterSphere 的工作空间概念）
 * - FileUploadRepository(JPA) → KbFileUploadMapper(MyBatis)
 */
@Service
public class KnowledgeSearchService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeSearchService.class);

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private EmbeddingClient embeddingClient;

    @Autowired
    private KbFileUploadMapper fileUploadMapper;

    /**
     * 带权限的混合检索
     * 确保用户只能搜索：自己的文档、公开文档、所属工作空间的文档
     *
     * @param query       查询关键词
     * @param userId      当前用户ID（由 Controller 通过 SessionUtils.getUserId() 获取）
     * @param workspaceId 当前工作空间ID（由 Controller 通过 SessionUtils.getCurrentWorkspaceId() 获取）
     * @param topK        返回结果数量
     * @return 检索结果列表
     */
    public List<KnowledgeSearchResult> searchWithPermission(String query, String userId, String workspaceId, int topK) {
        logger.debug("开始带权限搜索，查询: {}, 用户ID: {}, 工作空间: {}", query, userId, workspaceId);

        try {
            // 生成查询向量
            final List<Float> queryVector = embedToVectorList(query);

            // 向量生成失败时降级为纯文本匹配
            if (queryVector == null) {
                logger.warn("向量生成失败，降级为纯 BM25 文本匹配");
                return textOnlySearchWithPermission(query, userId, workspaceId, topK);
            }

            logger.debug("向量生成成功，执行 KNN + BM25 Rescore 混合检索");

            // KNN 召回窗口 = topK * 30，保证召回充分
            int recallK = topK * 30;

            SearchResponse<EsDocument> response = esClient.search(s -> {
                s.index("knowledge_base");

                // 第一阶段：KNN 向量召回
                s.knn(kn -> kn
                        .field("vector")
                        .queryVector(queryVector)
                        .k(recallK)
                        .numCandidates(recallK)
                );

                // 必须命中关键词 + 权限过滤
                s.query(q -> q.bool(b -> b
                        // must: 文本内容必须包含查询关键词
                        .must(mst -> mst.match(m -> m.field("textContent").query(query)))
                        // filter: 权限过滤（不影响评分）
                        .filter(f -> f.bool(bf -> bf
                                // 条件1: 用户自己上传的文档
                                .should(s1 -> s1.term(t -> t.field("userId").value(userId)))
                                // 条件2: 公开文档
                                .should(s2 -> s2.term(t -> t.field("public").value(true)))
                                // 条件3: 同一工作空间的文档（orgTag 在 MeterSphere 中映射为 workspaceId）
                                .should(s3 -> {
                                    if (workspaceId == null || workspaceId.isEmpty()) {
                                        return s3.matchNone(mn -> mn);
                                    }
                                    return s3.term(t -> t.field("orgTag").value(workspaceId));
                                })
                        ))
                ));

                // 第二阶段：BM25 Rescore，用关键词相关性重新排序
                s.rescore(r -> r
                        .windowSize(recallK)
                        .query(rq -> rq
                                .queryWeight(0.2d)        // 保留部分 KNN 分数
                                .rescoreQueryWeight(1.0d)  // BM25 主导最终排序
                                .query(rqq -> rqq.match(m -> m
                                        .field("textContent")
                                        .query(query)
                                        .operator(Operator.And)
                                ))
                        )
                );

                s.size(topK);
                return s;
            }, EsDocument.class);

            logger.debug("ES 查询完成，命中: {}, 最高分: {}",
                    response.hits().total().value(), response.hits().maxScore());

            // 将 ES 命中结果转为 DTO
            List<KnowledgeSearchResult> results = response.hits().hits().stream()
                    .map(hit -> {
                        EsDocument doc = hit.source();
                        assert doc != null;
                        return new KnowledgeSearchResult(
                                doc.getFileMd5(),
                                doc.getChunkId(),
                                doc.getTextContent(),
                                hit.score(),
                                doc.getUserId(),
                                doc.getOrgTag(),
                                doc.isPublic()
                        );
                    })
                    .toList();

            // 补充文件名
            attachFileNames(results);
            return results;

        } catch (Exception e) {
            logger.error("带权限的混合检索失败", e);
            // 降级：尝试纯文本检索
            try {
                logger.info("尝试纯文本检索作为降级方案");
                return textOnlySearchWithPermission(query, userId, workspaceId, topK);
            } catch (Exception fallback) {
                logger.error("降级检索也失败", fallback);
                return Collections.emptyList();
            }
        }
    }

    /**
     * 纯文本匹配的带权限检索（向量生成失败时的降级方案）
     */
    private List<KnowledgeSearchResult> textOnlySearchWithPermission(String query, String userId, String workspaceId, int topK) {
        try {
            logger.debug("执行纯文本带权限检索，用户: {}, 工作空间: {}", userId, workspaceId);

            SearchResponse<EsDocument> response = esClient.search(s -> s
                    .index("knowledge_base")
                    .query(q -> q.bool(b -> b
                            // 文本匹配
                            .must(m -> m.match(ma -> ma.field("textContent").query(query)))
                            // 权限过滤
                            .filter(f -> f.bool(bf -> bf
                                    .should(s1 -> s1.term(t -> t.field("userId").value(userId)))
                                    .should(s2 -> s2.term(t -> t.field("public").value(true)))
                                    .should(s3 -> {
                                        if (workspaceId == null || workspaceId.isEmpty()) {
                                            return s3.matchNone(mn -> mn);
                                        }
                                        return s3.term(t -> t.field("orgTag").value(workspaceId));
                                    })
                            ))
                    ))
                    .minScore(0.3d)
                    .size(topK),
                    EsDocument.class
            );

            List<KnowledgeSearchResult> results = response.hits().hits().stream()
                    .map(hit -> {
                        EsDocument doc = hit.source();
                        assert doc != null;
                        return new KnowledgeSearchResult(
                                doc.getFileMd5(), doc.getChunkId(), doc.getTextContent(),
                                hit.score(), doc.getUserId(), doc.getOrgTag(), doc.isPublic()
                        );
                    })
                    .toList();

            attachFileNames(results);
            return results;

        } catch (Exception e) {
            logger.error("纯文本带权限检索失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 不带权限过滤的混合检索（保留向后兼容，内部调试用）
     */
    public List<KnowledgeSearchResult> search(String query, int topK) {
        try {
            logger.debug("执行无权限混合检索，查询: {}, topK: {}", query, topK);

            final List<Float> queryVector = embedToVectorList(query);
            if (queryVector == null) {
                return textOnlySearch(query, topK);
            }

            int recallK = topK * 30;

            SearchResponse<EsDocument> response = esClient.search(s -> {
                s.index("knowledge_base");
                s.knn(kn -> kn
                        .field("vector")
                        .queryVector(queryVector)
                        .k(recallK)
                        .numCandidates(recallK)
                );
                s.query(q -> q.match(m -> m.field("textContent").query(query)));
                s.rescore(r -> r
                        .windowSize(recallK)
                        .query(rq -> rq
                                .queryWeight(0.2d)
                                .rescoreQueryWeight(1.0d)
                                .query(rqq -> rqq.match(m -> m
                                        .field("textContent")
                                        .query(query)
                                        .operator(Operator.And)
                                ))
                        )
                );
                s.size(topK);
                return s;
            }, EsDocument.class);

            return response.hits().hits().stream()
                    .map(hit -> {
                        EsDocument doc = hit.source();
                        assert doc != null;
                        return new KnowledgeSearchResult(
                                doc.getFileMd5(), doc.getChunkId(), doc.getTextContent(), hit.score()
                        );
                    })
                    .toList();

        } catch (Exception e) {
            logger.error("无权限混合检索失败", e);
            try {
                return textOnlySearch(query, topK);
            } catch (Exception fallback) {
                logger.error("降级检索也失败", fallback);
                return Collections.emptyList();
            }
        }
    }

    /**
     * 纯文本检索（无权限过滤）
     */
    private List<KnowledgeSearchResult> textOnlySearch(String query, int topK) throws Exception {
        SearchResponse<EsDocument> response = esClient.search(s -> s
                .index("knowledge_base")
                .query(q -> q.match(m -> m.field("textContent").query(query)))
                .size(topK),
                EsDocument.class
        );

        return response.hits().hits().stream()
                .map(hit -> {
                    EsDocument doc = hit.source();
                    assert doc != null;
                    return new KnowledgeSearchResult(
                            doc.getFileMd5(), doc.getChunkId(), doc.getTextContent(), hit.score()
                    );
                })
                .toList();
    }

    /**
     * 将文本转为向量列表（调用 Embedding API）
     * 失败时返回 null，调用方降级为纯文本检索
     */
    private List<Float> embedToVectorList(String text) {
        try {
            List<float[]> vecs = embeddingClient.embed(List.of(text));
            if (vecs == null || vecs.isEmpty()) {
                logger.warn("Embedding API 返回空向量");
                return null;
            }
            float[] raw = vecs.get(0);
            List<Float> list = new ArrayList<>(raw.length);
            for (float v : raw) {
                list.add(v);
            }
            return list;
        } catch (Exception e) {
            logger.error("向量生成失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 为检索结果补充原始文件名
     * 通过 fileMd5 批量查询 kb_file_upload 表获取文件名
     */
    private void attachFileNames(List<KnowledgeSearchResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }
        try {
            // 收集所有唯一的 fileMd5
            List<String> md5List = results.stream()
                    .map(KnowledgeSearchResult::getFileMd5)
                    .distinct()
                    .collect(Collectors.toList());

            // 批量查询文件记录
            List<KbFileUpload> uploads = fileUploadMapper.selectByFileMd5List(md5List);

            // 构建 md5 → fileName 映射
            Map<String, String> md5ToName = uploads.stream()
                    .collect(Collectors.toMap(KbFileUpload::getFileMd5, KbFileUpload::getFileName, (a, b) -> a));

            // 填充文件名
            results.forEach(r -> r.setFileName(md5ToName.get(r.getFileMd5())));
        } catch (Exception e) {
            logger.error("补充文件名失败", e);
        }
    }
}
