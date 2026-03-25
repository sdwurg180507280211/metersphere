SET SESSION innodb_lock_wait_timeout = 7200;

-- 需求池表，用于存储从需求平台同步过来的需求数据
CREATE TABLE IF NOT EXISTS `requirement_pool` (
    `id` varchar(32) NOT NULL COMMENT '主键',
    `dmp_num` varchar(64) NOT NULL COMMENT '需求编号',
    `requirement_name` varchar(255) DEFAULT NULL COMMENT '需求名称',
    `pool_status` varchar(32) DEFAULT 'PENDING' COMMENT '需求池状态：PENDING(待创建)、CREATED(已创建)、CANCELLED(已取消)',
    `system_name` varchar(255) DEFAULT NULL COMMENT '所属系统',
    `req_manager_name` varchar(64) DEFAULT NULL COMMENT '需求负责人',
    `req_father_class` varchar(255) DEFAULT NULL COMMENT '需求父类',
    `req_son_class` varchar(255) DEFAULT NULL COMMENT '需求子类',
    `create_time` bigint(20) DEFAULT NULL COMMENT '需求创建时间',
    `up_time` bigint(20) DEFAULT NULL COMMENT '需求更新时间',
    `linked_plan_id` varchar(32) DEFAULT NULL COMMENT '关联的测试计划ID',
    `linked_plan_name` varchar(255) DEFAULT NULL COMMENT '关联的测试计划名称',
    `trace_id` varchar(64) DEFAULT NULL COMMENT '追踪ID，用于链路追踪',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dmp_num` (`dmp_num`) COMMENT '需求编号唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求池表';

-- 为常用查询字段创建索引
CREATE INDEX `idx_pool_status` ON `requirement_pool` (`pool_status`);
CREATE INDEX `idx_system_name` ON `requirement_pool` (`system_name`);
CREATE INDEX `idx_req_manager_name` ON `requirement_pool` (`req_manager_name`);
CREATE INDEX `idx_create_time` ON `requirement_pool` (`create_time`);
CREATE INDEX `idx_up_time` ON `requirement_pool` (`up_time`);

SET SESSION innodb_lock_wait_timeout = DEFAULT;
