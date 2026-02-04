# 用户组权限过滤功能实施总结（纯前端方案）

## 需求概述

在缺陷管理页面实现用户组权限过滤：
- **开发人员组（developer）**：只能看到**处理人是自己**的缺陷
- **测试人员组（tester）**：只能看到**创建人是自己**的缺陷
- **其他用户组**：没有限制
- **初始化加载**时施加权限过滤
- **高级搜索/重置/筛选**时清除权限过滤
- **翻页**时保持当前过滤状态

## 最终方案：纯前端实现（方案五）✅

### 核心思路

利用现有的高级搜索过滤机制（`page.condition.filters`），在前端根据用户组设置过滤条件：
- **developer**: 设置 `filters['custom_single-处理人字段ID'] = [currentUserId]`
- **tester**: 设置 `filters['creator'] = [currentUserId]`
- **搜索时**：清除这些过滤条件
- **翻页时**：自动保持 `page.condition.filters` 中的所有条件

### 优势

1. **完全不需要修改后端SQL**：复用现有的高级搜索过滤机制
2. **改动面小**：只修改前端 `IssueList.vue` 一个文件
3. **边界清晰**：所有逻辑集中在前端，易于理解和维护
4. **可回滚**：升级时只需对照前端一个文件即可

## 实施细节

### 1. 后端接口（保留）

**文件**: `test-track/backend/src/main/java/io/metersphere/controller/IssuesController.java`

保留 `getUserGroupInProject` 接口，供前端查询用户组（此接口在之前的方案中已经添加）。

**说明**：
- ✅ 保留此接口，前端需要调用
- ❌ 回滚 `list()` 方法中的用户组过滤逻辑

### 2. 数据模型（回滚）

**文件**: `framework/sdk-parent/xpack-interface/src/main/java/io/metersphere/xpack/track/dto/request/IssuesRequest.java`

**回滚内容**：
- ❌ 删除 `currentUserId` 字段
- ❌ 删除 `userGroupId` 字段
- ❌ 删除 `isInitialLoad` 字段

### 3. SQL查询（回滚）

**文件**: `test-track/backend/src/main/java/io/metersphere/base/mapper/ext/ExtIssuesMapper.xml`

**回滚内容**：
- ❌ 删除 `queryWhereCondition` 中的用户组权限过滤SQL

**保留内容**：
- ✅ 保留 `getUserGroupInProject` SQL查询

### 4. 前端API（已创建）

**文件**: `test-track/frontend/src/api/user-group.js`

```javascript
import {get} from 'metersphere-frontend/src/plugins/request';

/**
 * 获取用户在项目中的用户组ID
 */
export function getUserGroupProject(projectId, userId) {
  return get(`/issues/user/group/${projectId}/${userId}`);
}
```

### 5. 前端实现（核心）

**文件**: `test-track/frontend/src/business/issue/IssueList.vue`

#### 5.1 数据属性

```javascript
data() {
  return {
    // ...
    currentUserGroupId: null, // 当前用户在项目中的用户组ID
    userGroupFilterKeys: [], // 记录施加的过滤条件key，用于搜索时清除
  };
}
```

#### 5.2 页面激活时的逻辑

```javascript
activated() {
  // 1. 重置用户组过滤相关状态
  this.currentUserGroupId = null;
  this.userGroupFilterKeys = [];

  // 2. 获取用户组
  getUserGroupProject(getCurrentProjectID(), getCurrentUserId())
    .then((response) => {
      this.currentUserGroupId = response.data;
    })
    .finally(() => {
      // 3. 加载成员列表和模板
      getProjectMember().then((response) => {
        this.members = response.data;
        
        // 4. 加载模板
        getIssuePartTemplateWithProject((template) => {
          this.initFields(template);
          
          // 5. 模板加载完成后，根据用户组设置过滤条件
          this.applyUserGroupFilter();
          
          // 6. 设置过滤条件后再加载数据
          this.getIssues();
        });
      });
    });
}
```

#### 5.3 施加用户组过滤

```javascript
applyUserGroupFilter() {
  if (!this.currentUserGroupId || 
      (this.currentUserGroupId !== 'developer' && this.currentUserGroupId !== 'tester')) {
    return;
  }

  if (!this.page.condition.filters) {
    this.page.condition.filters = {};
  }

  const currentUserId = getCurrentUserId();

  if (this.currentUserGroupId === 'developer') {
    // 从模板中查找"处理人"字段
    const handlerField = this.issueTemplate.customFields.find(f => f.name === '处理人');
    if (handlerField) {
      // 使用 generateColumnKey 函数生成过滤条件的key
      // 确保key格式与后端SQL中的格式一致（custom_single-{fieldId}）
      const filterKey = generateColumnKey(handlerField);
      this.page.condition.filters[filterKey] = [currentUserId];
      this.userGroupFilterKeys.push(filterKey);
    }
  } else if (this.currentUserGroupId === 'tester') {
    const filterKey = 'creator';
    this.page.condition.filters[filterKey] = [currentUserId];
    this.userGroupFilterKeys.push(filterKey);
  }
}
```

#### 5.4 清除用户组过滤

```javascript
clearUserGroupFilter() {
  if (!this.page.condition.filters) {
    return;
  }

  // 清除之前记录的过滤条件
  this.userGroupFilterKeys.forEach(key => {
    delete this.page.condition.filters[key];
  });
  this.userGroupFilterKeys = [];
}
```

#### 5.5 搜索时清除过滤

```javascript
search() {
  // 清除用户组权限过滤条件
  this.clearUserGroupFilter();

  this.page.currentPage = 1;
  this.pageRefresh = false;
  this.getIssues();
}
```

## 方案演进历史

### 方案一：后端判断isInitialLoad ❌
**问题**：后端无法区分"初始化加载"和"高级搜索重置"

### 方案二：前端isInitialLoadDone标记 ❌
**问题**：翻页时会丢失权限过滤

### 方案三：使用page.condition保持状态+后端SQL过滤 ❌
**问题**：实现了但前端API 404，需要创建后端接口

### 方案四：test-track模块添加接口 ❌
**问题**：已实现但用户提出更简单的方案

### 方案五：纯前端实现 ✅
**优势**：完全不需要修改后端SQL，复用现有的高级搜索过滤机制

## 技术要点

### 1. 为什么使用 `page.condition.filters`？

- `page.condition.filters` 是 MeterSphere 现有的高级搜索过滤机制
- 后端 `ExtIssuesMapper.xml` 中已经有完整的 `filters` 处理逻辑
- 支持多种过滤类型：`custom_single`、`custom_multiple`、`creator`、`platform` 等
- 翻页时 `page.condition` 会自动保持，不需要额外处理

### 2. 为什么记录 `userGroupFilterKeys`？

- 用户进行搜索/筛选/重置时，需要清除用户组权限过滤条件
- 但不能清除用户手动设置的其他过滤条件
- 通过记录施加的过滤条件key，可以精确清除用户组权限过滤，而不影响其他过滤条件

### 3. 为什么在 `activated()` 中重置状态？

- Vue 的 `keep-alive` 机制会缓存组件实例
- 从其他页面返回时，不会触发 `created()` 或 `mounted()`，只会触发 `activated()`
- 需要在 `activated()` 中重置状态，确保每次进入页面都重新获取用户组并施加权限过滤

### 4. 为什么在模板加载后才设置过滤条件？

- "处理人"是自定义字段，需要从模板中获取字段ID
- 过滤条件的key格式是 `custom_single-${fieldId}`
- 必须等模板加载完成后才能获取到字段ID

## 文件清单

### 修改的文件

1. `test-track/frontend/src/business/issue/IssueList.vue` - 前端核心实现
2. `test-track/backend/src/main/java/io/metersphere/controller/IssuesController.java` - 回滚后端逻辑
3. `framework/sdk-parent/xpack-interface/src/main/java/io/metersphere/xpack/track/dto/request/IssuesRequest.java` - 回滚字段
4. `test-track/backend/src/main/java/io/metersphere/base/mapper/ext/ExtIssuesMapper.xml` - 回滚SQL过滤

### 新增的文件

1. `test-track/frontend/src/api/user-group.js` - 用户组API

### 保留的后端接口

1. `IssuesController.getUserGroupInProject()` - 查询用户在项目中的用户组
2. `ExtIssuesMapper.getUserGroupInProject()` - SQL查询用户组

## 测试要点

### 1. 开发人员组测试

1. 使用开发人员账号登录
2. 进入缺陷管理页面
3. **预期**：只能看到处理人是自己的缺陷
4. 翻页测试：**预期**：翻页后仍然只能看到处理人是自己的缺陷
5. 搜索测试：输入搜索条件后点击搜索，**预期**：能看到所有符合搜索条件的缺陷（不限制处理人）
6. 重置测试：点击重置按钮，**预期**：能看到所有缺陷（不限制处理人）

### 2. 测试人员组测试

1. 使用测试人员账号登录
2. 进入缺陷管理页面
3. **预期**：只能看到创建人是自己的缺陷
4. 翻页测试：**预期**：翻页后仍然只能看到创建人是自己的缺陷
5. 搜索测试：输入搜索条件后点击搜索，**预期**：能看到所有符合搜索条件的缺陷（不限制创建人）
6. 重置测试：点击重置按钮，**预期**：能看到所有缺陷（不限制创建人）

### 3. 其他用户组测试

1. 使用其他用户组账号登录（如管理员）
2. 进入缺陷管理页面
3. **预期**：能看到所有缺陷（没有权限限制）

### 4. 高级搜索测试

1. 使用开发人员或测试人员账号登录
2. 进入缺陷管理页面（此时应该有权限过滤）
3. 使用高级搜索功能，添加任意搜索条件
4. **预期**：能看到所有符合搜索条件的缺陷（不限制处理人/创建人）
5. 清除搜索条件后，**预期**：能看到所有缺陷（不限制处理人/创建人）

### 5. 页面切换测试

1. 使用开发人员或测试人员账号登录
2. 进入缺陷管理页面（此时应该有权限过滤）
3. 切换到其他页面（如测试用例）
4. 再切换回缺陷管理页面
5. **预期**：重新施加权限过滤，只能看到处理人/创建人是自己的缺陷

## 总结

最终采用的**纯前端方案**是最优解：
- ✅ 完全不需要修改后端SQL
- ✅ 改动面小，只修改前端一个文件
- ✅ 边界清晰，所有逻辑集中在前端
- ✅ 可回滚，升级时只需对照前端一个文件
- ✅ 复用现有的高级搜索过滤机制
- ✅ 翻页时自动保持过滤状态
- ✅ 搜索时自动清除权限过滤

这个方案完美符合二次开发的原则：**改动面小、边界清晰、可回滚**。
