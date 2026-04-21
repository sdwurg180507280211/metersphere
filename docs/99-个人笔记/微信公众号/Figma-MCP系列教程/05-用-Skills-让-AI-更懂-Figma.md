# 🔥 用 Skills 让 AI 更懂 Figma：提升代码质量的秘密武器

你有没有发现：AI 虽然能生成代码，但有时候生成的代码"不太像"你们项目的风格？

这时候 **Skills** 就派上用场了！它能告诉 AI："我们项目是这么做代码的，请按这个来！"

---

## 一、Skills 是什么？

### 简单理解

**Skills = 给 AI 的操作手册 + 最佳实践**

没有 Skills 时：
```
你：根据这个 Figma 设计稿生成代码
AI：（生成了一个通用的 React 组件，用它自己喜欢的风格）
你：（心想：怎么和我们项目的代码风格差这么多...）
```

有 Skills 时：
```
你：根据这个 Figma 设计稿生成代码
AI：（先看了 Skills → 哦，他们项目用 Tailwind CSS，
    组件结构是这样的，命名规范是那样的...
    → 生成了完全符合你们项目风格的代码！）
```

### Skills 能做什么？

| 功能 | 说明 |
|-----|------|
| 📋 **工作流指导** | 告诉 AI 完成任务的步骤顺序 |
| 🎨 **设计系统规则** | 告诉 AI 你们项目的设计 Token 映射 |
| 🧩 **组件映射** | Figma 组件 ↔ 代码组件的对应关系 |
| 📝 **代码风格** | 告诉 AI 你们项目的代码规范 |

---

## 二、安装 Figma Skills

### 步骤 1：确保 Figma Plugin 已安装

在 Claude Code 中输入：

```
/plugin
```

确认 `figma` plugin 已安装。如果没有，先安装：

```
/plugin marketplace add ChromeDevTools/chrome-devtools-mcp
/plugin install chrome-devtools-mcp
```

### 步骤 2：重启 Claude Code

安装完 Plugin 后，完全退出 Claude Code，再重新启动。

### 步骤 3：验证 Skills 已加载

重启后，输入：

```
/skills
```

你应该能看到 Figma 相关的 Skills！

---

## 三、Skills 实战：3 个常用场景

### 场景 1：用 Code Connect 连接 Figma 组件和代码组件

**目标**：让 AI 知道 Figma 里的 Button 对应你们项目里的 `src/components/Button.tsx`

#### 步骤 1：在 Figma 中设置 Code Connect

1. 在 Figma 中选中你的 Button 组件
2. 打开 Dev Mode
3. 找到 "Code Connect" 部分
4. 关联到你的代码文件

#### 步骤 2：让 AI 使用 Code Connect

对 Claude Code 说：

```
根据这个 Figma 链接生成代码，使用 Code Connect：
https://www.figma.com/design/xxx?node-id=1-2
```

AI 会：
1. 读取 Figma 组件的 Code Connect 配置
2. 找到你项目里对应的 Button 组件
3. 直接使用你们的组件，而不是重新写一个！

---

### 场景 2：生成设计系统规则

**目标**：让 AI 知道你们项目的设计 Token（颜色、间距、字体等）

对 Claude Code 说：

```
分析这个 Figma 文件，生成设计系统规则：
https://www.figma.com/design/xxx/Design-System
```

AI 会：
1. 读取 Figma 中的 Variables（变量）
2. 提取颜色、间距、字体等设计 Token
3. 生成设计系统规则文档
4. 以后生成代码时会自动使用这些 Token！

示例输出：
```json
{
  "designTokens": {
    "colors": {
      "primary": "#1890FF",
      "secondary": "#52C41A",
      "danger": "#FF4D4F"
    },
    "spacing": {
      "sm": "8px",
      "md": "16px",
      "lg": "24px"
    }
  }
}
```

---

### 场景 3：把设计转换成生产级代码

**目标**：不只是生成能用的代码，而是生成符合你们项目规范的代码

对 Claude Code 说：

```
根据这个 Figma 链接生成生产级代码：
https://www.figma.com/design/xxx?node-id=1-2

要求：
1. 使用我们项目的设计 Token（不要用硬编码的颜色）
2. 使用 Code Connect 中定义的组件映射
3. 遵循我们项目的代码风格
4. 添加 TypeScript 类型定义
5. 添加必要的注释
```

AI 会：
1. 先看 Skills 中的设计系统规则
2. 用 Code Connect 找到对应的组件
3. 按照你们项目的风格生成代码
4. 结果就是：完全像是你们团队自己写的代码！

---

## 四、自定义 Skills：让 AI 更懂你的项目

### Skill 文件结构

一个 Skill 就是一个 `SKILL.md` 文件，放在你的项目目录里：

```
your-project/
├── .claude/
│   └── skills/
│       └── figma-design-system.md
└── src/
```

### 写一个自定义 Skill

创建 `.claude/skills/figma-design-system.md`：

```markdown
# Figma Design System Skill

## 设计 Token 映射

### 颜色
- Figma 的 Primary Blue → 项目的 `--color-primary`
- Figma 的 Secondary Green → 项目的 `--color-secondary`
- Figma 的 Danger Red → 项目的 `--color-danger`

### 间距
- Figma 的 8px → 项目的 `--spacing-sm`
- Figma 的 16px → 项目的 `--spacing-md`
- Figma 的 24px → 项目的 `--spacing-lg`

## 组件映射

- Figma 的 Button 组件 → `src/components/Button.tsx`
- Figma 的 Input 组件 → `src/components/Input.tsx`
- Figma 的 Card 组件 → `src/components/Card.tsx`

## 代码风格

- 使用 TypeScript
- 文件名用 kebab-case: `user-profile.tsx`
- 组件名用 PascalCase: `UserProfile`
- 使用 Tailwind CSS
- 组件必须有 PropTypes 或 TypeScript 类型

## 工作流

1. 先读取 Figma 设计稿的 Variables
2. 检查是否有 Code Connect 映射
3. 使用设计 Token，不要硬编码
4. 使用项目中的组件，不要重新造轮子
```

### 使用自定义 Skill

在 Claude Code 中说：

```
根据这个 Figma 链接生成代码，使用我们的 Design System Skill：
https://www.figma.com/design/xxx?node-id=1-2
```

---

## 五、常用 Skill 模板

### 模板 1：React + Tailwind 项目

```markdown
# React + Tailwind Skill

## 设计 Token
- 使用 Tailwind 的 utility classes
- 颜色从 `tailwind.config.js` 读取
- 间距用 Tailwind 的 spacing scale

## 组件规范
- 函数组件，用 `function` 或 `const`
- Props 用 TypeScript interface
- 默认导出组件

## 文件夹结构
- 组件放在 `src/components/`
- 页面放在 `src/pages/`
- Hooks 放在 `src/hooks/`
```

### 模板 2：Vue 项目

```markdown
# Vue Skill

## 设计 Token
- 使用 CSS Variables: `--color-primary`
- 在 `src/assets/styles/variables.css` 定义

## 组件规范
- 使用 `<script setup>` 语法
- Props 用 TypeScript 定义
- 单文件组件 `.vue`

## 文件夹结构
- 组件放在 `src/components/`
- 视图放在 `src/views/`
- Composables 放在 `src/composables/`
```

### 模板 3：设计系统专用

```markdown
# Design System Skill

## 必须遵守
1. 永远使用设计 Token，不要硬编码
2. 先检查 Code Connect，有映射就用现有组件
3. 保持组件的可访问性（aria-* 属性）
4. 添加必要的交互状态（hover、focus、active）

## 不要做
❌ 不要用 `!important`
❌ 不要重新实现已有组件
❌ 不要偏离设计系统的规范
```

---

## 六、常见问题

### Q: Skills 和 MCP 是什么关系？

A:
- **MCP** = 让 AI 能访问 Figma 的工具
- **Skills** = 告诉 AI 怎么用这些工具的指南

两者配合使用效果最好！

### Q: 我需要写代码才能创建 Skills 吗？

A: 不需要！Skills 就是 Markdown 文件，用自然语言写就可以。

### Q: Skills 能分享给团队吗？

A: 可以！把 Skills 文件提交到 Git，团队成员都能使用。

### Q: 有官方的 Figma Skills 吗？

A: 有！安装 Figma Plugin 后，官方 Skills 会自动加载。

---

## 七、最佳实践

### ✅ 应该做的

1. **把 Skills 提交到 Git**
   - 团队共享，保持一致
   - 版本控制，追踪变更

2. **从简单开始**
   - 先写最基本的规范
   - 逐步完善

3. **用例子说明**
   - 好的例子比长篇大论更有用
   - 提供"应该这样做"和"不要这样做"的对比

4. **定期更新**
   - 项目规范变化时更新 Skills
   - 保持 Skills 和项目同步

### ❌ 不应该做的

1. **不要写得太长太复杂**
   - AI 可能读不完
   - 保持简洁，重点突出

2. **不要太含糊**
   - "要写好代码" = 没用
   - "使用 TypeScript，函数组件，PascalCase 命名" = 有用

3. **不要重复造轮子**
   - 先看官方 Skills
   - 再根据项目需求自定义

---

## 八、总结

Skills 就像是给 AI 的"员工手册"：
- 告诉 AI 你们项目的规范
- 告诉 AI 你们的最佳实践
- 让 AI 生成的代码更像你们团队写的

配合 Figma MCP 使用，效果翻倍：
- MCP 让 AI 能"看"懂设计稿
- Skills 让 AI 知道"怎么"生成代码

快去试试吧！让 AI 更懂你的项目！
