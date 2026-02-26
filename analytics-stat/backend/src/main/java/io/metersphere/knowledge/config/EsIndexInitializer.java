package io.metersphere.knowledge.config;

import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

/**
 * ES 索引初始化器
 * 应用启动时自动检查并创建 knowledge_base 索引
 * 从 PaiSmart 迁移，改用 classpath 资源读取方式（兼容 JAR 包部署）
 */
@Component
public class EsIndexInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(EsIndexInitializer.class);

    @Autowired
    private ElasticsearchClient esClient;

    @Value("classpath:es-mappings/knowledge_base.json")
    private org.springframework.core.io.Resource mappingResource;

    @Override
    public void run(String... args) {
        try {
            initializeIndex();
        } catch (Exception e) {
            // ES 连接失败不阻止应用启动，只记录警告
            // 目的是：ES 不可用时，其他功能（SQL查询台、数据字典）仍可正常使用
            logger.warn("Elasticsearch 索引初始化失败，知识库检索功能将不可用: {}", e.getMessage());
            logger.debug("ES 初始化异常详情", e);
        }
    }

    /**
     * 初始化索引的核心逻辑
     */
    private void initializeIndex() throws Exception {
        BooleanResponse existsResponse = esClient.indices()
                .exists(ExistsRequest.of(e -> e.index("knowledge_base")));
        if (!existsResponse.value()) {
            createIndex();
        } else {
            logger.info("ES 索引 'knowledge_base' 已存在");
        }
    }

    /**
     * 创建索引，从 classpath 读取映射 JSON
     */
    private void createIndex() throws Exception {
        // 使用 InputStream 读取，兼容 JAR 包内资源（不能用 getFile()）
        try (InputStream is = mappingResource.getInputStream()) {
            String mappingJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            CreateIndexRequest request = CreateIndexRequest.of(c -> c
                    .index("knowledge_base")
                    .withJson(new StringReader(mappingJson))
            );
            esClient.indices().create(request);
            logger.info("ES 索引 'knowledge_base' 已创建");
        }
    }
}
