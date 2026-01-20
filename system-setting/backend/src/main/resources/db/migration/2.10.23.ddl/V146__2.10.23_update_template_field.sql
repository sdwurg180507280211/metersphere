SET SESSION innodb_lock_wait_timeout = 7200;

-- ============================================
-- 用例全局系统字段创建
-- 按照逻辑顺序组织：字段插入
-- ============================================

-- ============================================
-- 步骤1：插入自定义字段数据（设置为系统字段：system=1, global=1, project_id=NULL）
-- ============================================
-- 缺陷所属系统
INSERT INTO custom_field VALUES ('issue-associated-system-field-2024-12-16-001', '缺陷所属系统', 'ISSUE', 'associatedSystem', '', '[]', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', NULL, 0);

-- 用例所属系统
INSERT INTO custom_field VALUES ('case-associated-system-field-2024-12-01-002', '所属系统', 'TEST_CASE', 'associatedSystem', '', '[]', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', NULL, 0);

-- 用例执行人
INSERT INTO custom_field VALUES ('case_executor-field-2024-12-16-003', '用例执行人', 'TEST_CASE', 'member', '', '[]', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', NULL, 0);


-- 所属系统关联缺陷默认模版
INSERT INTO custom_field_template (id,field_id,template_id,scene,required,default_value) VALUES ('1n9j90ec-9c56-4f5d-ae06-522a93f47y6t','issue-associated-system-field-2024-12-16-001','5d7c87d2-f405-4ec1-9a3d-71b514cdfda3','ISSUE',0,'');
-- 用例执行人关联缺陷用例模版
INSERT INTO custom_field_template (id,field_id,template_id,scene,required,default_value) VALUES ('0d9h90ec-9c56-4f5d-ae06-522a93f48t78','case-associated-system-field-2024-12-01-002','b395d8fe-2ad6-4de7-81d3-2006b53a97c8','TEST_CASE',0,'');
-- 所属系统关联缺陷用例模版
INSERT INTO custom_field_template (id,field_id,template_id,scene,required,default_value) VALUES ('7x2z58wk-3d7b-6g9h-ae18-947f2d8c3y5r','case_executor-field-2024-12-16-003','b395d8fe-2ad6-4de7-81d3-2006b53a97c8','TEST_CASE',0,'');

-- 删掉系统用例模版关联责任人字段
DELETE from custom_field_template WHERE field_id='46065143-9d1d-11eb-b418-0242ac120002';
-- 删掉系统用例模版关联用例状态字段
DELETE from custom_field_template WHERE field_id='45f2de57-9d1d-11eb-b418-0242ac120002';
SET SESSION innodb_lock_wait_timeout = DEFAULT;
