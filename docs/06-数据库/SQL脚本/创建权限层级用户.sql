-- ============================================
-- MeterSphere 测试用户创建脚本
-- ============================================
-- 功能：创建各个权限层级的测试用户
-- 日期：2024-12-03
-- ============================================

USE metersphere;

-- ============================================
-- 第一步：为 Administrator 添加 super_group 权限
-- ============================================
INSERT INTO user_group (id, user_id, group_id, source_id, create_time, update_time)
SELECT
    UUID(),
    'admin',
    'super_group',
    'system',
    unix_timestamp() * 1000,
    unix_timestamp() * 1000
    WHERE NOT EXISTS (
    SELECT 1 FROM user_group
    WHERE user_id = 'admin' AND group_id = 'super_group'
);

-- ============================================
-- 第二步：创建测试用户（密码统一为 metersphere）
-- ============================================

-- 2.1 超级管理员用户
INSERT INTO user (id, name, email, password, status, source, create_time, update_time, language, last_workspace_id, last_project_id)
SELECT
    'super_admin_user',
    'SuperAdmin',
    'super@metersphere.io',
    MD5('metersphere'),
    '1',
    'LOCAL',  -- ⭐ 必须设置为 LOCAL，否则无法登录
    unix_timestamp() * 1000,
    unix_timestamp() * 1000,
    'zh_CN',
    NULL,
    NULL
    WHERE NOT EXISTS (SELECT 1 FROM user WHERE id = 'super_admin_user');

-- 2.2 系统管理员用户
INSERT INTO user (id, name, email, password, status, source, create_time, update_time, language, last_workspace_id, last_project_id)
SELECT
    'sys_admin_user',
    'SystemAdmin',
    'sysadmin@metersphere.io',
    MD5('metersphere'),
    '1',
    'LOCAL',  -- ⭐ 必须设置为 LOCAL，否则无法登录
    unix_timestamp() * 1000,
    unix_timestamp() * 1000,
    'zh_CN',
    NULL,
    NULL
    WHERE NOT EXISTS (SELECT 1 FROM user WHERE id = 'sys_admin_user');

-- 2.3 工作空间管理员用户
INSERT INTO user (id, name, email, password, status, source, create_time, update_time, language, last_workspace_id, last_project_id)
SELECT
    'ws_admin_user',
    'WorkspaceAdmin',
    'wsadmin@metersphere.io',
    MD5('metersphere'),
    '1',
    'LOCAL',  -- ⭐ 必须设置为 LOCAL，否则无法登录
    unix_timestamp() * 1000,
    unix_timestamp() * 1000,
    'zh_CN',
    NULL,
    NULL
    WHERE NOT EXISTS (SELECT 1 FROM user WHERE id = 'ws_admin_user');

-- 2.4 工作空间成员用户
INSERT INTO user (id, name, email, password, status, source, create_time, update_time, language, last_workspace_id, last_project_id)
SELECT
    'ws_member_user',
    'WorkspaceMember',
    'wsmember@metersphere.io',
    MD5('metersphere'),
    '1',
    'LOCAL',  -- ⭐ 必须设置为 LOCAL，否则无法登录
    unix_timestamp() * 1000,
    unix_timestamp() * 1000,
    'zh_CN',
    NULL,
    NULL
    WHERE NOT EXISTS (SELECT 1 FROM user WHERE id = 'ws_member_user');

-- 2.5 项目管理员用户
INSERT INTO user (id, name, email, password, status, source, create_time, update_time, language, last_workspace_id, last_project_id)
SELECT
    'proj_admin_user',
    'ProjectAdmin',
    'projadmin@metersphere.io',
    MD5('metersphere'),
    '1',
    'LOCAL',  -- ⭐ 必须设置为 LOCAL，否则无法登录
    unix_timestamp() * 1000,
    unix_timestamp() * 1000,
    'zh_CN',
    NULL,
    NULL
    WHERE NOT EXISTS (SELECT 1 FROM user WHERE id = 'proj_admin_user');

-- 2.6 项目成员用户
INSERT INTO user (id, name, email, password, status, source, create_time, update_time, language, last_workspace_id, last_project_id)
SELECT
    'proj_member_user',
    'ProjectMember',
    'projmember@metersphere.io',
    MD5('metersphere'),
    '1',
    'LOCAL',  -- ⭐ 必须设置为 LOCAL，否则无法登录
    unix_timestamp() * 1000,
    unix_timestamp() * 1000,
    'zh_CN',
    NULL,
    NULL
    WHERE NOT EXISTS (SELECT 1 FROM user WHERE id = 'proj_member_user');

-- 2.7 只读用户
INSERT INTO user (id, name, email, password, status, source, create_time, update_time, language, last_workspace_id, last_project_id)
SELECT
    'readonly_user',
    'ReadOnlyUser',
    'readonly@metersphere.io',
    MD5('metersphere'),
    '1',
    'LOCAL',  -- ⭐ 必须设置为 LOCAL，否则无法登录
    unix_timestamp() * 1000,
    unix_timestamp() * 1000,
    'zh_CN',
    NULL,
    NULL
    WHERE NOT EXISTS (SELECT 1 FROM user WHERE id = 'readonly_user');

-- ============================================
-- 第三步：分配用户权限
-- ============================================

-- 获取第一个工作空间ID（用于分配工作空间权限）
SET @workspace_id = (SELECT id FROM workspace LIMIT 1);

-- 获取第一个项目ID（用于分配项目权限）
SET @project_id = (SELECT id FROM project LIMIT 1);

-- 3.1 超级管理员权限
INSERT INTO user_group (id, user_id, group_id, source_id, create_time, update_time)
SELECT UUID(), 'super_admin_user', 'super_group', 'system', unix_timestamp() * 1000, unix_timestamp() * 1000
    WHERE NOT EXISTS (SELECT 1 FROM user_group WHERE user_id = 'super_admin_user' AND group_id = 'super_group');

-- 3.2 系统管理员权限
INSERT INTO user_group (id, user_id, group_id, source_id, create_time, update_time)
SELECT UUID(), 'sys_admin_user', 'admin', 'system', unix_timestamp() * 1000, unix_timestamp() * 1000
    WHERE NOT EXISTS (SELECT 1 FROM user_group WHERE user_id = 'sys_admin_user' AND group_id = 'admin');

-- 3.3 工作空间管理员权限
INSERT INTO user_group (id, user_id, group_id, source_id, create_time, update_time)
SELECT UUID(), 'ws_admin_user', 'ws_admin', @workspace_id, unix_timestamp() * 1000, unix_timestamp() * 1000
    WHERE NOT EXISTS (SELECT 1 FROM user_group WHERE user_id = 'ws_admin_user' AND group_id = 'ws_admin');

-- 3.4 工作空间成员权限
INSERT INTO user_group (id, user_id, group_id, source_id, create_time, update_time)
SELECT UUID(), 'ws_member_user', 'ws_member', @workspace_id, unix_timestamp() * 1000, unix_timestamp() * 1000
    WHERE NOT EXISTS (SELECT 1 FROM user_group WHERE user_id = 'ws_member_user' AND group_id = 'ws_member');

-- 3.5 项目管理员权限
INSERT INTO user_group (id, user_id, group_id, source_id, create_time, update_time)
SELECT UUID(), 'proj_admin_user', 'project_admin', @project_id, unix_timestamp() * 1000, unix_timestamp() * 1000
    WHERE NOT EXISTS (SELECT 1 FROM user_group WHERE user_id = 'proj_admin_user' AND group_id = 'project_admin');

-- 3.6 项目成员权限
INSERT INTO user_group (id, user_id, group_id, source_id, create_time, update_time)
SELECT UUID(), 'proj_member_user', 'project_member', @project_id, unix_timestamp() * 1000, unix_timestamp() * 1000
    WHERE NOT EXISTS (SELECT 1 FROM user_group WHERE user_id = 'proj_member_user' AND group_id = 'project_member');

-- 3.7 只读用户权限
INSERT INTO user_group (id, user_id, group_id, source_id, create_time, update_time)
SELECT UUID(), 'readonly_user', 'read_only', @project_id, unix_timestamp() * 1000, unix_timestamp() * 1000
    WHERE NOT EXISTS (SELECT 1 FROM user_group WHERE user_id = 'readonly_user' AND group_id = 'read_only');

-- ============================================
-- 第四步：修复已存在用户的 source 字段（如果之前创建时未设置）
-- ============================================
UPDATE user 
SET source = 'LOCAL'
WHERE source IS NULL 
  AND id IN (
    'super_admin_user',
    'sys_admin_user',
    'ws_admin_user',
    'ws_member_user',
    'proj_admin_user',
    'proj_member_user',
    'readonly_user'
  );

-- ============================================
-- 第五步：验证创建结果
-- ============================================

-- 查看所有用户（包含 source 字段）
SELECT '=== 所有用户 ===' as title;
SELECT id, name, email, status, source FROM user ORDER BY create_time;

-- 查看所有用户组分配
SELECT '=== 用户权限分配 ===' as title;
SELECT
    u.id as user_id,
    u.name as user_name,
    ug.group_id,
    g.name as group_name,
    g.type as group_type,
    ug.source_id
FROM user u
         JOIN user_group ug ON u.id = ug.user_id
         JOIN `group` g ON ug.group_id = g.id
ORDER BY u.id, g.type;

-- 统计各权限层级用户数
SELECT '=== 权限层级统计 ===' as title;
SELECT
    g.id as group_id,
    g.name as group_name,
    g.type,
    COUNT(DISTINCT ug.user_id) as user_count
FROM `group` g
         LEFT JOIN user_group ug ON g.id = ug.group_id
GROUP BY g.id, g.name, g.type
ORDER BY g.type, g.id;

-- ============================================
-- 完成！
-- ============================================
-- 所有用户的默认密码：metersphere
--
-- ⚠️ 重要提示：
-- 1. source 字段必须设置为 'LOCAL'，否则用户无法登录
-- 2. 如果之前创建的用户 source 为 NULL，执行第四步修复脚本
-- 3. 密码使用 MD5 加密：MD5('metersphere') = '3259a9d7f208ef9690025d1432558c5b'
--
-- 测试登录：
-- - super@metersphere.io / metersphere (超级管理员) ⭐ 可创建全局系统字段
-- - sysadmin@metersphere.io / metersphere (系统管理员)
-- - wsadmin@metersphere.io / metersphere (工作空间管理员)
-- - wsmember@metersphere.io / metersphere (工作空间成员)
-- - projadmin@metersphere.io / metersphere (项目管理员)
-- - projmember@metersphere.io / metersphere (项目成员)
-- - readonly@metersphere.io / metersphere (只读用户)
-- ============================================

