# 用户组权限过滤 - 前端实现方案

## 一、需求说明

### 核心需求
- **开发人员组（developer）**：只能看到**处理人是自己**的缺陷
- **测试人员组（tester）**：只能看到**创建人是自己**的缺陷
- **其他用户组**：没有限制，可以看到所有缺陷

### 触发时机
- **初始化加载**（页面首次打开）：施加权限过滤
- **高级搜索查询**：不施加权限过滤（包括点击"重置"后）
- **快捷筛选**：不施加权限过滤
- **翻页**：继续使用初始化加载时的过滤状态

## 二、实现方案

### 方案选择：纯前端实现

**我在做**：在前端判断用户组和加载类型，动态添加过滤条件到请求参数中。

**目的是**：
1. 后端无需判断是否为初始化加载，逻辑更简单
2. 前端完全控制何时施加权限过滤，灵活性更高
3. 高级搜索"重置"后不会误触发权限过滤

**如果不这样做**：后端无法区分"高级搜索重置"和"初始化加载"，会导致重置后仍然施加权限过滤。

### 核心逻辑

```javascript
// 1. 页面激活时获取用户组
activated() {
  // 获取当前用户在项目中的用户组
  getUserGroupProject(projectId, userId).then(response => {
    const groups = response.data || [];
    // 只关注 developer 和 tester
    const targetGroup = groups.find(g => g.id === 'developer' || g.id === 'tester');
    this.currentUserGroupId = targetGroup ? targetGroup.id : null;
  });
}

// 2. getIssues() 方法根据 isInitialLoad 参数决定是否添加过滤
getIssues(isInitialLoad = false) {
  // 只在初始化加载且用户属于特定用户组时添加过滤
  if (isInitialLoad && this.currentUserGroupId) {
    if (this.currentUserGroupId === 'developer') {
      // 开发人员：只看处理人是自己的
      this.page.condition.currentUserId = getCurrentUserId();
      this.page.condition.userGroupId = 'developer';
    } else if (this.currentUserGroupId === 'tester') {
      // 测试人员：只看创建人是自己的
      this.page.condition.currentUserId = getCurrentUserId();
      this.page.condition.userGroupId = 'tester';
    }
  } else {
    // 非初始化加载或非特定用户组，清除过滤条件
    delete this.page.condition.currentUserId;
    delete this.page.condition.userGroupId;
  }
  
  // 发起请求
  getIssues(this.page.currentPage, this.page.pageSize, this.page.condition)
    .then(response => {
      // 处理响应
    });
}

// 3. 不同场景的调用
activated() {
  this.getIssues(true);  // 初始化加载，施加权限过滤
}

search() {
  this.getIssues(false); // 搜索，不施加权限过滤
}

handlePageChange() {
  // 翻页时保持当前过滤状态（不传参数，使用已有的 condition）
  this.getIssues(false);
}
```

## 三、修改清单

### 1. IssueList.vue - data() 部分

**位置**：`test-track/frontend/src/business/issue/IssueList.vue` 第 266 行

**修改内容**：增加用户组相关字段

```javascript
data() {
  return {
    // ... 现有字段 ...
    
    // 用户组权限过滤相关（新增）
    currentUserGroupId: null, // 当前用户在项目中的用户组ID
  };
}
```

### 2. IssueList.vue - activated() 钩子

**位置**：`test-track/frontend/src/business/issue/IssueList.vue` 第 360 行

**修改内容**：
1. 导入 `getUserGroupProject` API
2. 在 `activated()` 中获取用户组
3. 修改 `getIssues()` 调用，传入 `true` 表示初始化加载

```javascript
// 在 import 区域添加
import {getUserGroupProject} from 'project-management-frontend/src/api/user-group';

// 在 activated() 中添加（在 getProjectMember() 之前）
activated() {
  // ... 现有代码 ...
  
  // 获取当前用户在项目中的用户组
  getUserGroupProject(getCurrentProjectID(), getCurrentUserId())
    .then((response) => {
      const groups = response.data || [];
      // 只关注 developer 和 tester
      const targetGroup = groups.find(g => g.id === 'developer' || g.id === 'tester');
      this.currentUserGroupId = targetGroup ? targetGroup.id : null;
    })
    .catch(() => {
      // 获取失败时不施加权限过滤
      this.currentUserGroupId = null;
    });
  
  // ... 现有的 getProjectMember() 代码 ...
  getIssuePartTemplateWithProject((template) => {
    this.initFields(template);
    // 修改这里：传入 true 表示初始化加载
    this.getIssues(true);
  }, () => {
    this.loading = false;
  });
}
```

### 3. IssueList.vue - getIssues() 方法

**位置**：`test-track/frontend/src/business/issue/IssueList.vue` 第 550 行

**修改内容**：增加 `isInitialLoad` 参数，根据参数和用户组动态添加过滤条件

```javascript
/**
 * 我在做：根据加载类型和用户组动态添加权限过滤条件
 * 目的是：
 *   1. 初始化加载时，开发人员和测试人员只看到与自己相关的缺陷
 *   2. 高级搜索时，不施加权限限制，可以搜索所有缺陷
 * 如果不这样做：高级搜索"重置"后会误触发权限过滤，用户无法看到所有数据
 * 
 * @param {Boolean} isInitialLoad - 是否为初始化加载（true=初始化，false=搜索/筛选）
 */
getIssues(isInitialLoad = false) {
  this.loading = true;
  
  // 根据加载类型和用户组决定是否添加权限过滤
  if (isInitialLoad && this.currentUserGroupId) {
    // 初始化加载且用户属于特定用户组，添加过滤条件
    this.page.condition.currentUserId = getCurrentUserId();
    this.page.condition.userGroupId = this.currentUserGroupId;
  } else {
    // 非初始化加载或非特定用户组，清除过滤条件
    delete this.page.condition.currentUserId;
    delete this.page.condition.userGroupId;
  }
  
  // ... 现有的数据加载逻辑 ...
  if (this.dataSelectRange === 'thisWeekUnClosedIssue') {
    this.page.condition.thisWeekUnClosedTestPlanIssue = true;
  } else if (this.dataSelectRange === 'unClosedRelatedTestPlan') {
    this.page.condition.unClosedTestPlanIssue = true;
  } else if (this.dataSelectRange === 'AllRelatedTestPlan') {
    this.page.condition.allTestPlanIssue = true;
  } else {
    delete this.page.condition['thisWeekUnClosedTestPlanIssue'];
    delete this.page.condition['unClosedTestPlanIssue'];
    delete this.page.condition['allTestPlanIssue'];
  }
  
  this.page.condition.projectId = this.projectId;
  this.page.condition.workspaceId = this.workspaceId;
  this.page.condition.orders = getLastTableSortField(this.tableHeaderKey);
  
  getIssues(this.page.currentPage, this.page.pageSize, this.page.condition)
    .then((response) => {
      this.page.total = response.data.itemCount;
      this.page.data = response.data.listObject;
      parseCustomFilesForList(this.page.data);
      this.initCustomFieldValue();
      if (this.pageRefresh) {
        this.$nextTick(() => {
          this.pageRefresh = false;
        });
      }
      this.loading = false;
    });
}
```

### 4. IssueList.vue - search() 方法

**位置**：`test-track/frontend/src/business/issue/IssueList.vue` 第 540 行

**修改内容**：传入 `false` 表示非初始化加载

```javascript
search() {
  // 添加搜索条件时，当前页设置成第一页
  this.page.currentPage = 1;
  this.pageRefresh = false;
  // 传入 false 表示这是搜索，不施加权限过滤
  this.getIssues(false);
}
```

### 5. IssueList.vue - handlePageChange() 方法

**位置**：`test-track/frontend/src/business/issue/IssueList.vue` 第 548 行

**修改内容**：翻页时不传参数（保持当前过滤状态）

```javascript
/**
 * 我在做：处理分页翻页事件，并标记 pageRefresh。
 * 目的是：让 MsTable 在翻页加载时不清空跨页勾选。
 * 如果不这样做，就无法实现：翻页后仍保留之前页的选中状态。
 */
handlePageChange() {
  this.pageRefresh = true;
  // 翻页时不传参数，保持当前的过滤状态（可能是初始化过滤，也可能是搜索状态）
  this.getIssues(false);
}
```

## 四、后端配套修改

### 1. ExtIssuesMapper.xml - SQL 过滤逻辑

**位置**：`test-track/backend/src/main/java/io/metersphere/base/mapper/ext/ExtIssuesMapper.xml`

**修改内容**：根据前端传入的 `userGroupId` 和 `currentUserId` 施加过滤

```xml
<sql id="queryWhereCondition">
  <where>
    <!-- 用户组权限过滤（由前端控制何时传入参数） -->
    <if test="request.userGroupId != null and request.currentUserId != null">
      <!-- 开发人员组：只能看到处理人是自己的缺陷 -->
      <if test="request.userGroupId == 'developer'">
        and exists (
          select 1 from custom_field_issues cfi
          inner join custom_field cf on cfi.field_id = cf.id
          where cfi.resource_id = issues.id
            and cf.name = '处理人'
            and cf.scene = 'ISSUE'
            and cf.system = 1
            and (cf.project_id = issues.project_id or cf.global = 1)
            and trim(both '"' from cfi.value) = #{request.currentUserId}
        )
      </if>
      
      <!-- 测试人员组：只能看到创建人是自己的缺陷 -->
      <if test="request.userGroupId == 'tester'">
        and issues.creator = #{request.currentUserId}
      </if>
    </if>
    
    <!-- ... 其他现有的查询条件 ... -->
  </where>
</sql>
```

### 2. IssuesRequest.java - DTO 字段

**位置**：`framework/sdk-parent/xpack-interface/src/main/java/io/metersphere/xpack/track/dto/request/IssuesRequest.java`

**修改内容**：保留现有的 `currentUserId` 和 `userGroupId` 字段，删除 `isInitialLoad` 字段

```java
/**
 * 当前用户ID（用于用户组权限过滤）
 */
private String currentUserId;

/**
 * 用户组ID（用于用户组权限过滤，如 'developer', 'tester'）
 */
private String userGroupId;
```

## 五、实现效果

| 场景 | 前端行为 | 后端行为 | 开发人员看到 | 测试人员看到 |
|------|---------|---------|------------|------------|
| **初始化加载** | 传入 `userGroupId` 和 `currentUserId` | 施加权限过滤 | 处理人是自己的缺陷 | 创建人是自己的缺陷 |
| **高级搜索查询** | 不传 `userGroupId` 和 `currentUserId` | 不施加权限过滤 | 所有符合条件的缺陷 | 所有符合条件的缺陷 |
| **高级搜索重置** | 不传 `userGroupId` 和 `currentUserId` | 不施加权限过滤 | 所有缺陷 | 所有缺陷 |
| **快捷筛选** | 不传 `userGroupId` 和 `currentUserId` | 不施加权限过滤 | 所有符合条件的缺陷 | 所有符合条件的缺陷 |
| **翻页** | 保持当前过滤状态 | 根据参数决定 | 保持当前过滤结果 | 保持当前过滤结果 |

## 六、技术要点

### 1. 前端控制权限过滤时机

**我在做**：在前端通过 `isInitialLoad` 参数控制何时添加权限过滤条件。

**目的是**：让前端完全掌控权限过滤的触发时机，后端只负责执行过滤逻辑。

**如果不这样做**：后端无法区分"高级搜索重置"和"初始化加载"，会导致逻辑混乱。

### 2. 用户组获取时机

**我在做**：在 `activated()` 钩子中获取用户组，而不是在每次 `getIssues()` 时获取。

**目的是**：减少 API 调用次数，提升性能。

**如果不这样做**：每次加载数据都要先查询用户组，会导致请求数量翻倍。

### 3. 翻页时的过滤状态保持

**我在做**：翻页时传入 `false`，不重新判断是否为初始化加载。

**目的是**：翻页时保持当前的过滤状态（可能是初始化过滤，也可能是搜索状态）。

**如果不这样做**：翻页后会丢失当前的过滤状态，用户体验差。

## 七、优势分析

### 相比方案二（后端判断）的优势

1. **逻辑清晰**：前端明确控制何时施加权限过滤，后端只负责执行
2. **易于调试**：前端可以直接看到传入的参数，后端不需要复杂的判断逻辑
3. **可扩展性强**：如需增加新的过滤场景，只需在前端调整调用参数即可
4. **避免误判**：不会出现"高级搜索重置"被误判为"初始化加载"的情况

### 二次开发友好性

1. **改动面小**：只修改 `IssueList.vue` 一个文件的几个方法
2. **边界清晰**：权限过滤逻辑集中在 `getIssues()` 方法中
3. **可回滚**：升级时只需对照 `getIssues()` 方法的变化即可快速搬运

## 八、测试验证

### 测试场景

1. **开发人员初始化加载**：只看到处理人是自己的缺陷
2. **测试人员初始化加载**：只看到创建人是自己的缺陷
3. **高级搜索查询**：可以搜索到所有符合条件的缺陷
4. **高级搜索重置**：可以看到所有缺陷
5. **快捷筛选**：可以筛选到所有符合条件的缺陷
6. **翻页**：保持当前的过滤状态

### 验证方法

1. 使用开发人员账号登录，查看缺陷列表
2. 使用测试人员账号登录，查看缺陷列表
3. 使用高级搜索，验证可以搜索到其他人的缺陷
4. 点击"重置"按钮，验证可以看到所有缺陷
5. 使用快捷筛选，验证可以筛选到所有符合条件的缺陷
6. 翻页，验证过滤状态保持不变
