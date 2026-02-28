# 小白也能用的 AI 编程终端：10 分钟上手 OpenCode（含 Chrome DevTools）

想用 AI 帮你写代码、查问题、改脚本，但又不想折腾一堆复杂配置？
这篇直接带你上手一个 AI 编程终端工具 OpenCode：不讲虚的，只讲怎么安装、怎么配置、怎么跑起来。

官方文档：
https://opencode.ai/docs/zh-cn

## 一、先说结论：你将完成什么？

看完这篇，你可以做到：

- 安装并启动 OpenCode
- 配置自己的模型提供商（OpenAI 兼容接口）
- 把 opencode 加入系统 PATH（终端直接调用）
- 接入 chrome-devtools 工具，增强网页调试能力

## 二、初始化配置（新手直接复制）

安装 OpenCode 最简单的方法是通过安装脚本。
curl -fsSL https://opencode.ai/install | bash

注意：下面示例中的 apiKey 请替换成你自己的，不要把真实 Key 发到公开平台。

```bash
bash << 'SETUP_SCRIPT'
mkdir -p ~/.config/opencode
[ -f ~/.config/opencode/opencode.json ] && cp ~/.config/opencode/opencode.json ~/.config/opencode/opencode.json.bak

cat > ~/.config/opencode/opencode.json << 'OPENCODE_CFG'
{
  "provider": {
    "openai": {
      "options": {
        "baseURL": "https://your-openai-compatible-endpoint/v1",
        "apiKey": "你的apikey"
      },
      "models": {
        "gpt-5.3-codex-spark": {
          "name": "GPT-5.3 Codex Spark",
          "options": {
            "store": false
          },
          "variants": {
            "low": {},
            "medium": {},
            "high": {},
            "xhigh": {}
          }
        },
        "gpt-5.2-codex": {
          "name": "GPT-5.2 Codex",
          "options": {
            "store": false
          },
          "variants": {
            "low": {},
            "medium": {},
            "high": {},
            "xhigh": {}
          }
        }
      }
    }
  },
  "agent": {
    "build": {
      "options": {
        "store": false
      }
    },
    "plan": {
      "options": {
        "store": false
      }
    }
  },
  "$schema": "https://opencode.ai/config.json"
}
OPENCODE_CFG

echo "Done!"
SETUP_SCRIPT
```

## 三、启动 OpenCode（首次必做）

```bash
~/.opencode/bin/opencode
```

把命令加入 PATH，后续就可以直接输入 opencode：

```bash
echo 'export PATH="$HOME/.opencode/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
opencode
```

## 四、接入 Chrome DevTools 工具（进阶实用）

如果你希望 OpenCode 具备浏览器调试能力，可以在配置中增加以下 mcp 配置：

```json
"mcp": {
  "chrome-devtools": {
    "type": "local",
    "command": [
      "/Users/yourname/.nvm/versions/node/v20.19.6/bin/npx",
      "-y",
      "chrome-devtools-mcp@latest",
      "--autoConnect",
      "--channel=stable"
    ],
    "environment": {
      "PATH": "/Users/yourname/.nvm/versions/node/v20.19.6/bin:/usr/local/bin:/usr/bin:/bin",
      "NODE_PATH": "/Users/yourname/.nvm/versions/node/v20.19.6/lib/node_modules"
    },
    "enabled": true
  }
}
```

## 五、常见坑位提醒（帮你省时间）

- apiKey 无效：优先检查 Key 是否正确、是否有额度
- baseURL 报错：确认是完整的 OpenAI 兼容地址，并带 /v1
- opencode: command not found：通常是 PATH 没生效，重新执行 source ~/.zshrc
- npx 找不到：先确认 Node.js / npm 是否安装成功

## 六、写在最后

如果你是第一次接触 AI 终端，不用担心。
按这篇一步步来，先跑通，再慢慢加工具链，你会很快进入效率状态。
