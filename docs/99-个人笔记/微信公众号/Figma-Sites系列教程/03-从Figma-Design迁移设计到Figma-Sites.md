# 🔥 把 Figma Design 设计移到 Figma Sites：无缝衔接！

你已经在 Figma Design 里做好了网站设计，现在想把它变成真正的网站？这篇文章带你把 Figma Design 的设计完美迁移到 Figma Sites！

---

## 一、先搞懂：为什么要迁移？

### 简单理解

**Figma Design = 做设计稿的地方**
**Figma Sites = 把设计变成网站的地方**

没有迁移时：
```
你：在 Figma Design 做好了设计
    ↓
你：把设计稿发给开发者
    ↓
开发者：（花 1-2 周还原成代码）
    ↓
你：怎么和设计稿不一样？
```

有了 Figma Sites 时：
```
你：在 Figma Design 做好了设计
    ↓
你：直接移到 Figma Sites
    ↓
你：点击发布，网站上线了！
```

---

## 二、迁移前的准备

### 检查清单

在开始迁移前，确保你的 Figma Design 文件：

| 检查项 | 说明 |
|-------|------|
| ✅ **设计完整** | 页面、组件都设计好了 |
| ✅ **命名规范** | Layer 名称清晰易懂 |
| ✅ **结构合理** | 用 Frame 组织内容 |
| ✅ **组件化** | 常用元素做成 Component |
| ✅ **图片资源** | 所有图片都已导入 |

### 优化建议

#### 1. 用 Frame 组织内容

**不好的做法：**
```
Page
  ├─ Text 1
  ├─ Text 2
  ├─ Rectangle 1
  ├─ Image 1
  └─ ...
```

**好的做法：**
```
Page
  ├─ Frame: 导航栏
  │   ├─ Logo
  │   └─ 导航链接
  ├─ Frame: 英雄区域
  │   ├─ 标题
  │   ├─ 副标题
  │   └─ 按钮
  └─ Frame: 页脚
      └─ ...
```

#### 2. 命名清晰

**不好的命名：**
```
Frame 1
Frame 2
Text 1
Rectangle 3
```

**好的命名：**
```
导航栏
英雄区域
主要标题
CTA 按钮
```

#### 3. 做成 Components

如果有重复使用的元素（比如按钮、卡片），做成 Component：
- 按钮 → Button Component
- 卡片 → Card Component
- 这样迁移后更容易维护

---

## 三、迁移方法 1：复制粘贴（最简单）

### 适合场景

- 简单的设计
- 少量页面
- 快速原型

### 步骤

#### 步骤 1：打开两个文件

1. 在 Figma 中打开你的 **Figma Design 文件**
2. 同时打开 **Figma Sites**（新建或打开现有项目）

#### 步骤 2：选择要复制的内容

在 Figma Design 中：
1. 选中你想复制的 Frame 或元素
2. 按 `Cmd + C`（Mac）或 `Ctrl + C`（Windows）

#### 步骤 3：粘贴到 Figma Sites

切换到 Figma Sites：
1. 按 `Cmd + V`（Mac）或 `Ctrl + V`（Windows）
2. 调整位置和大小

#### 步骤 4：转换为 Figma Sites Blocks

粘贴后，Figma Sites 会尝试自动转换：
- 文本 → Text Block
- 矩形 + 文字 → Button Block
- 图片 → Image Block
- Frame → Section Block

如果自动转换不理想，可以手动调整：
1. 选中元素
2. 在右侧面板调整属性
3. 或者删除后用 Figma Sites 的 Block 重新做

---

## 四、迁移方法 2：组件库方式（推荐）

### 适合场景

- 复杂的设计系统
- 多个页面
- 需要长期维护

### 思路

```
Figma Design（组件库）
        ↓
    发布为 Library
        ↓
Figma Sites（使用 Library）
```

### 步骤

#### 步骤 1：准备组件库

在 Figma Design 中：
1. 把所有可复用元素做成 Component
2. 组织好 Component Set
3. 确保命名规范

**建议的组件结构：**
```
Components
  ├─ Buttons
  │   ├─ Primary Button
  │   ├─ Secondary Button
  │   └─ Text Button
  ├─ Cards
  │   ├─ Product Card
  │   └─ Feature Card
  ├─ Forms
  │   ├─ Input
  │   └─ Textarea
  └─ Navigation
      └─ Navbar
```

#### 步骤 2：发布 Library

1. 在 Figma Design 文件中，点击右上角的 **"Share"**
2. 选择 **"Publish to library"**
3. 填写版本信息
4. 点击 **"Publish"**

#### 步骤 3：在 Figma Sites 中启用 Library

1. 打开 Figma Sites 项目
2. 点击顶部的 **"Assets"** 标签
3. 点击 **"Library"**
4. 找到你刚发布的组件库
5. 点击 **"Enable"**

#### 步骤 4：使用组件

现在你可以在 Figma Sites 中：
1. 从 Assets 面板拖入组件
2. 组件会保持和 Figma Design 的同步
3. 如果 Figma Design 更新了，Figma Sites 会收到更新提示

---

## 五、迁移时的常见问题和解决

### 问题 1：颜色不一样

**原因：** Figma Design 和 Figma Sites 的颜色配置可能不同。

**解决方法：**
1. 在 Figma Design 中查看颜色值（HEX/RGB）
2. 在 Figma Sites 中手动设置相同颜色
3. 或者创建颜色样式，确保一致

### 问题 2：字体不一样

**原因：** Figma Sites 支持的字体可能和 Figma Design 不同。

**解决方法：**
1. 检查 Figma Sites 支持的字体列表
2. 选择最接近的替代字体
3. 或者用 Web Fonts（付费版功能）

### 问题 3：布局错乱

**原因：** Figma Design 的绝对定位和 Figma Sites 的流式布局不同。

**解决方法：**
1. 不要直接复制整个页面
2. 用 Figma Sites 的 Blocks 重新搭建结构
3. 把 Figma Design 当作"参考图"，而不是"直接复制"

### 问题 4：图片模糊

**原因：** 复制的图片分辨率不够。

**解决方法：**
1. 在 Figma Sites 中重新上传高清图片
2. 确保图片尺寸至少是显示尺寸的 2 倍（Retina 屏幕）

---

## 六、更好的方式：直接在 Figma Sites 设计

### 什么时候推荐直接在 Figma Sites 设计？

| 场景 | 推荐方式 |
|-----|---------|
| 新项目，还没开始设计 | 直接在 Figma Sites 设计 |
| 快速原型，需要马上看效果 | 直接在 Figma Sites 设计 |
| 简单网站，不需要复杂设计 | 直接在 Figma Sites 设计 |
| 已经有 Figma Design 设计稿 | 迁移到 Figma Sites |
| 复杂的设计系统 | 用 Library 方式 |

### 直接在 Figma Sites 设计的优势

1. **实时预览**
   - 改一点就能看到实际效果
   - 不用在两个工具之间切换

2. **所见即所得**
   - 编辑器里看到的 = 发布后的效果
   - 没有"还原度"问题

3. **响应式简单**
   - 内置 Breakpoints
   - 一键切换设备视图

4. **更快发布**
   - 设计完成 = 网站准备好
   - 点击一下就发布

---

## 七、实战例子：迁移一个着陆页

### 假设你在 Figma Design 有这样的设计：

```
着陆页
  ├─ 导航栏
  │   ├─ Logo
  │   ├─ 首页
  │   ├─ 产品
  │   └─ 联系
  ├─ 英雄区域
  │   ├─ 主标题："让 AI 帮你做网站"
  │   ├─ 副标题："Figma Sites，设计师也能做网站"
  │   └─ 按钮："立即开始"
  ├─ 特性区域
  │   ├─ 特性 1
  │   ├─ 特性 2
  │   └─ 特性 3
  └─ 页脚
      └─ 版权信息
```

### 迁移步骤

#### 步骤 1：在 Figma Sites 创建新项目

1. 打开 Figma → 点击 Sites
2. 选择 Landing Page 模板（或者 Blank）
3. 命名为"我的网站"

#### 步骤 2：搭建框架（不用复制，直接用 Blocks）

1. 添加 Navigation Block
   - 设置 Logo
   - 添加导航链接
2. 添加 Section Block（英雄区域）
   - 加 Text Block（主标题）
   - 加 Text Block（副标题）
   - 加 Button Block
3. 添加 Section Block（特性区域）
   - 添加三个 Feature Cards
4. 添加 Footer Block

#### 步骤 3：参考 Figma Design 调整样式

打开 Figma Design 文件，对照着调整：
- 颜色：把 Figma Design 的颜色值填进去
- 字体：选择类似的字体
- 间距：调整 Padding/Margin 接近原图
- 圆角：设置相同的圆角值

#### 步骤 4：添加图片

1. 从 Figma Design 导出图片
2. 在 Figma Sites 中上传
3. 替换到对应的位置

#### 步骤 5：设置响应式

1. 切换到 Tablet 视图
   - 调整布局
   - 调整字号
2. 切换到 Mobile 视图
   - 优化导航
   - 垂直排列内容
   - 按钮全宽

#### 步骤 6：预览和发布

1. 点击 Preview
2. 在不同设备查看
3. 满意后点击 Publish

---

## 八、常见问题

### Q: 可以直接把 Figma Design 文件导入 Figma Sites 吗？

A: 目前不能直接"导入"，但可以：
1. 复制粘贴元素
2. 用 Library 方式共享组件
3. 或者直接在 Figma Sites 重新做（通常更快）

### Q: 迁移后会自动保持同步吗？

A: 如果你用 Library 方式：
- Figma Design 更新 → Figma Sites 收到更新提示
- 你可以选择是否接受更新

如果是复制粘贴：
- 不会自动同步
- 需要手动修改

### Q: 动画和交互能迁移吗？

A: 建议在 Figma Sites 中重新设置：
- Figma Design 的原型交互和 Figma Sites 的交互不同
- Figma Sites 的交互更适合真实网站

### Q: 迁移一个页面需要多长时间？

A: 取决于复杂度：
- 简单页面：10-30 分钟
- 中等页面：30-60 分钟
- 复杂页面：1-2 小时

---

## 九、最佳实践

### ✅ 应该做的

1. **把 Figma Design 当作参考**
   - 不要追求 100% 一模一样
   - 重点还原视觉风格和内容

2. **用 Figma Sites 的 Blocks**
   - 不要复制一堆零散元素
   - 用 Section、Text、Button 等 Blocks 重新搭建

3. **先搭框架，再调细节**
   - 先把所有 Block 放好位置
   - 再调整颜色、字体、间距

4. **经常预览**
   - 调整一点就预览看看
   - 确保效果符合预期

### ❌ 不应该做的

1. **不要尝试复制整个页面**
   - 直接复制通常效果不好
   - 用 Blocks 重新搭建更快

2. **不要纠结像素级对齐**
   - Figma Sites 是流式布局
   - 视觉上接近就行

3. **不要忽略响应式**
   - 不要只在 Desktop 视图设计
   - 记得检查 Tablet 和 Mobile

---

## 十、总结

迁移设计的三种方式：

1. **复制粘贴** - 适合简单、快速的场景
2. **Library 组件库** - 适合复杂、长期维护的项目
3. **直接在 Figma Sites 设计** - 新项目推荐这种！

核心思路：
- Figma Design 是"设计工具"
- Figma Sites 是"网站工具"
- 两者配合，效率翻倍！

下一篇我们讲：如何在 Figma Sites 中使用设计工具！
