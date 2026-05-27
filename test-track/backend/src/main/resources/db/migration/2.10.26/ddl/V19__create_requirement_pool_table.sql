SET SESSION innodb_lock_wait_timeout = 7200;

-- ============================================================
-- 需求池功能：建表 + 测试计划扩展
-- ============================================================

-- 1. 需求池表，用于存储从需求平台同步过来的需求数据
CREATE TABLE IF NOT EXISTS `requirement_pool` (
    `id`                  varchar(32)  NOT NULL COMMENT '主键',
    `dmp_num`             varchar(64)  NOT NULL COMMENT '需求编号',
    `requirement_name`    varchar(255) DEFAULT NULL COMMENT '需求名称',
    `pool_status`         varchar(32)  DEFAULT 'PENDING' COMMENT '需求池状态：PENDING(待创建)、CREATED(已创建)、CANCELLED(已取消)',
    `parent_wfinst_code`  varchar(100) DEFAULT NULL COMMENT '主流程编码',
    `act_name`            varchar(100) DEFAULT NULL COMMENT '当前环节',
    `operation_type`      varchar(32)  DEFAULT NULL COMMENT '操作类型：CREATED/UPDATED/CANCELLED',
    `system_name`         varchar(255) DEFAULT NULL COMMENT '所属系统',
    `req_manager_name`    varchar(64)  DEFAULT NULL COMMENT '需求负责人',
    `assignee_name`       varchar(64)  DEFAULT NULL COMMENT '当前处理人',
    `created_dept`        varchar(255) DEFAULT NULL COMMENT '需求申请部门',
    `create_user1`        varchar(64)  DEFAULT NULL COMMENT '需求申请人',
    `dept_name`           varchar(255) DEFAULT NULL COMMENT '需求负责人处室',
    `start_user_name`     varchar(64)  DEFAULT NULL COMMENT '创建人',
    `req_father_class`    varchar(255) DEFAULT NULL COMMENT '需求父类',
    `req_son_class`       varchar(255) DEFAULT NULL COMMENT '需求子类',
    `create_time`         bigint(20)   DEFAULT NULL COMMENT '需求提出时间',
    `up_time`             bigint(20)   DEFAULT NULL COMMENT '预计上线时间',
    `linked_plan_id`      varchar(50)  DEFAULT NULL COMMENT '关联的测试计划ID',
    `linked_plan_name`    varchar(255) DEFAULT NULL COMMENT '关联的测试计划名称',
    `test_status`         varchar(32)  DEFAULT NULL COMMENT '测试状态（模拟需求平台接收回传结果）',
    `plan_share_url`      varchar(500) DEFAULT NULL COMMENT '测试计划报告分享链接（模拟需求平台接收回传结果）',
    `last_callback_time`  bigint(20)   DEFAULT NULL COMMENT '最后回传时间',
    `last_sync_time`      bigint(20)   DEFAULT NULL COMMENT '最后同步时间',
    `event_time`          bigint(20)   DEFAULT NULL COMMENT '消息事件时间（幂等判断）',
    `trace_id`            varchar(64)  DEFAULT NULL COMMENT '追踪ID，用于链路追踪',
    `created_at`          bigint(20)   DEFAULT NULL COMMENT '记录创建时间',
    `updated_at`          bigint(20)   DEFAULT NULL COMMENT '记录更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dmp_num` (`dmp_num`) COMMENT '需求编号唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求池表';

-- 需求池常用查询索引
CREATE INDEX `idx_pool_status`       ON `requirement_pool` (`pool_status`);
CREATE INDEX `idx_act_name`          ON `requirement_pool` (`act_name`);
CREATE INDEX `idx_system_name`       ON `requirement_pool` (`system_name`);
CREATE INDEX `idx_req_manager_name`  ON `requirement_pool` (`req_manager_name`);
CREATE INDEX `idx_assignee_name`     ON `requirement_pool` (`assignee_name`);
CREATE INDEX `idx_created_dept`      ON `requirement_pool` (`created_dept`);
CREATE INDEX `idx_create_time`       ON `requirement_pool` (`create_time`);
CREATE INDEX `idx_up_time`           ON `requirement_pool` (`up_time`);

-- 插入需求池测试数据
INSERT INTO `requirement_pool` (`id`, `dmp_num`, `requirement_name`, `pool_status`, `act_name`, `system_name`, `req_manager_name`, `assignee_name`, `req_father_class`, `req_son_class`, `create_time`, `up_time`, `linked_plan_id`, `linked_plan_name`, `created_at`, `updated_at`, `trace_id`) VALUES
 ('req_pool_001', 'DMP-2024-001', '用户登录功能优化', 'PENDING', '测试待处理', '用户中心系统', '张三', '张三', '功能需求', '性能优化', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000, NULL, NULL, UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000, 'trace_001'),
 ('req_pool_002', 'DMP-2024-002', '订单支付流程改造', 'PENDING', '测试待处理', '订单管理系统', '李四', '李四', '功能需求', '流程优化', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000, NULL, NULL, UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000, 'trace_002'),
 ('req_pool_003', 'DMP-2024-003', '数据报表导出功能', 'PENDING', '测试待处理', '数据分析系统', '王五', '王五', '功能需求', '新增功能', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000, NULL, NULL, UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000, 'trace_003'),
 ('req_pool_004', 'DMP-2024-004', '系统安全加固',     'PENDING', '测试待处理', '安全管理系统', '赵六', '赵六', '非功能需求', '安全性', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000, NULL, NULL, UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000, 'trace_004'),
 ('req_pool_005', 'DMP-2024-005', '移动端适配优化',   'CANCELLED', '已取消',     '用户中心系统', '张三', '张三', '功能需求', '界面优化', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000, NULL, NULL, UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000, 'trace_005');

-- 2. 测试计划表扩展：添加需求编号字段，用于需求池与测试计划的一对一关联
ALTER TABLE `test_plan`
    ADD COLUMN `requirement_number` varchar(64) DEFAULT NULL COMMENT '需求编号' AFTER `node_path`;

CREATE UNIQUE INDEX `uk_requirement_number` ON `test_plan` (`requirement_number`);

SET SESSION innodb_lock_wait_timeout = DEFAULT;
