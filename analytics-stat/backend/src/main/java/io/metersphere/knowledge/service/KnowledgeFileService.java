package io.metersphere.knowledge.service;

import io.metersphere.base.mapper.KbFileUploadMapper;
import io.metersphere.commons.utils.LogUtil;
import io.metersphere.config.MinioProperties;
import io.metersphere.knowledge.dto.FileProcessingTask;
import io.metersphere.knowledge.dto.FileUploadResponse;
import io.metersphere.knowledge.dto.KbFileUpload;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;

/**
 * 知识库文件服务
 * 负责文件上传到MinIO、数据库记录、发送Kafka消息
 */
@Service
public class KnowledgeFileService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProperties minioProperties;

    @Autowired
    private KbFileUploadMapper fileUploadMapper;

    @Autowired
    private KnowledgeEsService esService;

    @Autowired
    private KafkaTemplate<String, FileProcessingTask> kafkaTemplate;

    @Value("${knowledge.kafka.topic:knowledge-file-processing}")
    private String kafkaTopic;

    /**
     * 上传文件到MinIO并发送Kafka消息进行异步处理
     */
    @Transactional
    public FileUploadResponse uploadFile(MultipartFile file, String userId, String workspaceId, boolean isPublic) {
        try {
            // 1. 计算文件MD5（流式计算，避免大文件占用内存）
            String fileMd5 = calculateMD5Stream(file.getInputStream());
            String fileName = file.getOriginalFilename();

            // 2. 检查文件是否已存在
            KbFileUpload existingFile = fileUploadMapper.selectByMd5(fileMd5);
            if (existingFile != null) {
                return new FileUploadResponse(
                        existingFile.getId(),
                        fileMd5,
                        fileName,
                        "EXISTS",
                        "文件已存在，无需重复上传"
                );
            }

            // 3. 上传到MinIO
            String bucket = minioProperties.getBucket();
            String objectName = "knowledge-base/" + workspaceId + "/" + fileMd5 + "/" + fileName;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            LogUtil.info("文件已上传到MinIO: {}", objectName);

            // 4. 保存数据库记录
            KbFileUpload fileUpload = new KbFileUpload();
            fileUpload.setFileMd5(fileMd5);
            fileUpload.setFileName(fileName);
            fileUpload.setTotalSize(file.getSize());
            fileUpload.setStatus(0); // 0-上传中
            fileUpload.setUserId(userId);
            fileUpload.setWorkspaceId(workspaceId);
            fileUpload.setPublic(isPublic);
            fileUpload.setCreatedAt(LocalDateTime.now());
            fileUpload.setUpdatedAt(LocalDateTime.now());

            fileUploadMapper.insert(fileUpload);

            // 5. 发送Kafka消息进行异步处理
            FileProcessingTask task = new FileProcessingTask();
            task.setFileId(fileUpload.getId());
            task.setFileMd5(fileMd5);
            task.setFileName(fileName);
            task.setStoragePath(objectName);
            task.setUserId(userId);
            task.setWorkspaceId(workspaceId);
            task.setPublic(isPublic);

            kafkaTemplate.send(kafkaTopic, fileMd5, task);

            LogUtil.info("已发送Kafka消息: topic={}, key={}", kafkaTopic, fileMd5);

            return new FileUploadResponse(
                    fileUpload.getId(),
                    fileMd5,
                    fileName,
                    "PROCESSING",
                    "文件上传成功，正在处理中"
            );

        } catch (Exception e) {
            LogUtil.error("文件上传失败", e);
            return new FileUploadResponse(
                    null,
                    null,
                    file.getOriginalFilename(),
                    "ERROR",
                    "文件上传失败: " + e.getMessage()
            );
        }
    }

    /**
     * 流式计算文件MD5，避免大文件占用内存
     */
    private String calculateMD5Stream(InputStream inputStream) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (DigestInputStream dis = new DigestInputStream(inputStream, md)) {
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {
                // 读取流，MD5 自动更新
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 获取文件列表
     */
    public java.util.List<KbFileUpload> listFiles(String userId, String workspaceId) {
        return fileUploadMapper.selectByUserAndWorkspace(userId, workspaceId);
    }

    /**
     * 删除文件
     */
    @Transactional
    public void deleteFile(Long fileId, String userId) {
        KbFileUpload file = fileUploadMapper.selectByPrimaryKey(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }

        // 权限检查
        if (!file.getUserId().equals(userId)) {
            throw new RuntimeException("无权限删除此文件");
        }

        // 删除数据库记录
        fileUploadMapper.deleteByPrimaryKey(fileId);

        // 删除 ES 中的向量索引
        try {
            esService.deleteByFileMd5(file.getFileMd5());
            LogUtil.info("ES索引已删除: fileMd5={}", file.getFileMd5());
        } catch (Exception e) {
            LogUtil.warn("删除ES索引失败（忽略）: fileMd5={}, error={}", file.getFileMd5(), e.getMessage());
        }

        // 删除 MinIO 中的文件
        try {
            String objectName = "knowledge-base/" + file.getWorkspaceId() + "/" + file.getFileMd5() + "/" + file.getFileName();
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectName)
                    .build());
            LogUtil.info("MinIO文件已删除: {}", objectName);
        } catch (Exception e) {
            LogUtil.warn("删除MinIO文件失败（忽略）: fileId={}, error={}", fileId, e.getMessage());
        }

        LogUtil.info("文件已完整删除: fileId={}, fileMd5={}", fileId, file.getFileMd5());
    }
}
