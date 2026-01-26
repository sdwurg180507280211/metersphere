# 用户组迁移方案对比

## 背景

正式环境中的"开发人员组"和"测试人员组"是通过页面创建的普通用户组（`system=0`），ID 是随机 UUID。而代码中使用固定 ID（`developer`、`tester`）进行判断，导致权限过滤功能无法生效。

## 方案对比

### 方案1：修改代码适配正式环境

**修改内容**：
1. 修改 `IssuesService.java` 中的 `addUserGroupFilter()` 方法
2. 修改 `ExtIssuesMapper.xml` 中的权限过滤条件
3. 添加配置文件或数据库配置表，存储用户组 ID

**需要修改的文件**：
- `test-track/backend/src/main/java/io/metersphere/service/IssuesService.java`
- `test-track/backend/src/main/java/io/metersphere/base/mapper/ext/ExtIssuesMapper.xml`
- 可能需要新增配置表或配置文件

**修改示例**：
```java
// IssuesService.java
private void addUserGroupFilter(IssuesRequest request) {
    String userId = SessionUtils.getUserId();
    String projectId = request.getProjectId();
    
    if (StringUtils.isBlank(userId) || StringUtils.isBlank(projectId)) {
        return;
    }
    
    // 查询用户在当前项目中的用户组
    String userGroupId = extIssuesMapper.getUserGroupInProject(userId, projectId);
    
    // 从配置中读取需要过滤的用户组 ID
    String developerGroupId = getConfigValue("user.group.developer.id"); // 需要实现
    String testerGroupId = getConfigValue("user.group.tester.id");       // 需要实现
    
    if (developerGroupId.equals(userGroupId) || testerGroupId.equals(userGroupId)) {
        request.setCurrentUserId(userId);
        request.setUserGroupId(userGroupId);
    }
}
```

**优点**：
- 不需要修改数据库
- 灵活性高，可以配置任意用户组

**缺点**：
- 需要修改代码，增加配置管理
- 需要在每个环境配置用户组 ID
- 升级时需要额外处理配置迁移
- 增加维护成本
- 不符合二次开发原则（改动面大）

---

### 方案2：修改正式环境用户组为系统用户组（推荐）

**修改内容**：
1. 在正式环境执行 SQL，将用户组 ID 改为固定值
2. 将用户组标记为系统用户组（`system=1`）

**需要执行的操作**：
1. 查询正式环境的用户组 ID
2. 执行 SQL 脚本（已提供）
3. 验证迁移结果

**优点**：
- ✅ 不需要修改代码
- ✅ 一次性操作，后续无需维护
- ✅ 语义正确（开发/测试人员组本应是系统级别）
- ✅ 符合二次开发原则（改动面小、边界清晰）
- ✅ 升级时无需额外处理
- ✅ 测试环境和正式环境保持一致

**缺点**：
- 需要在正式环境执行 SQL（但只需执行一次）
- 需要停机或在低峰期执行（建议）

---

## 推荐方案：方案2

### 理由

1. **代码稳定性**：不修改代码，避免引入新的 bug
2. **维护成本低**：一次性操作，后续无需维护
3. **语义正确**：开发人员组和测试人员组本身就应该是系统级别的用户组
4. **升级友好**：符合二次开发原则，升级时不需要额外处理
5. **环境一致性**：测试环境和正式环境保持一致，便于测试和部署

### 执行步骤

#### 步骤1：查询正式环境的用户组 ID

```sql
SELECT id, name, `system`, type 
FROM `group` 
WHERE name IN ('开发人员组', '测试人员组');
```

**示例输出**：
```
id                                   | name         | system | type
-------------------------------------|--------------|--------|--------
a1b2c3d4-e5f6-7890-abcd-ef1234567890 | 开发人员组   | 0      | PROJECT
b2c3d4e5-f6a7-8901-bcde-f12345678901 | 测试人员组   | 0      | PROJECT
```

#### 步骤2：修改 SQL 脚本

打开 `MIGRATE_USER_GROUP_SIMPLE.sql`，将：
```sql
SET @old_developer_id = 'YOUR_OLD_DEVELOPER_ID';
SET @old_tester_id = 'YOUR_OLD_TESTER_ID';
```

替换为实际的 ID：
```sql
SET @old_developer_id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';
SET @old_tester_id = 'b2c3d4e5-f6a7-8901-bcde-f12345678901';
```

#### 步骤3：备份数据库

```bash
mysqldump -h 正式环境IP -u root -p metersphere_dev > backup_$(date +%Y%m%d_%H%M%S).sql
```

#### 步骤4：执行迁移脚本

```bash
mysql -h 正式环境IP -u root -p metersphere_dev < MIGRATE_USER_GROUP_SIMPLE.sql
```

#### 步骤5：验证结果

```sql
-- 验证用户组
SELECT id, name, `system`, type, scope_id 
FROM `group` 
WHERE id IN ('developer', 'tester');

-- 验证用户关联
SELECT g.id, g.name, COUNT(ug.user_id) as user_count
FROM `group` g
LEFT JOIN user_group ug ON g.id = ug.group_id
WHERE g.id IN ('developer', 'tester')
GROUP BY g.id, g.name;
```

**预期结果**：
```
id        | name         | system | type    | scope_id
----------|--------------|--------|---------|----------
developer | 开发人员组   | 1      | PROJECT | global
tester    | 测试人员组   | 1      | PROJECT | global
```

#### 步骤6：测试功能

1. 使用开发人员组的用户登录
2. 进入测试跟踪 → 缺陷管理
3. 验证只能看到创建人或处理人是自己的缺陷

---

## 注意事项

### 1. 用户组名称

如果正式环境中的用户组名称不是"开发人员组"和"测试人员组"，需要修改 SQL 中的名称。

可以先查询所有项目类型的用户组：
```sql
SELECT id, name, `system`, type 
FROM `group` 
WHERE type = 'PROJECT' 
ORDER BY name;
```

### 2. 其他表的引用

如果有其他表引用了 `group.id`，也需要一并更新。可以通过以下 SQL 查找：
```sql
SELECT 
    TABLE_NAME, 
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'metersphere_dev'
  AND REFERENCED_TABLE_NAME = 'group';
```

目前已知的引用：
- `user_group.group_id`（已在脚本中处理）

### 3. 执行时机

建议在以下时机执行：
- 业务低峰期（如凌晨）
- 或者短暂停机维护

### 4. 回滚方案

如果迁移后发现问题，可以通过备份恢复：
```bash
mysql -h 正式环境IP -u root -p metersphere_dev < backup_20260126_020000.sql
```

---

## 总结

**推荐使用方案2**，理由如下：

| 维度 | 方案1（修改代码） | 方案2（修改数据库） |
|------|------------------|-------------------|
| 代码改动 | 大 | 无 |
| 维护成本 | 高 | 低 |
| 升级影响 | 需要额外处理 | 无影响 |
| 执行难度 | 中 | 低 |
| 风险 | 中 | 低（可回滚） |
| 语义正确性 | 一般 | 高 |
| 二次开发原则 | 不符合 | 符合 |

**最终建议**：使用方案2，在正式环境执行 SQL 迁移脚本。
