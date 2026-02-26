package io.metersphere.knowledge.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import io.metersphere.knowledge.dto.EsDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Elasticsearch 操作封装服务
 * 提供批量索引和按文件删除功能
 * 从 PaiSmart ElasticsearchService 迁移
 */
@Service
public class KnowledgeEsService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeEsService.class);

    @Autowired
    private ElasticsearchClient esClient;

    /**
     * 批量索引文档到 ES 的 knowledge_base 索引
     * @param documents 文档列表
     */
    public void bulkIndex(List<EsDocument> documents) {
        try {
            logger.info("开始批量索引文档到 ES，文档数量: {}", documents.size());

            List<BulkOperation> bulkOperations = documents.stream()
                    .map(doc -> BulkOperation.of(op -> op.index(idx -> idx
                            .index("knowledge_base")
                            .id(doc.getId())
                            .document(doc)
                    )))
                    .toList();

            BulkRequest request = BulkRequest.of(b -> b.operations(bulkOperations));
            BulkResponse response = esClient.bulk(request);

            if (response.errors()) {
                for (BulkResponseItem item : response.items()) {
                    if (item.error() != null) {
                        logger.error("文档索引失败 - ID: {}, 错误: {}", item.id(), item.error().reason());
                    }
                }
                throw new RuntimeException("批量索引部分失败，请检查日志");
            }
            logger.info("批量索引成功完成，文档数量: {}", documents.size());
        } catch (Exception e) {
            logger.error("批量索引失败，文档数量: {}", documents.size(), e);
            throw new RuntimeException("批量索引失败", e);
        }
    }

    /**
     * 根据 fileMd5 删除 ES 中的文档
     * @param fileMd5 文件指纹
     */
    public void deleteByFileMd5(String fileMd5) {
        try {
            DeleteByQueryRequest request = DeleteByQueryRequest.of(d -> d
                    .index("knowledge_base")
                    .query(q -> q.term(t -> t.field("fileMd5").value(fileMd5)))
            );
            esClient.deleteByQuery(request);
            logger.info("已删除 ES 中 fileMd5={} 的文档", fileMd5);
        } catch (Exception e) {
            logger.error("删除 ES 文档失败, fileMd5={}", fileMd5, e);
            throw new RuntimeException("删除文档失败", e);
        }
    }
}
