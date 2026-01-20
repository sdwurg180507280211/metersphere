SET SESSION innodb_lock_wait_timeout = 7200;

-- 创建缺陷变更审计表
CREATE TABLE IF NOT EXISTS `issue_change_log` (
    `id` VARCHAR(64) NOT NULL COMMENT '主键ID',
    `issue_id` VARCHAR(64) NOT NULL COMMENT '缺陷ID',
    `project_id` VARCHAR(64) DEFAULT NULL COMMENT '项目ID',
    `operator` VARCHAR(64) NOT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
    `source` VARCHAR(50) DEFAULT NULL COMMENT '来源：update/status/other',
    `create_time` BIGINT(13) NOT NULL COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_issue_id` (`issue_id`),
    KEY `idx_project_id` (`project_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE utf8mb4_general_ci COMMENT = '缺陷变更审计主表';

-- 创建缺陷变更审计明细表（一条变更记录可包含多字段变更）
CREATE TABLE IF NOT EXISTS `issue_change_log_detail` (
    `id` VARCHAR(64) NOT NULL COMMENT '主键ID',
    `log_id` VARCHAR(64) NOT NULL COMMENT '主表ID',
    `field_type` VARCHAR(20) NOT NULL COMMENT '字段类型：system/custom',
    `field_key` VARCHAR(100) DEFAULT NULL COMMENT '系统字段key，例如 title/description/status',
    `field_id` VARCHAR(64) DEFAULT NULL COMMENT '自定义字段ID',
    `field_name` VARCHAR(255) DEFAULT NULL COMMENT '字段名称（冗余存储，便于历史展示）',
    `old_value` TEXT DEFAULT NULL COMMENT '原值',
    `new_value` TEXT DEFAULT NULL COMMENT '新值',
    `create_time` BIGINT(13) NOT NULL COMMENT '操作时间（冗余，便于查询）',
    PRIMARY KEY (`id`),
    KEY `idx_log_id` (`log_id`),
    KEY `idx_field_id` (`field_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE utf8mb4_general_ci COMMENT = '缺陷变更审计明细表';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
