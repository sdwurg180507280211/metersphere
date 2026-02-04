-- ============================================
-- 将正式环境的普通用户组修改为系统用户组
-- ============================================
-- 
-- 目的：将"开发人员组"和"测试人员组"从普通用户组（system=0）
--       修改为系统用户组（system=1），并将 ID 改为固定值
--
-- 执行前提：
-- 1. 确认正式环境中存在"开发人员组"和"测试人员组"
-- 2. 确认这两个用户组的当前 ID（随机 UUID）
-- 3. 备份数据库
--
-- ============================================

-- 步骤1：查看当前用户组情况
SELECT 
    id,
    name,
    `system`,
    type,
    scope_id,
    creator
FROM `group`
WHERE name IN ('开发人员组', '测试人员组')
ORDER BY name;

-- ============================================
-- 步骤2：备份原有数据（可选，但强烈建议）
-- ============================================

-- 备份 group 表
CREATE TABLE IF NOT EXISTS `group_backup_20260126` AS 
SELECT * FROM `group` WHERE name IN ('开发人员组', '测试人员组');

-- 备份 user_group 表（用户组关联关系）
CREATE TABLE IF NOT EXISTS `user_group_backup_20260126` AS 
SELECT ug.* 
FROM user_group ug
INNER JOIN `group` g ON ug.group_id = g.id
WHERE g.name IN ('开发人员组', '测试人员组');

-- 备份 user_group_permission 表（用户组权限关联关系）
CREATE TABLE IF NOT EXISTS `user_group_permission_backup_20260126` AS 
SELECT ugp.* 
FROM user_group_permission ugp
INNER JOIN `group` g ON ugp.group_id = g.id
WHERE g.name IN ('开发人员组', '测试人员组');

-- ============================================
-- 步骤3：执行迁移（需要替换 OLD_DEVELOPER_ID 和 OLD_TESTER_ID）
-- ============================================

-- 3.1 更新 user_group 表中的关联关系（用户-用户组关联）
-- 将所有引用旧 ID 的记录更新为新 ID

-- 更新开发人员组的用户关联
UPDATE user_group 
SET group_id = 'developer'
WHERE group_id = 'OLD_DEVELOPER_ID';  -- 替换为实际的旧 ID

-- 更新测试人员组的用户关联
UPDATE user_group 
SET group_id = 'tester'
WHERE group_id = 'OLD_TESTER_ID';  -- 替换为实际的旧 ID

-- 3.2 更新 user_group_permission 表中的关联关系（用户组-权限关联）
-- 将所有引用旧 ID 的记录更新为新 ID

-- 更新开发人员组的权限关联
UPDATE user_group_permission 
SET group_id = 'developer'
WHERE group_id = 'OLD_DEVELOPER_ID';  -- 替换为实际的旧 ID

-- 更新测试人员组的权限关联
UPDATE user_group_permission 
SET group_id = 'tester'
WHERE group_id = 'OLD_TESTER_ID';  -- 替换为实际的旧 ID

-- 3.3 更新 group 表
-- 先删除旧记录，再插入新记录（因为主键冲突）

-- 删除旧的开发人员组
DELETE FROM `group` WHERE id = 'OLD_DEVELOPER_ID';  -- 替换为实际的旧 ID

-- 删除旧的测试人员组
DELETE FROM `group` WHERE id = 'OLD_TESTER_ID';  -- 替换为实际的旧 ID

-- 插入新的系统用户组（开发人员组）
INSERT INTO `group` (
    id, 
    name, 
    description, 
    `system`, 
    type, 
    create_time, 
    update_time, 
    creator, 
    scope_id
) VALUES (
    'developer',
    '开发人员组',
    '开发人员',
    1,  -- system = 1 表示系统用户组
    'PROJECT',
    UNIX_TIMESTAMP() * 1000,
    UNIX_TIMESTAMP() * 1000,
    'admin',
    'global'
);

-- 插入新的系统用户组（测试人员组）
INSERT INTO `group` (
    id, 
    name, 
    description, 
    `system`, 
    type, 
    create_time, 
    update_time, 
    creator, 
    scope_id
) VALUES (
    'tester',
    '测试人员组',
    '测试人员',
    1,  -- system = 1 表示系统用户组
    'PROJECT',
    UNIX_TIMESTAMP() * 1000,
    UNIX_TIMESTAMP() * 1000,
    'admin',
    'global'
);

-- ============================================
-- 步骤4：验证迁移结果
-- ============================================

-- 4.1 验证用户组表
SELECT 
    id,
    name,
    `system`,
    type,
    scope_id
FROM `group`
WHERE id IN ('developer', 'tester');

-- 预期结果：
-- id        | name         | system | type    | scope_id
-- developer | 开发人员组   | 1      | PROJECT | global
-- tester    | 测试人员组   | 1      | PROJECT | global

-- 4.2 验证用户组关联关系
SELECT 
    ug.user_id,
    u.name as user_name,
    ug.group_id,
    g.name as group_name,
    ug.source_id as project_id
FROM user_group ug
INNER JOIN `group` g ON ug.group_id = g.id
INNER JOIN user u ON ug.user_id = u.id
WHERE ug.group_id IN ('developer', 'tester')
ORDER BY g.name, u.name;

-- 4.3 统计用户数量和权限数量
SELECT 
    g.id,
    g.name,
    COUNT(DISTINCT ug.user_id) as user_count,
    COUNT(DISTINCT ugp.permission_id) as permission_count
FROM `group` g
LEFT JOIN user_group ug ON g.id = ug.group_id
LEFT JOIN user_group_permission ugp ON g.id = ugp.group_id
WHERE g.id IN ('developer', 'tester')
GROUP BY g.id, g.name;

-- ============================================
-- 步骤5：清理备份表（可选，建议保留一段时间）
-- ============================================

-- 确认无误后，可以删除备份表
-- DROP TABLE IF EXISTS `group_backup_20260126`;
-- DROP TABLE IF EXISTS `user_group_backup_20260126`;

-- ============================================
-- 使用说明
-- ============================================
-- 
-- 1. 先执行步骤1，查看当前用户组的 ID
-- 2. 将步骤3中的 OLD_DEVELOPER_ID 和 OLD_TESTER_ID 替换为实际的 ID
-- 3. 执行步骤2，备份数据
-- 4. 执行步骤3，进行迁移
-- 5. 执行步骤4，验证结果
-- 6. 确认无误后，可以删除备份表
--
-- ============================================
-- 注意事项
-- ============================================
--
-- 1. 如果正式环境中用户组名称不是"开发人员组"和"测试人员组"，
--    需要修改 SQL 中的名称
--
-- 2. 如果存在其他表引用了 group.id，也需要一并更新
--    可以通过以下 SQL 查找：
--    SELECT 
--        TABLE_NAME, 
--        COLUMN_NAME 
--    FROM information_schema.COLUMNS 
--    WHERE TABLE_SCHEMA = 'metersphere_dev' 
--      AND COLUMN_NAME LIKE '%group%';
--
-- 3. 执行前务必备份数据库
--
-- 4. 建议在测试环境先执行一遍，确认无误后再在正式环境执行
--
-- ============================================
