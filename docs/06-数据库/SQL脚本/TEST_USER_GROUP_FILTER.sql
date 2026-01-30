-- ==========================================
-- 用户组权限过滤功能测试 SQL
-- ==========================================
-- 用途：验证 admin 用户是否会触发权限过滤逻辑
-- 日期：2026-01-26
-- ==========================================

-- 1. 查询 admin 用户在所有项目中的用户组
SELECT 
    u.id as user_id,
    u.name as user_name,
    ug.source_id as project_id,
    g.id as group_id,
    g.name as group_name,
    g.type as group_type
FROM user u
INNER JOIN user_group ug ON u.id = ug.user_id
INNER JOIN `group` g ON ug.group_id = g.id
WHERE u.id = 'admin'
ORDER BY g.type, g.id;

-- 预期结果：
-- admin 用户属于以下用户组：
-- - admin（系统管理员）
-- - ws_admin（工作空间管理员）
-- - project_admin（项目管理员）
-- 不属于 developer 或 tester 组

-- ==========================================

-- 2. 模拟 getUserGroupInProject 查询（admin 用户）
-- 这是代码中实际执行的 SQL
SELECT g.id
FROM user_group ug
INNER JOIN `group` g ON ug.group_id = g.id
WHERE ug.user_id = 'admin'
  AND ug.source_id = 'c07bf043-d902-444b-9d59-7e98338392b3'  -- 替换为实际项目ID
  AND g.id IN ('developer', 'tester')
LIMIT 1;

-- 预期结果：
-- 空结果（admin 不属于 developer 或 tester 组）
-- 因此 addUserGroupFilter() 方法不会设置 currentUserId 和 userGroupId
-- 因此 SQL 中的权限过滤条件不会被触发

-- ==========================================

-- 3. 查询所有系统用户组
SELECT 
    id,
    name,
    type,
    scope_id,
    system
FROM `group`
WHERE system = 1
ORDER BY type, id;

-- 预期结果：
-- 系统预置的用户组，包括：
-- - admin（系统管理员）
-- - ws_admin（工作空间管理员）
-- - project_admin（项目管理员）
-- - project_member（项目成员）
-- - developer（开发人员）
-- - tester（测试人员）
-- - read_only（只读用户）

-- ==========================================

-- 4. 验证权限过滤逻辑（假设用户属于 developer 组）
-- 这是 ExtIssuesMapper.xml 中的权限过滤 SQL

-- 假设：
-- - 用户ID：'test-user-id'
-- - 项目ID：'test-project-id'
-- - 用户组ID：'developer'

SELECT issues.*
FROM issues
WHERE issues.project_id = 'test-project-id'
  AND (
    -- 条件1：创建人是自己
    issues.creator = 'test-user-id'
    OR 
    -- 条件2：处理人是自己（通过自定义字段查询）
    EXISTS (
        SELECT 1 FROM custom_field_issues cfi
        INNER JOIN custom_field cf ON cfi.field_id = cf.id
        WHERE cfi.resource_id = issues.id
          AND cf.name = '处理人'
          AND cf.scene = 'ISSUE'
          AND cf.system = 1
          AND (cf.project_id = issues.project_id OR cf.global = 1)
          AND cfi.value = 'test-user-id'
    )
  )
  AND (issues.platform_status != 'delete' OR issues.platform_status IS NULL);

-- 预期结果：
-- 只返回创建人或处理人是 'test-user-id' 的缺陷

-- ==========================================

-- 5. 验证 admin 用户不会触发权限过滤
-- admin 用户查询缺陷时，不会添加权限过滤条件

SELECT issues.*
FROM issues
WHERE issues.project_id = 'test-project-id'
  AND (issues.platform_status != 'delete' OR issues.platform_status IS NULL);

-- 预期结果：
-- 返回项目中的所有缺陷（无权限限制）

-- ==========================================

-- 6. 查询某个用户在某个项目中是否属于多个用户组
SELECT 
    ug.user_id,
    ug.source_id as project_id,
    g.id as group_id,
    g.name as group_name
FROM user_group ug
INNER JOIN `group` g ON ug.group_id = g.id
WHERE ug.user_id = 'test-user-id'  -- 替换为实际用户ID
  AND ug.source_id = 'test-project-id'  -- 替换为实际项目ID
ORDER BY g.id;

-- 预期结果：
-- 可能返回多条记录（用户在同一项目下属于多个用户组）
-- 例如：
-- - project_admin
-- - tester
-- 这就是为什么 SQL 中需要添加 g.id IN ('developer', 'tester') 过滤

-- ==========================================

-- 7. 测试场景：浏览器卡死问题排查

-- 场景描述：
-- - 用户：admin（超级管理员）
-- - 操作：访问缺陷管理列表页
-- - 现象：浏览器卡死

-- 排查步骤1：确认 admin 用户不会触发权限过滤
SELECT g.id
FROM user_group ug
INNER JOIN `group` g ON ug.group_id = g.id
WHERE ug.user_id = 'admin'
  AND ug.source_id = 'c07bf043-d902-444b-9d59-7e98338392b3'
  AND g.id IN ('developer', 'tester')
LIMIT 1;
-- 结果：空（不会触发权限过滤）

-- 排查步骤2：检查缺陷数量
SELECT COUNT(*) as total_issues
FROM issues
WHERE project_id = 'c07bf043-d902-444b-9d59-7e98338392b3'
  AND (platform_status != 'delete' OR platform_status IS NULL);
-- 如果数量很大（如 > 10000），可能导致查询慢

-- 排查步骤3：检查是否有慢查询
SHOW PROCESSLIST;
-- 查看是否有长时间运行的查询

-- 排查步骤4：检查索引
SHOW INDEX FROM issues;
-- 确认 project_id 和 platform_status 字段有索引

-- 结论：
-- admin 用户不会触发权限过滤 SQL（不会执行 EXISTS 子查询）
-- 浏览器卡死不是本次修改导致的
-- 需要从其他方向排查：
-- 1. 数据量是否过大
-- 2. 是否有其他慢查询
-- 3. 前端是否有死循环
-- 4. 网络请求是否超时

-- ==========================================
