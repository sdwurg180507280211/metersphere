# 范小白风格 - 公众号排版 + AI图片生成提示词

> 基于「范小白」公众号文章分析提取，可直接复制使用

---

## 📋 一、公众号文章排版格式

### 排版结构

```
[标题]
[作者 + 日期]
↓
[连续多张竖版插画，一张占一行]
↓
[结尾 3-5 行短句金句]
↓
[二维码 + 公众号名称（居中）]
```

### 版式参数

| 元素 | 规格 | 对齐 |
|------|------|------|
| 页面背景 | `#ffffff` 纯白色 | - |
| 标题 | 22px，深灰 `#333` | 居中 |
| 作者日期 | 14px，浅灰 `#888` | 居中 |
| 图片宽度 | 640px，占满内容区 | 居中 |
| 图片间距 | 5px 间距 | - |
| 结尾文字 | 16px，深灰 `#333`，行高 2 | 居中 |
| 二维码 | 300px | 居中 |

### 可直接使用的 HTML 模板

复制粘贴到公众号编辑器 / 秀米 / 135 的 HTML 模式：

```html
<div style="max-width: 640px; margin: 0 auto; background: #ffffff; padding: 10px;">
  <!-- 标题 -->
  <h1 style="text-align: center; font-size: 22px; color: #333333; margin: 20px 0 10px; font-weight: bold; line-height: 1.6;">
    这里是你的标题
  </h1>

  <!-- 作者日期 -->
  <div style="text-align: center; color: #888888; font-size: 14px; margin-bottom: 25px;">
    <span>你的名称</span> &nbsp;·&nbsp; <span>2026年4月7日</span>
  </div>

  <!-- 图片列表 -->
  <div style="text-align: center;">
    <img src="https://你的图片链接1.jpg" style="width: 100%; max-width: 640px; display: block; margin: 0 auto 5px; border: none;" />
    <img src="https://你的图片链接2.jpg" style="width: 100%; max-width: 640px; display: block; margin: 0 auto 5px; border: none;" />
    <img src="https://你的图片链接3.jpg" style="width: 100%; max-width: 640px; display: block; margin: 0 auto 5px; border: none;" />
    <img src="https://你的图片链接4.jpg" style="width: 100%; max-width: 640px; display: block; margin: 0 auto 5px; border: none;" />
    <img src="https://你的图片链接5.jpg" style="width: 100%; max-width: 640px; display: block; margin: 0 auto 5px; border: none;" />
    <!-- 继续添加图片... -->
  </div>

  <!-- 结尾文字 -->
  <div style="padding: 30px 20px; text-align: center; line-height: 2;">
    <p style="font-size: 16px; color: #333; margin: 0 0 15px;">这里是第一句</p>
    <p style="font-size: 16px; color: #333; margin: 0 0 15px;">这里是第二句</p>
    <p style="font-size: 16px; color: #333; margin: 0 0 15px;">这里是第三句</p>
    <p style="font-size: 16px; color: #333; margin: 0;">这里是最后一句</p>
  </div>

  <!-- 分割空行 -->
  <div style="height: 30px;"></div>

  <!-- 二维码 -->
  <div style="text-align: center; margin-bottom: 20px;">
    <img src="https://你的二维码链接.png" style="width: 300px; max-width: 80%; border: none;" />
    <div style="text-align: center; color: #666; font-size: 16px; margin-top: 10px;">
      你的公众号名称
    </div>
  </div>
</div>
```

### 使用步骤

1. 在公众号编辑器 / 秀米 中切换到 HTML 源码模式
2. 粘贴上面代码
3. 替换占位文字和图片链接
4. 预览发布，效果和原版一致

---

## 🎨 二、配色方案提取

从原图提取的主要配色：

| 颜色名称 | 色值 | 用途 |
|----------|------|------|
| 背景色 | `#ffffff` | 页面背景 |
| 主文字 | `#333333` | 正文标题 |
| 次要文字 | `#888888` | 作者日期 |
| 主色调1 | `#F8D7E2` | 浅粉色（皮肤/背景） |
| 主色调2 | `#F5E8C8` | 暖黄色（背景） |
| 主色调3 | `#8B7355` | 棕褐色（线条） |

---

## 🖼️ 三、AI 图片生成提示词

### Midjourney

#### 基础通用版

```
cute healing illustration, a soft young girl with short hair in a quiet daily scene, warm pastel color palette dominated by light pink and beige, clean simple line art, minimalist digital painting, vertical composition, soft shading, gentle atmosphere, for wechat official account, fanxiaobai style --ar 9:16 --style raw --quality 1 --stop 85
```

#### 场景变体

**室内窗边阅读：**
```
cute healing illustration, a soft young girl sitting by the window reading a book, afternoon sunshine streaming in, warm pastel color palette dominated by light pink and beige, clean simple line art, minimalist digital painting, vertical composition, soft shading, gentle cozy atmosphere, for wechat official account, fanxiaobai style --ar 9:16 --style raw
```

**喝咖啡放空：**
```
cute healing illustration, a soft young girl holding a warm cup of coffee looking out the window, lonely but peaceful mood, warm pastel color palette with light brown and cream, clean simple line art, minimalist digital painting, vertical composition, soft shading, quiet emotional atmosphere, for wechat official account, fanxiaobai style --ar 9:16 --style raw
```

**春日街道散步：**
```
cute healing illustration, a soft young girl walking on a tree-lined street in spring, cherry blossoms falling, warm pastel color palette with light green and pink, clean simple line art, minimalist digital painting, vertical composition, soft shading, fresh peaceful atmosphere, for wechat official account, fanxiaobai style --ar 9:16 --style raw
```

**沙发撸猫：**
```
cute healing illustration, a soft young girl lying on sofa with a fluffy cat, lazy weekend afternoon, warm pastel color palette with light gray and pink, clean simple line art, minimalist digital painting, vertical composition, soft shading, cozy relaxed atmosphere, for wechat official account, fanxiaobai style --ar 9:16 --style raw
```

**花店买花：**
```
cute healing illustration, a soft young girl holding a bouquet of flowers walking out of flower shop, warm spring afternoon, warm pastel color palette with green and pink, clean simple line art, minimalist digital painting, vertical composition, soft shading, happy sweet atmosphere, for wechat official account, fanxiaobai style --ar 9:16 --style raw
```

#### 风格微调技巧

| 问题 | 解决方法（添加关键词） |
|------|------------------------|
| 太写实 | `more flat colors, simpler lines, more minimalist` |
| 颜色太艳 | `lower saturation, more muted, softer colors` |
| 想要更接近原作 | 使用原图作参考：`[图片URL] --style ref [图片URL] --style 250` |

---

### Stable Diffusion

#### 正面提示词

```
(masterpiece, best quality, 8k), cute healing illustration, a young girl, (soft round features:1.2), warm pastel colors, light pink, beige, pale yellow, (clean outline:1.3), minimalist drawing, flat texture with soft shading, vertical composition, (full body:0.8), daily life scene, gentle atmosphere, cozy, emotional, wechat official account style, fanxiaobai style
```

#### 负面提示词

```
(worst quality, low quality:1.4), (ugly:1.2), (deformed:1.2), bad anatomy, extra limbs, missing limbs, messy lines, cluttered, harsh shadows, saturated colors, bright neon, (photorealistic:1.4), 3d render, (complex details:1.3), text, watermark, signature
```

#### 推荐参数

- **Sampler:** DPM++ 2M Karras
- **Steps:** 30-40
- **CFG scale:** 4-7
- **Resolution:** 768×1344（9:16 竖版）

---

### DALL-E 3 / Claude / GPT-4V 中文提示词

```
请生成一张符合以下风格的竖版插画：

风格：治愈系手绘插画，类似微信公众号"范小白"的风格
主体：一个可爱的年轻短发女孩，[替换成你的场景：例如窗边喝咖啡/公园散步/书桌前看书]
色彩：暖色调配色，以浅粉色、米黄色、浅棕色为主，色彩柔和低饱和
线条：干净简洁的轮廓线，不要太复杂
光影：柔和阴影，整体光线明亮温暖
氛围：安静治愈，给人情绪舒缓的感觉
构图：竖版构图，9:16比例，适合微信公众号文章使用
细节：不要多余杂乱的元素，保持简约干净
```

---

### 关键词速查（自由组合）

| 类别 | 可选关键词 |
|------|-----------|
| **主体人物** | `soft young girl`, `cute girl`, `short hair`, `gentle expression` |
| **场景** | `by the window`, `reading book`, `drinking coffee`, `walking in park`, `spring afternoon`, `rainy day indoors`, `holding flower`, `lying on bed`, `with cat on sofa` |
| **色彩** | `warm pastel`, `light pink`, `beige`, `pale yellow`, `soft brown`, `low saturation`, `muted colors` |
| **风格** | `healing illustration`, `minimalist`, `clean line art`, `simple drawing`, `digital illustration` |
| **氛围** | `gentle`, `peaceful`, `cozy`, `quiet`, `emotional`, `lonely but warm` |
| **构图** | `vertical composition`, `9:16`, `full body`, `centered` |

---

## ✨ 四、完整创作流程

1. **想标题** → 一句话情感标语（例如："想那么多干嘛 怎么开心怎么过"）
2. **生成图片** → 用上面提示词生成 8-15 张不同场景的竖版图
3. **上传图片** → 上传到公众号素材库，获取图片链接
4. **套用模板** → 把图片链接和结尾文案填入 HTML 模板
5. **添加二维码** → 在末尾放上你的公众号二维码
6. **预览发布** → 检查效果后发布

---

## 风格特点总结

- **图文比例**：图片占 90%，文字占 10%
- **文字策略**：结尾几句话点题，不说教，留留白
- **调性**：治愈情绪，给年轻人提供情绪价值
- **版式**：极简居中，大量留白，呼吸感强
