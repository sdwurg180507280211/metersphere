const express = require('express');
const { exec, spawn } = require('child_process');
const path = require('path');

const app = express();
const PORT = 3000;
const PROJECT_ROOT = path.join(__dirname, '..');

app.use(express.static('public'));
app.use(express.json());

const services = {
  'eureka': { name: 'Eureka', pom: 'framework/eureka/pom.xml', port: 8761 },
  'gateway': { name: 'Gateway', pom: 'framework/gateway/pom.xml', port: 8080 },
  'system-setting': { name: 'System Setting', pom: 'system-setting/backend/pom.xml', port: 8800 },
  'project-management': { name: 'Project Management', pom: 'project-management/backend/pom.xml', port: 8801 },
  'test-track': { name: 'Test Track', pom: 'test-track/backend/pom.xml', port: 8802 },
  'api-test': { name: 'API Test', pom: 'api-test/backend/pom.xml', port: 8803 },
  'performance-test': { name: 'Performance Test', pom: 'performance-test/backend/pom.xml', port: 8804 },
  'report-stat': { name: 'Report Stat', pom: 'report-stat/backend/pom.xml', port: 8805 },
  'workstation': { name: 'Workstation', pom: 'workstation/backend/pom.xml', port: 8806 },
  'workflow-service': { name: 'Workflow Service', pom: 'workflow-service/backend/pom.xml', port: 8807 },
  'analytics-stat': { name: 'Analytics Stat', pom: 'analytics-stat/backend/pom.xml', port: 8808 }
};

let logClients = [];

// SSE 日志流
app.get('/api/logs/stream', (req, res) => {
  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');

  logClients.push(res);

  req.on('close', () => {
    logClients = logClients.filter(client => client !== res);
  });
});

function sendLog(message, type = 'service') {
  logClients.forEach(client => {
    client.write(`data: ${JSON.stringify({ message, type })}\n\n`);
  });
}

// 获取所有服务状态
app.get('/api/services/status', (req, res) => {
  const cmd = 'ps aux | grep "spring-boot:run" | grep -v grep';
  exec(cmd, (error, stdout) => {
    const running = {};
    Object.keys(services).forEach(key => {
      running[key] = stdout.includes(services[key].pom);
    });
    res.json(running);
  });
});

// 启动服务
app.post('/api/services/:id/start', (req, res) => {
  const service = services[req.params.id];
  if (!service) return res.json({ success: false, error: '服务不存在' });

  sendLog(`\n========== 启动 ${service.name} ==========`);
  sendLog(`执行命令: ./mvnw spring-boot:run -f ${service.pom}`);

  const cmd = `./mvnw spring-boot:run -f ${service.pom}`;
  const child = spawn('sh', ['-c', cmd], { cwd: PROJECT_ROOT });

  child.stdout.on('data', (data) => {
    sendLog(data.toString());
  });

  child.stderr.on('data', (data) => {
    sendLog(data.toString());
  });

  child.on('close', (code) => {
    sendLog(`\n${service.name} 进程退出，代码: ${code}`);
  });

  child.unref();
  res.json({ success: true });
});

// 停止服务
app.post('/api/services/:id/stop', (req, res) => {
  const service = services[req.params.id];
  if (!service) return res.json({ success: false, error: '服务不存在' });

  const cmd = `pkill -f "${service.pom}"`;
  exec(cmd, () => res.json({ success: true }));
});

// 执行命令的辅助函数
function runCommand(command, res) {
  exec(command, { cwd: PROJECT_ROOT }, (error, stdout, stderr) => {
    if (error) {
      return res.json({ success: false, error: stderr || error.message });
    }
    res.json({ success: true, output: stdout });
  });
}

// 构建前端模块
app.post('/api/build-frontend', (req, res) => {
  const { module } = req.body;
  const modules = {
    'system-setting': 'system-setting',
    'project-management': 'project-management',
    'test-track': 'test-track',
    'api-test': 'api-test',
    'performance-test': 'performance-test',
    'report-stat': 'report-stat',
    'workstation': 'workstation',
    'analytics-stat': 'analytics-stat',
    'sdk-parent': 'framework/sdk-parent'
  };

  const modulePath = modules[module];
  if (!modulePath) {
    return res.json({ success: false, error: '未知模块' });
  }

  const isGateway = module === 'sdk-parent';
  const targetPath = isGateway ? '../../gateway/src/main/resources/static' : '../backend/src/main/resources/static';

  sendLog(`\n========== 构建 ${module} 前端 ==========`, 'build');
  sendLog(`cd ${modulePath}/frontend`, 'build');
  sendLog(`npm run build`, 'build');

  const buildCmd = `cd ${modulePath}/frontend && npm run build`;
  const child = spawn('sh', ['-c', buildCmd], { cwd: PROJECT_ROOT });

  child.stdout.on('data', (data) => sendLog(data.toString(), 'build'));
  child.stderr.on('data', (data) => sendLog(data.toString(), 'build'));

  child.on('close', (code) => {
    if (code === 0) {
      sendLog(`rm -rf ${targetPath}/*`, 'build');
      sendLog(`cp -r dist/* ${targetPath}/`, 'build');

      const copyCmd = `cd ${modulePath}/frontend && rm -rf ${targetPath}/* && cp -r dist/* ${targetPath}/`;
      exec(copyCmd, { cwd: PROJECT_ROOT }, (err) => {
        if (err) {
          sendLog(`\n✗ 复制文件失败: ${err.message}`, 'build');
        } else {
          sendLog(`cd /Users/edy/ideaProjects/metersphere`, 'build');
          sendLog(`\n✓ ${module} 构建完成`, 'build');
        }
      });
    } else {
      sendLog(`\n✗ ${module} 构建失败，退出码: ${code}`, 'build');
    }
  });

  res.json({ success: true });
});

app.listen(PORT, () => {
  console.log(`控制面板运行在 http://localhost:${PORT}`);
});
