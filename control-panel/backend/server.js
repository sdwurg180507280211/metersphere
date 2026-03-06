/**
 * MeterSphere 控制面板 - 主入口
 */
const express = require('express');
const path = require('path');
const config = require('./config');
const logger = require('./utils/logger');

// 导入路由
const serviceRoutes = require('./routes/services');
const buildRoutes = require('./routes/build');
const logRoutes = require('./routes/logs');

const app = express();

// 中间件
app.use(express.json());

// API 路由
app.use('/api/services', serviceRoutes);
app.use('/api/build', buildRoutes);
app.use('/api/logs', logRoutes);

// 静态文件 - 生产环境提供 React 构建产物
const publicPath = process.env.NODE_ENV === 'production' 
  ? path.join(__dirname, '../frontend/dist')
  : path.join(__dirname, '../frontend/dist');

app.use(express.static(publicPath));

// 健康检查
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// 所有其他请求返回前端应用
app.get('*', (req, res) => {
  res.sendFile(path.join(publicPath, 'index.html'));
});

// 错误处理中间件
app.use((err, req, res, next) => {
  console.error('Error:', err);
  logger.broadcast(`[错误] ${err.message}`, 'system');
  res.status(500).json({ 
    success: false, 
    error: process.env.NODE_ENV === 'production' 
      ? '服务器内部错误' 
      : err.message 
  });
});

// 优雅关闭
process.on('SIGTERM', async () => {
  console.log('收到 SIGTERM 信号，正在优雅关闭...');
  const processManager = require('./services/processManager');
  await processManager.stopAll();
  process.exit(0);
});

process.on('SIGINT', async () => {
  console.log('收到 SIGINT 信号，正在优雅关闭...');
  const processManager = require('./services/processManager');
  await processManager.stopAll();
  process.exit(0);
});

// 启动服务器
app.listen(config.port, () => {
  console.log(`控制面板运行在 http://localhost:${config.port}`);
  console.log(`项目根目录: ${config.projectRoot}`);
});

module.exports = app;
