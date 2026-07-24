CREATE TABLE IF NOT EXISTS `api_scenario_variable_override` (
  `id` varchar(50) NOT NULL COMMENT '副本ID',
  `scenario_id` varchar(50) NOT NULL COMMENT '场景ID',
  `variable_id` varchar(50) NOT NULL COMMENT '公共变量ID',
  `user_id` varchar(64) NOT NULL COMMENT '副本所属用户ID',
  `variable_json` longtext NOT NULL COMMENT '用户变量副本JSON',
  `create_time` bigint(13) NOT NULL COMMENT '创建时间',
  `update_time` bigint(13) NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scenario_variable_user` (`scenario_id`, `variable_id`, `user_id`),
  KEY `idx_scenario_user` (`scenario_id`, `user_id`),
  KEY `idx_scenario_variable` (`scenario_id`, `variable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='接口自动化场景变量用户副本';
