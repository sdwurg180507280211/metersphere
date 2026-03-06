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

  const cmd = `./mvnw spring-boot:run -f ${service.pom}`;
  spawn('sh', ['-c', cmd], { cwd: PROJECT_ROOT, detached: true, stdio: 'ignore' }).unref();

  setTimeout(() => res.json({ success: true }), 1000);
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

  const command = `cd ${modulePath}/frontend && npm run build && rm -rf ${targetPath}/* && cp -r dist/* ${targetPath}/`;
  runCommand(command, res);
});

app.listen(PORT, () => {
  console.log(`控制面板运行在 http://localhost:${PORT}`);
});
