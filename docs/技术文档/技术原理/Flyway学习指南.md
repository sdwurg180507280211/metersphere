# Flyway 学习指南

## 什么是 Flyway？

Flyway 是一个数据库版本控制和迁移工具，它通过版本化的 SQL 脚本来管理数据库结构的变化。

## Flyway 工作原理

### 1. 版本表（Version Table）

Flyway 会在数据库中创建一个**版本表**来记录所有已经执行过的迁移脚本。在你的项目中：

- **project-management 模块**使用 `project_management_version` 表
- **test-track 模块**使用 `track_version` 表
- **system-setting 模块**使用 `metersphere_version` 表
- **api-test 模块**使用 `api_version` 表
- **report-stat 模块**使用 `report_version` 表

### 2. 版本表的结构

Flyway 自动创建的版本表包含以下字段（标准结构）：

```sql
CREATE TABLE project_management_version (
    installed_rank INT NOT NULL,           -- 安装顺序
    version VARCHAR(50),                   -- 版本号（如 V3）
    description VARCHAR(200),              -- 描述（如 2.10.23_release）
    type VARCHAR(20),                      -- 类型（通常是 SQL）
    script VARCHAR(1000) NOT NULL,         -- 脚本文件名
    checksum INT,                          -- 校验和
    installed_by VARCHAR(100),             -- 执行用户
    installed_on TIMESTAMP,                -- 执行时间
    execution_time INT,                    -- 执行耗时（毫秒）
    success BOOLEAN                        -- 是否成功
);
```

### 3. 脚本命名规则

Flyway 脚本必须遵循特定的命名规则：

```
<前缀>__<描述>.sql

例如：
- V3__2.10.23_release.sql
- V4__2.10.23_release.sql
```

- **前缀**：V + 版本号（必须递增）
- **分隔符**：两个下划线 `__`
- **描述**：可选的描述信息

### 4. 执行流程

1. 应用启动时，Flyway 会扫描 `classpath:db/migration` 目录
2. 检查版本表中已执行的脚本
3. 按照版本号顺序执行未执行的脚本
4. 将执行结果记录到版本表中

## 如何重新执行已执行过的 SQL 脚本？

### 方法一：删除版本表中的记录（适用于开发环境）

**是的，你的理解是正确的！** 如果想重新执行某个已执行的脚本，需要从版本表中删除对应的记录。

#### 操作步骤：

1. **查看版本表中的记录**：
```sql
SELECT * FROM project_management_version 
ORDER BY installed_rank;
```

2. **找到对应的脚本记录**，例如 `V3__2.10.23_release.sql`：
```sql
SELECT * FROM project_management_version 
WHERE version = '3' AND description = '2.10.23_release';
```

3. **删除对应的记录**（只删除需要重新执行的）：
```sql
-- 删除 V3 的记录
DELETE FROM project_management_version 
WHERE version = '3' AND description = '2.10.23_release';
```

4. **重启应用**，Flyway 会重新执行这个脚本

#### ⚠️ 注意事项：

- **生产环境请谨慎操作**：删除版本记录可能导致数据不一致
- **回滚脚本的影响**：如果脚本包含不可逆操作（如 DROP TABLE），需要手动处理
- **依赖关系**：如果后续脚本依赖这个脚本的结果，需要一并处理

### 方法二：使用 Flyway 的回滚功能（推荐）

Flyway 支持**可重复迁移脚本**（Repeatable Migrations）：

#### 使用 R__ 前缀：

创建一个可重复执行的脚本：

```
R__update_test_case_template.sql
```

- **前缀**：`R__` 表示可重复执行
- **执行条件**：如果脚本内容发生变化（校验和不同），Flyway 会自动重新执行

#### 示例：

```sql
-- R__update_test_case_template.sql
-- 这个脚本会在内容变化时重新执行
DROP TABLE IF EXISTS temp_test_case;
CREATE TABLE temp_test_case AS SELECT * FROM test_case_template;
```

### 方法三：手动执行 SQL（开发/测试）

如果需要立即测试某个脚本，可以直接在数据库中执行：

```sql
-- 直接执行 SQL 文件内容
SOURCE /path/to/V3__2.10.23_release.sql;
```

但这种方式不会在版本表中记录，下次启动 Flyway 仍会尝试执行。

### 方法四：创建新的修复脚本

**最佳实践**：创建一个新的版本脚本修复问题，而不是重新执行旧脚本。

```
V5__fix_template_issue.sql
```

这样可以：
- 保持历史记录完整
- 便于追踪变更
- 避免破坏已有数据

## 配置文件说明

在 `project-management/backend/src/main/resources/application.properties` 中：

```properties
# flyway
spring.flyway.enabled=true                          # 启用 Flyway
spring.flyway.baseline-on-migrate=true              # 在迁移时创建基线
spring.flyway.locations=classpath:db/migration      # 脚本位置
spring.flyway.table=project_management_version      # 版本表名称
spring.flyway.baseline-version=0                    # 基线版本号
spring.flyway.encoding=UTF-8                        # 编码
spring.flyway.validate-on-migrate=false             # 迁移时是否验证
```

### 重要配置项说明：

- **baseline-on-migrate**：如果数据库已有数据，设置为 `true` 可以创建基线
- **validate-on-migrate**：设置为 `false` 可以跳过校验（不建议在生产环境使用）
- **table**：自定义版本表名称，默认为 `flyway_schema_history`

## 常见问题

### Q1: 脚本执行失败怎么办？

如果脚本执行失败：
1. Flyway 会在版本表中标记为 `success = false`
2. 修复脚本后，需要删除失败的记录
3. 重新启动应用

### Q2: 可以修改已执行的脚本吗？

**不建议修改已执行的脚本**，因为：
- Flyway 通过校验和验证脚本是否被修改
- 修改后可能导致校验失败
- 应该创建新脚本来修复问题

### Q3: 如何回滚？

Flyway **不支持自动回滚**。如果需要回滚：
1. 创建新的回滚脚本（例如：`V6__rollback_v3_changes.sql`）
2. 手动编写反向 SQL
3. 或者手动删除版本记录并恢复数据库状态

### Q4: 开发环境如何快速重置？

```sql
-- 1. 删除版本表中的所有记录
DELETE FROM project_management_version;

-- 2. 或者删除并重建表
DROP TABLE project_management_version;

-- 3. 重启应用，Flyway 会重新创建表并执行所有脚本
```

## 最佳实践

1. **版本号递增**：确保版本号始终递增，不要回退
2. **脚本幂等性**：尽量编写幂等的 SQL（如使用 `IF NOT EXISTS`、`IF EXISTS`）
3. **小步提交**：每次只做少量改动，便于排查问题
4. **测试先行**：在测试环境充分测试后再应用到生产环境
5. **文档记录**：在脚本中添加注释，说明变更原因
6. **备份数据**：执行前备份重要数据

## 参考资料

- [Flyway 官方文档](https://flywaydb.org/documentation/)
- [Spring Boot Flyway 集成](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway)

