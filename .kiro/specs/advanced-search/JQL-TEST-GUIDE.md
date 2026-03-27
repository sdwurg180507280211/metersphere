# JQL功能测试指南

## 测试准备

### 1. 启动服务
```bash
# 启动workstation服务
cd workstation/backend
mvn spring-boot:run
```

### 2. 准备测试数据
确保数据库中有以下测试数据：
- 测试用例（test_case表）
- 缺陷（issues表）
- 测试计划（test_plan表）
- 用例评审（test_case_review表）

---

## 测试用例

### 测试1：基础等值查询

**JQL查询：**
```sql
status = "Pass"
```

**预期SQL：**
```sql
test_case.status = 'Pass'
```

**测试方法：**
```bash
curl -X POST http://localhost:8007/workstation/advanced-search/query/1/10 \
  -H "Content-Type: application/json" \
  -d '{
    "module": "TEST_CASE",
    "useJQL": true,
    "jql": "status = \"Pass\"",
    "workspaceIds": [],
    "projectIds": []
  }'
```

---

### 测试2：AND逻辑组合

**JQL查询：**
```sql
status = "Pass" AND priority = "P0"
```

**预期SQL：**
```sql
(test_case.status = 'Pass' AND test_case.priority = 'P0')
```

**测试方法：**
```bash
curl -X POST http://localhost:8007/workstation/advanced-search/query/1/10 \
  -H "Content-Type: application/json" \
  -d '{
    "module": "TEST_CASE",
    "useJQL": true,
    "jql": "status = \"Pass\" AND priority = \"P0\"",
    "workspaceIds": [],
    "projectIds": []
  }'
```

---

### 测试3：OR逻辑组合

**JQL查询：**
```sql
priority = "P0" OR priority = "P1"
```

**预期SQL：**
```sql
(test_case.priority = 'P0' OR test_case.priority = 'P1')
```

---

### 测试4：IN列表查询

**JQL查询：**
```sql
status IN ("Pass", "Prepare", "Underway")
```

**预期SQL：**
```sql
test_case.status IN ('Pass', 'Prepare', 'Underway')
```

---

### 测试5：NOT IN列表查询

**JQL查询：**
```sql
priority NOT IN ("P3", "P4")
```

**预期SQL：**
```sql
test_case.priority NOT IN ('P3', 'P4')
```

---

### 测试6：模糊查询（CONTAINS）

**JQL查询：**
```sql
name CONTAINS "登录"
```

**预期SQL：**
```sql
test_case.name LIKE '%登录%'
```

---

### 测试7：比较操作符

**JQL查询：**
```sql
createTime >= "2024-01-01" AND updateTime <= "2024-12-31"
```

**预期SQL：**
```sql
(test_case.create_time >= '2024-01-01' AND test_case.update_time <= '2024-12-31')
```

---

### 测试8：复杂括号组合

**JQL查询：**
```sql
(priority = "P0" OR priority = "P1") AND status != "Deprecated"
```

**预期SQL：**
```sql
((test_case.priority = 'P0' OR test_case.priority = 'P1') AND test_case.status != 'Deprecated')
```

---

### 测试9：多层嵌套

**JQL查询：**
```sql
((priority = "P0" OR priority = "P1") AND status = "Pass") OR (priority = "P2" AND status = "Prepare")
```

**预期SQL：**
```sql
(((test_case.priority = 'P0' OR test_case.priority = 'P1') AND test_case.status = 'Pass') OR (test_case.priority = 'P2' AND test_case.status = 'Prepare'))
```

---

## 缺陷模块测试

### 测试10：缺陷查询

**JQL查询：**
```sql
status = "new" AND platform = "Jira"
```

**测试方法：**
```bash
curl -X POST http://localhost:8007/workstation/advanced-search/query/1/10 \
  -H "Content-Type: application/json" \
  -d '{
    "module": "ISSUE",
    "useJQL": true,
    "jql": "status = \"new\" AND platform = \"Jira\"",
    "workspaceIds": [],
    "projectIds": []
  }'
```

---

## 测试计划模块测试

### 测试11：测试计划查询

**JQL查询：**
```sql
status = "Underway" AND stage = "smoke"
```

**测试方法：**
```bash
curl -X POST http://localhost:8007/workstation/advanced-search/query/1/10 \
  -H "Content-Type: application/json" \
  -d '{
    "module": "TEST_PLAN",
    "useJQL": true,
    "jql": "status = \"Underway\" AND stage = \"smoke\"",
    "workspaceIds": [],
    "projectIds": []
  }'
```

---

## 用例评审模块测试

### 测试12：用例评审查询

**JQL查询：**
```sql
status = "Underway"
```

**测试方法：**
```bash
curl -X POST http://localhost:8007/workstation/advanced-search/query/1/10 \
  -H "Content-Type: application/json" \
  -d '{
    "module": "TEST_CASE_REVIEW",
    "useJQL": true,
    "jql": "status = \"Underway\"",
    "workspaceIds": [],
    "projectIds": []
  }'
```

---

## SQL注入安全测试

### 测试13：单引号转义

**JQL查询：**
```sql
name = "test's case"
```

**预期SQL：**
```sql
test_case.name = 'test''s case'
```

**说明：** 单引号应该被转义为两个单引号

---

### 测试14：反斜杠转义

**JQL查询：**
```sql
name = "test\\case"
```

**预期SQL：**
```sql
test_case.name = 'test\\\\case'
```

**说明：** 反斜杠应该被转义为两个反斜杠

---

## 错误处理测试

### 测试15：非法字段名

**JQL查询：**
```sql
invalidField = "value"
```

**预期结果：** 抛出异常 "未知的字段名: invalidField"

---

### 测试16：非法操作符

**JQL查询：**
```sql
status === "Pass"
```

**预期结果：** 词法分析失败或语法解析失败

---

### 测试17：括号不匹配

**JQL查询：**
```sql
(status = "Pass" AND priority = "P0"
```

**预期结果：** 抛出异常 "缺少右括号"

---

## 缓存测试

### 测试18：缓存命中

**步骤：**
1. 第一次执行查询：`status = "Pass"`
2. 第二次执行相同查询：`status = "Pass"`
3. 检查日志，确认第二次查询使用了缓存

**预期结果：** 第二次查询不应该重新解析JQL

---

## 性能测试

### 测试19：复杂查询性能

**JQL查询：**
```sql
((priority = "P0" OR priority = "P1") AND (status = "Pass" OR status = "Prepare")) OR ((priority = "P2" OR priority = "P3") AND (status = "Underway" OR status = "Completed"))
```

**测试方法：**
- 记录查询执行时间
- 对比JQL模式和combine模式的性能差异

---

## 验证清单

- [ ] 所有基础操作符测试通过（=, !=, ~, >, <, >=, <=）
- [ ] 列表操作符测试通过（IN, NOT IN）
- [ ] 逻辑操作符测试通过（AND, OR）
- [ ] 括号分组测试通过
- [ ] 4个业务模块测试通过（TEST_CASE, ISSUE, TEST_PLAN, TEST_CASE_REVIEW）
- [ ] SQL注入防护测试通过
- [ ] 错误处理测试通过
- [ ] 缓存机制测试通过
- [ ] 性能测试通过

---

## 调试技巧

### 1. 查看生成的SQL

在AdvancedSearchService.query方法中添加日志：
```java
log.info("Generated SQL WHERE clause: {}", request.getJqlWhereClause());
```

### 2. 查看AST结构

在JQLParser.parseJQL方法中添加日志：
```java
log.info("Parsed AST: {}", ast);
```

### 3. 查看缓存命中情况

在JQLCacheService中添加日志：
```java
log.info("Cache hit for JQL: {}", jql);
log.info("Cache miss for JQL: {}", jql);
```

---

## 常见问题

### Q1: 查询返回空结果
**原因：** 可能是字段名映射错误或数据库中没有匹配的数据
**解决：** 检查字段名映射表，确认数据库中有测试数据

### Q2: SQL语法错误
**原因：** 可能是SQL转义不正确
**解决：** 检查escapeSQLValue方法的实现

### Q3: 缓存不生效
**原因：** 可能是缓存键生成不一致
**解决：** 检查JQLCacheService.generateCacheKey方法

---

## 下一步

测试通过后，可以继续实现：
1. 详情查询功能
2. JQL智能提示
3. 前端JQL编辑器
4. Excel导出功能
