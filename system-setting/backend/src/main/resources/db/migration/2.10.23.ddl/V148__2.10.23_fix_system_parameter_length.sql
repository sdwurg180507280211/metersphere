SET SESSION innodb_lock_wait_timeout = 7200;

-- ============================================
-- 修复 system_parameter 表 param_value 字段长度限制
-- ============================================
-- 问题：param_value 字段为 varchar(255)，在 UTF-8 编码下只能存储约 85 个中文字符
-- 场景：公告设置（announcement.content）等长文本参数无法保存过长的中文内容
-- 解决方案：将 param_value 字段从 varchar(255) 改为 text 类型，支持最多 65535 字节（约 21845 个中文字符）

ALTER TABLE system_parameter 
MODIFY COLUMN param_value TEXT COMMENT '参数值';

-- 恢复默认锁等待时间
SET SESSION innodb_lock_wait_timeout = DEFAULT;
