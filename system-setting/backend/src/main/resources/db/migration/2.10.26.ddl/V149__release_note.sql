-- 需求上线内容管理表
-- 用于存储每次需求上线的记录信息，支持后台录入、前台展示
CREATE TABLE IF NOT EXISTS `release_note` (
    `id`           VARCHAR(50)  NOT NULL COMMENT '主键ID（UUID）',
    `title`        VARCHAR(100) NOT NULL COMMENT '上线标题',
    `content`      TEXT         NOT NULL COMMENT '内容详情',
    `creator`      VARCHAR(50)  NOT NULL COMMENT '创建人ID',
    `create_time`  BIGINT       NOT NULL COMMENT '创建时间戳',
    `update_time`  BIGINT       NOT NULL COMMENT '更新时间戳',
    PRIMARY KEY (`id`),
    INDEX `idx_create_time` (`create_time` DESC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
