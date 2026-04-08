SET SESSION innodb_lock_wait_timeout = 7200;

ALTER TABLE `test_plan`
    ADD COLUMN `requirement_number` varchar(64) DEFAULT NULL COMMENT '需求编号' AFTER `node_path`;

CREATE UNIQUE INDEX `uk_requirement_number` ON `test_plan` (`requirement_number`);

SET SESSION innodb_lock_wait_timeout = DEFAULT;
