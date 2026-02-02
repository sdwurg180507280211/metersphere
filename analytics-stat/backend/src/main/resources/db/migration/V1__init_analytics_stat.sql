-- 分析统计微服务初始化脚本
-- 创建时间: 2026-01-30

-- 创建SQL查询台历史记录表
CREATE TABLE IF NOT EXISTS `sql_query_history` (
    `id` VARCHAR(50) NOT NULL COMMENT '主键ID',
    `user_id` VARCHAR(50) NOT NULL COMMENT '用户ID',
    `query_sql` TEXT NOT NULL COMMENT '查询SQL',
    `query_time` BIGINT NOT NULL COMMENT '查询时间(ms)',
    `result_count` INT DEFAULT 0 COMMENT '结果行数',
    `status` VARCHAR(20) NOT NULL COMMENT '执行状态: SUCCESS/FAILED',
    `error_message` TEXT COMMENT '错误信息',
    `create_time` BIGINT NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='SQL查询历史记录表';

-- 创建数据字典表
CREATE TABLE IF NOT EXISTS `data_dictionary` (
    `id` VARCHAR(50) NOT NULL COMMENT '主键ID',
    `dict_type` VARCHAR(100) NOT NULL COMMENT '字典类型',
    `dict_code` VARCHAR(100) NOT NULL COMMENT '字典编码',
    `dict_value` VARCHAR(500) NOT NULL COMMENT '字典值',
    `dict_label` VARCHAR(200) NOT NULL COMMENT '字典标签',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` BIGINT NOT NULL COMMENT '创建时间',
    `update_time` BIGINT NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_code` (`dict_type`, `dict_code`),
    INDEX `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据字典表';
