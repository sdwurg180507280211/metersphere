# 用户组权限过滤功能 - 纯前端实现方案（最终版）

## 一、需求说明

在缺陷管理页面实现用户组权限过滤：
- **开发人员组（developer）**：只能看到**处理人是自己**的缺陷
- **测试人员组（tester）**：只能看到**创建人是自己**的缺陷
- **其他用户组**：没有限制，可以看到所有缺陷

### 交互要求

1. **初始化加载**：施加用户组权限过滤
2. **翻页**：保持权限过滤状态
3. **高级搜索/重置/筛选**：清除权限过滤，允许查看所有数据
4. **删除/同步/状态变更后刷新**：保持当前过滤状态（如果之前有权限过滤则保持，如果已清除则不恢复）

## 二、最终实现方案（使用 page.condition 保持状态）

### 核心思路

使用 `page.condition` 对象保持过滤状态，利用 MeterSphere 框架的特性：
- `page.condition` 在翻页时自动保持所有参数
- 搜索时可以主动清除特定参数
- 不需要给 `getIssues()` 传参数，避免遗漏调用点

### 实现细节

#### 1. 前端实现（IssueList.vue）

**数据字段**：
```javascript
data() {
  return {
    currentUserGroupId: null, // 当前用户在项目中的用户组ID
    isUserGroupFilterApplied: false, // 标记是否已经施加过用户组权限过滤
    // ...
  }
}
```

**activated() 生命周期**：
```javascript
activated() {
  // 重置标记
  this.isUserGroupFilterApplied = false;
  
  // 获取用户组
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
    });
  
  // 加载数据...
}
```

**search() 方法**：
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

**getIssues() 方法**：
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
      // 处理响应...
    });
}
```

#### 2. 后端实现（ExtIssuesMapper.xml）

**SQL 过滤逻辑**：
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
    
    <!-- 其他查询条件... -->
  </where>
</sql>
```

#### 3. DTO 定义（IssuesRequest.java）

```java
public class IssuesRequest extends BaseQueryRequest {
    /**
     * 当前用户ID（用于用户组权限过滤）
     */
    private String currentUserId;
    
    /**
     * 用户组ID（用于用户组权限过滤，如 'developer', 'tester'）
     */
    private String userGroupId;
    
    // 其他字段...
}
```

## 三、方案优势

### 1. 状态保持机制清晰
- 使用 `page.condition` 作为唯一的状态容器
- 翻页时自动保持所有参数（包括权限过滤）
- 搜索时主动清除权限过滤参数

### 2. 代码简洁
- `getIssues()` 不需要传参数
- 不需要在每个调用点判断是否施加过滤
- 所有调用点（删除后刷新、同步后刷新、状态变更后刷新）都自动正确

### 3. 易于维护
- 逻辑集中在 `activated()` 和 `search()` 两个方法
- 新增调用点不需要特殊处理
- 符合 MeterSphere 框架的设计理念

### 4. 用户体验好
- 初始化加载时自动施加权限过滤，用户看到与自己相关的缺陷
- 翻页时保持过滤状态，不会突然看到其他人的缺陷
- 搜索时清除过滤，允许查看所有符合条件的缺陷
- 删除/同步/状态变更后保持当前状态，不会意外恢复权限过滤

## 四、测试场景

### 场景1：初始化加载
1. 开发人员登录，进入缺陷管理页面
2. **预期**：只看到处理人是自己的缺陷

### 场景2：翻页
1. 在场景1的基础上，点击翻页
2. **预期**：第2页仍然只显示处理人是自己的缺陷

### 场景3：高级搜索
1. 在场景2的基础上，使用高级搜索查询特定条件
2. **预期**：搜索结果包含所有符合条件的缺陷（不限制处理人）

### 场景4：搜索后翻页
1. 在场景3的基础上，点击翻页
2. **预期**：第2页仍然显示所有符合搜索条件的缺陷（不限制处理人）

### 场景5：重置搜索
1. 在场景4的基础上，点击高级搜索的"重置"按钮
2. **预期**：显示所有缺陷（不限制处理人）

### 场景6：删除后刷新
1. 在场景5的基础上，删除一条缺陷
2. **预期**：刷新后仍然显示所有缺陷（保持当前状态）

### 场景7：重新进入页面
1. 在场景6的基础上，切换到其他页面，再返回缺陷管理页面
2. **预期**：重新施加权限过滤，只看到处理人是自己的缺陷

## 五、关键技术点

### 1. 自定义字段值的存储格式
MeterSphere 的自定义字段值使用 JSON 格式存储：
- 字符串类型：`"value"` （带双引号）
- 数组类型：`["value1", "value2"]`

因此在 SQL 中需要使用 `trim(both '"' from cfi.value)` 去除双引号。

### 2. page.condition 的生命周期
- `page.condition` 在组件 `activated()` 时保持上次的值
- 需要在 `activated()` 中重置 `isUserGroupFilterApplied` 标记
- 搜索时主动清除权限过滤参数

### 3. 用户组判断
- 通过 `getUserGroupProject()` API 获取用户在项目中的用户组
- 只关注 `developer` 和 `tester` 两个用户组
- 其他用户组不施加权限过滤

## 六、文件清单

### 前端文件
- `test-track/frontend/src/business/issue/IssueList.vue`
- `test-track/frontend/src/api/user-group.js`（新增）

### 后端文件
- `test-track/backend/src/main/java/io/metersphere/base/mapper/ext/ExtIssuesMapper.xml`
- `framework/sdk-parent/xpack-interface/src/main/java/io/metersphere/xpack/track/dto/request/IssuesRequest.java`

### 文档文件
- `docs/功能开发/用户组权限模型/USER_GROUP_FILTER_FRONTEND_IMPLEMENTATION_V2.md`（本文档）
- `docs/功能开发/用户组权限模型/IMPLEMENTATION_SUMMARY.md`

## 七、注意事项

1. **不要在 getIssues() 中判断是否施加过滤**：所有逻辑应该在 `activated()` 和 `search()` 中完成
2. **不要给 getIssues() 传参数**：使用 `page.condition` 保持状态
3. **确保 search() 清除权限过滤参数**：使用 `delete` 而不是设置为 `null`
4. **测试所有调用场景**：初始化、翻页、搜索、删除、同步、状态变更、重新进入页面

## 八、方案演进历史

### 方案一：后端判断 isInitialLoad（已放弃）
- **问题**：后端无法区分"初始化加载"和"高级搜索重置"
- **原因**：两者的请求参数完全相同

### 方案二：前端使用 isInitialLoadDone 标记（已放弃）
- **问题**：翻页时会丢失权限过滤
- **原因**：`isInitialLoadDone=true` 后，翻页时不会施加权限过滤

### 方案三：使用 page.condition 保持状态（最终方案）
- **优势**：翻页时自动保持权限过滤参数
- **实现**：在 `activated()` 中设置参数，在 `search()` 中清除参数

## 九、后续优化建议

1. **性能优化**：如果用户组查询频繁，可以考虑缓存用户组信息
2. **权限扩展**：如果需要支持更多用户组，可以在后端配置化处理
3. **日志记录**：添加权限过滤的日志，便于排查问题
