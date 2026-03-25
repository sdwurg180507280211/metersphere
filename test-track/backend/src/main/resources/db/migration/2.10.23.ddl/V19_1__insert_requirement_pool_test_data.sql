-- 插入需求池测试数据
INSERT INTO `requirement_pool` (`id`, `dmp_num`, `requirement_name`, `pool_status`, `system_name`, `req_manager_name`, `req_father_class`, `req_son_class`, `create_time`, `up_time`, `linked_plan_id`, `linked_plan_name`, `trace_id`) VALUES
('req_pool_001', 'DMP-2024-001', '用户登录功能优化', 'PENDING', '用户中心系统', '张三', '功能需求', '性能优化', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000, NULL, NULL, 'trace_001'),
('req_pool_002', 'DMP-2024-002', '订单支付流程改造', 'CREATED', '订单管理系统', '李四', '功能需求', '流程优化', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000, 'plan_001', '订单支付测试计划', 'trace_002'),
('req_pool_003', 'DMP-2024-003', '数据报表导出功能', 'PENDING', '数据分析系统', '王五', '功能需求', '新增功能', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000, NULL, NULL, 'trace_003'),
('req_pool_004', 'DMP-2024-004', '系统安全加固', 'CANCELLED', '安全管理系统', '赵六', '非功能需求', '安全性', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000, NULL, NULL, 'trace_004'),
('req_pool_005', 'DMP-2024-005', '移动端适配优化', 'CREATED', '用户中心系统', '张三', '功能需求', '界面优化', UNIX_TIMESTAMP(NOW()) * 1000, UNIX_TIMESTAMP(NOW()) * 1000, 'plan_002', '移动端测试计划', 'trace_005');
