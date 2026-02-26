-- 知识库检索功能建表脚本
-- 创建时间: 2026-02-26
-- 说明: 从 PaiSmart 迁移知识库检索功能到 analytics-stat 模块
--       表名加 kb_ 前缀避免与 MeterSphere 现有表冲突

SET SESSION innodb_lock_wait_timeout = 7200;

-- 文件上传记录表
CREATE TABLE IF NOT EXISTS kb_file_upload (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    file_md5    VARCHAR(32)  NOT NULL COMMENT '文件MD5指纹',
    file_name   VARCHAR(500) NOT NULL COMMENT '原始文件名',
    total_size  BIGINT       NOT NULL DEFAULT 0 COMMENT '文件总大小（字节）',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-上传中 1-已完成 2-解析中 3-已入库',
    user_id     VARCHAR(64)  NOT NULL COMMENT '上传用户ID（MeterSphere用户ID）',
    workspace_id VARCHAR(64) DEFAULT NULL COMMENT '工作空间ID（映射PaiSmart的orgTag）',
    is_public   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否公开: 0-否 1-是',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_file_md5 (file_md5),
    INDEX idx_user_id (user_id),
    INDEX idx_workspace_id (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库-文件上传记录';

-- 文本分块存储表
CREATE TABLE IF NOT EXISTS kb_document_vectors (
    vector_id    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    file_md5     VARCHAR(32)  NOT NULL COMMENT '文件MD5指纹',
    chunk_id     INT          NOT NULL COMMENT '分块序号',
    text_content LONGTEXT              COMMENT '文本内容',
    model_version VARCHAR(50) DEFAULT NULL COMMENT '向量模型版本',
    user_id      VARCHAR(64)  NOT NULL COMMENT '上传用户ID',
    workspace_id VARCHAR(64)  DEFAULT NULL COMMENT '工作空间ID',
    is_public    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否公开',
    INDEX idx_file_md5 (file_md5),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库-文本分块';

-- 文件分块元数据表（用于分片上传）
CREATE TABLE IF NOT EXISTS kb_chunk_info (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    file_md5     VARCHAR(32)  NOT NULL COMMENT '文件MD5指纹',
    chunk_index  INT          NOT NULL COMMENT '分片索引',
    chunk_md5    VARCHAR(32)  DEFAULT NULL COMMENT '分片MD5',
    storage_path VARCHAR(500) DEFAULT NULL COMMENT '分片存储路径',
    user_id      VARCHAR(64)  NOT NULL COMMENT '上传用户ID',
    INDEX idx_file_md5 (file_md5),
    INDEX idx_file_user (file_md5, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识库-文件分片元数据';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
