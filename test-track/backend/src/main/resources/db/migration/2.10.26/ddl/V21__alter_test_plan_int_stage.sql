ALTER TABLE test_plan
    ADD COLUMN requirement_doc_url varchar(1024) DEFAULT NULL COMMENT '全流程平台需求规格说明书链接' AFTER requirement_number,
    ADD COLUMN requirement_system_name varchar(255) DEFAULT NULL COMMENT '全流程平台所属系统' AFTER requirement_doc_url,
    ADD COLUMN requirement_sync_event_time bigint DEFAULT NULL COMMENT '最近一次需求同步事件时间' AFTER requirement_system_name,
    ADD COLUMN int_stage varchar(32) DEFAULT NULL COMMENT 'INT业务阶段：TEST_PLAN/TEST_PREPARATION/TEST_EXECUTION/DONE/CANCELLED' AFTER requirement_sync_event_time,
    ADD COLUMN requirement_approval_status varchar(32) DEFAULT NULL COMMENT '测试计划审批状态：APPROVED/REJECTED' AFTER int_stage,
    ADD COLUMN requirement_approval_comment varchar(1024) DEFAULT NULL COMMENT '测试计划审批意见' AFTER requirement_approval_status,
    ADD COLUMN requirement_approval_time bigint DEFAULT NULL COMMENT '测试计划审批时间' AFTER requirement_approval_comment;

CREATE INDEX idx_test_plan_int_stage ON test_plan (int_stage);
CREATE INDEX idx_test_plan_requirement_sync_event_time ON test_plan (requirement_sync_event_time);

CREATE TABLE requirement_system_mapping (
    id varchar(32) NOT NULL COMMENT '主键',
    system_key varchar(255) NOT NULL COMMENT '所属系统映射键，当前可使用systemName，后续优先systemCode',
    system_name varchar(255) DEFAULT NULL COMMENT '所属系统显示名称',
    project_id varchar(50) NOT NULL COMMENT 'MeterSphere项目ID',
    node_id varchar(50) NOT NULL COMMENT '测试计划模块节点ID',
    principal_id varchar(64) NOT NULL COMMENT '默认测试负责人用户ID',
    enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time bigint DEFAULT NULL COMMENT '创建时间',
    update_time bigint DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_requirement_system_mapping_key (system_key),
    KEY idx_requirement_system_mapping_name (system_name),
    KEY idx_requirement_system_mapping_project (project_id)
) COMMENT='全流程平台所属系统与测试计划创建位置映射';
