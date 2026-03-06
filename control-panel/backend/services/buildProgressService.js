/**
 * 构建进度追踪服务
 */
const cacheService = require('./cacheService');
const websocketService = require('./websocketService');
const { v4: uuidv4 } = require('uuid');

class BuildProgressService {
  constructor() {
    this.activeBuilds = new Map(); // buildId -> buildInfo
  }

  async startBuild(module) {
    const buildId = uuidv4();
    const buildInfo = {
      id: buildId,
      module,
      status: 'running',
      startTime: Date.now(),
      steps: [
        { name: '准备环境', status: 'pending', progress: 0 },
        { name: '安装依赖', status: 'pending', progress: 0 },
        { name: '编译构建', status: 'pending', progress: 0 },
        { name: '复制资源', status: 'pending', progress: 0 },
        { name: '重启服务', status: 'pending', progress: 0 }
      ],
      currentStep: 0,
      logs: [],
      error: null
    };

    this.activeBuilds.set(buildId, buildInfo);
    
    // 缓存构建信息
    await cacheService.set(`build:${buildId}`, buildInfo, 3600);
    
    // 添加到历史记录
    await cacheService.pushToList('build:history', {
      id: buildId,
      module,
      startTime: buildInfo.startTime,
      status: 'running'
    }, 50);

    return buildId;
  }

  async updateStep(buildId, stepIndex, status, progress, log = '') {
    const build = this.activeBuilds.get(buildId);
    if (!build) return;

    build.steps[stepIndex] = {
      ...build.steps[stepIndex],
      status,
      progress
    };
    build.currentStep = stepIndex;

    if (log) {
      build.logs.push({
        time: Date.now(),
        message: log
      });
    }

    // 广播进度
    websocketService.broadcastBuildProgress(buildId, {
      module: build.module,
      status: build.status,
      currentStep: stepIndex,
      totalSteps: build.steps.length,
      stepName: build.steps[stepIndex].name,
      stepProgress: progress,
      overallProgress: Math.round((stepIndex / build.steps.length) * 100 + (progress / build.steps.length)),
      log
    });

    // 更新缓存
    await cacheService.set(`build:${buildId}`, build, 3600);
  }

  async completeBuild(buildId, success, error = null) {
    const build = this.activeBuilds.get(buildId);
    if (!build) return;

    build.status = success ? 'success' : 'failed';
    build.endTime = Date.now();
    build.duration = build.endTime - build.startTime;
    build.error = error;

    // 更新所有步骤状态
    build.steps.forEach((step, idx) => {
      if (idx < build.currentStep) {
        step.status = 'completed';
        step.progress = 100;
      } else if (idx === build.currentStep) {
        step.status = success ? 'completed' : 'failed';
        step.progress = success ? 100 : step.progress;
      }
    });

    // 广播完成
    websocketService.broadcastBuildProgress(buildId, {
      module: build.module,
      status: build.status,
      currentStep: build.currentStep,
      totalSteps: build.steps.length,
      overallProgress: success ? 100 : build.steps.findIndex(s => s.status === 'failed') / build.steps.length * 100,
      duration: build.duration,
      error
    });

    // 更新缓存
    await cacheService.set(`build:${buildId}`, build, 3600);
    
    // 清理内存
    setTimeout(() => this.activeBuilds.delete(buildId), 300000); // 5分钟后清理

    return build;
  }

  async getBuild(buildId) {
    // 先从内存获取
    const build = this.activeBuilds.get(buildId);
    if (build) return build;

    // 从缓存获取
    return await cacheService.get(`build:${buildId}`);
  }

  async getRecentBuilds(limit = 10) {
    return await cacheService.getList('build:history', 0, limit - 1);
  }

  getActiveBuilds() {
    return Array.from(this.activeBuilds.values()).map(b => ({
      id: b.id,
      module: b.module,
      status: b.status,
      startTime: b.startTime,
      currentStep: b.currentStep,
      totalSteps: b.steps.length,
      overallProgress: Math.round((b.currentStep / b.steps.length) * 100)
    }));
  }

  async cancelBuild(buildId) {
    const build = this.activeBuilds.get(buildId);
    if (!build) return false;

    build.status = 'cancelled';
    build.endTime = Date.now();

    await cacheService.set(`build:${buildId}`, build, 3600);
    this.activeBuilds.delete(buildId);

    return true;
  }
}

module.exports = new BuildProgressService();
