-- 分析统计模块菜单配置
-- 创建时间: 2026-02-09
-- 说明: 在 system_parameter 表中添加 analytics-stat 模块的启用配置，用于控制左侧菜单显示

SET SESSION innodb_lock_wait_timeout = 7200;

-- 插入分析统计模块配置
-- param_key: metersphere.module.analyticsStat (驼峰命名，与其他模块保持一致)
-- param_value: ENABLE (启用) / DISABLE (禁用)
-- type: text
-- sort: 1
INSERT INTO system_parameter (param_key, param_value, type, sort)
VALUES ('metersphere.module.analytics-stat', 'ENABLE', 'text', 1)
ON DUPLICATE KEY UPDATE param_value = 'ENABLE';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
