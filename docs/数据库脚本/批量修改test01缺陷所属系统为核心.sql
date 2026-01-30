-- ==========================================
-- 批量修改 test01 创建的缺陷的"缺陷所属系统"为"核心"
-- ==========================================
-- 执行时间：2026-01-29
-- 执行环境：生产环境
-- 影响范围：test01 创建的 6 条缺陷
-- ==========================================

-- 步骤1：查看当前数据（执行前验证）
SELECT 
    i.id,
    i.num,
    i.title,
    i.creator,
    cfi.value as current_system_id,
    CASE 
        WHEN cfi.value = '"8e56d21c-2694-4812-934e-1327c62aa5ba"' THEN '核心'
        WHEN cfi.value = '"0b2f1fd0-020b-407e-bdab-7831155c3dee"' THEN '瑞博士智能陪练平台'
        WHEN cfi.value = '"77dd2fab-c65a-4446-ad36-2a9c9935a7c4"' THEN '瑞博士管理系统'
        WHEN cfi.value = '"ba542846-61fc-45cb-b7a1-978e01d77eea"' THEN '瑞众保险出单中心平台'
        ELSE cfi.value
    END as current_system_name
FROM issues i
LEFT JOIN custom_field_issues cfi 
    ON i.id = cfi.resource_id 
    AND cfi.field_id = 'issue-associated-system-field-2024-12-16-001'
WHERE i.creator = 'test01'
ORDER BY i.num;

-- ==========================================
-- 步骤2：批量修改（核心SQL）
-- ==========================================

-- 方式1：UPDATE 已存在的记录
UPDATE custom_field_issues
SET value = '"8e56d21c-2694-4812-934e-1327c62aa5ba"',  -- "核心"系统的ID（带引号）
    update_time = UNIX_TIMESTAMP() * 1000
WHERE field_id = 'issue-associated-system-field-2024-12-16-001'
AND resource_id IN (
    SELECT id FROM issues WHERE creator = 'test01'
);

-- 方式2：INSERT 不存在的记录（如果某些缺陷没有设置过所属系统）
INSERT INTO custom_field_issues (resource_id, field_id, value, text_value, create_time, update_time)
SELECT 
    i.id as resource_id,
    'issue-associated-system-field-2024-12-16-001' as field_id,
    '"8e56d21c-2694-4812-934e-1327c62aa5ba"' as value,  -- "核心"系统的ID（带引号）
    NULL as text_value,
    UNIX_TIMESTAMP() * 1000 as create_time,
    UNIX_TIMESTAMP() * 1000 as update_time
FROM issues i
WHERE i.creator = 'test01'
AND NOT EXISTS (
    SELECT 1 FROM custom_field_issues cfi
    WHERE cfi.resource_id = i.id
    AND cfi.field_id = 'issue-associated-system-field-2024-12-16-001'
);

-- ==========================================
-- 步骤3：验证修改结果
-- ==========================================

SELECT 
    i.id,
    i.num,
    i.title,
    i.creator,
    cfi.value as system_id,
    CASE 
        WHEN cfi.value = '"8e56d21c-2694-4812-934e-1327c62aa5ba"' THEN '核心'
        ELSE '其他系统'
    END as system_name
FROM issues i
LEFT JOIN custom_field_issues cfi 
    ON i.id = cfi.resource_id 
    AND cfi.field_id = 'issue-associated-system-field-2024-12-16-001'
WHERE i.creator = 'test01'
ORDER BY i.num;

-- 预期结果：所有 test01 创建的缺陷的 system_name 都应该是 "核心"

-- ==========================================
-- 步骤4：统计验证
-- ==========================================

SELECT 
    COUNT(*) as total_count,
    SUM(CASE WHEN cfi.value = '"8e56d21c-2694-4812-934e-1327c62aa5ba"' THEN 1 ELSE 0 END) as core_system_count
FROM issues i
LEFT JOIN custom_field_issues cfi 
    ON i.id = cfi.resource_id 
    AND cfi.field_id = 'issue-associated-system-field-2024-12-16-001'
WHERE i.creator = 'test01';

-- 预期结果：total_count = 6, core_system_count = 6

-- ==========================================
-- 回滚SQL（如果需要）
-- ==========================================

-- 备份当前数据
CREATE TABLE IF NOT EXISTS custom_field_issues_backup_20260129 AS
SELECT * FROM custom_field_issues
WHERE field_id = 'issue-associated-system-field-2024-12-16-001'
AND resource_id IN (
    SELECT id FROM issues WHERE creator = 'test01'
);

-- 回滚到备份数据
-- UPDATE custom_field_issues cfi
-- INNER JOIN custom_field_issues_backup_20260129 backup
--     ON cfi.resource_id = backup.resource_id
--     AND cfi.field_id = backup.field_id
-- SET cfi.value = backup.value,
--     cfi.update_time = backup.update_time
-- WHERE cfi.field_id = 'issue-associated-system-field-2024-12-16-001'
-- AND cfi.resource_id IN (
--     SELECT id FROM issues WHERE creator = 'test01'
-- );

-- ==========================================
-- 关键信息
-- ==========================================
-- 字段ID：issue-associated-system-field-2024-12-16-001
-- 字段名称：缺陷所属系统
-- 字段类型：associatedSystem
-- 目标系统ID：8e56d21c-2694-4812-934e-1327c62aa5ba
-- 目标系统名称：核心
-- 创建人：test01
-- 影响记录数：6条
-- 
-- 注意事项：
-- 1. value 字段存储的是带引号的JSON字符串格式："系统ID"
-- 2. 执行前请先执行步骤1查看当前数据
-- 3. 执行步骤2进行批量修改
-- 4. 执行步骤3和步骤4验证修改结果
-- 5. 如需回滚，先执行备份SQL，再执行回滚SQL
-- ==========================================
