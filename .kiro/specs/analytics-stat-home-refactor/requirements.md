# analytics-stat 首页重构需求文档

## 一、需求概述

将 analytics-stat 模块的首页从简单的导航页面重构为与其他模块一致的工作台首页，采用左右布局，左侧为功能菜单，右侧为内容区域。

## 二、背景

### 2.1 当前状态

- **路由**：`/analytics-stat/dashboard`
- **组件**：`Dashboard.vue`（简单的欢迎页 + 功能入口卡片）
- **布局**：上下布局，无侧边菜单
- **功能**：仅提供功能入口导航

### 2.2 目标状态

- **路由**：`/analytics-stat/home`
- **组件**：`AnalyticsStatHome.vue`（工作台首页）
- **布局**：左右布局（左侧菜单 + 右侧内容区）
- **功能**：展示统计数据、快捷入口、任务列表等

### 2.3 参考模块

- `test-track/frontend/src/business/home/TrackHome.vue`
- `api-test/frontend/src/business/home/ApiHome.vue`
- `performance-test/frontend/src/business/home/PerformanceTestHome.vue`

## 三、用户故事

### 3.1 作为分析统计模块用户

**我希望**：进入模块后看到一个工作台首页

**以便**：快速了解系统数据概况和访问常用功能

**验收标准**：
1. 访问 `/analytics-stat/home` 显示工作台首页
2. 左侧显示功能菜单（综合查询、SQL查询台、数据字典等）
3. 右侧显示统计卡片和数据概览
4. 布局与其他模块（test-track、api-test）保持一致

### 3.2 作为开发者

**我希望**：analytics-stat 模块遵循统一的命名约定和结构

**以便**：降低维护成本和新人学习成本

**验收标准**：
1. 路由命名遵循 `/{module}/home` 约定
2. 组件命名遵循 `{Module}Home.vue` 约定
3. 目录结构与其他模块一致
4. 删除旧的 `Dashboard.vue` 和相关路由

## 四、功能需求

### 4.1 路由重构

#### 4.1.1 修改主路由

**文件**：`analytics-stat/frontend/src/router/modules/analytics.js`

**变更**：
```javascript
// 修改前
export default {
  path: "/analytics-stat",
  name: "analytics-stat",
  redirect: "/analytics-stat/dashboard",
  component: Layout,
  children: [
    {
      path: "dashboard",
      name: "Dashboard",
      component: () => import("@/views/Dashboard.vue"),
      meta: { title: "数据概览" }
    },
    // ...其他路由
  ]
};

// 修改后
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
      meta: { title: "工作台" }
    },
    // ...其他路由
  ]
};
```

#### 4.1.2 删除旧路由

- 删除 `dashboard` 路由配置
- 删除 `@/views/Dashboard.vue` 组件

### 4.2 组件重构

#### 4.2.1 创建 AnalyticsStatHome 组件

**文件**：`analytics-stat/frontend/src/business/home/AnalyticsStatHome.vue`

**布局结构**：
```
┌─────────────────────────────────────────┐
│  Layout (metersphere-frontend)          │
│  ┌───────────────────────────────────┐  │
│  │  顶部导航栏                       │  │
│  └───────────────────────────────────┘  │
│  ┌──────┬────────────────────────────┐  │
│  │      │                            │  │
│  │ 左侧 │  右侧内容区                │  │
│  │ 菜单 │  - 统计卡片                │  │
│  │      │  - 数据图表                │  │
│  │      │  - 快捷入口                │  │
│  │      │                            │  │
│  └──────┴────────────────────────────┘  │
└─────────────────────────────────────────┘
```

**功能模块**：
1. **左侧菜单**：
   - 综合查询入口
   - SQL查询台入口
   - 数据字典入口
   - 其他功能入口（预留）

2. **右侧内容区**：
   - 统计卡片（查询次数、数据量等）
   - 数据图表（趋势图、分布图等）
   - 快捷操作（常用查询、最近访问等）

#### 4.2.2 参考其他模块的实现

**TrackHome.vue 的布局特点**：
- 使用 `ms-container` 和 `ms-main-container`
- 使用 `el-row` 和 `el-col` 进行栅格布局
- 统计卡片使用 12 列布局（左右各一个）
- 表格使用全宽布局

**ApiHome.vue 的布局特点**：
- 顶部有版本选择器（可选）
- 4个统计卡片（2x2 布局）
- 底部有表格列表

**建议采用的布局**：
- 参考 TrackHome 的简洁布局
- 2个统计卡片（左右布局）
- 1个快捷入口区域
- 1个最近查询列表

### 4.3 目录结构调整

#### 4.3.1 创建 home 目录

```
analytics-stat/frontend/src/business/home/
├── AnalyticsStatHome.vue           # 主组件
└── components/                     # 子组件
    ├── QueryCountCard.vue          # 查询次数统计卡片
    ├── DataVolumeCard.vue          # 数据量统计卡片
    ├── QuickAccessCard.vue         # 快捷入口卡片
    └── RecentQueryList.vue         # 最近查询列表
```

#### 4.3.2 删除旧文件

```
analytics-stat/frontend/src/views/
├── Dashboard.vue                   # 删除
├── SqlConsole.vue                  # 保留
└── DataDictionary.vue              # 保留
```

### 4.4 侧边菜单实现

#### 4.4.1 菜单项配置

**菜单项**：
1. 综合查询
2. SQL查询台
3. 数据字典
4. 查询历史（预留）
5. 数据导出（预留）

#### 4.4.2 菜单交互

- 点击菜单项跳转到对应页面
- 当前页面高亮显示
- 支持折叠/展开（可选）

## 五、非功能需求

### 5.1 性能要求

- 首页加载时间 < 2秒
- 统计数据刷新时间 < 1秒
- 支持数据缓存，避免重复请求

### 5.2 兼容性要求

- 与现有 qiankun 微前端架构兼容
- 与 metersphere-frontend 共享组件兼容
- 支持主流浏览器（Chrome、Firefox、Safari、Edge）

### 5.3 可维护性要求

- 遵循 MeterSphere 代码规范
- 组件化设计，便于复用
- 添加详细的中文注释

## 六、验收标准

### 6.1 功能验收

- [ ] 访问 `/analytics-stat/home` 显示新首页
- [ ] 左侧菜单正常显示和交互
- [ ] 右侧统计卡片正常显示数据
- [ ] 点击菜单项正常跳转
- [ ] 旧的 `/analytics-stat/dashboard` 路由已删除
- [ ] 旧的 `Dashboard.vue` 组件已删除

### 6.2 布局验收

- [ ] 左右布局正常显示
- [ ] 响应式布局正常工作
- [ ] 与其他模块（test-track、api-test）布局一致
- [ ] 样式与 metersphere-frontend 统一

### 6.3 代码验收

- [ ] 路由命名遵循 `/{module}/home` 约定
- [ ] 组件命名遵循 `{Module}Home.vue` 约定
- [ ] 目录结构与其他模块一致
- [ ] 代码通过 ESLint 检查
- [ ] 添加了详细的中文注释

### 6.4 文档验收

- [ ] 更新 `analytics-stat模块差异对比分析.md`
- [ ] 更新路由配置说明
- [ ] 更新组件结构说明

## 七、实施计划

### 7.1 第一阶段：准备工作

1. 分析其他模块的 Home 组件实现
2. 设计 AnalyticsStatHome 组件结构
3. 确定统计数据来源和 API

### 7.2 第二阶段：组件开发

1. 创建 `AnalyticsStatHome.vue` 主组件
2. 创建统计卡片子组件
3. 创建快捷入口子组件
4. 创建最近查询列表子组件

### 7.3 第三阶段：路由重构

1. 修改路由配置
2. 更新路由重定向
3. 删除旧路由和组件

### 7.4 第四阶段：测试和优化

1. 功能测试
2. 布局测试
3. 性能优化
4. 文档更新

## 八、风险和依赖

### 8.1 风险

1. **数据来源不明确**：需要确认统计数据的 API 接口
2. **布局兼容性**：需要确保与 qiankun 微前端架构兼容
3. **用户习惯**：用户可能已习惯旧的导航页面

### 8.2 依赖

1. **后端 API**：需要提供统计数据的 API 接口
2. **metersphere-frontend**：依赖共享组件和样式
3. **qiankun**：依赖微前端框架

## 九、后续优化

### 9.1 功能增强

1. 添加数据图表（ECharts）
2. 添加查询历史记录
3. 添加数据导出功能
4. 添加个性化配置（用户可自定义首页布局）

### 9.2 性能优化

1. 实现数据懒加载
2. 实现虚拟滚动（大数据量列表）
3. 实现数据缓存策略

### 9.3 用户体验优化

1. 添加骨架屏加载效果
2. 添加数据刷新动画
3. 添加快捷键支持
