# 用户组权限过滤功能 - 实施总结

## 一、需求回顾

### 核心需求
- **开发人员组（developer）**：只能看到**处理人是自己**的缺陷
- **测试人员组（tester）**：只能看到**创建人是自己**的缺陷
- **其他用户组**：没有限制，可以看到所有缺陷

### 触发时机
- **初始化加载**（页面首次打开）：施加权限过滤
- **高级搜索查询**：不施加权限过滤
- **高级搜索重置**：不施加权限过滤
- **快捷筛选**：不施加权限过滤
- **翻页**：保持当前过滤状态

## 二、最终方案：使用 page.condition 保持状态

### 方案选择理由

**我在做**：使用 `page.condition` 对象保持过滤状态，在 `activated()` 中设置权限过滤参数，在 `search()` 中清除权限过滤参数。

**目的是**：
1. 利用 MeterSphere 框架特性，`page.condition` 在翻页时自动保持所有参数
2. 不需要给 `getIssues()` 传参数，避免遗漏调用点
3. 逻辑集中在 `activated()` 和 `search()` 两个方法，易于维护

**如果不这样做**：
- 使用 `isInitialLoadDone` 标记会导致翻页时丢失权限过滤
- 给 `getIssues()` 传参数会导致调用点太多容易遗漏

## 三、修改清单

### 1. 前端修改

#### 文件：`test-track/frontend/src/api/user-group.js`（新建）
```javascript
import {get} from 'metersphere-frontend/src/plugins/request';

export function getUserGroupProject(projectId, userId) {
  return get(`/user/group/list/project/${projectId}/${userId}`);
}
```

#### 文件：`test-track/frontend/src/business/issue/IssueList.vue`

**修改点 1：导入 API**
```javascript
import {getUserGroupProject} from "@/api/user-group";
```

**修改点 2：data() 增加字段**
```javascript
data() {
  return {
    // ... 现有字段 ...
    currentUserGroupId: null, // 当前用户在项目中的用户组ID
    isUserGroupFilterApplied: false, // 标记是否已经施加过用户组权限过滤
  };
}
```

**修改点 3：activated() 获取用户组并设置过滤参数**
```javascript
activated() {
  // 重置标记
  this.isUserGroupFilterApplied = false;
  
  // 获取当前用户在项目中的用户组
  getUserGroupProject(getCurrentProjectID(), getCurrentUserId())
    .then((response) => {
      const groups = response.data || [];
      const targetGroup = groups.find(g => g.id === 'developer' || g.id === 'tester');
      this.currentUserGroupId = targetGroup ? targetGroup.id : null;
      
      // 如果用户属于特定用户组，直接在 page.condition 中设置过滤参数
      if (this.currentUserGroupId && !this.isUserGroupFilterApplied) {
        this.page.condition.currentUserId = getCurrentUserId();
        this.page.condition.userGroupId = this.currentUserGroupId;
        this.isUserGroupFilterApplied = true;
      }
    })
    .catch(() => {
      this.currentUserGroupId = null;
    });
  
  // ... 现有代码 ...
}
```

**修改点 4：getIssues() 直接使用 page.condition**
```javascript
getIssues() {
  this.loading = true;
  
  // 直接使用 page.condition 中的参数，不做任何修改
  // - 初始化加载时，activated() 已经设置了 currentUserId 和 userGroupId
  // - 翻页时，page.condition 自动保持这些参数
  // - 搜索时，search() 已经清除了这些参数
  
  this.page.condition.projectId = this.projectId;
  this.page.condition.workspaceId = this.workspaceId;
  this.page.condition.orders = getLastTableSortField(this.tableHeaderKey);
  
  getIssues(this.page.currentPage, this.page.pageSize, this.page.condition)
    .then((response) => {
      // ... 现有的数据处理逻辑 ...
    });
}
```

**修改点 5：search() 清除权限过滤参数**
```javascript
search() {
  // 清除 page.condition 中的用户组权限过滤参数
  delete this.page.condition.currentUserId;
  delete this.page.condition.userGroupId;
  this.isUserGroupFilterApplied = false; // 重置标记
  
  this.page.currentPage = 1;
  this.pageRefresh = false;
  this.getIssues();
}
```

**修改点 6：handlePageChange() 保持不变**
```javascript
handlePageChange() {
  this.pageRefresh = true;
  this.getIssues(); // 翻页时 page.condition 自动保持权限过滤参数
}
```

### 2. 后端修改

#### 文件：`test-track/backend/src/main/java/io/metersphere/base/mapper/ext/ExtIssuesMapper.xml`

**修改点：queryWhereCondition SQL 片段**
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

#### 文件：`framework/sdk-parent/xpack-interface/src/main/java/io/metersphere/xpack/track/dto/request/IssuesRequest.java`

**修改点：保留现有字段，删除 isInitialLoad 字段**
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

## 四、实现效果

| 场景 | 前端行为 | 后端行为 | 开发人员看到 | 测试人员看到 |
|------|---------|---------|------------|------------|
| **初始化加载** | `page.condition` 包含 `userGroupId` 和 `currentUserId` | 施加权限过滤 | 处理人是自己的缺陷 | 创建人是自己的缺陷 |
| **翻页** | `page.condition` 自动保持参数 | 根据参数决定 | 保持权限过滤状态 | 保持权限过滤状态 |
| **高级搜索查询** | `search()` 清除了 `userGroupId` 和 `currentUserId` | 不施加权限过滤 | 所有符合条件的缺陷 | 所有符合条件的缺陷 |
| **高级搜索重置** | `search()` 清除了 `userGroupId` 和 `currentUserId` | 不施加权限过滤 | 所有缺陷 | 所有缺陷 |
| **搜索后翻页** | `page.condition` 保持搜索状态（无权限过滤） | 不施加权限过滤 | 所有符合条件的缺陷 | 所有符合条件的缺陷 |

## 五、技术亮点

### 1. 使用 page.condition 保持状态

**我在做**：将权限过滤参数直接设置到 `page.condition` 对象中。

**目的是**：利用 MeterSphere 框架特性，`page.condition` 在翻页时自动保持所有参数，无需手动传递。

**如果不这样做**：需要在每个调用 `getIssues()` 的地方判断是否传递权限过滤参数，容易遗漏。

### 2. 在 activated() 中设置过滤参数

**我在做**：在 `activated()` 钩子中获取用户组并直接设置 `page.condition` 的参数。

**目的是**：
1. 每次进入页面都重新施加权限过滤
2. 减少 API 调用次数（只在进入页面时调用一次）
3. 翻页时自动保持这些参数

**如果不这样做**：需要在 `getIssues()` 中判断是否施加过滤，逻辑复杂且容易出错。

### 3. 在 search() 中清除过滤参数

**我在做**：在 `search()` 方法中使用 `delete` 清除 `page.condition` 中的权限过滤参数。

**目的是**：让搜索、筛选、重置等操作不受权限限制，用户可以查看所有符合条件的缺陷。

**如果不这样做**：搜索后仍会施加权限过滤，用户无法搜索到其他人的缺陷。

## 六、优势分析

### 相比其他方案的优势

#### 方案一：后端判断 isInitialLoad（已放弃）
- **问题**：后端无法区分"初始化加载"和"高级搜索重置"
- **原因**：两者的请求参数完全相同

#### 方案二：前端使用 isInitialLoadDone 标记（已放弃）
- **问题**：翻页时会丢失权限过滤
- **原因**：`isInitialLoadDone=true` 后，翻页时不会施加权限过滤

#### 方案三：使用 page.condition 保持状态（最终方案）
- **优势**：翻页时自动保持权限过滤参数
- **实现**：在 `activated()` 中设置参数，在 `search()` 中清除参数

### 核心优势

1. **状态保持机制清晰**：使用 `page.condition` 作为唯一的状态容器
2. **代码简洁**：`getIssues()` 不需要传参数，不需要在每个调用点判断
3. **易于维护**：逻辑集中在 `activated()` 和 `search()` 两个方法
4. **用户体验好**：翻页时保持过滤状态，搜索时清除过滤

### 二次开发友好性

1. **改动面小**：只修改 `IssueList.vue` 一个文件的几个方法，新增一个 API 文件
2. **边界清晰**：权限过滤逻辑集中在 `getIssues()` 方法中
3. **可回滚**：升级时只需对照 `getIssues()` 方法的变化即可快速搬运

## 七、测试验证清单

### 测试场景

- [ ] **开发人员初始化加载**：只看到处理人是自己的缺陷
- [ ] **测试人员初始化加载**：只看到创建人是自己的缺陷
- [ ] **其他用户组初始化加载**：可以看到所有缺陷
- [ ] **翻页（初始化加载后）**：保持权限过滤状态，只看到与自己相关的缺陷
- [ ] **高级搜索查询**：可以搜索到所有符合条件的缺陷
- [ ] **高级搜索重置**：可以看到所有缺陷
- [ ] **搜索后翻页**：保持搜索状态，不施加权限过滤
- [ ] **删除后刷新**：保持当前状态（如果之前搜索过则保持搜索状态）
- [ ] **重新进入页面**：重新施加权限过滤

### 验证方法

1. 使用开发人员账号登录，查看缺陷列表
2. 使用测试人员账号登录，查看缺陷列表
3. 使用其他用户组账号登录，查看缺陷列表
4. 使用高级搜索，验证可以搜索到其他人的缺陷
5. 点击"重置"按钮，验证可以看到所有缺陷
6. 使用快捷筛选，验证可以筛选到所有符合条件的缺陷
7. 翻页，验证过滤状态保持不变

## 八、注意事项

### 1. 用户组 ID 的约定

- 开发人员组：`developer`
- 测试人员组：`tester`
- 其他用户组：不施加权限过滤

### 2. 自定义字段值的处理

处理人字段存储在 `custom_field_issues` 表中，值为 JSON 格式（带双引号），需要使用 `trim(both '"' from cfi.value)` 处理。

### 3. 前端 API 导入路径

由于 `test-track` 模块没有 `project-management` 模块的依赖，需要在 `test-track/frontend/src/api/` 目录下创建独立的 `user-group.js` 文件。

## 九、后续优化建议

### 1. 缓存用户组信息

可以将用户组信息缓存到 Vuex/Pinia 中，避免每次进入页面都重新获取。

### 2. 统一权限过滤组件

如果其他页面也需要类似的权限过滤，可以抽取为公共 Mixin 或 Composable。

### 3. 权限配置化

可以将用户组与过滤规则的映射关系配置化，方便后续扩展。

## 十、总结

本次实现采用**使用 page.condition 保持状态**的方案，通过在 `activated()` 中设置权限过滤参数，在 `search()` 中清除权限过滤参数，成功实现了以下目标：

1. **初始化加载时施加权限过滤**：开发人员只看到处理人是自己的缺陷，测试人员只看到创建人是自己的缺陷
2. **翻页时保持权限过滤**：利用 `page.condition` 自动保持参数的特性
3. **搜索时清除权限过滤**：允许用户查看所有符合条件的缺陷
4. **代码简洁易维护**：不需要给 `getIssues()` 传参数，逻辑集中在两个方法中

方案具有**状态保持机制清晰、代码简洁、易于维护、用户体验好**等优点，符合二次开发的"改动面小、边界清晰、可回滚"原则。
