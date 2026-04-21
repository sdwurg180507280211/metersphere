# 🔥 Code Connect：让生成的代码更像你的项目！

你有没有遇到过这种情况：AI 生成的代码能用，但和你们项目现有的组件风格完全不一样，还得自己重写一遍？

这时候 **Code Connect** 就派上用场了！它能告诉 AI："我们项目已经有这些组件了，请直接用它们！"

---

## 一、Code Connect 是什么？

### 简单理解

**Code Connect = Figma 组件 ↔ 代码组件的映射表**

没有 Code Connect 时：
```
Figma 设计稿：Button 组件
    ↓
AI：（重新写一个 Button 组件，用它自己的风格）
    ↓
你：（心想：怎么和我们项目的 Button 不一样？还得改...）
```

有 Code Connect 时：
```
Figma 设计稿：Button 组件
    ↓
Code Connect：（查映射表 → 哦，这个对应 src/components/Button.tsx）
    ↓
AI：（直接 import 你们的 Button，不用重新写！）
    ↓
你：（太棒了！完全符合项目规范！）
```

### Code Connect 能做什么？

| 功能 | 说明 |
|-----|------|
| 🔗 **组件映射** | Figma 组件 → 代码文件 |
| 🎨 **属性映射** | Figma 属性 → Props |
| 📝 **示例代码** | 告诉 AI 怎么用这个组件 |
| 🎯 **设计 Token** | Figma 变量 → CSS 变量 |

---

## 二、在 Figma 中设置 Code Connect

### 步骤 1：打开 Dev Mode

在 Figma 中：
1. 选中一个组件（比如你的 Button）
2. 点击右上角的 "Dev Mode"（开发模式）
3. 切换到 "Code" 标签

### 步骤 2：找到 Code Connect 部分

在 Dev Mode 的 Code 标签中，找到：
- "Code Connect" 或 "Connect to code"
- 点击 "Set up Code Connect"

### 步骤 3：关联代码文件

有两种方式：

#### 方式 A：手动关联（简单）

1. 在 Code Connect 中输入代码文件路径：
   ```
   src/components/Button.tsx
   ```
2. 保存

#### 方式 B：用 Code Connect 文件（高级）

在项目根目录创建 `figma.config.json` 或 `.code-connect.json`：

```json
{
  "codeConnect": {
    "components": [
      {
        "figmaNode": "Button",
        "source": "src/components/Button.tsx",
        "props": {
          "variant": {
            "type": "enum",
            "values": ["primary", "secondary"]
          },
          "size": {
            "type": "enum",
            "values": ["sm", "md", "lg"]
          }
        },
        "example": "import { Button } from './Button'\n\n<Button variant=\"primary\" size=\"md\">Click me</Button>"
      }
    ]
  }
}
```

---

## 三、Code Connect 配置详解

### 基础配置

```json
{
  "codeConnect": {
    "components": [
      {
        "figmaNode": "Button",
        "source": "src/components/Button.tsx"
      }
    ]
  }
}
```

| 字段 | 说明 |
|-----|------|
| `figmaNode` | Figma 中的组件名称 |
| `source` | 代码文件路径 |

### 属性映射配置

```json
{
  "codeConnect": {
    "components": [
      {
        "figmaNode": "Button",
        "source": "src/components/Button.tsx",
        "props": {
          "variant": {
            "type": "enum",
            "values": ["primary", "secondary", "danger"]
          },
          "size": {
            "type": "enum",
            "values": ["sm", "md", "lg"]
          },
          "disabled": {
            "type": "boolean"
          }
        }
      }
    ]
  }
}
```

### 示例代码配置

告诉 AI 怎么用这个组件：

```json
{
  "codeConnect": {
    "components": [
      {
        "figmaNode": "Button",
        "source": "src/components/Button.tsx",
        "example": "import { Button } from './Button'\n\n<Button variant=\"primary\" size=\"md\">\n  Click me\n</Button>"
      }
    ]
  }
}
```

### 设计 Token 映射

```json
{
  "codeConnect": {
    "tokens": {
      "colors": {
        "Primary Blue": "--color-primary",
        "Secondary Green": "--color-secondary",
        "Danger Red": "--color-danger"
      },
      "spacing": {
        "8px": "--spacing-sm",
        "16px": "--spacing-md",
        "24px": "--spacing-lg"
      }
    }
  }
}
```

---

## 四、实际例子：Button 组件

### 你的 Button 组件

`src/components/Button.tsx`:

```tsx
import React from 'react';

type ButtonVariant = 'primary' | 'secondary' | 'danger';
type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps {
  variant?: ButtonVariant;
  size?: ButtonSize;
  disabled?: boolean;
  children: React.ReactNode;
  onClick?: () => void;
}

export const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'md',
  disabled = false,
  children,
  onClick,
}) => {
  return (
    <button
      className={`button button--${variant} button--${size}`}
      disabled={disabled}
      onClick={onClick}
    >
      {children}
    </button>
  );
};
```

### Code Connect 配置

```json
{
  "codeConnect": {
    "components": [
      {
        "figmaNode": "Button",
        "source": "src/components/Button.tsx",
        "props": {
          "variant": {
            "type": "enum",
            "values": ["primary", "secondary", "danger"]
          },
          "size": {
            "type": "enum",
            "values": ["sm", "md", "lg"]
          },
          "disabled": {
            "type": "boolean"
          }
        },
        "example": "import { Button } from 'src/components/Button'\n\n<Button variant=\"primary\" size=\"md\">\n  Click me\n</Button>"
      }
    ]
  }
}
```

### AI 生成的代码（有 Code Connect）

```tsx
import { Button } from 'src/components/Button';

export function LoginForm() {
  return (
    <form>
      {/* 输入框... */}
      <Button variant="primary" size="md" type="submit">
        登录
      </Button>
    </form>
  );
}
```

完美！直接用了你们项目的 Button 组件！

---

## 五、AI 看到 Code Connect 后会怎么做？

### 没有 Code Connect 时

你：
```
根据这个 Figma 设计稿生成登录页代码
```

AI（心里想）：
```
哦，这个设计稿里有个 Button...
嗯，我得自己写一个 Button 组件...
用什么颜色呢？蓝色吧...
圆角多大？8px 吧...
好，写好了！
```

AI 生成的代码：
```tsx
// AI 自己写的 Button，和你们项目完全不一样
function MyButton({ children }) {
  return (
    <button style={{
      backgroundColor: 'blue',
      borderRadius: '8px',
      padding: '10px 20px'
    }}>
      {children}
    </button>
  );
}
```

### 有 Code Connect 时

你：
```
根据这个 Figma 设计稿生成登录页代码
```

AI（心里想）：
```
哦，这个设计稿里有个 Button...
让我看看 Code Connect...
哦！映射到 src/components/Button.tsx！
Props 有 variant、size、disabled...
还有示例代码！
好的，直接 import 就行！
```

AI 生成的代码：
```tsx
import { Button } from 'src/components/Button';

// 直接用你们项目的组件！
```

---

## 六、多组件配置示例

```json
{
  "codeConnect": {
    "components": [
      {
        "figmaNode": "Button",
        "source": "src/components/Button.tsx",
        "props": {
          "variant": { "type": "enum", "values": ["primary", "secondary"] }
        },
        "example": "import { Button } from 'src/components/Button'\n\n<Button variant=\"primary\">Click</Button>"
      },
      {
        "figmaNode": "Input",
        "source": "src/components/Input.tsx",
        "props": {
          "type": { "type": "enum", "values": ["text", "email", "password"] },
          "placeholder": { "type": "string" }
        },
        "example": "import { Input } from 'src/components/Input'\n\n<Input type=\"email\" placeholder=\"请输入邮箱\" />"
      },
      {
        "figmaNode": "Card",
        "source": "src/components/Card.tsx",
        "example": "import { Card } from 'src/components/Card'\n\n<Card>\n  <Card.Header>标题</Card.Header>\n  <Card.Body>内容</Card.Body>\n</Card>"
      }
    ]
  }
}
```

---

## 七、常见问题

### Q: Code Connect 要钱吗？

A: 不需要！完全免费。

### Q: 每个组件都要配置吗？

A: 建议配置常用的核心组件，比如 Button、Input、Card 等。不常用的可以不配置。

### Q: 配置文件放哪里？

A: 项目根目录，文件名可以是：
- `figma.config.json`
- `.code-connect.json`
- `codeconnect.json`

### Q: 可以和 Skills 一起用吗？

A: 当然可以！Code Connect + Skills 效果翻倍：
- Code Connect 告诉 AI 用哪个组件
- Skills 告诉 AI 怎么用这些组件

### Q: 已有项目能加 Code Connect 吗？

A: 可以！随时可以添加，不需要改现有代码。

---

## 八、最佳实践

### ✅ 应该做的

1. **从核心组件开始**
   - Button、Input、Card、Modal...
   - 不要一开始就配置所有组件

2. **提供示例代码**
   - AI 看示例代码学得最快
   - 提供常用场景的例子

3. **保持简单**
   - 配置文件不要太复杂
   - 重点覆盖常用场景

4. **提交到 Git**
   - 团队共享
   - 版本控制

### ❌ 不应该做的

1. **不要过度配置**
   - 每个变体都要配置？不需要
   - 覆盖 80% 常用场景就行

2. **不要写得太复杂**
   - AI 可能理解不了太复杂的逻辑
   - 保持简单直观

3. **不要忘记示例代码**
   - 只配置属性映射不够
   - 示例代码能让 AI 真正理解怎么用

---

## 九、总结

Code Connect 是一个简单但强大的工具：
- 告诉 AI 你们项目已经有哪些组件
- 让 AI 直接用你们的组件，而不是重新写
- 生成的代码完全符合项目规范

配合 Skills 使用，效果更好：
- Code Connect：用哪个组件
- Skills：怎么用这些组件

快去给你们项目的核心组件加上 Code Connect 吧！
