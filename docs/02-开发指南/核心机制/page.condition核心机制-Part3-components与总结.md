# page.condition 核心机制深度解析（Part 3）

## 一、components（搜索组件配置）

### 1.1 核心概念

**作用**：定义高级搜索UI的组件配置

**数据结构**：
```javascript
components: [
  {
    key: 'creator',              // 字段key（对应 filters 或 combine 的 key）
    name: 'MsTableSearchSelect', // Vue组件名称
    label: '创建人',             // 显示标签
    operator: {                  // 操作符配置
      value: 'in',               // 默认操作符
      options: [OPERATORS.IN, OPERATORS.NOT_IN]  // 可选操作符
    },
    options: [],                 // 选项列表（下拉框数据源）
    props: {                     // 组件props
      multiple: true
    }
  }
]
```

**特点**：
- ✅ 定义高级搜索UI
- ✅ 支持多种组件类型
- ✅ 支持动态选项
- ✅ 支持操作符切换

---

### 1.2 组件类型

**位置**：`framework/sdk-parent/frontend/src/components/search/search-components.js`

**支持的组件**：
```javascript
export default {
  MsTableSearchInput,           // 文本输入框
  MsTableSearchDatePicker,      // 日期选择器
  MsTableSearchDateTimePicker,  // 日期时间选择器
  MsTableSearchSelect,          // 下拉选择框
  MsTableSearchInputNumber,     // 数字输入框
  MsTableSearchMix,             // 混合组件
  MsTableSearchNodeTree         // 树形选择器
}
```

---

### 1.3 文本输入框（MsTableSearchInput）

**用途**：输入文本进行模糊搜索

**示例1：名称搜索**
```javascript
export const NAME = {
  key: "name",
  name: 'MsTableSearchInput',
  label: 'commons.name',
  operator: {
    value: OPERATORS.LIKE.value,
    options: [OPERATORS.LIKE, OPERATORS.NOT_LIKE]
  }
}
```

**UI效果**：
```
[名称] [包含 ▼] [输入框]
```

**用户操作**：
1. 选择操作符：包含 / 不包含
2. 输入搜索关键词：缺陷
3. 点击搜索

**生成的 combine**：
```javascript
combine: {
  'name': { operator: 'like', value: '缺陷' }
}
```

---

### 1.4 下拉选择框（MsTableSearchSelect）

**用途**：从预定义选项中选择

**示例1：创建人选择**
```javascript
export const CREATOR = {
  key: "creator",
  name: 'MsTableSearchSelect',
  label: 'api_test.creator',
  operator: {
    options: [OPERATORS.IN, OPERATORS.NOT_IN, OPERATORS.CURRENT_USER],
    change: function (component, value) {
      if (value === OPERATORS.CURRENT_USER.value) {
        component.value = value;
      }
    }
  },
  options: {
    url: "/user/project/member/list",  // 动态获取选项
    labelKey: "name",
    valueKey: "id",
    showLabel: option => {
      return option.label + "(" + option.value + ")";
    }
  },
  props: {
    multiple: true
  },
  isShow: operator => {
    return operator !== OPERATORS.CURRENT_USER.value;
  }
}
```

**UI效果**：
```
[创建人] [属于 ▼] [下拉选择框（多选）]
```

**用户操作**：
1. 选择操作符：属于 / 不属于 / 当前用户
2. 选择用户：张三、李四
3. 点击搜索

**生成的 filters**：
```javascript
filters: {
  'creator': ['user-001', 'user-002']
}
```

---

### 1.5 日期时间选择器（MsTableSearchDateTimePicker）

**用途**：选择日期时间范围

**示例1：创建时间**
```javascript
export const CREATE_TIME = {
  key: "createTime",
  name: 'MsTableSearchDateTimePicker',
  label: 'commons.create_time',
  operator: {
    options: [OPERATORS.BETWEEN, OPERATORS.GT, OPERATORS.LT]
  }
}
```

**UI效果**：
```
[创建时间] [之间 ▼] [开始时间] - [结束时间]
```

**用户操作**：
1. 选择操作符：之间 / 大于 / 小于
2. 选择时间范围：2024-01-01 到 2024-12-31
3. 点击搜索

**生成的 combine**：
```javascript
combine: {
  'createTime': {
    operator: 'between',
    value: [1704067200000, 1735689599000]
  }
}
```

---

### 1.6 树形选择器（MsTableSearchNodeTree）

**用途**：从树形结构中选择节点

**示例1：模块选择**
```javascript
export const TEST_CASE_MODULE_TREE = {
  key: "moduleIds",
  name: 'MsTableSearchNodeTree',
  label: "test_track.case.module",
  operator: {
    value: OPERATORS.IN.value,
    options: [OPERATORS.IN, OPERATORS.NOT_IN]
  },
  options: {
    url: "/case/node/list",
    type: "POST",
    params: {}
  },
  init: undefined  // 高级搜索框非首次打开时会执行该函数
}
```

**UI效果**：
```
[所属模块] [属于 ▼] [树形选择器]
```

**用户操作**：
1. 选择操作符：属于 / 不属于
2. 选择模块：功能模块 > 登录模块
3. 点击搜索

**生成的 filters**：
```javascript
filters: {
  'moduleIds': ['module-001', 'module-002']
}
```

---

### 1.7 自定义字段组件

**关键函数**：`getAdvSearchCustomField`

**位置**：`framework/sdk-parent/frontend/src/components/search/custom-component.js`

**作用**：根据自定义字段生成搜索组件配置

**示例**：
```javascript
// IssueList.vue
initFields(template) {
  // 过滤自定义字段
  this.page.condition.components = this.page.condition.components.filter(item => item.custom !== true);
  
  // 添加自定义字段组件
  let comp = getAdvSearchCustomField(this.page.condition, template.customFields);
  this.page.condition.components.push(...comp);
}
```

**生成的组件配置**：
```javascript
[
  {
    key: 'custom_single-field-001',
    name: 'MsTableSearchSelect',
    label: '处理人',
    custom: true,  // 标记为自定义字段
    type: 'member',
    operator: {
      options: [OPERATORS.IN, OPERATORS.NOT_IN]
    },
    options: [],  // 成员列表
    props: {
      multiple: true
    }
  }
]
```

---

### 1.8 components 配置示例

**缺陷列表的 components**：
```javascript
// 位置：framework/sdk-parent/frontend/src/components/search/search-components.js

export const TEST_TRACK_ISSUE_LIST = [
  NAME,           // 名称（文本输入）
  PLATFORM,       // 所属平台（下拉选择）
  CREATE_TIME,    // 创建时间（日期时间选择）
  UPDATE_TIME,    // 更新时间（日期时间选择）
  CREATOR,        // 创建人（下拉选择）
  PLATFORM_STATUS // 平台状态（下拉选择）
];
```

**展开后的配置**：
```javascript
[
  {
    key: "name",
    name: 'MsTableSearchInput',
    label: 'commons.name',
    operator: {
      value: 'like',
      options: [OPERATORS.LIKE, OPERATORS.NOT_LIKE]
    }
  },
  {
    key: "platform",
    name: 'MsTableSearchSelect',
    label: "所属平台",
    operator: {
      options: [OPERATORS.IN, OPERATORS.NOT_IN]
    },
    options: [],
    props: {
      multiple: true
    }
  },
  {
    key: "createTime",
    name: 'MsTableSearchDateTimePicker',
    label: 'commons.create_time',
    operator: {
      options: [OPERATORS.BETWEEN, OPERATORS.GT, OPERATORS.LT]
    }
  },
  // ... 其他组件
]
```

---

### 1.9 高级搜索UI渲染

**MsTableHeader 组件**：
```vue
<!-- IssueList.vue -->
<ms-table-header
  :condition.sync="page.condition"
  @search="search"
>
</ms-table-header>
```

**渲染流程**：
1. MsTableHeader 读取 `page.condition.components`
2. 遍历 components 数组
3. 根据 `name` 动态渲染对应的组件
4. 用户操作后更新 `page.condition.filters` 或 `page.condition.combine`
5. 点击搜索触发 `@search` 事件

---

### 1.10 动态选项加载

**示例：创建人选项**
```javascript
options: {
  url: "/user/project/member/list",  // 接口URL
  labelKey: "name",                  // 显示字段
  valueKey: "id",                    // 值字段
  showLabel: option => {             // 自定义显示
    return option.label + "(" + option.value + ")";
  }
}
```

**加载流程**：
1. 组件挂载时调用 `url` 接口
2. 获取数据：`[{ id: 'user-001', name: '张三' }, ...]`
3. 根据 `labelKey` 和 `valueKey` 生成选项
4. 使用 `showLabel` 自定义显示：`张三(user-001)`

---

### 1.11 操作符切换

**示例：当前用户操作符**
```javascript
operator: {
  options: [OPERATORS.IN, OPERATORS.NOT_IN, OPERATORS.CURRENT_USER],
  change: function (component, value) {
    if (value === OPERATORS.CURRENT_USER.value) {
      component.value = value;  // 自动设置值为 'current user'
    }
  }
}
```

**用户操作**：
1. 选择操作符：当前用户
2. 自动隐藏选择框（通过 `isShow` 函数）
3. 自动设置值为 'current user'

**生成的 filters**：
```javascript
filters: {
  'creator': ['current user']
}
```

---

## 二、完整流程示例

### 2.1 用户操作流程

**场景**：用户在缺陷列表页面进行高级搜索

**步骤1：打开高级搜索**
- 点击"高级搜索"按钮
- MsTableHeader 渲染搜索组件

**步骤2：设置搜索条件**
- 名称：包含 "登录"
- 创建人：属于 "张三"
- 创建时间：之间 2024-01-01 到 2024-12-31

**步骤3：点击搜索**
- MsTableHeader 更新 `page.condition`
- 触发 `@search` 事件
- 调用 `search()` 方法

**步骤4：发送请求**
```javascript
search() {
  this.page.currentPage = 1;
  this.getIssues();
}

getIssues() {
  getIssues(this.page.currentPage, this.page.pageSize, this.page.condition);
}
```

**实际请求**：
```http
POST /issues/list/1/10
Content-Type: application/json

{
  "projectId": "project-001",
  "filters": {
    "creator": ["user-001"]
  },
  "combine": {
    "name": { "operator": "like", "value": "登录" },
    "createTime": {
      "operator": "between",
      "value": [1704067200000, 1735689599000]
    }
  },
  "orders": [
    { "name": "create_time", "type": "desc" }
  ]
}
```

---

### 2.2 后端处理流程

**步骤1：接收参数**
```java
@PostMapping("/list/{goPage}/{pageSize}")
public Pager<List<IssuesDao>> list(
    @PathVariable int goPage,
    @PathVariable int pageSize,
    @RequestBody IssuesRequest request) {
    
    Page<Object> page = PageHelper.startPage(goPage, pageSize, true);
    return PageUtils.setPageInfo(page, extIssuesMapper.getIssues(request));
}
```

**步骤2：构建SQL**
```xml
<select id="getIssues" resultType="IssuesDao">
  SELECT * FROM issues
  WHERE project_id = #{request.projectId}
  
  <!-- 处理 filters -->
  AND issues.creator IN ('user-001')
  
  <!-- 处理 combine -->
  AND issues.name LIKE '%登录%'
  AND issues.create_time BETWEEN 1704067200000 AND 1735689599000
  
  <!-- 处理 orders -->
  ORDER BY create_time DESC
  
  LIMIT 0, 10
</select>
```

**步骤3：返回结果**
```json
{
  "itemCount": 5,
  "listObject": [
    { "id": "issue-001", "title": "登录失败", "creator": "user-001", ... },
    { "id": "issue-002", "title": "登录超时", "creator": "user-001", ... },
    ...
  ]
}
```

---

## 三、四大参数总结

### 3.1 参数对比表

| 参数 | 作用 | 数据类型 | 匹配方式 | 翻页保持 | 搜索保持 | 使用场景 |
|-----|------|---------|---------|---------|---------|---------|
| **filters** | 列过滤 | Object | 精确匹配（IN） | ✅ | ❌（可选） | 表头筛选、权限过滤 |
| **combine** | 组合查询 | Object | 模糊匹配（LIKE等） | ✅ | ✅ | 高级搜索 |
| **orders** | 排序规则 | Array | - | ✅ | ✅ | 列排序 |
| **components** | 搜索组件配置 | Array | - | ✅ | ✅ | 高级搜索UI |

---

### 3.2 数据流向图

```
用户操作
  ↓
高级搜索UI（components）
  ↓
更新 page.condition
  ├─ filters（表头筛选、权限过滤）
  ├─ combine（高级搜索）
  └─ orders（排序）
  ↓
发送HTTP请求
  ↓
后端SQL处理
  ├─ WHERE 子句（filters + combine）
  └─ ORDER BY 子句（orders）
  ↓
返回结果
  ↓
渲染列表
```

---

### 3.3 最佳实践总结

**1. filters 使用场景**
```javascript
// ✅ 表头筛选
this.page.condition.filters['creator'] = ['user-001'];

// ✅ 权限过滤
this.page.condition.filters['custom_single-field-001'] = [currentUserId];

// ❌ 模糊搜索（应该用 combine）
this.page.condition.filters['title'] = ['缺陷'];  // 错误！
```

**2. combine 使用场景**
```javascript
// ✅ 模糊搜索
this.page.condition.combine['title'] = { operator: 'like', value: '缺陷' };

// ✅ 范围查询
this.page.condition.combine['createTime'] = {
  operator: 'between',
  value: [startTime, endTime]
};

// ❌ 精确匹配（应该用 filters）
this.page.condition.combine['creator'] = { operator: 'in', value: ['user-001'] };  // 不推荐
```

**3. orders 使用场景**
```javascript
// ✅ 单字段排序
this.page.condition.orders = [{ name: 'create_time', type: 'desc' }];

// ✅ 多字段排序
this.page.condition.orders = [
  { name: 'priority', type: 'desc' },
  { name: 'create_time', type: 'desc' }
];

// ✅ 恢复上次排序
this.page.condition.orders = getLastTableSortField(this.tableHeaderKey);
```

**4. components 使用场景**
```javascript
// ✅ 初始化组件配置
this.page.condition.components = TEST_TRACK_ISSUE_LIST;

// ✅ 添加自定义字段组件
let comp = getAdvSearchCustomField(this.page.condition, template.customFields);
this.page.condition.components.push(...comp);

// ✅ 过滤自定义字段
this.page.condition.components = this.page.condition.components.filter(item => item.custom !== true);
```

---

### 3.4 常见问题

**Q1: filters 和 combine 有什么区别？**

A: 
- filters：精确匹配（IN查询），用于表头筛选和权限过滤
- combine：支持模糊匹配（LIKE）和范围查询（BETWEEN），用于高级搜索

**Q2: 为什么翻页时权限过滤会保持？**

A: 因为翻页时只改变 `page.currentPage`，不改变 `page.condition.filters`

**Q3: 为什么搜索时要清除权限过滤？**

A: 因为用户进行搜索时，应该能看到所有符合条件的数据，而不是只看到自己的数据

**Q4: 如何生成自定义字段的 filterKey？**

A: 使用 `generateColumnKey(field)` 函数，它会根据字段类型生成正确的 key

**Q5: components 配置有什么用？**

A: 定义高级搜索UI的组件类型、操作符、选项等，MsTableHeader 根据配置动态渲染搜索组件

---

## 四、完整示例代码

### 4.1 前端完整示例

```javascript
// IssueList.vue
export default {
  data() {
    return {
      page: getPageInfo({
        components: TEST_TRACK_ISSUE_LIST,  // 初始化 components
        custom: false,
      }),
      currentUserGroupId: null,
      userGroupFilterKeys: []
    };
  },
  
  activated() {
    // 1. 获取用户组
    getUserGroupProject(getCurrentProjectID(), getCurrentUserId())
      .then((response) => {
        this.currentUserGroupId = response.data;
      })
      .finally(() => {
        // 2. 加载模板
        getIssuePartTemplateWithProject((template) => {
          this.initFields(template);
          
          // 3. 施加权限过滤
          this.applyUserGroupFilter();
          
          // 4. 加载数据
          this.getIssues();
        });
      });
  },
  
  methods: {
    // 初始化字段和组件
    initFields(template) {
      // 过滤自定义字段
      this.page.condition.components = this.page.condition.components.filter(item => item.custom !== true);
      
      // 添加自定义字段组件
      let comp = getAdvSearchCustomField(this.page.condition, template.customFields);
      this.page.condition.components.push(...comp);
    },
    
    // 施加用户组权限过滤
    applyUserGroupFilter() {
      if (!this.currentUserGroupId) return;
      
      if (!this.page.condition.filters) {
        this.page.condition.filters = {};
      }
      
      const currentUserId = getCurrentUserId();
      
      if (this.currentUserGroupId === 'developer') {
        // 开发人员：过滤处理人
        const handlerField = this.issueTemplate.customFields.find(f => f.name === '处理人');
        if (handlerField) {
          const filterKey = generateColumnKey(handlerField);
          this.page.condition.filters[filterKey] = [currentUserId];
          this.userGroupFilterKeys.push(filterKey);
        }
      } else if (this.currentUserGroupId === 'tester') {
        // 测试人员：过滤创建人
        const filterKey = 'creator';
        this.page.condition.filters[filterKey] = [currentUserId];
        this.userGroupFilterKeys.push(filterKey);
      }
    },
    
    // 清除用户组权限过滤
    clearUserGroupFilter() {
      if (!this.page.condition.filters) return;
      
      this.userGroupFilterKeys.forEach(key => {
        delete this.page.condition.filters[key];
      });
      this.userGroupFilterKeys = [];
    },
    
    // 搜索
    search() {
      this.clearUserGroupFilter();
      this.page.currentPage = 1;
      this.getIssues();
    },
    
    // 翻页
    handlePageChange() {
      this.getIssues();
    },
    
    // 加载数据
    getIssues() {
      this.page.condition.orders = getLastTableSortField(this.tableHeaderKey);
      getIssues(this.page.currentPage, this.page.pageSize, this.page.condition)
        .then((response) => {
          this.page.data = response.data.listObject;
          this.page.total = response.data.itemCount;
        });
    }
  }
};
```

---

## 五、总结

`page.condition` 的4个核心参数构成了 MeterSphere 前端列表查询的完整体系：

1. **filters**：精确过滤，用于表头筛选和权限控制
2. **combine**：灵活查询，支持模糊匹配和范围查询
3. **orders**：排序规则，支持多字段组合排序
4. **components**：UI配置，定义高级搜索界面

理解这4个参数的作用和使用场景，是掌握 MeterSphere 前端开发的关键！
