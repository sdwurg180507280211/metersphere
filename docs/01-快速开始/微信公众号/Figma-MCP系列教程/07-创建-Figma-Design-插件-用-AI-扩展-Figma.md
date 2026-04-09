# 🔥 创建 Figma Design 插件：用 AI 扩展 Figma 的能力！

你有没有想过：让 AI 直接在 Figma 里帮你画图、改设计、生成组件？

这篇文章带你用 **Figma MCP + Agentic Tools**，创建一个能让 AI 直接操作 Figma 的插件！

---

## 一、先搞懂：我们要做什么？

### 目标

创建一个 Figma 插件，让 AI 能够：
- ✅ 直接在 Figma 画布上创建图层
- ✅ 修改现有设计（改颜色、改布局、改样式）
- ✅ 创建组件和变体
- ✅ 设置设计变量
- ✅ 操作 Auto Layout

### 工作原理

```
你的想法
    ↓
Claude Code (AI)
    ↓
Figma MCP Server
    ↓
Figma 画布
    ↓
看到 AI 创建的设计！
```

---

## 二、准备工作

在开始之前，确保你有：

1. ✅ **Claude Code 已安装**
2. ✅ **Figma 账号**（Dev 或 Full seat）
3. ✅ **Remote MCP 已配置**（必须用 Remote，Desktop 不支持写入画布）
4. ✅ **Node.js 已安装**（版本 18+）

---

## 三、确认 Remote MCP 已连接

在 Claude Code 中输入：

```
/mcp
```

确认看到：
```
✓ figma (connected)
  - get_design_context
  - use_figma
  - create_design
  - ...
```

如果没有，先配置：

```
/plugin marketplace add ChromeDevTools/chrome-devtools-mcp
/plugin install chrome-devtools-mcp
```

重启 Claude Code。

---

## 四、第一个例子：让 AI 创建一个按钮

### 对 Claude Code 说

```
在 Figma 里创建一个按钮组件：
- 主按钮：蓝色背景 (#1890FF)，白色文字
- 圆角：8px
- 内边距：12px 24px
- 字体大小：14px
- 字重：500
```

### AI 会怎么做

1. 通过 Figma MCP 连接到 Figma
2. 创建一个 Frame 作为按钮容器
3. 设置背景色为 #1890FF
4. 设置圆角为 8px
5. 设置内边距为 12px 24px
6. 添加文本图层，设置字体样式
7. 把它变成 Component

### 结果

几秒钟后，你会在 Figma 里看到 AI 创建的按钮组件！

---

## 五、第二个例子：创建整个登录页

### 对 Claude Code 说

```
在 Figma 里创建一个登录页设计：
1. 页面尺寸：375px × 667px（iPhone SE）
2. 顶部：Logo（居中，灰色占位符）
3. 中间：
   - 邮箱输入框：圆角 8px，边框 #E0E0E0
   - 密码输入框：同上
   - 登录按钮：蓝色 #1890FF，圆角 8px
4. 元素之间间距：16px
5. 整体居中，上下边距 40px
```

### AI 会怎么做

1. 创建一个 375×667 的 Frame
2. 添加 Logo 占位符
3. 创建两个输入框组件
4. 创建登录按钮
5. 用 Auto Layout 布局
6. 设置正确的间距

---

## 六、第三个例子：修改现有设计

### 对 Claude Code 说

```
打开这个 Figma 文件：
https://www.figma.com/design/xxx?node-id=1-2

把登录页改成暗黑模式：
- 背景色改成 #141414
- 文字改成 #FFFFFF
- 输入框背景改成 #1F1F1F
- 输入框边框改成 #333333
- 保持其他样式不变
```

### AI 会怎么做

1. 读取现有设计
2. 找到背景图层，改成 #141414
3. 找到所有文字图层，改成白色
4. 找到输入框，更新背景和边框
5. 保持圆角、间距等不变

---

## 七、第四个例子：从代码生成设计

### 对 Claude Code 说

```
把这个 React 组件转成 Figma 设计：

import { Button } from './Button'

function Header() {
  return (
    <nav style={{
      display: 'flex',
      justifyContent: 'space-between',
      padding: '16px 24px',
      backgroundColor: '#F5F5F5'
    }}>
      <div>Logo</div>
      <div style={{ display: 'flex', gap: '16px' }}>
        <Button variant="secondary">登录</Button>
        <Button variant="primary">注册</Button>
      </div>
    </nav>
  )
}
```

### AI 会怎么做

1. 分析代码结构
2. 创建导航栏 Frame
3. 设置 Flex 布局，两端对齐
4. 添加 Logo 占位符
5. 创建两个按钮
6. 设置间距和背景色

---

## 八、高级：创建组件变体

### 对 Claude Code 说

```
在 Figma 里创建一个 Button 组件集，包含这些变体：

Variants:
- Primary: 蓝色背景 #1890FF，白色文字
- Secondary: 白色背景，蓝色边框 #1890FF，蓝色文字
- Danger: 红色背景 #FF4D4F，白色文字

Sizes:
- Small: 内边距 8px 16px，字体 12px
- Medium: 内边距 12px 24px，字体 14px
- Large: 内边距 16px 32px，字体 16px

States:
- Default
- Hover: 颜色加深 10%
- Disabled: 不透明度 50%

用 Component Set 组织起来。
```

### AI 会怎么做

1. 创建 Component Set
2. 为每个变体创建实例
3. 设置正确的属性
4. 组织成变体组合

---

## 九、常用 Prompt 模板

### 模板 1：创建单个组件

```
在 Figma 里创建一个 [组件名] 组件：
- [属性1]: [值]
- [属性2]: [值]
- [属性3]: [值]
```

### 模板 2：创建整个页面

```
在 Figma 里创建一个 [页面名] 设计：

页面尺寸: [宽度] × [高度]

结构:
1. [区域1]: [描述]
2. [区域2]: [描述]
3. [区域3]: [描述]

样式:
- 背景色: [颜色]
- 主色调: [颜色]
- 间距: [值]
```

### 模板 3：修改现有设计

```
打开这个 Figma 文件：
[链接]

把设计改成 [新风格]：
- [修改1]
- [修改2]
- [修改3]

保持其他不变。
```

### 模板 4：从代码生成设计

```
把这个代码转成 Figma 设计：

[代码]

请创建对应的 Figma 设计，保持布局和样式一致。
```

---

## 十、最佳实践

### ✅ 应该做的

1. **描述得越详细越好**
   - 不要只说"创建一个按钮"
   - 要说"创建一个蓝色按钮，圆角 8px，内边距 12px 24px"

2. **从简单开始**
   - 先创建单个组件
   - 再创建整个页面
   - 逐步增加复杂度

3. **用具体的数值**
   - 不要说"大一点"
   - 要说"从 14px 改成 16px"

4. **分步骤进行**
   - 不要一次性创建整个网站
   - 先创建 Header，再创建 Footer，再创建内容

### ❌ 不应该做的

1. **不要太含糊**
   - ❌ "创建一个漂亮的页面"
   - ✅ "创建一个 375×667 的页面，背景白色，顶部居中 Logo..."

2. **不要一次性太复杂**
   - ❌ "创建整个电商网站，包含首页、列表页、详情页、购物车..."
   - ✅ "先创建首页的 Header 部分"

3. **不要害怕迭代**
   - 第一次生成的设计可能不完美
   - 可以让 AI 继续修改："把按钮改大一点，颜色改成深蓝色"

---

## 十一、常见问题

### Q: Desktop MCP 能用吗？

A: 不能！写入画布只支持 **Remote MCP**。

### Q: 免费版能用吗？

A: 需要 Dev 或 Full seat（付费计划）。

### Q: 能撤销 AI 的操作吗？

A: 可以！像平常一样用 `Cmd+Z`（macOS）或 `Ctrl+Z`（Windows）撤销。

### Q: AI 生成的设计质量怎么样？

A: 取决于你的 Prompt 写得好不好。描述得越详细，结果越好。

### Q: 能用中文 Prompt 吗？

A: 可以！AI 能理解中文，用中文描述就行。

---

## 十二、总结

用 AI 直接在 Figma 里创建设计是一个全新的工作流：
- 以前：你手动画图 → 花几个小时
- 现在：你描述想法 → AI 帮你画 → 几分钟搞定

关键是写好 Prompt：
- 详细
- 具体
- 分步骤

快去试试吧！让 AI 成为你的设计助手！
