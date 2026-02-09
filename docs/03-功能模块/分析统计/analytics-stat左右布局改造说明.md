# analytics-stat 左右布局改造说明

## 改造概述

将 analytics-stat 模块从**上下布局**（顶部横向菜单）改造为**左右布局**（左侧垂直菜单），与 system-setting、project-management 等模块保持一致的设计风格。

**改造日期**：2026-02-09

---

## 一、改造前后对比

### 1.1 布局对比

**改造前（上下布局）**：
```
┌─────────────────────────────────────────┐
│  顶部导航栏 (Layout)                     │
├─────────────────────────────────────────┤
│  二级横向菜单 (AnalyticsStatHeaderMenus) │
│  [工作台] [SQL查询台] [数据字典]         │
├─────────────────────────────────────────┤
│                                         │
│  内容区域 (router-view)                  │
│                                         │
└─────────────────────────────────────────┘
```

**改造后（左右布局）**：
```
┌─────────────────────────────────────────┐
│  顶部导航栏 (Layout)                     │
├──────────┬──────────────────────────────┤
│ 左侧菜单  │  右侧内容区域                 │
│          │                              │
│ ▼ 分析统计│  AnalyticsStatHome.vue       │
│  - 工作台 │  (统计卡片 + 快捷入口)        │
│  - SQL   │                              │
│    查询台 │                              │
│  - 数据  │                              │
│    字典  │                              │
└──────────┴──────────────────────────────┘
```

### 1.2 组件结构对比

**改造前**：
```
AnalyticsStat.vue (容器)
├── AnalyticsStatHeaderMenus.vue (横向菜单)
└── router-view (内容区域)
    └── AnalyticsStatHome.vue
        ├── ms-container
        │   └── ms-main-container
        │       └── 内容
```

**改造后**：
```
AnalyticsStat.vue (容器)
├── ms-container
│   ├── ms-aside-container (左侧)
│   │   └── AnalyticsStatMenu.vue (垂直菜单)
│   └── ms-main-container (右侧)
│       └── router-view
│           └── AnalyticsStatHome.vue (内容)
```

---

## 二、改造内容

### 2.1 新增文件

| 文件路径 | 说明 |
|---------|------|
| `analytics-stat/frontend/src/business/AnalyticsStatMenu.vue` | 左侧垂直菜单组件 |
| `docs/03-功能模块/分析统计/analytics-stat左右布局改造说明.md` | 本文档 |

### 2.2 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `analytics-stat/frontend/src/business/AnalyticsStat.vue` | 从上下布局改为左右布局，使用 ms-container 组件 |
| `analytics-stat/frontend/src/business/home/AnalyticsStatHome.vue` | 移除多余的 ms-container 嵌套，简化组件结构 |
| `analytics-stat/frontend/src/i18n/lang/zh-CN.js` | 添加 "分析统计" 和 "工作台" 的国际化配置 |
| `analytics-stat/frontend/src/router/modules/analytics.js` | 路由路径从 `/dashboard` 改为 `/home` |
| `analytics-stat/frontend/src/router/index.js` | 根路径重定向从 `/dashboard` 改为 `/home` |
| `analytics-stat/frontend/package.json` | 添加 `npm run analytics` 启动命令 |

### 2.3 保留文件（待删除）

| 文件路径 | 说明 | 状态 |
|---------|------|------|
| `analytics-stat/frontend/src/business/head/AnalyticsStatHeaderMenus.vue` | 旧的横向菜单组件 | 已更新路径，可选择性删除 |

---

## 三、技术实现细节

### 3.1 左侧菜单组件 (AnalyticsStatMenu.vue)

**功能**：
- 显示分析统计模块的功能菜单
- 支持路由跳转
- 自动高亮当前激活菜单

**关键代码**：
```vue
<el-menu 
  menu-trigger="click" 
  :default-active="$route.path"
  :default-openeds="['1']"
  router 
  class="analytics-menu">
  <el-submenu index="1">
    <template v-slot:title>
      <font-awesome-icon class="icon analytics" :icon="['fas', 'chart-bar']" size="lg"/>
      <span>{{ $t('commons.analytics_stat') }}</span>
    </template>
    <el-menu-item 
      v-for="menu in menus" 
      :key="menu.index"
      :index="menu.index" 
      class="menu-item">
      <i :class="menu.icon"></i>
      {{ menu.title }}
    </el-menu-item>
  </el-submenu>
</el-menu>
```

**菜单配置**：
```javascript
menus: [
  {
    index: '/analytics-stat/home',
    title: '工作台',
    icon: 'el-icon-s-home'
  },
  {
    index: '/analytics-stat/sql-console',
    title: 'SQL查询台',
    icon: 'el-icon-document'
  },
  {
    index: '/analytics-stat/data-dictionary',
    title: '数据字典',
    icon: 'el-icon-collection'
  }
]
```

### 3.2 容器组件 (AnalyticsStat.vue)

**布局结构**：
```vue
<ms-container>
  <!-- 左侧菜单 -->
  <ms-aside-container :width="'200px'">
    <analytics-stat-menu />
  </ms-aside-container>
  
  <!-- 右侧内容区域 -->
  <ms-main-container>
    <keep-alive>
      <router-view />
    </keep-alive>
  </ms-main-container>
</ms-container>
```

**关键样式**：
```css
.ms-aside-container {
  height: calc(100vh) !important;
  padding: 0px;
}

.ms-main-container {
  height: calc(100vh) !important;
}
```

### 3.3 首页组件 (AnalyticsStatHome.vue)

**简化前**：
```vue
<div style="background-color: #f5f6f7; overflow: auto">
  <ms-container>
    <ms-main-container style="padding: 0px">
      <div class="analytics-stat-home-layout">
        <!-- 内容 -->
      </div>
    </ms-main-container>
  </ms-container>
</div>
```

**简化后**：
```vue
<div class="analytics-stat-home">
  <!-- 内容 -->
</div>
```

**样式调整**：
```css
.analytics-stat-home {
  padding: 20px;
  background-color: #f5f6f7;
  min-height: calc(100vh - 50px);
}
```

---

## 四、路由配置变更

### 4.1 路由模块 (analytics.js)

**变更内容**：
```javascript
// 改造前
redirect: "/analytics-stat/dashboard"

// 改造后
redirect: "/analytics-stat/home"
```

```javascript
// 改造前
{
  path: "dashboard",
  name: "analyticsStatDashboard",
  component: () => import("@/views/Dashboard.vue"),
  meta: { title: "数据概览" }
}

// 改造后
{
  path: "home",
  name: "analyticsStatHome",
  component: () => import("@/business/home/AnalyticsStatHome.vue"),
  meta: { 
    title: "工作台",
    requiresAuth: true
  }
}
```

### 4.2 路由主文件 (index.js)

**变更内容**：
```javascript
// 改造前
{ path: "/", redirect: "/analytics-stat/dashboard" }

// 改造后
{ path: "/", redirect: "/analytics-stat/home" }
```

---

## 五、国际化配置

### 5.1 中文语言包 (zh-CN.js)

**新增配置**：
```javascript
const message = {
  analytics: {
    home: "工作台",  // 新增
    dashboard: "数据概览",
    sql_console: "SQL查询台",
    data_dictionary: "数据字典",
    // ...
  },
  commons: {
    analytics_stat: "分析统计"  // 新增
  }
};
```

---

## 六、启动命令优化

### 6.1 package.json

**新增命令**：
```json
{
  "scripts": {
    "analytics": "vue-cli-service serve --port 4009",  // 新增
    "serve": "vue-cli-service serve --port 4009",
    "build": "vue-cli-service build",
    "lint": "vue-cli-service lint"
  }
}
```

**使用方式**：
```bash
# 改造前
cd analytics-stat/frontend
npm run serve

# 改造后（与其他模块保持一致）
cd analytics-stat/frontend
npm run analytics
```

---

## 七、改造优势

### 7.1 视觉一致性 ⭐⭐⭐⭐⭐
- 与 system-setting、project-management 等模块保持一致
- 用户在不同模块间切换时体验更统一
- 符合企业级应用的标准布局模式

### 7.2 空间利用 ⭐⭐⭐⭐
- 左侧菜单固定宽度（200px），不占用过多空间
- 右侧内容区域更宽敞，适合展示统计图表
- 避免了横向菜单在菜单项多时的拥挤问题

### 7.3 扩展性 ⭐⭐⭐⭐⭐
- 未来新增功能时，只需在左侧菜单添加新项
- 支持菜单分组（使用 el-submenu）
- 可以添加子菜单实现多级导航

### 7.4 用户体验 ⭐⭐⭐⭐
- 左侧菜单始终可见，方便快速切换
- 菜单项可以显示图标，更直观
- 支持菜单折叠功能（可选）

---

## 八、测试验证

### 8.1 功能测试清单

- [ ] 左侧菜单显示正常
- [ ] 菜单项点击跳转正常
- [ ] 当前激活菜单高亮显示
- [ ] 工作台页面加载正常
- [ ] 统计卡片显示正常
- [ ] 快捷入口点击跳转正常
- [ ] 最近查询列表显示正常
- [ ] 路由切换正常
- [ ] 页面刷新后状态保持

### 8.2 样式测试清单

- [ ] 左侧菜单宽度合适（200px）
- [ ] 右侧内容区域自适应
- [ ] 统计卡片布局正常
- [ ] 响应式布局正常
- [ ] 不同分辨率下显示正常
- [ ] 与其他模块样式一致

### 8.3 兼容性测试清单

- [ ] Chrome 浏览器正常
- [ ] Firefox 浏览器正常
- [ ] Safari 浏览器正常
- [ ] qiankun 微前端加载正常
- [ ] 路由跳转正常
- [ ] 跨模块交互正常

---

## 九、启动测试

### 9.1 前端独立启动

```bash
# 进入前端目录
cd analytics-stat/frontend

# 安装依赖（如果需要）
npm install

# 启动开发服务器
npm run analytics

# 访问地址
# http://localhost:4009
```

### 9.2 完整微服务启动

```bash
# 启动后端服务
cd analytics-stat/backend
mvn spring-boot:run

# 启动前端服务
cd analytics-stat/frontend
npm run analytics

# 访问地址
# http://localhost:8080/analytics-stat/home
```

---

## 十、后续优化建议

### 10.1 功能扩展

1. **菜单权限控制**
   - 根据用户权限动态显示菜单项
   - 参考 system-setting 的权限控制实现

2. **菜单折叠功能**
   - 添加菜单折叠/展开按钮
   - 折叠后只显示图标，节省空间

3. **菜单分组**
   - 将功能菜单分组（查询类、管理类、统计类）
   - 使用多个 el-submenu 实现

### 10.2 性能优化

1. **路由懒加载**
   - 所有路由组件使用动态导入
   - 减少首屏加载时间

2. **组件缓存**
   - 使用 keep-alive 缓存页面状态
   - 避免重复加载数据

3. **数据缓存**
   - 统计数据缓存 5 分钟
   - 减少后端请求次数

### 10.3 用户体验优化

1. **加载状态**
   - 添加骨架屏
   - 优化加载动画

2. **错误处理**
   - 统一错误提示
   - 添加重试机制

3. **快捷键支持**
   - 支持键盘快捷键切换菜单
   - 提升操作效率

---

## 十一、参考资料

### 11.1 参考组件

- `system-setting/frontend/src/business/Setting.vue` - 左右布局容器
- `system-setting/frontend/src/business/SettingMenu.vue` - 左侧菜单
- `system-setting/frontend/src/business/SettingHome.vue` - 首页组件

### 11.2 相关文档

- [analytics-stat模块差异对比分析.md](./analytics-stat模块差异对比分析.md)
- [analytics-stat首页布局结构说明.md](./analytics-stat首页布局结构说明.md)
- [qiankun微前端集成指南.md](./qiankun微前端集成指南.md)

### 11.3 技术栈文档

- [Element UI 官方文档](https://element.eleme.cn/)
- [Vue.js 2.x 官方文档](https://v2.cn.vuejs.org/)
- [Vue Router 3.x 官方文档](https://v3.router.vuejs.org/zh/)

---

## 十二、更新记录

| 日期 | 版本 | 说明 | 作者 |
|------|------|------|------|
| 2026-02-09 | 1.0.0 | 完成左右布局改造 | AI |

---

## 附录：完整目录结构

```
analytics-stat/frontend/src/
├── api/                               # API 接口定义
│   └── home.js                        # 首页统计 API（待实现）
├── business/
│   ├── AnalyticsStat.vue              # 容器组件（左右布局）✅ 已改造
│   ├── AnalyticsStatMenu.vue          # 左侧菜单组件 ✅ 新增
│   ├── head/
│   │   └── AnalyticsStatHeaderMenus.vue  # 旧的横向菜单（可删除）
│   └── home/                          # 首页业务组件
│       ├── AnalyticsStatHome.vue      # 工作台首页主组件 ✅ 已简化
│       └── components/                # 首页子组件
│           ├── QueryCountCard.vue     # 查询次数统计卡片
│           ├── DataVolumeCard.vue     # 数据量统计卡片
│           ├── QuickAccessCard.vue    # 快捷入口卡片
│           └── RecentQueryList.vue    # 最近查询列表
├── i18n/                              # 国际化
│   └── lang/
│       └── zh-CN.js                   # 中文语言包 ✅ 已更新
├── router/
│   ├── modules/
│   │   └── analytics.js               # 路由模块 ✅ 已更新
│   └── index.js                       # 路由配置 ✅ 已更新
├── store/                             # 状态管理
├── views/
│   ├── SqlConsole.vue                 # SQL查询台
│   └── DataDictionary.vue             # 数据字典
├── App.vue                            # 根组件
├── main.js                            # 入口文件
└── public-path.js                     # qiankun publicPath
```

