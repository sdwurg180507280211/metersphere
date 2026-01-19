SET SESSION innodb_lock_wait_timeout = 7200;
-- 所属系统表创建
CREATE TABLE IF NOT EXISTS associated_system (
    `id`           varchar(50) NOT NULL COMMENT 'Associated System ID',
    `workspace_id` varchar(50) NOT NULL COMMENT 'Workspace ID this Associated System belongs to',

    `name`         varchar(64) NOT NULL COMMENT 'Associated System name',
    `description`  varchar(255) DEFAULT NULL COMMENT '系统编码',
    `create_time`  bigint(13)  NOT NULL COMMENT 'Create timestamp',
    `update_time`  bigint(13)  NOT NULL COMMENT 'Update timestamp',
    PRIMARY KEY (`id`)
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE utf8mb4_general_ci;
SET SESSION innodb_lock_wait_timeout = DEFAULT;