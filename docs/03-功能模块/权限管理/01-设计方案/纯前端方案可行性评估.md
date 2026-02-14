# 纯前端方案可行性评估报告

## 📋 需求回顾

**原始需求**：
- 初始化加载时：满足一定的用户组（developer/tester），则施加查询条件
- 高级搜索时：不限制

**用户期望**：
- 撤销后端修改
- 只通过前端修改实现功能

## ❌ 评估结论：纯前端方案 **不可行**

### 核心问题

**前端无法获取当前用户的用户组信息**

### 详细分析

#### 1. 前端缺少用户组查询接口

通过代码搜索发现：
- ✅ 前端有 `getCurrentUser()` 方法（来自 `metersphere-frontend/src/utils/token`）
- ✅ 前端有 `getUserGroupList()` 等方法（用于用户组管理页面）
- ❌ **但这些方法都不能获取"当前用户在当前项目中的用户组"**

**getCurrentUser() 返回的数据**：
```javascript
{
  id: "用户ID",
  name: "用户名",
  email: "邮箱",
  userGroups: [
    {
      sourceId: "项目ID",
      // 但没有 groupId 字段！
    }
  ]
}
```

**问题**：
- `userGroups` 数组只包含 `sourceId`（项目ID），不包含 `groupId`（用户组ID）
- 无法判断用户在项目中属于哪个用户组（developer/tester/admin）

#### 2. 用户组信息存储在后端数据库

用户组信息存储在 `user_group` 表中：

```sql
SELECT ug.user_id, ug.source_id, ug.group_id, g.id, g.name
FROM user_group ug
INNER JOIN `group` g ON ug.group_id = g.id
WHERE ug.user_id = '当前用户ID'
  AND ug.source_id = '当前项目ID';
```

**关键字段**：
- `user_id`：用户ID
- `source_id`：项目ID
- `group_id`：用户组ID（如 `developer`、`tester`）

**前端无法直接访问数据库**，必须通过后端接口查询。

#### 3. 前端无法实现权限过滤逻辑

即使前端能获取用户组信息，也无法实现权限过滤：

**原因**：
- 权限过滤需要在 SQL 层面实现（`WHERE` 条件）
- 前端只能过滤已经返回的数据，无法控制 SQL 查询
- 如果后端返回全量数据，前端再过滤：
  - ❌ 分页会失效（前端只能过滤当前页的数据）
  - ❌ 性能问题（后端查询全量数据，前端再过滤）
  - ❌ 安全问题（前端可以绕过过滤，看到所有数据）

**示例**：
```javascript
// ❌ 错误的前端过滤方案
getIssues() {
  // 1. 后端返回全量数据（1000 条）
  getIssues(this.page.currentPage, this.page.pageSize, this.page.condition)
    .then((response) => {
      let allData = response.data.listObject; // 1000 条
      
      // 2. 前端过滤（只保留创建人或处理人是自己的）
      let filteredData = allData.filter(issue => {
        return issue.creator === this.currentUserId 
            || issue.handler === this.currentUserId;
      }); // 假设过滤后只剩 50 条
      
      // 3. 问题：
      // - 分页失效：前端只能显示当前页的 10 条，但实际应该显示 50 条中的 10 条
      // - 性能问题：后端查询了 1000 条，但前端只用了 50 条
      // - 安全问题：前端可以绕过过滤，看到所有 1000 条数据
    });
}
```

#### 4. 前端无法区分"初始化加载"和"高级搜索"

**用户需求**：
- 初始化加载：施加权限过滤
- 高级搜索：不施加权限过滤

**问题**：
- 前端的 `getIssues()` 方法对两种场景使用同一个接口
- 区别只在于 `condition.combine` 是否有值
- 但后端无法通过 `combine` 判断是"初始化加载"还是"高级搜索"

**原因**：
- 高级搜索也可能不传 `combine`（用户没有选择任何高级搜索条件）
- 初始化加载也可能传 `combine`（用户之前保存了高级搜索条件）

**解决方案**：
- 必须在前端显式传递一个标识（如 `isInitialLoad`）
- 但这需要修改后端接口，无法纯前端实现

### 为什么后端方案是必须的

#### 1. 用户组查询必须在后端

```java
// 后端查询用户组
String userGroupId = extIssuesMapper.getUserGroupInProject(userId, projectId);
```

**原因**：
- 用户组信息存储在数据库中
- 前端无法直接访问数据库
- 必须通过后端接口查询

#### 2. 权限过滤必须在 SQL 层面

```xml
<!-- 后端 SQL 权限过滤 -->
<if test="request.userGroupId == 'developer' or request.userGroupId == 'tester'">
    and (
        issues.creator = #{request.currentUserId}
        or exists (
            select 1 from custom_field_issues cfi
            where cfi.resource_id = issues.id
              and trim(both '"' from cfi.value) = #{request.currentUserId}
        )
    )
</if>
```

**原因**：
- 权限过滤需要在 SQL 查询时就过滤掉不符合条件的数据
- 这样才能保证分页正确、性能最优、安全可靠
- 前端过滤无法实现这些目标

#### 3. 区分场景必须通过显式标识

```java
// 后端判断是否是初始化加载
if (request.isInitialLoad()) {
    // 施加权限过滤
    addUserGroupFilter(request);
}
```

**原因**：
- 前端无法通过 `combine` 等隐式条件判断场景
- 必须显式传递 `isInitialLoad` 标识
- 这需要修改后端接口，无法纯前端实现

## ✅ 推荐方案：保留后端修改 + 前端增加场景标识

### 方案概述

**后端**：
- 保留 `getUserGroupInProject()` 查询
- 保留 SQL 权限过滤逻辑
- 增加 `isInitialLoad` 判断

**前端**：
- 在 `getIssues()` 调用前设置 `condition.isInitialLoad`
- 初始化加载时：`condition.isInitialLoad = true`
- 高级搜索时：`condition.isInitialLoad = false`

### 实现步骤

#### 1. 后端修改（IssuesRequest.java）

```java
/**
 * 是否是初始化加载（用于区分初始化加载和高级搜索）
 */
private Boolean isInitialLoad;
```

#### 2. 后端修改（IssuesController.java）

```java
@PostMapping("/list/{goPage}/{pageSize}")
public Pager<List<IssuesDao>> list(@PathVariable int goPage, @PathVariable int pageSize, 
                                    @RequestBody IssuesRequest request) {
    issuesService.setFilterParam(request);
    
    // 只在初始化加载时添加用户组权限过滤
    if (isInitialLoadRequest(request)) {
        issuesService.addUserGroupFilter(request);
    }
    
    Page<List<Issues>> page = PageHelper.startPage(goPage, pageSize, true);
    return PageUtils.setPageInfo(page, issuesService.list(request));
}

/**
 * 判断是否是初始化加载请求
 */
private boolean isInitialLoadRequest(IssuesRequest request) {
    // 显式标识优先
    if (request.getIsInitialLoad() != null) {
        return request.getIsInitialLoad();
    }
    
    // 兜底逻辑：没有高级搜索条件 = 初始化加载
    return request.getCombine() == null || request.getCombine().isEmpty();
}
```

#### 3. 前端修改（IssueList.vue）

```javascript
getIssues() {
  this.loading = true;
  
  // 判断是否是初始化加载
  let isInitialLoad = !this.page.condition.combine || 
                      Object.keys(this.page.condition.combine).length === 0;
  
  // 设置标识
  this.page.condition.isInitialLoad = isInitialLoad;
  
  // 其他逻辑保持不变
  this.page.condition.projectId = this.projectId;
  this.page.condition.workspaceId = this.workspaceId;
  this.page.condition.orders = getLastTableSortField(this.tableHeaderKey);
  
  getIssues(this.page.currentPage, this.page.pageSize, this.page.condition)
    .then((response) => {
      // ...
    });
}
```

### 方案优势

1. **改动面小**：
   - 后端只增加一个字段和一个判断方法
   - 前端只增加一行代码设置标识
   - 不影响现有逻辑

2. **边界清晰**：
   - 通过 `isInitialLoad` 明确区分两种场景
   - 不依赖隐式条件判断

3. **可回滚**：
   - 如果需要回滚，只需删除 `isInitialLoad` 相关代码
   - 不影响其他功能

4. **性能最优**：
   - 权限过滤在 SQL 层面实现
   - 分页正确、性能最优

5. **安全可靠**：
   - 前端无法绕过权限过滤
   - 后端强制执行权限控制

## 📊 方案对比

| 方案 | 可行性 | 改动面 | 性能 | 安全性 | 可维护性 |
|------|--------|--------|------|--------|----------|
| 纯前端方案 | ❌ 不可行 | 小 | 差 | 差 | 差 |
| 后端 + 前端方案 | ✅ 可行 | 中 | 优 | 优 | 优 |

## 🎯 最终建议

**强烈建议采用"后端 + 前端方案"**：

1. **纯前端方案不可行**：
   - 前端无法获取用户组信息
   - 前端无法实现 SQL 层面的权限过滤
   - 前端无法保证分页正确和性能最优

2. **后端方案是必须的**：
   - 用户组查询必须在后端
   - 权限过滤必须在 SQL 层面
   - 区分场景必须通过显式标识

3. **改动面可控**：
   - 后端只增加一个字段和一个判断方法
   - 前端只增加一行代码设置标识
   - 符合二次开发原则（改动面小、边界清晰、可回滚）

## 📝 总结

**纯前端方案不可行的根本原因**：
- 前端无法获取用户组信息（数据存储在后端数据库）
- 前端无法实现 SQL 层面的权限过滤（必须在后端实现）
- 前端过滤会导致分页失效、性能问题、安全问题

**推荐方案**：
- 保留后端修改（用户组查询 + SQL 权限过滤）
- 前端增加场景标识（`isInitialLoad`）
- 后端根据标识决定是否施加权限过滤

**符合二次开发原则**：
- ✅ 改动面小：只增加一个字段和一个判断方法
- ✅ 边界清晰：通过 `isInitialLoad` 明确区分场景
- ✅ 可回滚：删除相关代码即可回滚

---

**评估完成！** 🎉
