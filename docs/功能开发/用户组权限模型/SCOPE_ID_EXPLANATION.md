# scope_id 字段说明

## 📋 什么是 scope_id

`scope_id` 字段定义了用户组的**作用域范围**：

| scope_id 值 | 含义 | 适用场景 |
|------------|------|---------|
| `global` | 全局用户组 | 跨所有工作空间生效 |
| `<工作空间ID>` | 工作空间级别用户组 | 只在特定工作空间生效 |

## 🔍 正式环境的实际情况

根据你的描述，正式环境的用户组配置如下：

```sql
-- 开发人员组（工作空间级别）
id: <随机UUID>
name: 开发人员组
system: 0  -- 普通用户组
scope_id: <某个工作空间ID>  -- 只在特定工作空间生效

-- 测试人员组（全局级别）
id: <随机UUID>
name: 测试人员组
system: 0  -- 普通用户组
scope_id: global  -- 跨所有工作空间生效
```

## ✅ 迁移策略

### 关键原则：保留原有的 scope_id

迁移脚本会：
1. ✅ 保存旧用户组的 `scope_id`
2. ✅ 删除旧用户组记录
3. ✅ 插入新用户组记录，使用保存的 `scope_id`

**代码实现**：
```sql
-- 保存旧的 scope_id
SET @old_developer_scope_id = (SELECT scope_id FROM `group` WHERE id = @old_developer_id);
SET @old_tester_scope_id = (SELECT scope_id FROM `group` WHERE id = @old_tester_id);

-- 插入新记录时使用保存的 scope_id
INSERT INTO `group` (..., scope_id) VALUES (..., @old_developer_scope_id);
INSERT INTO `group` (..., scope_id) VALUES (..., @old_tester_scope_id);
```

### 迁移前后对比

#### 开发人员组

**迁移前**：
```
id: a1b2c3d4-e5f6-7890-abcd-ef1234567890
name: 开发人员组
system: 0
scope_id: 698604cf-d1a4-11f0-a2f8-cead5f5242ae  -- 工作空间ID
```

**迁移后**：
```
id: developer  -- 固定ID
name: 开发人员组
system: 1  -- 系统用户组
scope_id: 698604cf-d1a4-11f0-a2f8-cead5f5242ae  -- 保持不变
```

#### 测试人员组

**迁移前**：
```
id: b2c3d4e5-f6a7-8901-bcde-f12345678901
name: 测试人员组
system: 0
scope_id: global  -- 全局
```

**迁移后**：
```
id: tester  -- 固定ID
name: 测试人员组
system: 1  -- 系统用户组
scope_id: global  -- 保持不变
```

## 🎯 为什么要保留 scope_id

### 1. 保持权限范围不变

如果开发人员组原本只在某个工作空间生效，迁移后应该继续只在该工作空间生效，不应该变成全局生效。

**错误做法**（统一改成 global）：
```sql
-- ❌ 错误：会导致开发人员组在所有工作空间生效
INSERT INTO `group` (..., scope_id) VALUES (..., 'global');
```

**正确做法**（保留原有 scope_id）：
```sql
-- ✅ 正确：保持原有的作用域范围
INSERT INTO `group` (..., scope_id) VALUES (..., @old_developer_scope_id);
```

### 2. 避免权限扩大

如果将工作空间级别的用户组改成全局级别，会导致：
- ❌ 该用户组的成员在其他工作空间也能看到数据
- ❌ 权限范围扩大，可能违反安全策略
- ❌ 影响其他工作空间的用户

### 3. 保持业务逻辑一致

MeterSphere 的权限体系基于工作空间隔离：
- 工作空间级别的用户组：只能访问该工作空间下的项目
- 全局级别的用户组：可以访问所有工作空间的项目

迁移后应该保持这个逻辑不变。

## 🔍 验证 scope_id 是否正确

### 迁移前查询

```sql
-- 查询正式环境的用户组 scope_id
SELECT 
    id,
    name,
    `system`,
    scope_id,
    CASE 
        WHEN scope_id = 'global' THEN '全局用户组'
        ELSE CONCAT('工作空间用户组 (', scope_id, ')')
    END as scope_type
FROM `group`
WHERE name IN ('开发人员组', '测试人员组');
```

**示例输出**：
```
id                                   | name         | system | scope_id                             | scope_type
-------------------------------------|--------------|--------|--------------------------------------|------------------
a1b2c3d4-e5f6-7890-abcd-ef1234567890 | 开发人员组   | 0      | 698604cf-d1a4-11f0-a2f8-cead5f5242ae | 工作空间用户组 (698604cf-d1a4-11f0-a2f8-cead5f5242ae)
b2c3d4e5-f6a7-8901-bcde-f12345678901 | 测试人员组   | 0      | global                               | 全局用户组
```

### 迁移后验证

```sql
-- 验证迁移后的 scope_id 是否保持不变
SELECT 
    id,
    name,
    `system`,
    scope_id,
    CASE 
        WHEN scope_id = 'global' THEN '全局用户组'
        ELSE CONCAT('工作空间用户组 (', scope_id, ')')
    END as scope_type
FROM `group`
WHERE id IN ('developer', 'tester');
```

**预期输出**：
```
id        | name         | system | scope_id                             | scope_type
----------|--------------|--------|--------------------------------------|------------------
developer | 开发人员组   | 1      | 698604cf-d1a4-11f0-a2f8-cead5f5242ae | 工作空间用户组 (698604cf-d1a4-11f0-a2f8-cead5f5242ae)
tester    | 测试人员组   | 1      | global                               | 全局用户组
```

**验证要点**：
- ✅ `developer` 的 `scope_id` 应该是原来的工作空间ID
- ✅ `tester` 的 `scope_id` 应该是 `global`
- ✅ 两者的 `scope_id` 应该与迁移前完全一致

## ⚠️ 常见错误

### 错误1：统一改成 global

```sql
-- ❌ 错误做法
INSERT INTO `group` (..., scope_id) VALUES (..., 'global');
INSERT INTO `group` (..., scope_id) VALUES (..., 'global');
```

**后果**：
- 开发人员组变成全局用户组
- 该用户组的成员可以访问所有工作空间
- 权限范围扩大，违反安全策略

### 错误2：硬编码 scope_id

```sql
-- ❌ 错误做法
INSERT INTO `group` (..., scope_id) VALUES (..., '698604cf-d1a4-11f0-a2f8-cead5f5242ae');
```

**后果**：
- 如果正式环境的工作空间ID不同，会导致用户组失效
- 不同环境需要修改脚本，增加维护成本

### 错误3：忘记保存 scope_id

```sql
-- ❌ 错误做法
DELETE FROM `group` WHERE id = @old_developer_id;
-- 没有先保存 scope_id，删除后就找不到了
INSERT INTO `group` (..., scope_id) VALUES (..., ???);
```

**后果**：
- 无法恢复原有的 scope_id
- 用户组作用域丢失

## ✅ 正确的迁移流程

```sql
-- 1. 保存 scope_id（在删除之前）
SET @old_developer_scope_id = (SELECT scope_id FROM `group` WHERE id = @old_developer_id);
SET @old_tester_scope_id = (SELECT scope_id FROM `group` WHERE id = @old_tester_id);

-- 2. 更新关联表
UPDATE user_group SET group_id = 'developer' WHERE group_id = @old_developer_id;
UPDATE user_group SET group_id = 'tester' WHERE group_id = @old_tester_id;
UPDATE user_group_permission SET group_id = 'developer' WHERE group_id = @old_developer_id;
UPDATE user_group_permission SET group_id = 'tester' WHERE group_id = @old_tester_id;

-- 3. 删除旧记录
DELETE FROM `group` WHERE id = @old_developer_id;
DELETE FROM `group` WHERE id = @old_tester_id;

-- 4. 插入新记录（使用保存的 scope_id）
INSERT INTO `group` (..., scope_id) VALUES (..., @old_developer_scope_id);
INSERT INTO `group` (..., scope_id) VALUES (..., @old_tester_scope_id);
```

## 📝 总结

**关键点**：
1. ✅ 迁移脚本会自动保留原有的 `scope_id`
2. ✅ 不会改变用户组的作用域范围
3. ✅ 工作空间级别的用户组仍然是工作空间级别
4. ✅ 全局级别的用户组仍然是全局级别
5. ✅ 用户的权限范围不会扩大或缩小

**验证方法**：
- 迁移前后对比 `scope_id` 字段
- 确保两者完全一致
- 测试用户在不同工作空间的访问权限
