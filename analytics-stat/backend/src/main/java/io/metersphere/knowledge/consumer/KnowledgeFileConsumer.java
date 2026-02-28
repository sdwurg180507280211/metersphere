package io.metersphere.knowledge.consumer;

import io.metersphere.base.mapper.KbFileUploadMapper;
import io.metersphere.commons.utils.LogUtil;
import io.metersphere.config.MinioProperties;
import io.metersphere.knowledge.dto.FileProcessingTask;
import io.metersphere.knowledge.service.KnowledgeParseService;
import io.metersphere.knowledge.service.KnowledgeVectorService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 知识库文件处理 Kafka 消费者
 * 从 PaiSmart FileProcessingConsumer 迁移
 *
 * 处理流程：
 * 1. 从 MinIO 下载文件
 * 2. 使用 Tika 解析文件内容并分块（KnowledgeParseService）
 * 3. 调用 Embedding API 生成向量（KnowledgeVectorService）
 * 4. 将向量和文本索引到 Elasticsearch
 * 5. 更新数据库文件状态
 */
@Component
public class KnowledgeFileConsumer {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProperties minioProperties;

    @Autowired
    private KnowledgeParseService parseService;

    @Autowired
    private KnowledgeVectorService vectorService;

    @Autowired
    private KbFileUploadMapper fileUploadMapper;

    @KafkaListener(
            topics = "${knowledge.kafka.topic:knowledge-file-processing}",
            groupId = "${spring.kafka.consumer.group-id:knowledge-processing-group}",
            containerFactory = "knowledgeKafkaListenerContainerFactory")
    public void processFile(FileProcessingTask task) {
        LogUtil.info("收到文件处理任务: fileId=" + task.getFileId()
                + ", fileMd5=" + task.getFileMd5() + ", fileName=" + task.getFileName());

        try {
            // 1. 更新状态为"解析中"
            fileUploadMapper.updateStatus(task.getFileId(), 2);

            // 2. 从 MinIO 下载文件
            String bucket = minioProperties.getBucket();
            InputStream fileStream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(task.getStoragePath())
                    .build());

            LogUtil.info("开始解析文件: {}", task.getFileName());

            // 3. 解析文件并分块保存到数据库
            parseService.parseAndSave(
                    task.getFileMd5(),
                    fileStream,
                    task.getUserId(),
                    task.getWorkspaceId(),
                    task.isPublic()
            );

            LogUtil.info("文件解析完成，开始向量化: {}", task.getFileName());

            // 4. 向量化并索引到 ES
            vectorService.vectorize(
                    task.getFileMd5(),
                    task.getUserId(),
                    task.getWorkspaceId(),
                    task.isPublic()
            );

            // 5. 更新状态为"已入库"
            fileUploadMapper.updateStatus(task.getFileId(), 3);

            LogUtil.info("文件处理完成: fileId={}, fileMd5={}", task.getFileId(), task.getFileMd5());

        } catch (Throwable t) {
            LogUtil.error("文件处理失败: fileId=" + task.getFileId(), t);

            // 更新状态为"失败"（可以定义状态码 -1 表示失败）
            try {
                fileUploadMapper.updateStatus(task.getFileId(), -1);
            } catch (Exception ex) {
                LogUtil.error("更新文件状态失败", ex);
            }
        }
    }
}
