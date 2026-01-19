# 设计文档

## 概述

高级检索功能为 MeterSphere 工作台模块提供统一的跨模块数据查询能力。该功能采用前后端分离架构，前端使用 Vue.js 2.7 + Element UI 构建交互界面，后端使用 Spring Boot + MyBatis 提供 RESTful API 服务。

### 核心设计目标

1. **统一查询入口**：整合测试用例、缺陷、测试计划、用例评审的查询功能
2. **灵活的条件组合**：支持动态添加筛选条件，不同字段类型使用对应的输入控件
3. **多视图展示**：提供列表模式和详情模式两种结果展示方式
4. **可扩展性**：字段元数据驱动，便于后续扩展新的业务模块
5. **复用现有机制**：基于项目已有的 `BaseQueryRequest` 和 `combine` 高级搜索机制扩展
6. **用户维度筛选**：支持按创建人、维护人、指派人等用户字段进行多选筛选
7. **跨项目字段兼容**：智能处理不同项目模板的自定义字段差异

### 技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 前端框架 | Vue.js 2.7.3 | 与现有项目保持一致 |
| UI 组件库 | Element UI 2.15.7 | 提供 Table、Select、Popover 等组件 |
| 状态管理 | Pinia 2.0.14 | 管理查询状态和用户配置（项目已使用） |
| 后端框架 | Spring Boot 3.2.12 | RESTful API 服务 |
| ORM | MyBatis 3.0.3 | 动态 SQL 构建 |
| 查询语言 | JQL (Jira Query Language) | 类似Jira的查询语法，支持复杂条件组合 |
| 数据库 | MySQL 8.0 | 数据存储 |
| 分页插件 | PageHelper 6.0.0 | 分页查询 |

## 字段设计

### 跨项目字段处理策略

由于 MeterSphere 支持每个项目配置独立的模板和自定义字段，跨项目查询时需要特殊处理：

#### 查询模式

| 模式 | 项目选择 | 可用字段 | 适用场景 |
|------|---------|---------|---------|
| **跨项目模式** | 可选多个项目 | 仅系统字段 + 全局自定义字段 | 跨项目数据汇总分析 |
| **单项目模式** | 只能选1个项目 | 系统字段 + 该项目的全部自定义字段 | 精细化条件筛选 |

#### 字段分类

| 字段类型 | 跨项目可用 | 说明 |
|---------|-----------|------|
| 系统字段 | ✅ | 所有项目都有：ID、标题、状态、创建人、创建时间等 |
| 全局自定义字段 | ✅ | 管理员创建的全局字段（global=1） |
| 项目自定义字段 | ❌ | 仅在单项目模式下可用 |

#### 交互设计

```
┌─────────────────────────────────────────────────────────────────┐
│  [测试用例 ▼]  [工作空间 ▼]  [项目: 全部 ▼]  [+ 筛选条件]      │
│                                                                 │
│  ⚠️ 跨项目查询仅支持系统字段筛选，如需使用自定义字段请选择单个项目  │
└─────────────────────────────────────────────────────────────────┘
```

### 用户维度字段

支持按用户进行数据筛选，不同业务模块有不同的用户字段：

| 用户字段 | 适用模块 | 数据库字段 | 说明 |
|---------|---------|-----------|------|
| **创建人** | 全部 | `create_user` | 数据创建者 |
| **最后更新人** | 全部 | `update_user` | 最后修改者 |
| **维护人** | 测试用例 | `maintainer` | 用例负责人 |
| **指派给** | 缺陷 | `assignee` | 缺陷处理人 |
| **负责人** | 测试计划 | `principal` | 计划负责人 |
| **评审人** | 用例评审 | `reviewer` | 评审执行人 |

#### 用户字段交互设计

```javascript
// 用户选择器配置
{
  field: 'creator',
  label: '创建人',
  type: 'user',           // 用户类型
  multiple: true,         // 支持多选
  maxSelection: 10,       // 最多选择10个用户
  searchable: true,       // 支持搜索
  showAvatar: true,       // 显示头像
  quickOptions: [         // 快捷选项
    { value: 'CURRENT_USER', label: '我自己' }
  ]
}
```

### 各模块字段清单

#### 通用系统字段（所有模块）

| 字段 | 类型 | 操作符 | 说明 |
|------|------|--------|------|
| name | text | like, =, != | 标题/名称 |
| num | text | =, like | 编号/ID |
| status | select | in, not_in | 状态 |
| createUser | user | in | 创建人 |
| updateUser | user | in | 最后更新人 |
| createTime | date | between, >, < | 创建时间 |
| updateTime | date | between, >, < | 更新时间 |

#### 测试用例专属字段

| 字段 | 类型 | 操作符 | 说明 |
|------|------|--------|------|
| priority | select | in | 优先级（P0-P3） |
| maintainer | user | in | 维护人 |
| type | select | in | 用例类型 |
| method | select | in | 用例方式 |
| nodeId | treeSelect | in | 所属模块 |
| reviewStatus | select | in | 评审状态 |

#### 缺陷专属字段

| 字段 | 类型 | 操作符 | 说明 |
|------|------|--------|------|
| assignee | user | in | 指派给 |
| severity | select | in | 严重程度 |
| platform | select | in | 缺陷平台 |
| resourceId | text | = | 关联用例ID |

## 架构

### 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Vue.js)                             │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │  Sidebar    │  │ FilterBar   │  │ ResultView  │              │
│  │  导航栏     │  │ 筛选条件栏  │  │ 结果展示区  │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│         │               │                │                       │
│         └───────────────┼────────────────┘                       │
│                         ▼                                        │
│              ┌─────────────────────┐                             │
│              │   Pinia Store       │                             │
│              │   (查询状态管理)    │                             │
│              └─────────────────────┘                             │
│                         │                                        │
│              ┌─────────────────────┐                             │
│              │   JQL Editor        │                             │
│              │   (JQL查询编辑器)   │                             │
│              └─────────────────────┘                             │
└─────────────────────────────────────────────────────────────────┘
                          │ HTTP/JSON
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                     后端 (Spring Boot)                           │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────┐  ┌─────────────────────┐               │
│  │ AdvancedSearchCtrl  │  │ FieldMetadataCtrl   │               │
│  │ 查询控制器          │  │ 字段元数据控制器    │               │
│  └─────────────────────┘  └─────────────────────┘               │
│              │                      │                            │
│              ▼                      ▼                            │
│  ┌─────────────────────┐  ┌─────────────────────┐               │
│  │ AdvancedSearchSvc   │  │ FieldMetadataSvc    │               │
│  │ 查询服务            │  │ 字段元数据服务      │               │
│  └─────────────────────┘  └─────────────────────┘               │
│              │                                                   │
│              ▼                                                   │
│  ┌─────────────────────────────────────────────┐                │
│  │           JQL Parser                         │                │
│  │           (JQL语法解析器)                    │                │
│  └─────────────────────────────────────────────┘                │
│              │                                                   │
│              ▼                                                   │
│  ┌─────────────────────────────────────────────┐                │
│  │           QueryBuilder                       │                │
│  │           (动态 SQL 构建器)                  │                │
│  └─────────────────────────────────────────────┘                │
│              │                                                   │
│              ▼                                                   │
│  ┌─────────────────────────────────────────────┐                │
│  │           MyBatis Mapper                     │                │
│  │           (数据访问层)                       │                │
│  └─────────────────────────────────────────────┘                │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                     MySQL 数据库                                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐   │
│  │test_case │ │ issues   │ │test_plan │ │test_case_review  │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 前端组件架构

```
AdvancedSearch.vue (主页面)
├── Sidebar.vue (侧边栏导航)
│   ├── NavGroup (查询中心)
│   └── NavGroup (共享视图)
├── TopFilterBar.vue (顶部筛选栏)
│   ├── ModuleSelector (业务模块选择)
│   ├── WorkspaceSelector (工作空间多选)
│   ├── ProjectSelector (项目多选)
│   ├── FilterPopover (筛选条件选择器)
│   └── QueryModeSwitch (查询模式切换：可视化/JQL)
├── ActiveTagsBar.vue (已选条件标签栏)
│   └── ConditionTag (条件标签)
├── JQLEditor.vue (JQL查询编辑器)
│   ├── JQLInput (JQL输入框)
│   ├── SyntaxValidator (语法验证)
│   ├── AutoComplete (智能提示)
│   └── JQLHelper (语法帮助)
├── ResultToolbar.vue (结果工具栏)
│   ├── ViewModeSwitch (视图模式切换)
│   ├── ColumnConfig (列配置)
│   └── ExportButton (导出按钮)
└── ResultView.vue (结果展示区)
    ├── ListView.vue (列表视图)
    └── SplitView.vue (详情视图)
        ├── ItemList (左侧列表)
        └── DetailPanel (右侧详情)
```

## 组件和接口

### 后端 API 接口

#### 1. 查询接口

```
POST /workstation/advanced-search/query/{goPage}/{pageSize}
```

**请求体（支持JQL和传统combine两种模式）：**

**JQL模式：**
```json
{
  "module": "TEST_CASE",
  "workspaceIds": ["ws-001", "ws-002"],
  "projectIds": ["proj-001"],
  "useJQL": true,
  "jql": "project = '电商平台' AND status IN ('Pass', 'Prepare') AND priority = 'P0'",
  "orders": [
    {"name": "update_time", "type": "desc"}
  ]
}
```

**传统Combine模式（向后兼容）：**
```json
{
  "module": "TEST_CASE",
  "workspaceIds": ["ws-001", "ws-002"],
  "projectIds": ["proj-001"],
  "useJQL": false,
  "combine": {
    "name": {
      "operator": "like",
      "value": "登录"
    },
    "status": {
      "operator": "in",
      "value": ["Prepare", "Pass"]
    },
    "createTime": {
      "operator": "between",
      "value": [1704067200000, 1704153600000]
    }
  },
  "filters": {
    "priority": ["P0", "P1"]
  },
  "orders": [
    {"name": "update_time", "type": "desc"}
  ]
}
```

**响应体：**
```json
{
  "success": true,
  "data": {
    "listObject": [
      {
        "id": "tc-001",
        "name": "用户登录功能测试",
        "status": "Pass",
        "priority": "P0",
        "projectName": "电商平台",
        "workspaceName": "研发中心",
        "createUser": "张三",
        "createTime": 1704067200000,
        "updateTime": 1704153600000
      }
    ],
    "itemCount": 156
  }
}
```

#### 2. 字段元数据接口

```
GET /workstation/advanced-search/fields/{module}
```

**请求参数：**
- `module`: 业务模块（TEST_CASE, ISSUE, TEST_PLAN, TEST_CASE_REVIEW）
- `projectId`: 项目ID（可选，传入时返回该项目的自定义字段）

```
GET /workstation/advanced-search/fields/ISSUE?projectId=proj-001
```

**响应体：**
```json
{
  "success": true,
  "data": {
    "systemFields": [
      {
        "field": "name",
        "label": "标题",
        "type": "text",
        "operators": ["like", "=", "!="],
        "group": "basic"
      },
      {
        "field": "status",
        "label": "状态",
        "type": "select",
        "operators": ["in", "not_in"],
        "options": [
          {"value": "new", "label": "新建"},
          {"value": "resolved", "label": "已解决"},
          {"value": "closed", "label": "已关闭"}
        ],
        "group": "basic"
      },
      {
        "field": "creator",
        "label": "创建人",
        "type": "user",
        "operators": ["in"],
        "multiple": true,
        "maxSelection": 10,
        "group": "audit"
      },
      {
        "field": "assignee",
        "label": "指派给",
        "type": "user",
        "operators": ["in"],
        "multiple": true,
        "maxSelection": 10,
        "group": "module"
      },
      {
        "field": "createTime",
        "label": "创建时间",
        "type": "date",
        "operators": ["between", ">", "<"],
        "group": "audit"
      }
    ],
    "customFields": [
      {
        "field": "custom_severity",
        "label": "严重级别",
        "type": "select",
        "operators": ["in"],
        "options": [
          {"value": "一般", "label": "一般"},
          {"value": "严重", "label": "严重"},
          {"value": "阻断", "label": "阻断"}
        ],
        "group": "custom",
        "projectSpecific": true
      }
    ],
    "fieldGroups": [
      {"key": "basic", "label": "基础信息"},
      {"key": "module", "label": "模块专属"},
      {"key": "audit", "label": "审计追踪"},
      {"key": "custom", "label": "自定义字段"}
    ]
  }
}
```

#### 3. 用户列表接口（用于用户字段选择器）

```
GET /workstation/advanced-search/users
```

**请求参数：**
- `workspaceIds`: 工作空间ID列表（逗号分隔）
- `keyword`: 搜索关键词（用户名/姓名）
- `pageNum`: 页码
- `pageSize`: 每页数量

**响应体：**
```json
{
  "success": true,
  "data": {
    "list": [
      {
        "id": "user-001",
        "name": "张三",
        "email": "zhangsan@example.com",
        "avatar": "/avatar/user-001.png"
      }
    ],
    "total": 50
  }
}
```

#### 4. JQL语法验证接口

```
POST /workstation/advanced-search/jql/validate
```

**请求体：**
```json
{
  "jql": "project = '电商平台' AND status IN ('Pass', 'Prepare')",
  "module": "TEST_CASE"
}
```

**响应体：**
```json
{
  "success": true,
  "data": {
    "valid": true,
    "message": "JQL语法正确",
    "suggestions": []
  }
}
```

#### 5. JQL智能提示接口

```
POST /workstation/advanced-search/jql/suggestions
```

**请求体：**
```json
{
  "context": "project = '电商' AND ",
  "module": "TEST_CASE",
  "cursorPosition": 20
}
```

**响应体：**
```json
{
  "success": true,
  "data": [
    {
      "type": "field",
      "value": "status",
      "description": "状态字段",
      "insertText": "status"
    },
    {
      "type": "field", 
      "value": "priority",
      "description": "优先级字段",
      "insertText": "priority"
    }
  ]
}
```

#### 6. 详情接口

```
GET /workstation/advanced-search/detail/{module}/{id}
```

**响应体：**
```json
{
  "success": true,
  "data": {
    "id": "tc-001",
    "name": "用户登录功能测试",
    "status": "Pass",
    "priority": "P0",
    "prerequisite": "用户已注册",
    "steps": [
      {"num": 1, "desc": "输入用户名", "result": "显示用户名"}
    ],
    "remark": "核心功能测试",
    "customFields": [
      {"name": "所属模块", "value": "用户中心"}
    ]
  }
}
```

### 前端组件接口

#### 1. Pinia Store (advancedSearchStore)

```javascript
// store/modules/advancedSearch.js
import { defineStore } from 'pinia';

export const useAdvancedSearchStore = defineStore('advancedSearch', {
  state: () => ({
    // 当前选中的业务模块
    currentModule: 'TEST_CASE',
    // 已选工作空间
    selectedWorkspaces: [],
    // 已选项目
    selectedProjects: [],
    // 查询模式：'visual' | 'jql'
    queryMode: 'visual',
    // JQL查询字符串
    jqlQuery: '',
    // 筛选条件（combine 格式）
    combine: {},
    // 过滤条件（filters 格式）
    filters: {},
    // 排序条件
    orders: [{ name: 'update_time', type: 'desc' }],
    // 查询结果
    results: {
      total: 0,
      list: [],
      pageNum: 1,
      pageSize: 20
    },
    // 视图模式: 'list' | 'split'
    viewMode: 'list',
    // 当前选中的详情项
    selectedItem: null,
    // 字段元数据缓存
    fieldMetadata: {},
    // 列配置
    columnConfig: {}
  }),
  
  actions: {
    // 执行查询
    async executeQuery(pageNum = 1, pageSize = 20) {
      const request = {
        module: this.currentModule,
        workspaceIds: this.selectedWorkspaces,
        projectIds: this.selectedProjects,
        useJQL: this.queryMode === 'jql',
        orders: this.orders
      };
      
      if (this.queryMode === 'jql') {
        request.jql = this.jqlQuery;
      } else {
        request.combine = this.combine;
        request.filters = this.filters;
      }
      
      // 调用 API
    },
    // 切换查询模式
    switchQueryMode(mode) {
      this.queryMode = mode;
      if (mode === 'jql') {
        // 将当前combine条件转换为JQL
        this.jqlQuery = this.convertCombineToJQL();
      } else {
        // 清空JQL，使用可视化条件
        this.jqlQuery = '';
      }
    },
    // 将combine条件转换为JQL
    convertCombineToJQL() {
      // 实现combine到JQL的转换逻辑
      return '';
    },
    // 验证JQL语法
    async validateJQL(jql) {
      // 调用后端验证接口
    },
    // 获取JQL智能提示
    async getJQLSuggestions(context, cursorPosition) {
      // 调用后端智能提示接口
    },
    // 添加筛选条件
    addCondition(field, operator, value) {
      this.combine[field] = { operator, value };
    },
    // 移除筛选条件
    removeCondition(field) {
      delete this.combine[field];
    },
    // 清空所有条件
    clearConditions() {
      this.combine = {};
      this.filters = {};
    },
    // 切换视图模式
    setViewMode(mode) {
      this.viewMode = mode;
    },
    // 加载字段元数据
    async loadFieldMetadata(module, projectId = null) {
      // 调用 API 获取字段元数据
      // 如果 projectId 为空或选择了多个项目，只返回系统字段
      // 如果选择了单个项目，返回系统字段 + 该项目的自定义字段
    },
    // 切换模块时重置状态
    switchModule(module) {
      this.currentModule = module;
      this.combine = {};
      this.filters = {};
      this.jqlQuery = '';
      this.results = { total: 0, list: [], pageNum: 1, pageSize: 20 };
      this.selectedItem = null;
      // 重新加载字段元数据
      this.loadFieldMetadata(module, this.singleProjectId);
    },
    // 项目选择变更时更新可用字段
    onProjectChange(projectIds) {
      this.selectedProjects = projectIds;
      // 如果选择了单个项目，加载该项目的自定义字段
      const projectId = projectIds.length === 1 ? projectIds[0] : null;
      this.loadFieldMetadata(this.currentModule, projectId);
      // 清除不再可用的筛选条件
      this.clearInvalidConditions();
    }
  },
  
  getters: {
    // 是否为单项目模式
    isSingleProjectMode: (state) => state.selectedProjects.length === 1,
    // 当前单选的项目ID
    singleProjectId: (state) => state.selectedProjects.length === 1 ? state.selectedProjects[0] : null,
    // 可用的筛选字段（根据项目选择动态变化）
    availableFields: (state) => {
      if (state.isSingleProjectMode) {
        return [...state.fieldMetadata.systemFields, ...state.fieldMetadata.customFields];
      }
      return state.fieldMetadata.systemFields || [];
    }
  },
  
  // 持久化配置（使用 pinia-plugin-persistedstate）
  persist: {
    key: 'advanced-search',
    paths: ['viewMode', 'columnConfig', 'queryMode']
  }
});
```

#### 2. 组件 Props 定义

**TopFilterBar.vue**
```javascript
props: {
  // 当前模块
  module: { type: String, required: true },
  // 字段元数据
  fields: { type: Array, default: () => [] }
}

emits: ['module-change', 'condition-add', 'search']
```

**ResultView.vue**
```javascript
props: {
  // 结果数据
  data: { type: Array, default: () => [] },
  // 总数
  total: { type: Number, default: 0 },
  // 视图模式
  viewMode: { type: String, default: 'list' },
  // 列配置
  columns: { type: Array, default: () => [] }
}

emits: ['page-change', 'item-select', 'sort-change']
```

### 数据模型

#### 1. 查询请求（继承 BaseQueryRequest，扩展JQL支持）

```java
/**
 * 高级检索请求 DTO
 * 继承 BaseQueryRequest 复用现有的 filters、combine、orders 机制
 * 扩展支持JQL查询语法
 */
@Getter
@Setter
public class AdvancedSearchRequest extends BaseQueryRequest {
    
    /**
     * 业务模块：TEST_CASE, ISSUE, TEST_PLAN, TEST_CASE_REVIEW
     */
    private String module;
    
    /**
     * 工作空间ID列表（跨工作空间查询）
     */
    private List<String> workspaceIds;
    
    /**
     * 项目ID列表（跨项目查询）
     */
    private List<String> projectIds;
    
    /**
     * 是否使用JQL模式
     */
    private Boolean useJQL = false;
    
    /**
     * JQL查询字符串
     */
    private String jql;
    
    /**
     * 排序字段
     */
    private String sortField;
    
    /**
     * 排序方向：ASC/DESC
     */
    private String sortOrder;
}
```

#### 2. JQL语法支持

支持类似Jira JQL的查询语法：

```sql
-- 基础语法示例
project = "电商平台" AND status IN ("Pass", "Prepare") AND priority = "P0"

-- 用户字段查询
assignee = "张三" OR creator IN ("李四", "王五")

-- 时间范围查询
createTime >= "2024-01-01" AND updateTime <= "2024-12-31"

-- 文本搜索
name ~ "登录" AND description CONTAINS "功能测试"

-- 复合条件
(priority = "P0" OR priority = "P1") AND status != "Deprecated"
```

**支持的操作符：**

| 操作符 | 说明 | 示例 |
|--------|------|------|
| `=` | 等于 | `status = "Pass"` |
| `!=` | 不等于 | `priority != "P0"` |
| `~` | 模糊匹配 | `name ~ "登录"` |
| `IN` | 包含于 | `status IN ("Pass", "Prepare")` |
| `NOT IN` | 不包含于 | `priority NOT IN ("P3", "P4")` |
| `>`, `>=` | 大于、大于等于 | `createTime >= "2024-01-01"` |
| `<`, `<=` | 小于、小于等于 | `updateTime <= "2024-12-31"` |
| `CONTAINS` | 包含文本 | `description CONTAINS "功能测试"` |
| `AND` | 逻辑与 | `status = "Pass" AND priority = "P0"` |
| `OR` | 逻辑或 | `assignee = "张三" OR creator = "李四"` |

#### 3. 查询条件（复用 combine 机制）

项目已有的 `combine` 字段支持高级搜索，格式如下：

```json
{
  "combine": {
    "name": {
      "operator": "like",
      "value": "登录"
    },
    "status": {
      "operator": "in",
      "value": ["Prepare", "Pass"]
    },
    "createTime": {
      "operator": "between",
      "value": [1704067200000, 1704153600000]
    },
    "creator": {
      "operator": "in",
      "value": ["user-001", "user-002"]
    }
  }
}
```

#### 4. 字段元数据 (FieldMetadata)

```java
@Getter
@Setter
public class FieldMetadata {
    private String field;           // 字段名（对应 combine 的 key）
    private String label;           // 显示名称
    private String type;            // 类型: text, select, date, user, multiSelect, treeSelect
    private List<String> operators; // 支持的操作符: like, =, in, between, gt, lt
    private List<Option> options;   // 选项（select/multiSelect 类型）
    private String group;           // 分组：basic, module, audit, custom
    private Boolean multiple;       // 是否多选（user 类型）
    private Integer maxSelection;   // 最大选择数量（user 类型）
    private Boolean projectSpecific;// 是否项目特有字段（自定义字段）
}
```

#### 5. 查询结果 (AdvancedSearchResult)

```java
@Getter
@Setter
public class AdvancedSearchResult<T> {
    private Long total;             // 总记录数
    private List<T> list;           // 数据列表
    private Integer pageNum;        // 当前页码
    private Integer pageSize;       // 每页数量
}
```

## 数据流

### 查询流程

```
用户操作                    前端组件                     后端服务                    数据库
   │                          │                           │                          │
   │  1. 选择模块/条件        │                           │                          │
   ├─────────────────────────>│                           │                          │
   │                          │  2. 更新 Store 状态       │                          │
   │                          ├──────────────┐            │                          │
   │                          │              │            │                          │
   │                          │<─────────────┘            │                          │
   │                          │                           │                          │
   │  3. 点击查询             │                           │                          │
   ├─────────────────────────>│                           │                          │
   │                          │  4. POST /query           │                          │
   │                          ├──────────────────────────>│                          │
   │                          │                           │  5. 构建动态 SQL         │
   │                          │                           ├──────────────┐           │
   │                          │                           │              │           │
   │                          │                           │<─────────────┘           │
   │                          │                           │  6. 执行查询             │
   │                          │                           ├─────────────────────────>│
   │                          │                           │                          │
   │                          │                           │  7. 返回结果             │
   │                          │                           │<─────────────────────────│
   │                          │  8. 返回 JSON             │                          │
   │                          │<──────────────────────────│                          │
   │                          │                           │                          │
   │                          │  9. 更新结果状态          │                          │
   │                          ├──────────────┐            │                          │
   │                          │              │            │                          │
   │                          │<─────────────┘            │                          │
   │  10. 渲染结果            │                           │                          │
   │<─────────────────────────│                           │                          │
   │                          │                           │                          │
```

### JQL解析和SQL构建流程

```java
// JQL解析和SQL构建核心逻辑
public class JQLQueryProcessor {
    
    @Resource
    private JQLParser jqlParser;
    
    @Resource
    private JQLToSQLConverter sqlConverter;
    
    /**
     * 处理JQL查询请求
     */
    public String processJQLQuery(AdvancedSearchRequest request) {
        if (request.getUseJQL() && StringUtils.isNotBlank(request.getJql())) {
            // 1. 解析JQL为抽象语法树
            QueryNode ast = jqlParser.parseJQL(request.getJql());
            
            // 2. 验证字段权限和有效性
            validateFields(ast, request.getModule());
            
            // 3. 转换为SQL WHERE子句
            return sqlConverter.convertToSQL(ast, request.getModule());
        } else {
            // 4. 使用传统combine机制（向后兼容）
            return buildCombineSQL(request);
        }
    }
    
    /**
     * JQL缓存策略
     */
    public QueryNode getCachedAST(String jql) {
        String cacheKey = "jql:ast:" + DigestUtils.md5Hex(jql);
        return redisTemplate.opsForValue().get(cacheKey);
    }
}
```

## 正确性属性

### P1: 查询条件完整性
- 所有用户添加的筛选条件必须正确转换为 SQL WHERE 子句
- 条件之间默认使用 AND 连接
- 空条件不应影响查询结果

### P2: 分页正确性
- pageNum 和 pageSize 必须正确应用到 SQL LIMIT 子句
- total 必须返回不带分页的总记录数
- 页码超出范围时返回空列表而非错误

### P3: 权限过滤
- 查询结果必须限制在用户有权限访问的工作空间和项目范围内
- 即使请求中包含无权限的 workspaceId/projectId，也不应返回对应数据

### P4: SQL 注入防护
- 所有用户输入必须通过参数化查询处理
- 字段名必须在白名单内，不允许任意字段查询

### P6: JQL语法安全性
- JQL解析器必须验证字段名白名单，防止任意字段查询
- JQL转SQL过程必须使用参数化查询，防止SQL注入
- 复杂JQL表达式必须有深度限制，防止解析栈溢出

### P7: JQL性能约束
- JQL解析结果必须缓存，避免重复解析相同查询
- 单个JQL表达式的条件数量不超过50个
- JQL解析时间不超过100ms
- 单次查询响应时间 < 3 秒（1000 条以内数据）
- 支持的最大 pageSize = 100
- 条件数量上限 = 20

## 文件结构

### 后端文件

```
workstation/backend/src/main/java/io/metersphere/
├── workstation/
│   ├── controller/
│   │   └── AdvancedSearchController.java      # 查询控制器
│   └── service/
│       ├── AdvancedSearchService.java         # 查询服务
│       ├── FieldMetadataService.java          # 字段元数据服务
│       ├── JQLParser.java                     # JQL语法解析器
│       ├── JQLToSQLConverter.java             # JQL到SQL转换器
│       └── JQLCacheService.java               # JQL缓存服务
├── request/
│   └── AdvancedSearchRequest.java             # 查询请求 DTO（继承 BaseQueryRequest）
├── dto/
│   ├── AdvancedSearchResult.java              # 查询结果 DTO
│   ├── FieldMetadata.java                     # 字段元数据 DTO
│   ├── JQLValidationResult.java               # JQL验证结果 DTO
│   └── JQLSuggestion.java                     # JQL智能提示 DTO
└── base/mapper/
    ├── AdvancedSearchMapper.java              # MyBatis Mapper 接口
    └── ext/
        └── ExtAdvancedSearchMapper.java       # 扩展 Mapper
    
workstation/backend/src/main/resources/
└── mapper/
    └── ext/
        └── ExtAdvancedSearchMapper.xml        # MyBatis XML（动态 SQL）
```

### 前端文件

```
workstation/frontend/src/
├── business/
│   └── advanced-search/                       # 高级检索业务模块
│       ├── AdvancedSearch.vue                 # 主页面
│       └── components/
│           ├── Sidebar.vue                    # 侧边栏
│           ├── TopFilterBar.vue               # 顶部筛选栏
│           ├── ActiveTagsBar.vue              # 已选条件标签
│           ├── JQLEditor.vue                  # JQL查询编辑器
│           ├── QueryModeSwitch.vue            # 查询模式切换
│           ├── ResultToolbar.vue              # 结果工具栏
│           ├── ResultView.vue                 # 结果展示区
│           ├── ListView.vue                   # 列表视图
│           ├── SplitView.vue                  # 分屏视图
│           ├── FieldSelector.vue              # 字段选择器
│           └── ConditionInput.vue             # 条件输入组件
├── api/
│   └── advanced-search.js                     # API 接口定义
├── store/
│   └── modules/
│       └── advancedSearch.js                  # Pinia Store（新增）
├── router/
│   └── modules/
│       └── workstation.js                     # 路由配置（添加新路由）
└── i18n/
    └── lang/
        ├── zh-CN.js                           # 中文国际化（添加词条）
        └── en-US.js                           # 英文国际化（添加词条）
```

### 与现有代码的集成点

| 集成点 | 现有代码 | 集成方式 |
|--------|---------|---------|
| 请求基类 | `BaseQueryRequest` | 继承，复用 combine/filters/orders |
| 分页工具 | `PageHelper` + `PageUtils` | 直接使用 |
| 权限校验 | `@RequiresPermissions` | 添加新权限点 |
| 用户服务 | `UserService` | 调用获取用户列表 |
| 项目服务 | `ProjectService` | 调用获取项目列表 |
| 工作空间服务 | `WorkspaceService` | 调用获取工作空间列表 |
| 前端 Store | `pinia` + `pinia-plugin-persistedstate` | 新增 Store 模块 |
| 前端组件 | `metersphere-frontend` 共享组件 | 复用 MsTable、MsSelect 等 |

## 实现注意事项

### 安全性
1. 所有查询必须经过权限校验（使用 `@RequiresPermissions` 注解）
2. 使用 MyBatis `#{}` 参数化查询防止 SQL 注入
3. 字段名白名单校验，不允许任意字段查询
4. 工作空间/项目 ID 必须校验用户访问权限

### 性能优化
1. 字段元数据缓存（前端 Store + 后端 Redis）
2. 查询结果分页，使用 PageHelper 限制单次返回数量
3. 复杂查询添加索引提示
4. 跨表查询使用 LEFT JOIN 而非子查询

### 可扩展性
1. 新增业务模块只需添加字段配置和 Mapper XML
2. 操作符和字段类型可通过配置扩展
3. 视图模式可插件化扩展

### 与现有代码的兼容性
1. 继承 `BaseQueryRequest`，复用现有的 `combine`、`filters`、`orders` 机制
2. 使用项目已有的 `PageHelper` + `PageUtils` 分页方案
3. 复用 `metersphere-frontend` 共享组件库
4. 遵循项目现有的 Controller → Service → Mapper 分层架构
5. 使用项目已有的 Pinia + pinia-plugin-persistedstate 状态管理方案

### 国际化
1. 所有前端文案使用 `$t('key')` 国际化
2. 字段 label 支持国际化 key
3. 错误提示信息国际化
