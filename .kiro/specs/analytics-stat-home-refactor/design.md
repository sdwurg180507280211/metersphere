# analytics-stat 首页重构设计文档

## 一、设计概述

本文档描述 analytics-stat 模块首页重构的技术实现方案，包括组件设计、路由配置、数据流设计等。

## 二、架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│  qiankun 主应用                                         │
│  ┌───────────────────────────────────────────────────┐  │
│  │  analytics-stat 子应用                            │  │
│  │  ┌─────────────────────────────────────────────┐  │  │
│  │  │  Layout (metersphere-frontend)              │  │  │
│  │  │  ┌───────────────────────────────────────┐  │  │  │
│  │  │  │  AnalyticsStatHome.vue                │  │  │  │
│  │  │  │  ┌──────┬──────────────────────────┐  │  │  │  │
│  │  │  │  │ 左侧 │  右侧内容区              │  │  │  │  │
│  │  │  │  │ 菜单 │  - QueryCountCard        │  │  │  │  │
│  │  │  │  │      │  - DataVolumeCard        │  │  │  │  │
│  │  │  │  │      │  - QuickAccessCard       │  │  │  │  │
│  │  │  │  │      │  - RecentQueryList       │  │  │  │  │
│  │  │  │  └──────┴──────────────────────────┘  │  │  │  │
│  │  │  └───────────────────────────────────────┘  │  │  │
│  │  └─────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 2.2 组件层次结构

```
AnalyticsStatHome.vue (主组件)
├── ms-container (容器组件)
│   └── ms-main-container (主内容容器)
│       ├── 左侧菜单区域
│       │   └── el-menu (Element UI 菜单)
│       └── 右侧内容区域
│           ├── el-row (统计卡片行)
│           │   ├── el-col (左列)
│           │   │   └── QueryCountCard.vue
│           │   └── el-col (右列)
│           │       └── DataVolumeCard.vue
│           ├── el-row (快捷入口行)
│           │   └── QuickAccessCard.vue
│           └── el-row (最近查询行)
│               └── RecentQueryList.vue
```

## 三、组件设计

### 3.1 AnalyticsStatHome.vue (主组件)

**职责**：
- 管理整体布局
- 协调子组件
- 处理路由跳转
- 管理全局状态

**Props**：无

**Data**：
```javascript
{
  activeMenu: 'home',           // 当前激活的菜单项
  queryCount: 0,                // 查询次数
  dataVolume: 0,                // 数据量
  recentQueries: [],            // 最近查询列表
  loading: false                // 加载状态
}
```


**Methods**：
```javascript
{
  // 初始化数据
  initData() {},
  
  // 刷新统计数据
  refreshStats() {},
  
  // 处理菜单点击
  handleMenuClick(menuItem) {},
  
  // 跳转到指定页面
  redirectTo(page) {}
}
```

**生命周期**：
- `mounted()`: 初始化数据，加载统计信息
- `activated()`: 子应用激活时刷新数据

### 3.2 QueryCountCard.vue (查询次数统计卡片)

**职责**：展示查询次数统计

**Props**：
```javascript
{
  count: {
    type: Number,
    default: 0
  },
  trend: {
    type: String,  // 'up' | 'down' | 'stable'
    default: 'stable'
  }
}
```

**参考**：`test-track/frontend/src/business/home/components/CaseCountCard.vue`

### 3.3 DataVolumeCard.vue (数据量统计卡片)

**职责**：展示数据量统计

**Props**：
```javascript
{
  volume: {
    type: Number,
    default: 0
  },
  unit: {
    type: String,  // 'MB' | 'GB' | 'TB'
    default: 'MB'
  }
}
```

### 3.4 QuickAccessCard.vue (快捷入口卡片)

**职责**：提供常用功能的快捷入口

**Props**：
```javascript
{
  items: {
    type: Array,
    default: () => [
      { name: 'SQL查询台', icon: 'el-icon-document', path: '/analytics-stat/sql-console' },
      { name: '数据字典', icon: 'el-icon-collection', path: '/analytics-stat/data-dictionary' },
      { name: '综合查询', icon: 'el-icon-search', path: '/analytics-stat/query' }
    ]
  }
}
```

### 3.5 RecentQueryList.vue (最近查询列表)

**职责**：展示最近的查询记录

**Props**：
```javascript
{
  queries: {
    type: Array,
    default: () => []
  }
}
```

**数据结构**：
```javascript
{
  id: String,
  name: String,
  type: String,      // 'sql' | 'query'
  createTime: Date,
  status: String     // 'success' | 'failed'
}
```

## 四、路由设计

### 4.1 路由配置

**文件**：`analytics-stat/frontend/src/router/modules/analytics.js`

```javascript
import Layout from "metersphere-frontend/src/business/app-layout";

export default {
  path: "/analytics-stat",
  name: "analytics-stat",
  redirect: "/analytics-stat/home",
  component: Layout,
  children: [
    {
      path: "home",
      name: "analyticsStatHome",
      component: () => import("@/business/home/AnalyticsStatHome.vue"),
      meta: { 
        title: "工作台",
        requiresAuth: true
      }
    },
    {
      path: "sql-console",
      name: "SqlConsole",
      component: () => import("@/views/SqlConsole.vue"),
      meta: { title: "SQL查询台" }
    },
    {
      path: "data-dictionary",
      name: "DataDictionary",
      component: () => import("@/views/DataDictionary.vue"),
      meta: { title: "数据字典" }
    }
  ]
};
```

### 4.2 路由守卫

使用 metersphere-frontend 的统一路由守卫：
- 权限验证
- 登录状态检查
- 页面标题设置

## 五、数据流设计

### 5.1 数据获取流程

```
AnalyticsStatHome.vue
    ↓ mounted()
    ↓ initData()
    ↓
API 请求
    ↓
    ├─→ getQueryCount()      → QueryCountCard
    ├─→ getDataVolume()      → DataVolumeCard
    └─→ getRecentQueries()   → RecentQueryList
```

### 5.2 API 接口设计

**基础路径**：`/analytics-stat/api/v1/`

**接口列表**：

1. **获取查询次数统计**
   - 路径：`GET /stats/query-count`
   - 响应：
   ```json
   {
     "code": 0,
     "data": {
       "total": 1234,
       "today": 56,
       "trend": "up"
     }
   }
   ```

2. **获取数据量统计**
   - 路径：`GET /stats/data-volume`
   - 响应：
   ```json
   {
     "code": 0,
     "data": {
       "volume": 1024,
       "unit": "MB"
     }
   }
   ```

3. **获取最近查询列表**
   - 路径：`GET /queries/recent?limit=10`
   - 响应：
   ```json
   {
     "code": 0,
     "data": [
       {
         "id": "1",
         "name": "用户统计查询",
         "type": "sql",
         "createTime": "2026-02-09T10:00:00",
         "status": "success"
       }
     ]
   }
   ```

## 六、样式设计

### 6.1 布局样式

参考 `test-track/frontend/src/business/home/TrackHome.vue` 的样式：

```scss
.analytics-stat-home-layout {
  margin: 12px 24px;
  min-width: 1100px;
  
  // 统计卡片样式
  .dashboard-card {
    height: 208px;
    background-color: #ffffff;
    border: 1px solid #dee0e3;
    border-radius: 4px;
  }
  
  // 表格样式
  .home-table {
    background-color: #ffffff;
    margin-top: 16px;
  }
}
```

### 6.2 响应式设计

- 最小宽度：1100px
- 栅格布局：使用 Element UI 的 24 列栅格
- 卡片间距：16px

## 七、状态管理

### 7.1 本地状态

使用 Vue 组件的 `data` 管理本地状态：
- 加载状态
- 统计数据
- 最近查询列表

### 7.2 全局状态

使用 Pinia store 管理全局状态（如需要）：
- 用户信息
- 权限信息
- 主题配置

## 八、性能优化

### 8.1 懒加载

- 路由组件懒加载
- 图表组件按需加载

### 8.2 数据缓存

- 统计数据缓存 5 分钟
- 最近查询列表缓存 1 分钟

### 8.3 防抖节流

- 搜索输入防抖 300ms
- 滚动加载节流 200ms

## 九、错误处理

### 9.1 API 错误处理

```javascript
try {
  const res = await getQueryCount();
  this.queryCount = res.data.total;
} catch (error) {
  this.$message.error('获取统计数据失败');
  console.error(error);
}
```

### 9.2 组件错误边界

使用 Vue 的 `errorCaptured` 钩子捕获子组件错误。

## 十、测试策略

### 10.1 单元测试

- 组件渲染测试
- 方法逻辑测试
- Props 验证测试

### 10.2 集成测试

- 路由跳转测试
- API 调用测试
- 用户交互测试

### 10.3 E2E 测试

- 完整用户流程测试
- 跨模块跳转测试

## 十一、迁移方案

### 11.1 向后兼容

- 保留旧路由 `/analytics-stat/dashboard` 重定向到 `/analytics-stat/home`
- 保留 1 个版本后删除

### 11.2 数据迁移

- 无需数据迁移（纯前端重构）

### 11.3 用户通知

- 在首页显示提示信息："首页已升级，体验更好的工作台"
- 提供反馈入口

## 十二、正确性属性

### 12.1 路由正确性

**属性 1.1**：访问 `/analytics-stat` 应重定向到 `/analytics-stat/home`

**验证方式**：
```javascript
// 单元测试
it('should redirect to home', () => {
  const router = createRouter();
  router.push('/analytics-stat');
  expect(router.currentRoute.value.path).toBe('/analytics-stat/home');
});
```

### 12.2 布局正确性

**属性 2.1**：左侧菜单应始终可见

**验证方式**：
```javascript
// 组件测试
it('should render left menu', () => {
  const wrapper = mount(AnalyticsStatHome);
  expect(wrapper.find('.left-menu').exists()).toBe(true);
});
```

### 12.3 数据正确性

**属性 3.1**：统计数据应为非负整数

**验证方式**：
```javascript
// 属性测试
property('query count should be non-negative', () => {
  const count = getQueryCount();
  return count >= 0 && Number.isInteger(count);
});
```

## 十三、实施步骤

### 步骤 1：创建组件目录结构

```bash
mkdir -p analytics-stat/frontend/src/business/home/components
```

### 步骤 2：创建主组件

创建 `AnalyticsStatHome.vue`

### 步骤 3：创建子组件

创建统计卡片和列表组件

### 步骤 4：修改路由配置

更新 `router/modules/analytics.js`

### 步骤 5：删除旧组件

删除 `views/Dashboard.vue`

### 步骤 6：测试验证

功能测试、布局测试、性能测试

### 步骤 7：文档更新

更新相关文档
