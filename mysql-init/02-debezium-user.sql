-- ./mysql-init/02-debezium-user.sql

-- 为 Debezium 创建一个专用的数据库用户
CREATE USER 'debezium'@'%' IDENTIFIED BY 'dbz_123456';

-- 授予必要的权限
GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT, LOCK TABLES, CREATE TEMPORARY TABLES ON *.* TO 'debezium'@'%';
-- LOCK TABLES 和 CREATE TEMPORARY TABLES 对于某些模式演变和快照可能需要

-- 如果你的 Outbox 表在 main_service_db 数据库中
GRANT ALL PRIVILEGES ON main_service_db.notification_outbox TO 'debezium'@'%';
-- 如果还需要监控其他表，也需要相应授权

FLUSH PRIVILEGES;