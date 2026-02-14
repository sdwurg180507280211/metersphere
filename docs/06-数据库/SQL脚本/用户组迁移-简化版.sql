-- ============================================
-- 简化版：用户组迁移脚本
-- ============================================
-- 使用说明：
-- 1. 先查询正式环境的用户组 ID
-- 2. 将下面的 'YOUR_OLD_DEVELOPER_ID' 和 'YOUR_OLD_TESTER_ID' 替换为实际 ID
-- 3. 执行整个脚本
-- ============================================

-- 开始事务（如果出错可以回滚）
START TRANSACTION;

-- 查看当前用户组（确认 ID）
SELECT id, name, `system`, type FROM `group` WHERE name IN ('开发人员组', '测试人员组');

-- ============================================
-- 替换下面两行中的 ID
-- ============================================
SET @old_developer_id = 'YOUR_OLD_DEVELOPER_ID';  -- 替换为实际的开发人员组 ID
SET @old_tester_id = 'YOUR_OLD_TESTER_ID';        -- 替换为实际的测试人员组 ID

-- 更新 user_group 表的关联关系（用户-用户组关联）
UPDATE user_group SET group_id = 'developer' WHERE group_id = @old_developer_id;
UPDATE user_group SET group_id = 'tester' WHERE group_id = @old_tester_id;

-- 更新 user_group_permission 表的关联关系（用户组-权限关联）
UPDATE user_group_permission SET group_id = 'developer' WHERE group_id = @old_developer_id;
UPDATE user_group_permission SET group_id = 'tester' WHERE group_id = @old_tester_id;

-- 删除旧的用户组记录
DELETE FROM `group` WHERE id = @old_developer_id;
DELETE FROM `group` WHERE id = @old_tester_id;

-- 插入新的系统用户组
INSERT INTO `group` (id, name, description, `system`, type, create_time, update_time, creator, scope_id)
VALUES 
('developer', '开发人员组', '开发人员', 1, 'PROJECT', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'global'),
('tester', '测试人员组', '测试人员', 1, 'PROJECT', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'global');

-- 验证结果
SELECT '=== 验证用户组 ===' as step;
SELECT id, name, `system`, type, scope_id FROM `group` WHERE id IN ('developer', 'tester');

SELECT '=== 验证用户关联 ===' as step;
SELECT g.id, g.name, COUNT(ug.user_id) as user_count
FROM `group` g
LEFT JOIN user_group ug ON g.id = ug.group_id
WHERE g.id IN ('developer', 'tester')
GROUP BY g.id, g.name;

-- 如果一切正常，提交事务
COMMIT;

-- 如果有问题，可以回滚
-- ROLLBACK;

-- ============================================
-- 说明：迁移完成后的影响
-- ============================================
-- 
-- ✅ 不会影响用户使用：
-- 1. 用户-用户组关联关系已更新（user_group 表）
-- 2. 用户组-权限关联关系已更新（user_group_permission 表）
-- 3. 所有用户仍然属于原来的用户组，只是用户组 ID 变了
-- 4. 所有权限配置保持不变
-- 
-- ✅ 用户体验无变化：
-- 1. 用户登录后看到的功能菜单不变
-- 2. 用户的权限不变
-- 3. 用户能访问的项目不变
-- 4. 唯一变化是缺陷管理的权限过滤功能生效
-- 
-- ============================================
