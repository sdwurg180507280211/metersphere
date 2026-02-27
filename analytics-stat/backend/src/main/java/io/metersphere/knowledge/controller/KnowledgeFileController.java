package io.metersphere.knowledge.controller;

import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.knowledge.dto.FileUploadResponse;
import io.metersphere.knowledge.dto.KbFileUpload;
import io.metersphere.knowledge.service.KnowledgeFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库文件管理 Controller
 * 提供文件上传、列表查询、删除等接口
 */
@RestController
@RequestMapping("/knowledge/file")
public class KnowledgeFileController {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeFileController.class);

    @Autowired
    private KnowledgeFileService fileService;

    /**
     * 上传文件到知识库
     *
     * @param file 上传的文件
     * @param workspaceId 工作空间ID（可选，默认使用当前工作空间）
     * @param isPublic 是否公开（默认false）
     * @return 上传结果
     */
    @PostMapping("/upload")
    public FileUploadResponse uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "workspaceId", required = false) String workspaceId,
            @RequestParam(value = "isPublic", defaultValue = "false") boolean isPublic) {

        String userId = SessionUtils.getUserId();
        if (workspaceId == null || workspaceId.isEmpty()) {
            workspaceId = SessionUtils.getCurrentWorkspaceId();
        }

        logger.info("知识库文件上传 - 用户: {}, 工作空间: {}, 文件: {}, 大小: {}, 公开: {}",
                userId, workspaceId, file.getOriginalFilename(), file.getSize(), isPublic);

        return fileService.uploadFile(file, userId, workspaceId, isPublic);
    }

    /**
     * 获取文件列表
     *
     * @param workspaceId 工作空间ID（可选）
     * @return 文件列表
     */
    @GetMapping("/list")
    public List<KbFileUpload> listFiles(
            @RequestParam(value = "workspaceId", required = false) String workspaceId) {

        String userId = SessionUtils.getUserId();
        if (workspaceId == null || workspaceId.isEmpty()) {
            workspaceId = SessionUtils.getCurrentWorkspaceId();
        }

        logger.info("查询知识库文件列表 - 用户: {}, 工作空间: {}", userId, workspaceId);

        return fileService.listFiles(userId, workspaceId);
    }

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     */
    @DeleteMapping("/{fileId}")
    public void deleteFile(@PathVariable Long fileId) {
        String userId = SessionUtils.getUserId();

        logger.info("删除知识库文件 - 用户: {}, 文件ID: {}", userId, fileId);

        fileService.deleteFile(fileId, userId);
    }
}
