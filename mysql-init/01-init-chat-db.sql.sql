
-- ./mysql-init/01-init-chat-db.sql

-- 创建 chat_service 的数据库
CREATE DATABASE IF NOT EXISTS chat_service_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 授予 'admin' 用户对新数据库的所有权限
-- (如果 'admin'@'%' 已有全局权限，此步可能非必需，但明确授权更安全)
GRANT ALL PRIVILEGES ON chat_service_db.* TO 'admin'@'%';

-- 刷新权限使之生效
FLUSH PRIVILEGES;