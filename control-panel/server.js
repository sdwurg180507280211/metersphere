const express = require('express');
const { exec, spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = 3000;
const PROJECT_ROOT = path.join(__dirname, '..');
const PID_DIR = path.join(__dirname, '.pids');

// 确保 PID 目录存在
if (!fs.existsSync(PID_DIR)) {
  fs.mkdirSync(PID_DIR);
}

// 追踪服务进程
const serviceProcesses = {};

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

  // 保存进程 PID
  serviceProcesses[req.params.id] = child.pid;
  const pidFile = path.join(PID_DIR, `${req.params.id}.pid`);
  fs.writeFileSync(pidFile, child.pid.toString());

  child.stdout.on('data', (data) => {
    sendLog(data.toString());
  });

  child.stderr.on('data', (data) => {
    sendLog(data.toString());
  });

  child.on('close', (code) => {
    sendLog(`\n${service.name} 进程退出，代码: ${code}`);
    delete serviceProcesses[req.params.id];
    if (fs.existsSync(pidFile)) {
      fs.unlinkSync(pidFile);
    }
  });

  res.json({ success: true });
});

// 停止服务
app.post('/api/services/:id/stop', (req, res) => {
  const service = services[req.params.id];
  if (!service) return res.json({ success: false, error: '服务不存在' });

  const pidFile = path.join(PID_DIR, `${req.params.id}.pid`);

  // 优先使用内存中的 PID
  if (serviceProcesses[req.params.id]) {
    try {
      process.kill(serviceProcesses[req.params.id], 'SIGTERM');
      delete serviceProcesses[req.params.id];
      if (fs.existsSync(pidFile)) {
        fs.unlinkSync(pidFile);
      }
      return res.json({ success: true });
    } catch (e) {
      // 进程不存在，继续尝试从文件读取
    }
  }

  // 从 PID 文件读取
  if (fs.existsSync(pidFile)) {
    const pid = parseInt(fs.readFileSync(pidFile, 'utf8'));
    try {
      process.kill(pid, 'SIGTERM');
      fs.unlinkSync(pidFile);
      return res.json({ success: true });
    } catch (e) {
      fs.unlinkSync(pidFile);
      return res.json({ success: false, error: '进程不存在' });
    }
  }

  res.json({ success: false, error: '服务未运行' });
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
          sendLog(`\n✓ ${module} 构建完成`, 'build');

          // 重启对应的后端服务
          const serviceId = isGateway ? 'gateway' : module;
          const service = services[serviceId];

          if (service) {
            sendLog(`\n========== 重启 ${service.name} 服务 ==========`, 'build');

            // 停止服务
            const pidFile = path.join(PID_DIR, `${serviceId}.pid`);
            let stopped = false;

            // 优先使用内存中的 PID
            if (serviceProcesses[serviceId]) {
              try {
                process.kill(serviceProcesses[serviceId], 'SIGTERM');
                delete serviceProcesses[serviceId];
                if (fs.existsSync(pidFile)) {
                  fs.unlinkSync(pidFile);
                }
                stopped = true;
              } catch (e) {
                // 进程不存在
              }
            }

            // 从 PID 文件读取
            if (!stopped && fs.existsSync(pidFile)) {
              const pid = parseInt(fs.readFileSync(pidFile, 'utf8'));
              try {
                process.kill(pid, 'SIGTERM');
                fs.unlinkSync(pidFile);
                stopped = true;
              } catch (e) {
                fs.unlinkSync(pidFile);
              }
            }

            if (stopped) {
              sendLog(`已停止 ${service.name}`, 'build');
            }

            // 等待 2 秒后启动
            setTimeout(() => {
              sendLog(`启动 ${service.name}...`, 'build');
              const startCmd = `./mvnw spring-boot:run -f ${service.pom}`;
              const restartChild = spawn('sh', ['-c', startCmd], { cwd: PROJECT_ROOT });

              // 保存进程 PID
              serviceProcesses[serviceId] = restartChild.pid;
              const newPidFile = path.join(PID_DIR, `${serviceId}.pid`);
              fs.writeFileSync(newPidFile, restartChild.pid.toString());

              restartChild.stdout.on('data', (data) => sendLog(data.toString()));
              restartChild.stderr.on('data', (data) => sendLog(data.toString()));
              restartChild.on('close', (code) => {
                sendLog(`\n${service.name} 进程退出，代码: ${code}`);
                delete serviceProcesses[serviceId];
                if (fs.existsSync(newPidFile)) {
                  fs.unlinkSync(newPidFile);
                }
              });
            }, 2000);
          }
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
