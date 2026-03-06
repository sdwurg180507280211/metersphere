/**
 * 服务路由
 */
const express = require('express');
const router = express.Router();
const serviceController = require('../controllers/serviceController');

// 获取所有服务状态
router.get('/status', serviceController.getAllStatus);

// 批量操作
router.post('/start-all', serviceController.startAll);
router.post('/stop-all', serviceController.stopAll);

// 单个服务操作
router.get('/:id/status', serviceController.getStatus);
router.get('/:id/health', serviceController.healthCheck);
router.post('/:id/start', serviceController.start);
router.post('/:id/stop', serviceController.stop);
router.post('/:id/restart', serviceController.restart);

module.exports = router;
