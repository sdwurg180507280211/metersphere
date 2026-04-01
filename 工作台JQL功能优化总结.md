# 工作台 JQL 综合查询 - 国际化和交互体验优化总结

## 📋 优化概览

本次优化主要针对 **国际化** 和 **交互体验** 两个方面进行了增强。

---

## 🌍 一、国际化优化

### 1. 语言文件结构

创建了独立的 advanced-search 翻译文件，便于维护：

```
workstation/frontend/src/i18n/lang/
├── zh-CN-advanced-search.js    # 简体中文翻译（150+ 键）
├── en-US-advanced-search.js     # 英文翻译（150+ 键）
└── zh-TW-advanced-search.js     # 繁体中文翻译（新增，150+ 键）
```

### 2. 翻译覆盖范围

| 功能模块 | 翻译键数 | 说明 |
|---------|---------|------|
| 业务模块选择 | 6 | 模块名称、选择提示 |
| 筛选条件 | 8 | 工作空间、项目选择 |
| 查询模式 | 5 | 可视化/JQL 模式切换 |
| JQL 编辑器 | 12 | 占位符、帮助标题 |
| 操作符说明 | 12 | 每个操作符的说明 |
| 智能提示 | 5 | 字段/操作符/值/关键字 |
| 视图模式 | 5 | 列表/分屏视图 |
| 结果展示 | 5 | 结果数量、无数据提示 |
| 工具栏 | 8 | 刷新、列设置等 |
| 导出功能 | 10 | 导出选项、提示 |
| 保存查询 | 8 | 保存对话框、提示 |
| 查询历史 | 8 | 历史记录管理 |
| 字段信息 | 12 | 字段类型说明 |
| 验证错误 | 12 | 语法错误提示 |
| 快捷键 | 4 | 快捷键说明 |
| 确认对话框 | 6 | 各种确认提示 |
| 示例查询 | 10 | 示例描述和 JQL |
| 其他通用 | 15 | 清空、保存、删除等 |

**总计：150+ 个翻译键**

### 3. 翻译合并策略

在主语言文件中优雅地合并 advanced-search 翻译：

```javascript
// zh-CN.js
import advancedSearch from "./zh-CN-advanced-search";

// 合并翻译（不覆盖原有内容）
if (advancedSearch && advancedSearch.advanced_search) {
  message.advanced_search = { ...message.advanced_search, ...advancedSearch.advanced_search };
}
```

---

## 🎨 二、交互体验优化

### 1. JQLEditor 组件完全重构

#### 新增工具栏

```
┌─────────────────────────────────────────────────────────────┐
│ [查询历史] [已保存] [示例] │ [格式化] [复制] [清空] [保存] │
└─────────────────────────────────────────────────────────────┘
```

#### 左侧功能面板（可展开）

| 面板 | 功能 |
|------|------|
| **查询历史** | 显示最近 50 条查询记录，相对时间显示 |
| **已保存查询** | 保存常用查询，支持命名和管理 |
| **示例查询** | 5 个常用示例，一键使用 |

#### 智能提示增强

- 按类型颜色区分：字段(蓝)、操作符(绿)、值(橙)、关键字(红)
- 图标 + 文字双重提示
- 键盘导航（↑↓选择，Enter/Tab确认）

#### 验证反馈

- 实时语法验证
- 成功/错误状态清晰展示
- 错误位置（行号、列号）提示

#### 快捷键支持

| 快捷键 | 功能 |
|--------|------|
| `Ctrl + Enter` | 执行查询 |
| `Ctrl + /` | 切换帮助面板 |
| `↑ / ↓` | 智能提示导航 |
| `Enter / Tab` | 选中智能提示 |
| `Esc` | 关闭智能提示 |

---

## 📁 文件变更清单

### 新增文件

| 文件 | 说明 |
|------|------|
| `workstation/frontend/src/i18n/lang/zh-CN-advanced-search.js` | 简体中文翻译 |
| `workstation/frontend/src/i18n/lang/en-US-advanced-search.js` | 英文翻译 |
| `workstation/frontend/src/i18n/lang/zh-TW-advanced-search.js` | 繁体中文翻译 |

### 修改文件

| 文件 | 修改内容 |
|------|---------|
| `workstation/frontend/src/i18n/lang/zh-CN.js` | 合并 advanced-search 翻译 |
| `workstation/frontend/src/i18n/lang/en-US.js` | 合并 advanced-search 翻译 |
| `workstation/frontend/src/i18n/lang/zh-TW.js` | 合并 advanced-search 翻译 |
| `workstation/frontend/src/business/advanced-search/JQLEditor.vue` | 完全重构 |
| `workstation/frontend/src/business/advanced-search/AdvancedSearch.vue` | 添加 execute 事件处理 |

---

## 🎯 核心功能详解

### 查询历史功能

```javascript
// 本地存储结构
[
  {
    jql: 'status = "Pass" AND priority = "P0"',
    timestamp: 1712345678901
  }
]

// 显示逻辑
- 1分钟内 → "刚刚"
- 1小时内 → "X分钟前"
- 1天内 → "X小时前"
- 更早 → 完整日期时间
```

### 保存查询功能

```javascript
// 本地存储结构
[
  {
    name: 'P0 优先用例',
    jql: 'priority = "P0" AND status != "Trash"',
    shared: false,
    timestamp: 1712345678901
  }
]
```

### 示例查询

| 示例 | 说明 |
|------|------|
| `status = "Pass" AND priority = "P0"` | 按状态和优先级 |
| `status IN ("Pass", "Prepare") AND priority NOT IN ("P3", "P4")` | IN 列表查询 |
| `name ~ "登录" AND createTime >= "2024-01-01"` | 模糊匹配 + 日期 |
| `(priority = "P0" OR priority = "P1") AND status != "Deprecated"` | 复杂组合 |
| `name CONTAINS "测试" AND status IN ("Underway", "Completed")` | 包含查询 |

---

## 💡 技术亮点

### 1. 翻译文件分离

- 独立的 advanced-search 翻译文件
- 不影响原有翻译内容
- 便于后续维护和更新

### 2. 渐进式增强

- 保持原有 API 兼容
- 新功能通过事件/插槽扩展
- 本地存储持久化用户数据

### 3. 无障碍设计

- 键盘快捷键支持
- 清晰的视觉反馈
- 友好的错误提示

---

## 🚀 使用指南

### 快速开始

1. 选择业务模块（测试用例/缺陷/测试计划/用例评审）
2. 选择工作空间和项目（可选）
3. 切换到 JQL 模式
4. 在编辑器中输入 JQL，或点击「示例查询」使用模板
5. 按 `Ctrl + Enter` 或点击「查询」执行

### 查询历史

- 每次成功执行的查询会自动保存到历史
- 点击「查询历史」查看和使用历史记录
- 可删除单条或清空全部历史

### 保存查询

1. 输入常用的 JQL
2. 点击「保存查询」
3. 输入查询名称
4. 点击「保存」
5. 在「已保存查询」中快速使用

---

## 📝 后续优化建议

### 短期优化

1. **查询模板变量** - 支持 `{project}`、`{me}` 等变量
2. **拖拽列宽** - 表格列宽可调整并记忆
3. **导出选项** - 支持选择导出字段和格式

### 长期规划

1. **服务端同步** - 查询历史和保存查询同步到服务端
2. **查询分享** - 生成分享链接或二维码
3. **可视化生成器** - 拖拽方式生成 JQL
4. **查询审计** - 记录查询执行时间和结果数
5. **AI 辅助** - 自然语言转 JQL

---

## 📊 统计数据

| 指标 | 数值 |
|------|------|
| 新增翻译键 | 150+ |
| 支持语言 | 3种（简中、英文、繁中） |
| 新增功能模块 | 5个（历史、保存、示例、格式化、复制） |
| 新增快捷键 | 4个 |
| 修改文件数 | 5个 |
| 新增文件数 | 3个 |

---

*优化完成日期：2026-04-01*
