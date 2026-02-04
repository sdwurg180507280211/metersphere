# 数据库

本目录集中管理数据库相关资源，包括表结构文档、SQL脚本和迁移脚本。

## 📁 目录结构

### 表结构文档
数据库设计文档和表结构说明。

- 数据库表结构说明
- 功能用例模块树递归机制
- 迁移脚本分类分析
- 数据库隔离机制

### SQL脚本
数据修复、功能实现、测试用的SQL脚本。

**脚本分类**：
- 数据修复脚本
- 功能实现脚本
- 测试数据脚本
- 统计查询脚本

**现有脚本**：
- 工作空间项目顶级模块统计.sql
- 创建权限层级用户.sql
- 清理重复字段关联.sql
- 执行清理重复字段.sql
- 全局模板和字段异常数据修复.sql
- 删除自定义字段.sql
- 银保通项目状态字段重复问题修复.sql
- 用户组迁移相关脚本

### 迁移脚本
Flyway 数据库迁移脚本说明和管理。

## 🎯 快速查找

### 我想了解表结构
👉 `表结构文档/数据库表结构说明.md`

### 我想修复数据问题
👉 `SQL脚本/` 目录，查找对应的修复脚本

### 我想了解迁移机制
👉 `表结构文档/迁移脚本分类分析.md`

### 我想了解数据库隔离
👉 `表结构文档/数据库隔离/`

## 📝 SQL脚本使用规范

### 执行前检查
```bash
# 1. 备份数据库
mysqldump -u root -p metersphere_dev > backup_$(date +%Y%m%d_%H%M%S).sql

# 2. 在测试环境先执行
mysql -u root -p metersphere_test < script.sql

# 3. 验证结果
mysql -u root -p metersphere_test -e "SELECT COUNT(*) FROM affected_table;"

# 4. 生产环境执行
mysql -u root -p metersphere_prod < script.sql
```

### 脚本命名规范
- 功能描述清晰
- 包含日期或版本号
- 使用中文命名（便于理解）

示例：
- `清理重复字段关联.sql`
- `2025-01-30_修复用户组权限.sql`
- `统计工作空间项目数据.sql`

### 脚本编写规范
```sql
-- ==========================================
-- 脚本名称：清理重复字段关联
-- 创建日期：2025-01-30
-- 作者：开发者姓名
-- 用途：清理自定义字段表中的重复关联数据
-- 影响范围：custom_field_template 表
-- 注意事项：执行前请备份数据库
-- ==========================================

-- 1. 查看重复数据
SELECT field_id, template_id, COUNT(*) as cnt
FROM custom_field_template
GROUP BY field_id, template_id
HAVING cnt > 1;

-- 2. 删除重复数据（保留ID最小的）
DELETE t1 FROM custom_field_template t1
INNER JOIN custom_field_template t2
WHERE t1.field_id = t2.field_id
  AND t1.template_id = t2.template_id
  AND t1.id > t2.id;

-- 3. 验证结果
SELECT COUNT(*) as total_count FROM custom_field_template;
```

## 💡 数据库开发建议

### 表设计原则
1. 遵循第三范式
2. 合理使用索引
3. 字段命名清晰
4. 添加必要注释

### 查询优化
1. 避免 SELECT *
2. 使用索引字段
3. 合理使用 JOIN
4. 分页查询大数据量

### 数据安全
1. 生产环境操作前必须备份
2. 使用事务保证数据一致性
3. 敏感数据加密存储
4. 定期清理历史数据

## 🔗 相关文档

### 数据库连接配置
参考：`02-开发指南/环境搭建/`

### 数据库隔离机制
参考：`04-技术架构/数据库设计/`

### Flyway 迁移
参考：`04-技术架构/微服务架构/Flyway学习指南.md`

### 数据库性能优化
参考：`05-部署运维/性能优化/`

## 📊 常用数据库操作

### 连接数据库
```bash
# 开发环境
mysql -h localhost -u root -p'Password123@mysql' -e "USE metersphere_dev;"

# 查看所有数据库
SHOW DATABASES;

# 查看所有表
SHOW TABLES;

# 查看表结构
DESC table_name;
```

### 常用查询
```sql
-- 查看表数据量
SELECT TABLE_NAME, TABLE_ROWS
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'metersphere_dev'
ORDER BY TABLE_ROWS DESC;

-- 查看表索引
SHOW INDEX FROM table_name;

-- 查看表创建语句
SHOW CREATE TABLE table_name;

-- 搜索包含某字段的表
SELECT TABLE_NAME, COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'metersphere_dev'
AND COLUMN_NAME LIKE '%keyword%';
```

---

**注意**：所有数据库操作请在测试环境验证后再在生产环境执行！
