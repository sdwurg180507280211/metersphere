# 缺陷管理权限过滤方案对比分析

## 📋 需求回顾

**核心需求**：
- **开发人员组（developer）**：只能看到创建人是自己 OR 处理人是自己的缺陷
- **测试人员组（tester）**：只能看到创建人是自己 OR 处理人是自己的缺陷
- **其他用户组**：没有限制，可以看到所有缺陷

**实现前提**：
1. 获取用户组
2. 判断是否是高级搜索查询
3. 根据上述两个条件控制查询参数

## 🔍 方案一：前端方案

### 实现思路

1. **完善前端通过接口获取用户组的机制**，在前端判断用户组
2. **在前端判断是否是高级搜索**
3. **在前端控制参数传递**

### 详细实现

#### 1. 新增后端接口：获取用户组

```java
// IssuesController.java
@GetMapping("/user/group/{projectId}")
@RequiresPermissions(PermissionConstants.PROJECT_TRACK_ISSUE_READ)
public String getUserGroupInProject(@PathVariable String projectId) {
    String userId = SessionUtils.getUserId();
    return issuesService.getUserGroupInProject(userId, projectId);
}
```

#### 2. 前端调用接口获取用户组

```javascript
// IssueList.vue
data() {
  return {
    currentUserGroup: null, // 当前用户的用户组
    // ...
  };
},

activated() {
  // 获取当前用户的用户组
  this.getCurrentUserGroup();
  // ...
},

methods: {
  getCurrentUserGroup() {
    this.$get(`/issues/user/group/${this.projectId}`).then((response) => {
      this.currentUserGroup = response.data;
    });
  },
  
  getIssues() {
    this.loading = true;
    
    // 判断是否是初始化加载（没有高级搜索条件）
    let isInitialLoad = !this.page.condition.combine || 
                        Object.keys(this.page.condition.combine).length === 0;
    
    // 如果是初始化加载 && 用户组是 developer 或 tester，则添加过滤条件
    if (isInitialLoad && 
        (this.currentUserGroup === 'developer' || this.currentUserGroup === 'tester')) {
      // 在前端构造过滤条件
      if (!this.page.condition.filters) {
        this.page.condition.filters = {};
      }
      // 添加创建人过滤
      this.page.condition.filters.creator = [getCurrentUserId()];
      
      // 添加处理人过滤（通过高级搜索的自定义字段）
      if (!this.page.condition.combine) {
        this.page.condition.combine = {};
      }
      if (!this.page.condition.combine.customs) {
        this.page.condition.combine.customs = [];
      }
      // 找到处理人字段
      let handlerField = this.issueTemplate.customFields.find(f => f.name === '处理人');
      if (handlerField) {
        this.page.condition.combine.customs.push({
          id: handlerField.id,
          name: '处理人',
          type: handlerField.type,
          operator: 'in',
          value: [getCurrentUserId()]
        });
      }
    }
    
    // 调用接口
    getIssues(this.page.currentPage, this.page.pageSize, this.page.condition)
      .then((response) => {
        // ...
      });
  }
}
```

### 优点

1. ✅ **不影响后端代码**（除了新增一个查询接口）
2. ✅ **如若拓展到其它页面，可以写个公共组件**
3. ✅ **方案拓展，对后端代码影响程度低**

### 缺点

1. ❌ **前端逻辑复杂**
   - 需要在前端构造复杂的过滤条件
   - 需要处理创建人和处理人的 OR 逻辑
   - 需要处理自定义字段的查询条件

2. ❌ **无法实现 OR 逻辑**
   - 前端的 `filters` 和 `combine` 是 AND 关系
   - 无法实现"创建人是自己 OR 处理人是自己"
   - 只能实现"创建人是自己 AND 处理人是自己"（这不符合需求）

3. ❌ **性能问题**
   - 前端构造的过滤条件会转换为多个 SQL 查询
   - 无法利用 SQL 的 OR 优化
   - 查询性能较差

4. ❌ **安全问题**
   - 前端可以绕过过滤（修改 JavaScript 代码）
   - 无法保证权限控制的强制性

5. ❌ **维护成本高**
   - 前端需要了解后端的查询逻辑
   - 前端需要构造复杂的查询条件
   - 前后端耦合度高

### 关键问题：无法实现 OR 逻辑

**需求**：创建人是自己 **OR** 处理人是自己

**前端实现**：
```javascript
// ❌ 错误：这是 AND 逻辑，不是 OR 逻辑
this.page.condition.filters.creator = [getCurrentUserId()];
this.page.condition.combine.customs.push({
  name: '处理人',
  value: [getCurrentUserId()]
});
```

**转换为 SQL**：
```sql
-- ❌ 错误：这是 AND 逻辑
WHERE creator = '当前用户'
  AND 处理人 = '当前用户'
```

**正确的 SQL**：
```sql
-- ✅ 正确：这是 OR 逻辑
WHERE creator = '当前用户'
   OR 处理人 = '当前用户'
```

**结论**：前端无法通过 `filters` 和 `combine` 实现 OR 逻辑。

## 🔍 方案二：后端方案

### 实现思路

1. **在控制层获取用户组**
2. **在控制层判断是否是高级搜索**
3. **在控制层判断是否需要参数传递**

### 详细实现

#### 1. 控制层实现

```java
// IssuesController.java
@PostMapping("/list/{goPage}/{pageSize}")
public Pager<List<IssuesDao>> list(@PathVariable int goPage, @PathVariable int pageSize, 
                                    @RequestBody IssuesRequest request) {
    issuesService.setFilterParam(request);
    
    // 1. 判断是否是初始化加载
    boolean isInitialLoad = isInitialLoadRequest(request);
    
    // 2. 如果是初始化加载，获取用户组并添加过滤
    if (isInitialLoad) {
        String userId = SessionUtils.getUserId();
        String projectId = request.getProjectId();
        
        // 3. 查询用户组
        String userGroupId = extIssuesMapper.getUserGroupInProject(userId, projectId);
        
        // 4. 如果是 developer 或 tester，设置过滤参数
        if ("developer".equals(userGroupId) || "tester".equals(userGroupId)) {
            request.setCurrentUserId(userId);
            request.setUserGroupId(userGroupId);
        }
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

#### 2. SQL 层实现

```xml
<!-- ExtIssuesMapper.xml -->
<sql id="queryWhereCondition">
    <where>
        <!-- 用户组权限过滤：开发人员组和测试人员组只能看到创建人或处理人是自己的缺陷 -->
        <if test="request.userGroupId != null and request.currentUserId != null">
            <if test="request.userGroupId == 'developer' or request.userGroupId == 'tester'">
                and (
                    issues.creator = #{request.currentUserId}
                    or exists (
                        select 1 from custom_field_issues cfi
                        inner join custom_field cf on cfi.field_id = cf.id
                        where cfi.resource_id = issues.id
                          and cf.name = '处理人'
                          and cf.scene = 'ISSUE'
                          and cf.system = 1
                          and (cf.project_id = issues.project_id or cf.global = 1)
                          and trim(both '"' from cfi.value) = #{request.currentUserId}
                    )
                )
            </if>
        </if>
        <!-- 其他查询条件 -->
    </where>
</sql>
```

### 优点

1. ✅ **逻辑清晰**
   - 控制层负责判断场景和获取用户组
   - SQL 层负责权限过滤
   - 职责分明，易于理解

2. ✅ **性能最优**
   - 在 SQL 层面实现 OR 逻辑
   - 利用数据库索引优化查询
   - 只查询符合条件的数据

3. ✅ **安全可靠**
   - 后端强制执行权限控制
   - 前端无法绕过过滤
   - 符合安全最佳实践

4. ✅ **易于扩展**
   - 如若扩展到其它页面，可以以**切面编程**的方式实现
   - 不必修改 SQL（SQL 已经支持权限过滤）
   - 只需在控制层添加相同的逻辑

5. ✅ **维护成本低**
   - 前端不需要了解权限过滤逻辑
   - 后端统一管理权限控制
   - 前后端解耦

### 缺点

1. ⚠️ **需要修改后端代码**
   - 需要在控制层添加判断逻辑
   - 需要在 SQL 中添加权限过滤条件

2. ⚠️ **需要修改 SQL**
   - 需要在 `queryWhereCondition` 中添加权限过滤
   - 但这是一次性修改，后续扩展不需要再改

### 扩展性：切面编程实现

```java
// 定义注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserGroupFilter {
    /**
     * 是否启用用户组过滤
     */
    boolean enabled() default true;
    
    /**
     * 需要过滤的用户组
     */
    String[] groups() default {"developer", "tester"};
}

// 定义切面
@Aspect
@Component
public class UserGroupFilterAspect {
    
    @Autowired
    private ExtIssuesMapper extIssuesMapper;
    
    @Around("@annotation(userGroupFilter)")
    public Object around(ProceedingJoinPoint joinPoint, UserGroupFilter userGroupFilter) throws Throwable {
        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        
        // 找到 IssuesRequest 参数
        for (Object arg : args) {
            if (arg instanceof IssuesRequest) {
                IssuesRequest request = (IssuesRequest) arg;
                
                // 判断是否是初始化加载
                boolean isInitialLoad = isInitialLoadRequest(request);
                
                if (isInitialLoad) {
                    // 获取用户组并设置过滤参数
                    String userId = SessionUtils.getUserId();
                    String projectId = request.getProjectId();
                    String userGroupId = extIssuesMapper.getUserGroupInProject(userId, projectId);
                    
                    // 判断是否需要过滤
                    if (Arrays.asList(userGroupFilter.groups()).contains(userGroupId)) {
                        request.setCurrentUserId(userId);
                        request.setUserGroupId(userGroupId);
                    }
                }
                break;
            }
        }
        
        // 执行原方法
        return joinPoint.proceed();
    }
    
    private boolean isInitialLoadRequest(IssuesRequest request) {
        if (request.getIsInitialLoad() != null) {
            return request.getIsInitialLoad();
        }
        return request.getCombine() == null || request.getCombine().isEmpty();
    }
}

// 使用注解
@PostMapping("/list/{goPage}/{pageSize}")
@UserGroupFilter // 只需添加这个注解
public Pager<List<IssuesDao>> list(@PathVariable int goPage, @PathVariable int pageSize, 
                                    @RequestBody IssuesRequest request) {
    issuesService.setFilterParam(request);
    Page<List<Issues>> page = PageHelper.startPage(goPage, pageSize, true);
    return PageUtils.setPageInfo(page, issuesService.list(request));
}
```

**扩展到其他页面**：
```java
// 测试计划缺陷列表
@PostMapping("/plan/list/{goPage}/{pageSize}")
@UserGroupFilter // 只需添加这个注解
public Pager<List<IssuesDao>> planList(...) {
    // ...
}

// 工作台缺陷列表
@PostMapping("/dashboard/list/{goPage}/{pageSize}")
@UserGroupFilter // 只需添加这个注解
public Pager<List<IssuesDao>> dashboardList(...) {
    // ...
}
```

## 📊 方案对比

| 维度 | 方案一：前端方案 | 方案二：后端方案 |
|------|-----------------|-----------------|
| **实现难度** | 🔴 高（需要构造复杂的查询条件） | 🟢 低（逻辑清晰，易于实现） |
| **OR 逻辑** | ❌ 无法实现 | ✅ 可以实现 |
| **性能** | 🔴 差（无法利用 SQL 优化） | 🟢 优（SQL 层面优化） |
| **安全性** | 🔴 差（前端可绕过） | 🟢 优（后端强制执行） |
| **维护成本** | 🔴 高（前后端耦合） | 🟢 低（前后端解耦） |
| **扩展性** | 🟡 中（需要复制前端代码） | 🟢 优（切面编程，只需加注解） |
| **对后端影响** | 🟢 小（只需新增查询接口） | 🟡 中（需要修改控制层和 SQL） |
| **对前端影响** | 🔴 大（需要构造查询条件） | 🟢 小（不需要修改） |
| **符合需求** | ❌ 不符合（无法实现 OR 逻辑） | ✅ 符合 |

## 🎯 推荐方案

**强烈推荐方案二：后端方案**

### 推荐理由

1. **方案一无法实现需求**
   - 前端无法通过 `filters` 和 `combine` 实现 OR 逻辑
   - 只能实现 AND 逻辑，不符合需求
   - **这是致命缺陷，方案一不可行**

2. **方案二性能最优**
   - 在 SQL 层面实现 OR 逻辑
   - 利用数据库索引优化查询
   - 只查询符合条件的数据

3. **方案二安全可靠**
   - 后端强制执行权限控制
   - 前端无法绕过过滤
   - 符合安全最佳实践

4. **方案二易于扩展**
   - 可以使用切面编程实现
   - 扩展到其他页面只需添加注解
   - 不需要修改 SQL（SQL 已经支持权限过滤）

5. **方案二维护成本低**
   - 前端不需要了解权限过滤逻辑
   - 后端统一管理权限控制
   - 前后端解耦

### 实施建议

#### 第一阶段：基础实现（当前）

1. 在控制层添加判断逻辑
2. 在 SQL 中添加权限过滤条件
3. 测试验证功能正确性

#### 第二阶段：切面优化（后续）

1. 定义 `@UserGroupFilter` 注解
2. 实现 `UserGroupFilterAspect` 切面
3. 将控制层的判断逻辑移到切面中
4. 控制层只需添加注解

#### 第三阶段：扩展应用（未来）

1. 在其他需要权限过滤的接口上添加 `@UserGroupFilter` 注解
2. 不需要修改 SQL（SQL 已经支持权限过滤）
3. 不需要修改前端（前端不感知权限过滤）

## 📝 总结

### 方案一的致命缺陷

**无法实现 OR 逻辑**：
- 需求：创建人是自己 **OR** 处理人是自己
- 前端只能实现：创建人是自己 **AND** 处理人是自己
- **这不符合需求，方案一不可行**

### 方案二的优势

1. ✅ **可以实现 OR 逻辑**（在 SQL 层面）
2. ✅ **性能最优**（利用数据库索引）
3. ✅ **安全可靠**（后端强制执行）
4. ✅ **易于扩展**（切面编程）
5. ✅ **维护成本低**（前后端解耦）

### 最终建议

**采用方案二：后端方案**

- 第一阶段：在控制层实现（当前）
- 第二阶段：使用切面优化（后续）
- 第三阶段：扩展到其他页面（未来）

**符合二次开发原则**：
- ✅ 改动面小：只修改控制层和 SQL
- ✅ 边界清晰：控制层判断场景，SQL 层过滤数据
- ✅ 可回滚：删除相关代码即可回滚

---

**评估完成！** 🎉
