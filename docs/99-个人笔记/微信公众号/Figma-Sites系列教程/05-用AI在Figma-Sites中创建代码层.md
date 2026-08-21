# 🔥 用 AI 在 Figma Sites 中创建代码层：让你的网站更强大！

Figma Sites 已经很强大了，但如果你想实现一些自定义功能，比如评论、购物车、实时聊天——这时候 **Code Layers + AI** 就是答案！

这篇文章带你用 AI 在 Figma Sites 中创建代码层，让你的网站功能更强大！

---

## 一、先搞懂：Code Layers 是什么？

### 简单理解

**Code Layers = 在 Figma Sites 的无代码基础上，加自定义代码！**

没有 Code Layers 时：
```
你：我想加个评论功能
Figma Sites：抱歉，这个功能不支持
你：（只能放弃，或者找开发者）
```

有了 Code Layers 时：
```
你：我想加个评论功能
AI：好的，我来帮你写代码
    ↓
你：把代码加到 Code Layer
    ↓
网站：（评论功能上线了！）
```

### Code Layers 能做什么？

| 功能 | 例子 |
|-----|------|
| 💬 **评论系统** | 让用户评论你的网站 |
| 🛒 **购物车** | 电商网站 |
| 💬 **实时聊天** | 在线客服 |
| 📝 **自定义动画** | 比默认更酷的动画 |
| 🔌 **第三方集成** | Google Analytics、Stripe 等 |
| 🎨 **自定义样式** | 特殊的视觉效果 |

---

## 二、AI 怎么帮你？

### AI 的角色

在这个工作流里，AI 是你的**代码助手：

1. **你描述想法** → "我想加个评论功能
2. **AI 写代码** → 生成完整的代码
3. **你复制粘贴** → 把代码加到 Code Layer
4. **网站上线** → 功能直接能用！

---

## 三、准备工作

在开始之前，确保你有：

1. ✅ Figma Sites 网站已创建
2. ✅ 基本设计已完成
3. ✅ 想好要加什么功能
4. ✅ 一个 AI 工具（Claude Code、ChatGPT、Gemini 等）

---

## 四、实战 1：添加评论系统

### 步骤 1：想清楚你要什么

对 AI 说：

```
我想在 Figma Sites 的网站上加一个评论系统，需求：

1. 用户可以输入名字和评论
2. 点击"提交"按钮保存
3. 显示所有评论列表
4. 用 localStorage 存储（不需要后端）
5. 样式要简洁，和 Figma Sites 的风格一致
```

### 步骤 2：AI 生成代码

AI 会给你这样的代码：

```html
<!-- 评论系统代码 -->
<div id="comments-section">
  <h3>评论</h3>

  <!-- 评论表单 -->
  <form id="comment-form">
    <input type="text" id="name-input" placeholder="你的名字" required />
    <textarea id="comment-input" placeholder="写下你的评论..." required></textarea>
    <button type="submit">提交评论</button>
  </form>

  <!-- 评论列表 -->
  <div id="comments-list"></div>
</div>

<style>
  #comments-section {
    max-width: 600px;
    margin: 40px auto;
    padding: 20px;
  }

  #comment-form {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-bottom: 32px;
  }

  #name-input, #comment-input {
    padding: 12px;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    font-size: 16px;
  }

  #comment-input {
    min-height: 100px;
    resize: vertical;
  }

  #comment-form button {
    padding: 12px 24px;
    background: #1890ff;
    color: white;
    border: none;
    border-radius: 8px;
    font-size: 16px;
    cursor: pointer;
  }

  .comment-item {
    padding: 16px;
    border-bottom: 1px solid #f0f0f0;
  }

  .comment-name {
    font-weight: 600;
    margin-bottom: 8px;
  }
</style>

<script>
  // 加载评论
  function loadComments() {
    const comments = JSON.parse(localStorage.getItem('comments') || '[]');
    const list = document.getElementById('comments-list');
    list.innerHTML = comments.map(comment => `
      <div class="comment-item">
        <div class="comment-name">${comment.name}</div>
        <div class="comment-text">${comment.text}</div>
      </div>
    `).join('');
  }

  // 提交评论
  document.getElementById('comment-form').addEventListener('submit', function(e) {
    e.preventDefault();
    const name = document.getElementById('name-input').value;
    const text = document.getElementById('comment-input').value;

    const comments = JSON.parse(localStorage.getItem('comments') || '[]');
    comments.unshift({ name, text, date: new Date().toISOString() });
    localStorage.setItem('comments', JSON.stringify(comments));

    document.getElementById('name-input').value = '';
    document.getElementById('comment-input').value = '';
    loadComments();
  });

  // 页面加载时显示评论
  loadComments();
</script>
```

### 步骤 3：在 Figma Sites 中添加 Code Layer

1. 在 Figma Sites 中，选中要加功能的位置
2. 点击 "Add" → "Code Layer"
3. 把 AI 生成的代码粘贴进去
4. 保存！

### 步骤 4：预览和发布

1. 点击 "Preview" 看看效果
2. 如果需要调整，让 AI 修改
3. 满意后点击 "Publish"！

---

## 五、实战 2：添加自定义动画

### 对 AI 说

```
我想在 Figma Sites 的网站上加一个滚动动画效果：

1. 当用户滚动页面时，元素逐渐出现
2. 元素从下方淡入并上移
3. 不要太夸张，要优雅一点
4. 性能要好
```

### AI 生成的代码

```html
<style>
  .scroll-reveal {
    opacity: 0;
    transform: translateY(30px);
    transition: opacity 0.6s ease, transform 0.6s ease;
  }

  .scroll-reveal.revealed {
    opacity: 1;
    transform: translateY(0);
  }
</style>

<script>
  // 滚动显示动画
  const observerOptions = {
    threshold: 0.1,
    rootMargin: '0px 0px -50px 0px'
  };

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('revealed');
      }
    });
  }, observerOptions);

  // 给所有 Section 添加动画
  document.querySelectorAll('.scroll-reveal').forEach(el => {
    observer.observe(el);
  });
</script>
```

然后在 Figma Sites 中，给想加动画的元素加上 `scroll-reveal` 这个 class 就行！

---

## 六、实战 3：集成 Google Analytics

### 对 AI 说

```
帮我写一段 Google Analytics 代码，集成到 Figma Sites 网站：

1. 我的 GA ID 是 G-XXXXXXXXXX
2. 要跟踪页面浏览
3. 要跟踪按钮点击
4. 要符合隐私法规
```

### AI 生成的代码

```html
<!-- Google Analytics -->
<script async src="https://www.googletagmanager.com/gtag/js?id=G-XXXXXXXXXX"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());
  gtag('config', 'G-XXXXXXXXXX');

  // 跟踪按钮点击
  document.addEventListener('click', function(e) {
    if (e.target.tagName === 'BUTTON') {
      gtag('event', 'button_click', {
        'button_text': e.target.textContent
      });
    }
  });
</script>
```

---

## 七、AI 提示词技巧

### 好的提示词包含这些要素

1. **明确功能**
```
❌ "加个评论功能"
✅ "加个评论系统：用户输入名字和评论，提交保存，显示列表，用 localStorage"
```

2. **技术要求**
```
❌ "写得好一点"
✅ "用 localStorage 存储，不需要后端，样式简洁"
```

3. **风格要求**
```
❌ "好看一点"
✅ "和 Figma Sites 的风格一致，圆角 8px，主色 #1890FF"
```

### 完整提示词模板

```
我想在 Figma Sites 中加一个 [功能名称]，需求：

1. [功能描述 1]
2. [功能描述 2]
3. [功能描述 3]

技术要求：
- [技术要求 1]
- [技术要求 2]

样式要求：
- [样式要求 1]
- [样式要求 2]
```

---

## 八、常见问题

### Q: Code Layers 要我自己写代码吗？

A: 不需要！让 AI 帮你写，你只要复制粘贴就行。

### Q: AI 写的代码有问题怎么办？

A: 让 AI 修改！把问题描述给 AI：
```
这个代码有个问题：[描述问题]
帮我改一下。
```

### Q: 可以加多个 Code Layers 吗？

A: 可以！一个网站可以加多个 Code Layer，每个实现不同的功能。

### Q: Code Layers 影响网站性能吗？

A: 只要代码写得好就不会影响。可以让 AI 注意性能优化。

### Q: 可以用 React/Vue 这些框架吗？

A: 可以！但推荐用原生 HTML/CSS/JavaScript，更简单，性能更好。

---

## 九、最佳实践

### ✅ 应该做的

1. **从小功能开始**
   - 先加个简单的动画
   - 再加点交互
   - 最后加复杂功能

2. **详细描述需求**
   - 越详细越好
   - AI 理解得越准确

3. **分步测试**
   - 加一个功能，测试一下
   - 没问题再加下一个

4. **让 AI 优化**
   - 第一版能用就行
   - 然后让 AI 优化代码、改进体验

### ❌ 不应该做的

1. **不要一次性加太多功能**
   - 一个一个来
   - 出问题好排查

2. **不要太复杂**
   - 能用原生 JS 就别用框架
   - 简单就是最好

3. **不要忽略性能**
   - 让 AI 注意性能
   - 特别是动画和交互

---

## 十、总结

Code Layers + AI 是 Figma Sites 的超能力：
- Figma Sites 负责无代码，做 80% 的功能
- Code Layers 负责剩下 20% 的自定义功能
- AI 负责帮你写那 20% 的代码

三者配合，你就能做出：
- 设计好看
- 功能强大
- 完全自定义
- 不需要开发者

快去试试用 AI 给你的 Figma Sites 网站加功能吧！
