/**
 * 构建控制器
 */
const processManager = require('../services/processManager');
const validator = require('../utils/validator');
const config = require('../config');
const logger = require('../utils/logger');

const buildController = {
  /**
   * 构建前端模块
   */
  async build(req, res) {
    try {
      const { module } = req.body;
      
      if (!validator.isValidModule(module)) {
        return res.status(400).json({ success: false, error: '未知的模块' });
      }

      const modulePath = validator.getValidModulePath(module);
      
      // 先启动构建任务获取 buildId
      const buildId = await processManager.initBuild(module, modulePath);
      
      // 立即返回，构建在后台进行
      res.json({ success: true, message: '构建任务已开始', buildId });

      // 在后台执行构建（不阻塞响应）
      processManager.executeBuild(module, modulePath, buildId).then(async (result) => {
        if (result.success) {
          // 构建成功，重启对应服务
          const isGateway = module === 'sdk-parent';
          const serviceId = isGateway ? 'gateway' : module;
          const service = config.services[serviceId];

          if (service) {
            logger.broadcast(`\n========== 重启 ${service.name} 服务 ==========`, 'build');
            await processManager.restart(serviceId, service, 2000);
          }
        }
      }).catch(error => {
        logger.broadcast(`构建失败: ${error.message}`, 'build');
      });
    } catch (error) {
      logger.broadcast(`构建失败: ${error.message}`, 'build');
      res.status(500).json({ success: false, error: error.message });
    }
  },

  /**
   * 批量构建多个模块
   */
  async buildBatch(req, res) {
    try {
      const { modules } = req.body;
      
      if (!Array.isArray(modules) || modules.length === 0) {
        return res.status(400).json({ success: false, error: '请提供模块列表' });
      }

      const invalidModules = modules.filter(m => !validator.isValidModule(m));
      if (invalidModules.length > 0) {
        return res.status(400).json({ 
          success: false, 
          error: `无效的模块: ${invalidModules.join(', ')}` 
        });
      }

      res.json({ success: true, message: '批量构建任务已开始', modules });

      // 串行执行构建
      for (const module of modules) {
        const modulePath = validator.getValidModulePath(module);
        await processManager.buildFrontend(module, modulePath);
      }
      
      logger.broadcast('\n========== 所有模块构建完成 ==========', 'build');
    } catch (error) {
      logger.broadcast(`批量构建失败: ${error.message}`, 'build');
    }
  },

  /**
   * 获取可构建的模块列表
   */
  getModules(req, res) {
    const modules = [
      { id: 'system-setting', name: 'System Setting', path: 'system-setting' },
      { id: 'project-management', name: 'Project Management', path: 'project-management' },
      { id: 'test-track', name: 'Test Track', path: 'test-track' },
      { id: 'api-test', name: 'API Test', path: 'api-test' },
      { id: 'performance-test', name: 'Performance Test', path: 'performance-test' },
      { id: 'report-stat', name: 'Report Stat', path: 'report-stat' },
      { id: 'workstation', name: 'Workstation', path: 'workstation' },
      { id: 'analytics-stat', name: 'Analytics Stat', path: 'analytics-stat' },
      { id: 'sdk-parent', name: 'SDK Parent (Gateway)', path: 'framework/sdk-parent' }
    ];
    res.json({ success: true, data: modules });
  }
};

module.exports = buildController;
