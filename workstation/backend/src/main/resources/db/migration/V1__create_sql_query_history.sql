CREATE TABLE IF NOT EXISTS `workstation_sql_query_history`
(
    `id`          varchar(50)  NOT NULL COMMENT 'SQL query history ID',
    `user_id`     varchar(50)  NOT NULL COMMENT 'User ID',
    `sql_content` longtext     NOT NULL COMMENT 'SQL content',
    `title`       varchar(120) NOT NULL COMMENT 'SQL title',
    `description` varchar(500) DEFAULT NULL COMMENT 'SQL description',
    `saved`       tinyint(1)   NOT NULL DEFAULT 0 COMMENT 'Saved flag',
    `create_time` bigint       NOT NULL COMMENT 'Create time',
    `update_time` bigint       DEFAULT NULL COMMENT 'Update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sql_query_history_user_title` (`user_id`, `title`),
    KEY `idx_sql_query_history_user_create_time` (`user_id`, `create_time`),
    KEY `idx_sql_query_history_user_saved_update_time` (`user_id`, `saved`, `update_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='Workstation SQL query history';
