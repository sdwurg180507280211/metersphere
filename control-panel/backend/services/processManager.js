/**
 * 进程管理服务
 * 统一管理服务的启动、停止和状态追踪
 */
const { spawn, exec } = require('child_process');
const fs = require('fs');
const path = require('path');
const config = require('../config');
const logger = require('../utils/logger');

const PID_DIR = path.join(__dirname, '../../.pids');
const LOG_DIR = path.join(__dirname, '../../logs');

// 确保 PID 目录存在
if (!fs.existsSync(PID_DIR)) {
  fs.mkdirSync(PID_DIR, { recursive: true });
}

// 内存中的进程映射
const serviceProcesses = {};

class ProcessManager {
  constructor() {
    this.pidDir = PID_DIR;
    this.projectRoot = config.projectRoot;
  }

  /**
   * 获取 PID 文件路径
   */
  _getPidFile(serviceId) {
    return path.join(this.pidDir, `${serviceId}.pid`);
  }

  /**
   * 保存进程 ID
   */
  _savePid(serviceId, pid) {
    serviceProcesses[serviceId] = pid;
    fs.writeFileSync(this._getPidFile(serviceId), pid.toString());
  }

  /**
   * 清除进程 ID
   */
  _clearPid(serviceId) {
    delete serviceProcesses[serviceId];
    const pidFile = this._getPidFile(serviceId);
    if (fs.existsSync(pidFile)) {
      fs.unlinkSync(pidFile);
    }
  }

  /**
   * 获取进程 ID
   */
  _getPid(serviceId) {
    // 优先从内存获取
    if (serviceProcesses[serviceId]) {
      return serviceProcesses[serviceId];
    }
    // 从文件读取
    const pidFile = this._getPidFile(serviceId);
    if (fs.existsSync(pidFile)) {
      return parseInt(fs.readFileSync(pidFile, 'utf8'), 10);
    }
    return null;
  }

  /**
   * 检查进程是否运行中
   */
  _isProcessRunning(pid) {
    try {
      process.kill(pid, 0);
      return true;
    } catch (e) {
      return false;
    }
  }

  /**
   * 启动服务
   */
  async start(serviceId, serviceConfig) {
    const { pom, name } = serviceConfig;

    logger.broadcast(`\n========== 启动 ${name} ==========`, 'service');
    logger.broadcast(`执行命令: ./mvnw spring-boot:run -f ${pom}`, 'service');

    const cmd = `./mvnw spring-boot:run -f ${pom}`;
    const child = spawn('sh', ['-c', cmd], { 
      cwd: this.projectRoot,
      detached: false
    });

    // 保存进程 ID
    this._savePid(serviceId, child.pid);

    // 处理输出
    child.stdout.on('data', (data) => {
      logger.broadcast(data.toString(), 'service');
    });

    child.stderr.on('data', (data) => {
      logger.broadcast(data.toString(), 'service');
    });

    child.on('close', (code) => {
      logger.broadcast(`\n${name} 进程退出，代码: ${code}`, 'service');
      this._clearPid(serviceId);
    });

    child.on('error', (err) => {
      logger.broadcast(`\n${name} 进程错误: ${err.message}`, 'service');
      this._clearPid(serviceId);
    });

    return { pid: child.pid };
  }

  /**
   * 停止服务
   */
  async stop(serviceId, serviceConfig) {
    const { pom, name } = serviceConfig;
    const pid = this._getPid(serviceId);

    // 优先使用内存中的 PID
    if (pid && this._isProcessRunning(pid)) {
      try {
        process.kill(pid, 'SIGTERM');
        this._clearPid(serviceId);
        logger.broadcast(`${name} 已停止 (PID: ${pid})`, 'service');
        return { success: true, method: 'pid' };
      } catch (e) {
        // 进程不存在，继续尝试其他方式
      }
    }

    // 清除可能遗留的 PID 文件
    this._clearPid(serviceId);

    // 回退方案：通过进程名称停止
    return new Promise((resolve) => {
      const cmd = `pkill -f "${pom}"`;
      exec(cmd, (error) => {
        if (error) {
          resolve({ success: false, error: '服务未运行或停止失败' });
        } else {
          logger.broadcast(`${name} 已通过进程名停止`, 'service');
          resolve({ success: true, method: 'pkill' });
        }
      });
    });
  }

  /**
   * 重启服务
   */
  async restart(serviceId, serviceConfig, delay = 2000) {
    await this.stop(serviceId, serviceConfig);
    
    return new Promise((resolve) => {
      setTimeout(() => {
        this.start(serviceId, serviceConfig)
          .then(result => resolve({ ...result, restarted: true }))
          .catch(err => resolve({ success: false, error: err.message }));
      }, delay);
    });
  }

  /**
   * 获取所有服务状态
   */
  async getAllStatus() {
    return new Promise((resolve) => {
      const cmd = 'ps aux | grep "spring-boot:run" | grep -v grep';
      exec(cmd, (error, stdout) => {
        const running = {};
        Object.entries(config.services).forEach(([key, svc]) => {
          running[key] = stdout.includes(svc.pom);
        });
        resolve(running);
      });
    });
  }

  /**
   * 获取单个服务状态
   */
  async getStatus(serviceId) {
    const pid = this._getPid(serviceId);
    if (pid) {
      return { running: this._isProcessRunning(pid), pid };
    }
    
    // 通过 ps 命令检查
    const { pom } = config.services[serviceId];
    return new Promise((resolve) => {
      const cmd = `ps aux | grep "${pom}" | grep -v grep`;
      exec(cmd, (error, stdout) => {
        resolve({ running: stdout.length > 0, pid: null });
      });
    });
  }

  /**
   * 停止所有服务
   */
  async stopAll() {
    const results = [];
    for (const [serviceId, serviceConfig] of Object.entries(config.services)) {
      const result = await this.stop(serviceId, serviceConfig);
      results.push({ serviceId, ...result });
    }
    return results;
  }

  /**
   * 按依赖顺序启动所有服务
   */
  async startAll() {
    // 按 startOrder 排序
    const sortedServices = Object.entries(config.services)
      .sort((a, b) => (a[1].startOrder || 99) - (b[1].startOrder || 99));

    const results = [];
    for (const [serviceId, serviceConfig] of sortedServices) {
      const status = await this.getStatus(serviceId);
      if (!status.running) {
        const result = await this.start(serviceId, serviceConfig);
        results.push({ serviceId, ...result });
        // 等待服务启动
        await new Promise(r => setTimeout(r, 3000));
      }
    }
    return results;
  }

  /**
   * 构建前端模块
   */
  async buildFrontend(module, modulePath) {
    const isGateway = module === 'sdk-parent';
    const targetPath = isGateway 
      ? '../../gateway/src/main/resources/static' 
      : '../backend/src/main/resources/static';

    logger.broadcast(`\n========== 构建 ${module} 前端 ==========`, 'build');
    logger.broadcast(`cd ${modulePath}/frontend`, 'build');
    logger.broadcast(`npm run build`, 'build');

    return new Promise((resolve) => {
      const buildCmd = `cd ${modulePath}/frontend && npm run build`;
      const child = spawn('sh', ['-c', buildCmd], { cwd: this.projectRoot });

      let output = '';
      let errorOutput = '';

      child.stdout.on('data', (data) => {
        const str = data.toString();
        output += str;
        logger.broadcast(str, 'build');
      });

      child.stderr.on('data', (data) => {
        const str = data.toString();
        errorOutput += str;
        logger.broadcast(str, 'build');
      });

      child.on('close', (code) => {
        if (code === 0) {
          logger.broadcast(`\n构建成功，复制文件到 ${targetPath}`, 'build');
          this._copyBuildFiles(modulePath, targetPath)
            .then(() => resolve({ success: true, output }))
            .catch(err => resolve({ success: false, error: err.message }));
        } else {
          resolve({ success: false, error: errorOutput || '构建失败', code });
        }
      });
    });
  }

  /**
   * 复制构建文件
   */
  async _copyBuildFiles(modulePath, targetPath) {
    return new Promise((resolve, reject) => {
      const copyCmd = `cd ${modulePath}/frontend && rm -rf ${targetPath}/* && cp -r dist/* ${targetPath}/`;
      exec(copyCmd, { cwd: this.projectRoot }, (err) => {
        if (err) {
          reject(new Error(`复制文件失败: ${err.message}`));
        } else {
          resolve();
        }
      });
    });
  }
}

module.exports = new ProcessManager();
