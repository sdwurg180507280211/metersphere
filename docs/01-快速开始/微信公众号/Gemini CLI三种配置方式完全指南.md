# Gemini CLI三种配置方式完全指南：从入门到精通

## 🎯 写在前面

Gemini CLI是Google官方推出的命令行工具，让你可以在终端直接使用Gemini AI。但很多人在配置时遇到困难，不知道该选择哪种方式。

本文详细介绍三种配置方式，帮你找到最适合自己的方案。

## 📦 前置准备

### 安装Node.js

Gemini CLI需要Node.js 14.x或更高版本。

**macOS安装**：
```bash
# 使用Homebrew
brew update
brew install node

# 验证安装
node --version
npm --version
```

### 安装Gemini CLI

```bash
# 配置npm镜像（中国用户推荐）
npm config set registry https://registry.npmmirror.com

# 全局安装
npm install -g @google/gemini-cli

# 验证安装
gemini --version
```

## 🔐 三种配置方式对比

| 对比项 | OAuth登录 | 中转平台API | Google官方API |
|--------|----------|------------|--------------|
| 难度 | ⭐ 简单 | ⭐⭐ 中等 | ⭐⭐⭐ 复杂 |
| 费用 | 固定月费 | 按平台定价 | 按token计费 |
| 网络要求 | 需科学上网 | 无要求 | 需科学上网 |
| 适用场景 | 重度个人使用 | 轻度使用/开发 | 企业开发 |
| 配额限制 | 有限制 | 看平台 | 按付费 |

## 方式一：OAuth登录（会员方式）

### 💡 适合人群

- 已订阅Gemini Advanced（Pro会员）
- 重度使用，不想计算token
- 有稳定的科学上网环境

### 📝 配置步骤

**1. 编辑配置文件**

```bash
nano ~/.gemini/settings.json
```

内容如下：
```json
{
  "security": {
    "auth": {
      "selectedType": "oauth-personal"
    }
  },
  "general": {
    "sessionRetention": {
      "enabled": true,
      "maxAge": "30d",
      "warningAcknowledged": true
    }
  },
  "ide": {
    "enabled": true
  }
}
```

**2. 清空.env文件**

```bash
cat > ~/.gemini/.env << 'EOF'
# OAuth认证不需要配置API Key
EOF
```

**3. 启动登录**

```bash
gemini
```

会自动打开浏览器，用Google账号登录即可。

### ✅ 优点

- 配置简单，一键登录
- 固定月费，使用量大更划算
- 无需管理API Key

### ❌ 缺点

- 需要订阅会员（$19.99/月）
- 需要科学上网
- 有使用配额限制
- OAuth登录可能不稳定

### 💰 费用说明

- 月费：$19.99/月（Gemini Advanced订阅）
- 不按token计费
- 有每日/每小时请求限制

## 方式二：中转平台API Key

### 💡 适合人群

- 轻度使用或开发测试
- 国内网络环境
- 想要灵活控制成本
- 不想处理Google官方API的复杂配置

### 📝 配置步骤

**1. 注册中转平台**

推荐平台：
- https://api.aicoding.sh
- https://code.newcli.com

注册账号并创建API Key

**2. 编辑配置文件**

```bash
nano ~/.gemini/settings.json
```

内容如下：
```json
{
  "security": {
    "auth": {
      "selectedType": "gemini-api-key"
    }
  },
  "general": {
    "sessionRetention": {
      "enabled": true,
      "maxAge": "30d",
      "warningAcknowledged": true
    }
  },
  "ide": {
    "enabled": true
  }
}
```

**3. 配置环境变量**

```bash
nano ~/.gemini/.env
```

内容如下：
```
GOOGLE_GEMINI_BASE_URL=https://api.aicoding.sh
GEMINI_API_KEY=你的API密钥
GEMINI_MODEL=gemini-3-pro-high
```

**4. 测试运行**

```bash
gemini -p "你好"
```

### ✅ 优点

- 无需科学上网
- 配置简单
- 按需付费，成本可控
- 国内访问速度快

### ❌ 缺点

- 需要在中转平台注册
- 依赖第三方平台稳定性
- 可能有额外的平台费用

### 💰 费用说明

取决于具体平台的定价，通常：
- 按token计费
- 或提供套餐包
- 比Google官方API略贵，但更方便

## 方式三：Google官方API Key

### 💡 适合人群

- 企业级应用开发
- 需要最高稳定性
- 有Google Cloud使用经验
- 能够科学上网

### 📝 配置步骤

**1. 创建Google Cloud项目**

- 访问 https://console.cloud.google.com
- 创建新项目
- 启用Gemini API

**2. 创建API Key**

- 进入"API和服务" > "凭据"
- 创建API密钥
- 设置API密钥限制

**3. 配置Gemini CLI**

```bash
nano ~/.gemini/settings.json
```

内容如下：
```json
{
  "security": {
    "auth": {
      "selectedType": "gemini-api-key"
    }
  },
  "ide": {
    "enabled": true
  }
}
```

**4. 配置环境变量**

```bash
nano ~/.gemini/.env
```

内容如下：
```
GEMINI_API_KEY=你的Google API密钥
GEMINI_MODEL=gemini-1.5-pro
```

注意：不需要配置`GOOGLE_GEMINI_BASE_URL`，会自动使用Google官方端点。

### ✅ 优点

- 官方支持，最稳定
- 价格透明，按实际使用计费
- 无第三方依赖
- 适合企业级应用

### ❌ 缺点

- 配置复杂
- 需要Google Cloud账号
- 需要科学上网
- 需要绑定信用卡

### 💰 费用说明

**gemini-1.5-pro定价**（参考）：
- 输入：$3.50 / 1M tokens
- 输出：$10.50 / 1M tokens

**计算示例**：
```
一次对话：
- 输入1000 tokens
- 输出500 tokens

费用 = (1000 × $3.50 / 1,000,000) + (500 × $10.50 / 1,000,000)
     = $0.0035 + $0.00525
     = $0.00875（约0.9分钱）
```

## 🔄 如何切换配置方式

### 从OAuth切换到API Key

```bash
# 1. 修改认证方式
nano ~/.gemini/settings.json
# 将 "oauth-personal" 改为 "gemini-api-key"

# 2. 配置API Key
nano ~/.gemini/.env
# 添加API Key和Base URL

# 3. 清除OAuth缓存
rm -f ~/.gemini/oauth_creds.json ~/.gemini/google_accounts.json
```

### 从API Key切换到OAuth

```bash
# 1. 修改认证方式
nano ~/.gemini/settings.json
# 将 "gemini-api-key" 改为 "oauth-personal"

# 2. 清空.env文件
echo "# OAuth不需要API Key" > ~/.gemini/.env

# 3. 重新登录
gemini
```

## 🎯 选择建议

### 个人使用

**重度使用**（每天大量对话）
→ 推荐：OAuth登录
→ 理由：固定月费，不用担心token消耗

**轻度使用**（偶尔使用）
→ 推荐：中转平台API Key
→ 理由：按需付费，成本更低

### 开发场景

**学习测试**
→ 推荐：中转平台API Key
→ 理由：配置简单，国内可用

**企业应用**
→ 推荐：Google官方API
→ 理由：稳定可靠，适合生产环境

### 网络环境

**有科学上网**
→ OAuth或Google官方API都可以

**无科学上网**
→ 只能选择中转平台API Key

## ⚠️ 常见问题

### OAuth登录一直加载？

**原因**：
- 网络无法访问Google服务
- 浏览器阻止弹出窗口
- OAuth缓存过期

**解决**：
```bash
# 清除OAuth缓存
rm -f ~/.gemini/oauth_creds.json ~/.gemini/google_accounts.json

# 重新登录
gemini
```

如果还是不行，建议改用API Key方式。

### API Key无法连接？

**检查清单**：
1. Base URL是否正确
2. API Key是否有效
3. 网络是否正常
4. 认证方式是否设置为`gemini-api-key`

**测试连接**：
```bash
curl -I https://api.aicoding.sh
```

### 如何查看当前配置？

```bash
# 查看认证方式
cat ~/.gemini/settings.json | grep selectedType

# 查看API配置
cat ~/.gemini/.env

# 查看已登录账号
cat ~/.gemini/google_accounts.json
```

## 📚 学习资源

- **Gemini CLI官方文档**：https://github.com/google/generative-ai-cli
- **Google AI Studio**：https://aistudio.google.com
- **中转平台文档**：查看具体平台的帮助中心

## 🎬 总结

三种配置方式各有优劣：

✅ **OAuth登录**：适合重度个人用户，简单但需会员
✅ **中转平台**：适合国内用户，方便但依赖第三方
✅ **官方API**：适合企业开发，稳定但配置复杂

根据自己的使用场景和网络环境，选择最合适的方式即可。

如果你是新手，建议从**中转平台API Key**开始，配置简单且国内可用。

---

**关注我们，获取更多AI工具教程** 👇

*本文最后更新：2026年3月*
