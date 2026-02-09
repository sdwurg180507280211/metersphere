# analytics-stat 首页重构任务列表

## 任务概述

将 analytics-stat 模块从简单导航页重构为工作台首页，采用左右布局，遵循其他模块的设计规范。

---

## 1. 准备工作

- [ ] 1.1 分析其他模块的 Home 组件实现
  - 分析 `test-track/frontend/src/business/home/TrackHome.vue`
  - 分析 `api-test/frontend/src/business/home/ApiHome.vue`
  - 总结布局模式和组件结构

- [ ] 1.2 确认后端 API 接口
  - 确认统计数据 API 是否存在
  - 如不存在，创建 Mock 数据或后端接口

- [ ] 1.3 创建目录结构
  - 创建 `analytics-stat/frontend/src/business/home/` 目录
  - 创建 `analytics-stat/frontend/src/business/home/components/` 目录

---

## 2. 组件开发

- [ ] 2.1 创建 AnalyticsStatHome.vue 主组件
  - 创建基础组件结构
  - 实现左右布局
  - 添加容器组件（ms-container、ms-main-container）
  - 实现数据加载逻辑
  - 添加错误处理

- [ ] 2.2 创建 QueryCountCard.vue 统计卡片
  - 创建组件文件
  - 实现统计数据展示
  - 添加趋势图标（上升/下降/持平）
  - 添加点击跳转功能

- [ ] 2.3 创建 DataVolumeCard.vue 统计卡片
  - 创建组件文件
  - 实现数据量展示
  - 添加单位转换（MB/GB/TB）
  - 添加样式

- [ ] 2.4 创建 QuickAccessCard.vue 快捷入口卡片
  - 创建组件文件
  - 实现功能入口列表
  - 添加图标和描述
  - 实现点击跳转

- [ ] 2.5 创建 RecentQueryList.vue 最近查询列表
  - 创建组件文件
  - 实现表格展示
  - 添加分页功能
  - 添加点击查看详情功能

---

## 3. 路由重构

- [ ] 3.1 修改路由配置
  - 修改 `analytics-stat/frontend/src/router/modules/analytics.js`
  - 将 `redirect` 从 `/analytics-stat/dashboard` 改为 `/analytics-stat/home`
  - 添加 `home` 路由配置
  - 更新路由 meta 信息

- [ ] 3.2 更新路由导入
  - 更新组件导入路径
  - 确保懒加载正常工作

- [ ] 3.3 删除旧路由配置
  - 删除 `dashboard` 路由配置
  - 或保留重定向（向后兼容）

---

## 4. API 集成

- [ ] 4.1 创建 API 接口文件
  - 创建 `analytics-stat/frontend/src/api/home.js`
  - 定义 `getQueryCount()` 接口
  - 定义 `getDataVolume()` 接口
  - 定义 `getRecentQueries()` 接口

- [ ] 4.2 集成 API 到组件
  - 在 AnalyticsStatHome 中调用 API
  - 处理加载状态
  - 处理错误情况
  - 添加数据刷新功能

---

## 5. 样式开发

- [ ] 5.1 实现主组件样式
  - 参考 TrackHome 的样式
  - 实现左右布局样式
  - 添加响应式样式
  - 确保与 metersphere-frontend 样式一致

- [ ] 5.2 实现子组件样式
  - 统计卡片样式
  - 快捷入口卡片样式
  - 表格样式
  - 确保样式统一

---

## 6. 清理旧代码

- [ ] 6.1 删除旧组件
  - 删除 `analytics-stat/frontend/src/views/Dashboard.vue`
  - 确认无其他地方引用该组件

- [ ] 6.2 清理无用代码
  - 删除无用的导入
  - 删除无用的样式
  - 清理注释

---

## 7. 测试

- [ ] 7.1 功能测试
  - 测试路由跳转
  - 测试数据加载
  - 测试菜单点击
  - 测试快捷入口跳转
  - 测试最近查询列表

- [ ] 7.2 布局测试
  - 测试左右布局显示
  - 测试响应式布局
  - 测试不同分辨率下的显示
  - 对比其他模块的布局

- [ ] 7.3 兼容性测试
  - 测试 Chrome 浏览器
  - 测试 Firefox 浏览器
  - 测试 Safari 浏览器
  - 测试 qiankun 微前端加载

- [ ] 7.4 性能测试
  - 测试首页加载时间
  - 测试数据刷新时间
  - 测试内存占用

---

## 8. 文档更新

- [ ] 8.1 更新差异对比文档
  - 更新 `docs/03-功能模块/分析统计/analytics-stat模块差异对比分析.md`
  - 说明路由变更
  - 说明组件变更
  - 更新目录结构说明

- [ ] 8.2 更新开发文档
  - 更新组件使用说明
  - 更新 API 接口文档
  - 添加开发注意事项

---

## 9. 部署验证

- [ ] 9.1 本地验证
  - 启动前端开发服务器
  - 验证功能正常
  - 验证样式正常

- [ ] 9.2 构建验证
  - 执行前端构建
  - 验证构建产物
  - 验证打包到后端 JAR

- [ ] 9.3 集成验证
  - 启动完整微服务环境
  - 验证 qiankun 加载
  - 验证路由跳转
  - 验证跨模块交互

---

## 任务优先级

**P0 (必须完成)**：
- 1.3 创建目录结构
- 2.1 创建 AnalyticsStatHome.vue 主组件
- 3.1 修改路由配置
- 6.1 删除旧组件
- 7.1 功能测试

**P1 (重要)**：
- 2.2-2.5 创建子组件
- 4.1-4.2 API 集成
- 5.1-5.2 样式开发
- 7.2 布局测试

**P2 (可选)**：
- 4.2 数据刷新功能
- 7.3 兼容性测试
- 7.4 性能测试
- 8.1-8.2 文档更新

---

## 预估工时

- 准备工作：2 小时
- 组件开发：8 小时
- 路由重构：1 小时
- API 集成：3 小时
- 样式开发：3 小时
- 清理旧代码：1 小时
- 测试：4 小时
- 文档更新：2 小时
- 部署验证：2 小时

**总计**：约 26 小时（3-4 个工作日）
