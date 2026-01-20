SET SESSION innodb_lock_wait_timeout = 7200;

-- ============================================
-- 修复"所属系统"字段未关联到所有用例模板的问题
-- 说明：V146脚本只将字段关联到默认模板，但用户项目可能使用自定义模板
-- 解决方案：将全局"所属系统"字段批量关联到所有TEST_CASE模板
-- ============================================

-- 步骤1：将"所属系统"字段关联到所有用例模板（排除已关联的）
INSERT INTO custom_field_template (id, field_id, template_id, scene, required, default_value)
SELECT 
    UUID() as id,
    'case-associated-system-field-2024-12-01-002' as field_id,  -- 所属系统字段ID
    tct.id as template_id,
    'TEST_CASE' as scene,
    0 as required,
    '' as default_value
FROM test_case_template tct
WHERE NOT EXISTS (
    -- 排除已经关联的模板
    SELECT 1 
    FROM custom_field_template cft 
    WHERE cft.template_id = tct.id 
    AND cft.field_id = 'case-associated-system-field-2024-12-01-002'
);

-- 步骤2：将"用例执行人"字段关联到所有用例模板（排除已关联的）
INSERT INTO custom_field_template (id, field_id, template_id, scene, required, default_value)
SELECT 
    UUID() as id,
    'case_executor-field-2024-12-16-003' as field_id,  -- 用例执行人字段ID
    tct.id as template_id,
    'TEST_CASE' as scene,
    0 as required,
    '' as default_value
FROM test_case_template tct
WHERE NOT EXISTS (
    -- 排除已经关联的模板
    SELECT 1 
    FROM custom_field_template cft 
    WHERE cft.template_id = tct.id 
    AND cft.field_id = 'case_executor-field-2024-12-16-003'
);

-- 步骤3：将"缺陷所属系统"字段关联到所有缺陷模板（排除已关联的）
INSERT INTO custom_field_template (id, field_id, template_id, scene, required, default_value)
SELECT 
    UUID() as id,
    'issue-associated-system-field-2024-12-16-001' as field_id,  -- 缺陷所属系统字段ID
    it.id as template_id,
    'ISSUE' as scene,
    0 as required,
    '' as default_value
FROM issue_template it
WHERE NOT EXISTS (
    -- 排除已经关联的模板
    SELECT 1 
    FROM custom_field_template cft 
    WHERE cft.template_id = it.id 
    AND cft.field_id = 'issue-associated-system-field-2024-12-16-001'
);

SET SESSION innodb_lock_wait_timeout = DEFAULT;


