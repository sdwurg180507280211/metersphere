-- AI工作台模块菜单配置
-- 创建时间: 2026-02-09
-- 更新时间: 2026-05-29 (模块标识从 analytics 改为 ai)
-- 说明: 在 system_parameter 表中添加 AI 工作台模块的启用配置，用于控制左侧菜单显示

SET SESSION innodb_lock_wait_timeout = 7200;

-- 插入 AI 工作台模块配置
-- param_key: metersphere.module.ai
-- param_value: ENABLE (启用) / DISABLE (禁用)
-- type: text
-- sort: 1
INSERT INTO system_parameter (param_key, param_value, type, sort)
VALUES ('metersphere.module.ai', 'ENABLE', 'text', 1)
ON DUPLICATE KEY UPDATE param_value = 'ENABLE';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
