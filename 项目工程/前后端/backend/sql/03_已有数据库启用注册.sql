-- 仅升级已有课堂库。用MySQL管理员连接，先选中signup_demo或signup_lab。
-- 两个库需要分别执行；新建的库已自动使用自增ID，不必再执行。
-- 保留现有账号、ID、密码和报名记录，只给users.id增加AUTO_INCREMENT。
SELECT DATABASE();
SET @signup_previous_fk_checks = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE users MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
SET FOREIGN_KEY_CHECKS = @signup_previous_fk_checks;
SHOW COLUMNS FROM users LIKE 'id';
-- 若客户端在ALTER报错后中断，请重新连接，或先执行SET FOREIGN_KEY_CHECKS=1。
