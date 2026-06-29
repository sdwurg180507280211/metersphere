-- SQL 查询台只读账号初始化脚本
-- 由 DBA 或部署人员手动执行，不随 Flyway 自动执行。
-- 密码不要固化在仓库中，执行前请替换 <change-me>。

CREATE USER 'ms_sql_query_ro'@'%' IDENTIFIED BY '<change-me>';

GRANT SELECT, SHOW VIEW
ON `metersphere`.*
TO 'ms_sql_query_ro'@'%';

FLUSH PRIVILEGES;

