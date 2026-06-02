# 设计文档

## 概述

本优化方案采用**并行加载 + 延迟加载 + 防抖优化**的组合策略，针对接口自动化页面的性能瓶颈进行针对性优化。方案基于 Vue.js 2.7 框架，使用原生 JavaScript 实现，不引入额外依赖。

## 核心设计目标

1. **减少白屏时间**: 通过并行加载和立即渲染，让用户尽快看到页面内容
2. **优化首屏渲染**: 延迟加载非关键资源，优先渲染用户需要的内容
3. **降低服务器负载**: 通过防抖减少无效请求，提升系统整体性能
4. **保持向后兼容**: 不改变现有业务逻辑，确保功能正常
5. **容错性**: 部分接口失败不影响整体功能

## 技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 前端框架 | Vue.js 2.7.3 | 与现有项目保持一致 |
| 异步处理 | Promise.allSettled | 并行加载，部分失败不影响整体 |
| 延迟加载 | Vue $nextTick | 延迟到下一个 DOM 更新周期 |
| 防抖实现 | 原生 JavaScript | 轻量级实现，避免引入 lodash |

## 架构设计

### 优化策略对比

| 策略 | 适用场景 | 优点 | 缺点 |
|------|---------|------|------|
| **并行加载** | 多个独立接口 | 减少总等待时间 | 需确保接口无依赖关系 |
| **延迟加载** | 非首屏必需资源 | 优先渲染关键内容 | 可能导致后续加载延迟 |
| **防抖优化** | 高频触发事件 | 减少无效请求 | 需要合理设置延迟时间 |

### 数据流设计

```
用户访问页面
    ↓
组件 mounted/created
    ↓
┌─────────────────────────────────────┐
│  并行加载关键数据                      │
│  Promise.allSettled([                │
│    getProject(),                     │
│    getTrashCase(),                   │
│    getApiScenario(),                 │
│    ...                               │
│  ])                                  │
└─────────────────────────────────────┘
    ↓
关键数据加载完成
    ↓
┌─────────────────────────────────────┐
│  延迟加载非关键数据                    │
│  $nextTick(() => {                   │
│    initEnvironment()                 │
│    getPlugins()                      │
│  })                                  │
└─────────────────────────────────────┘
    ↓
页面完全就绪
```

## 详细设计

### 方案 1: ApiAutomation.vue 并行加载

**问题分析**:
- 原代码在 `mounted()` 中串行执行 3 个方法
- `getProject()` 和 `getTrashCase()` 无依赖关系，可并行
- `init()` 只处理路由参数，不依赖前两个方法的结果

**设计方案**:
```javascript
// 修改前：串行执行
mounted() {
  this.getProject();      // 等待完成
  this.getTrashCase();    // 等待完成
  this.init();            // 最后执行
}

// 修改后：并行执行
mounted() {
  Promise.allSettled([
    this.getProject(),
    this.getTrashCase()
  ]).then(() => {
    this.init();
  });
}
```

**关键点**:
- 使用 `Promise.allSettled` 而非 `Promise.all`，部分失败不影响整体
- 需修复 `getProject()` 和 `getTrashCase()` 方法，确保返回 Promise
- 组件立即渲染，不等待数据加载

**预期效果**: 加载时间减少 20-40%

---

### 方案 2: EditApiScenario.vue 场景编辑器并行加载

**问题分析**:
- 原代码在 `created()` 中串行执行 6 个方法
- `getApiScenario()` 是最重的接口，加载场景数据
- 插件和环境配置不是首屏必需

**设计方案**:
```javascript
created() {
  this.buttonData = buttons(this);

  // 并行加载关键数据
  Promise.allSettled([
    this.getApiScenario(),        // 最重要
    this.getWsProjects(),
    this.getMaintainerOptions(),
    this.getDefaultVersion()
  ]).then(() => {
    // 关键数据加载完成
  });

  // 延迟加载非关键数据
  this.$nextTick(() => {
    this.getPlugins().then(() => this.initPlugins());
    this.result = getEnvironmentByProjectId(this.projectId)
      .then(response => this.environments = response.data);
  });
}
```

**关键点**:
- 4 个关键接口并行加载
- 插件和环境配置延迟到 `$nextTick`
- 需修复 3 个方法的 Promise 返回

**预期效果**: 加载时间减少 30-50%

---

### 方案 3: ApiScenarioList.vue 延迟加载环境配置

**问题分析**:
- 环境配置在 `created()` 中立即加载
- 环境配置不是首屏必需数据
- 可延迟到首屏渲染后加载

**设计方案**:
```javascript
created() {
  // ... 其他初始化代码

  // 延迟加载环境配置
  this.$nextTick(() => {
    this.initEnvironment();
  });
}
```

**关键点**:
- 使用 `$nextTick` 延迟 1 个 tick（几毫秒）
- 不影响环境配置功能
- 优先渲染关键内容

**预期效果**: 首屏渲染时间减少 15-25%

---

### 方案 4: ApiScenarioList.vue 搜索防抖

**问题分析**:
- 用户快速输入时会触发多次搜索
- 每次搜索都执行完整的过滤逻辑
- 导致大量无效请求和性能问题

**设计方案**:
```javascript
// 1. 实现轻量级防抖函数
methods: {
  debounce(fn, delay) {
    let timer = null;
    const self = this;
    const debounced = function(...args) {
      clearTimeout(timer);
      timer = setTimeout(() => fn.apply(self, args), delay);
    };
    debounced.cancel = () => clearTimeout(timer);
    return debounced;
  }
}

// 2. 创建防抖实例
created() {
  this.debouncedNodeChange = this.debounce(this.nodeChange, 300);
}

// 3. 使用防抖
search(projectId, immediate = false) {
  if (immediate) {
    this.nodeChange(projectId);  // 立即执行
  } else {
    this.debouncedNodeChange(projectId);  // 防抖执行
  }
}

// 4. 清理
beforeDestroy() {
  if (this.debouncedNodeChange && this.debouncedNodeChange.cancel) {
    this.debouncedNodeChange.cancel();
  }
}
```

**关键点**:
- 自实现防抖函数，仅 6 行代码，避免引入 lodash
- 300ms 延迟，平衡响应速度和性能
- 支持 `immediate` 参数，删除/刷新等操作立即执行
- 组件销毁时清理定时器

**预期效果**: 减少 60-80% 无效请求

## 涉及文件清单

| 文件 | 修改类型 | 修改位置 | 说明 |
|------|---------|---------|------|
| `api-test/frontend/src/business/automation/ApiAutomation.vue` | 修改 | mounted() 钩子 (约 206 行) | 并行加载项目配置和回收站数据 |
| `api-test/frontend/src/business/automation/ApiAutomation.vue` | 修改 | getProject() 方法 (约 823 行) | 添加 return 语句 |
| `api-test/frontend/src/business/automation/ApiAutomation.vue` | 修改 | getTrashCase() 方法 (约 816 行) | 添加 return 语句 |
| `api-test/frontend/src/business/automation/scenario/EditApiScenario.vue` | 修改 | created() 钩子 (约 898 行) | 并行加载 + 延迟加载 |
| `api-test/frontend/src/business/automation/scenario/EditApiScenario.vue` | 修改 | getWsProjects() 等方法 | 添加 return 语句 |
| `api-test/frontend/src/business/automation/scenario/ApiScenarioList.vue` | 修改 | created() 钩子 (约 760 行) | 延迟加载环境配置 |
| `api-test/frontend/src/business/automation/scenario/ApiScenarioList.vue` | 新增 | debounce() 方法 | 防抖工具函数 |
| `api-test/frontend/src/business/automation/scenario/ApiScenarioList.vue` | 修改 | search() 方法 (约 892 行) | 使用防抖 + immediate 参数 |
| `api-test/frontend/src/business/automation/scenario/ApiScenarioList.vue` | 修改 | beforeDestroy() 钩子 | 清理防抖定时器 |

## 技术决策

### 为什么使用 Promise.allSettled？

**对比 Promise.all**:
- `Promise.all`: 任一失败则全部失败，返回第一个错误
- `Promise.allSettled`: 等待所有完成，返回每个结果（成功或失败）

**选择理由**:
- 更健壮，部分接口失败不影响其他接口
- 页面仍可正常使用，用户体验更好
- 便于错误追踪和降级处理

### 为什么不使用 lodash？

**现状**:
- 项目中只有 lodash 子包（如 `lodash.isempty`）
- 没有完整的 lodash 或 lodash-es

**选择理由**:
- 避免增加依赖（lodash-es 约 24KB）
- 自实现防抖函数仅 6 行代码
- 满足当前需求，无需引入完整库

### 为什么不添加 loading 状态？

**选择理由**:
- 遵循最小改动原则
- 先完成核心性能优化
- 如测量后加载时间仍 > 1 秒，再考虑添加
- loading 状态只改善感知性能，不改善实际性能

## 风险评估

### 低风险

- **延迟加载**: 仅调整加载时机，不改变业务逻辑
- **防抖优化**: 成熟模式，向后兼容

### 中风险

- **并行加载**: 需确保方法返回 Promise
  - **缓解措施**: 添加 return 语句，添加错误处理
  - **验证方法**: 测试接口失败场景

### 兼容性风险

- **Promise.allSettled**: Chrome 76+, Firefox 71+, Safari 13+
  - **缓解措施**: 如需兼容旧浏览器，使用 polyfill
  - **验证方法**: 检查项目浏览器支持范围

### 内存泄漏风险

- **防抖定时器**: 组件销毁时未清理
  - **缓解措施**: 在 `beforeDestroy` 中调用 `cancel()`
  - **验证方法**: Chrome DevTools Memory 面板检测

## 回滚方案

所有修改都是代码级别的优化，如出现问题可快速回滚：

1. 使用 `git revert` 回退到优化前的版本
2. 建议每个方案单独提交，便于精确回滚
3. 保留原始代码注释，便于理解修改意图
