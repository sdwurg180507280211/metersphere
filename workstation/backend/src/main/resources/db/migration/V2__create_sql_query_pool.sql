CREATE TABLE IF NOT EXISTS `workstation_sql_query_pool` (
    `id` varchar(50) NOT NULL COMMENT 'SQL pool ID',
    `sql_content` longtext NOT NULL COMMENT 'SQL content',
    `title` varchar(120) NOT NULL COMMENT 'SQL title',
    `summary` varchar(200) NOT NULL COMMENT 'SQL summary',
    `description` varchar(500) DEFAULT NULL COMMENT 'SQL description',
    `create_user` varchar(50) NOT NULL COMMENT 'Create user ID',
    `update_user` varchar(50) DEFAULT NULL COMMENT 'Update user ID',
    `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Enabled flag',
    `use_count` bigint NOT NULL DEFAULT 0 COMMENT 'Use count',
    `create_time` bigint NOT NULL COMMENT 'Create time',
    `update_time` bigint DEFAULT NULL COMMENT 'Update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sql_query_pool_title` (`title`),
    KEY `idx_sql_query_pool_enabled_update_time` (`enabled`, `update_time`),
    KEY `idx_sql_query_pool_create_user_update_time` (`create_user`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Workstation public SQL query pool';
