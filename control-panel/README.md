# MeterSphere 控制面板

一个简单的网页控制面板，用于管理 MeterSphere 服务和构建前端模块。

## 快速开始

```bash
# 1. 安装依赖
cd control-panel
npm install

# 2. 启动控制面板
npm start

# 3. 打开浏览器访问
open http://localhost:3000
```

## 功能

- **服务管理**：一键启动/停止所有后端服务
- **前端构建**：点击按钮构建指定模块的前端并自动复制到后端资源目录

## 升级方案

如果需要更好的体验，可以考虑：

### 方案 A：Electron 桌面应用
```bash
npm install electron electron-builder
# 可打包成 .app，支持系统托盘、开机启动
```

### 方案 B：使用 OpenClaw
通过 WhatsApp/Telegram 远程控制服务
```bash
npm install -g openclaw
openclaw onboard
# 配置后可以手机发消息控制服务
```

### 方案 C：Alfred/Raycast Workflow
创建快捷键直接执行命令
