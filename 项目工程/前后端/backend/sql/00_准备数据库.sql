-- 仅在自己的本机课堂MySQL中，用管理员连接执行。
-- 如果已经有同名库或账号，先核对用途，不要删除已有数据。
CREATE DATABASE IF NOT EXISTS signup_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS signup_lab CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 将密码占位符换成自己设置的课堂密码。
CREATE USER IF NOT EXISTS 'workshop'@'localhost' IDENTIFIED BY '替换成自己的课堂密码';
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,REFERENCES ON signup_demo.* TO 'workshop'@'localhost';
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,REFERENCES ON signup_lab.* TO 'workshop'@'localhost';

-- 本文件只准备库和权限。完整工程启动后自动建表；练习工程课上手动导入。
