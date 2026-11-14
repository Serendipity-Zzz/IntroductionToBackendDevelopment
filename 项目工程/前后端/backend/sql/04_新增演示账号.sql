-- 在IDEA中选中要使用的课堂库，再执行全部语句。
-- 新库先执行01_schema.sql和02_seed.sql；旧库如users.id未自增，先用管理员执行03脚本。
-- 本脚本只新增缺少的账号，不删除旧账号，不改已有账号密码或报名。
SELECT DATABASE();
-- 新增演示登录账号；保留原账号，数字ID由MySQL生成。密码均为demo123。
INSERT IGNORE INTO users(username,display_name,password_hash) VALUES
('zhh','zhh','$2a$10$Kyre8Dvtf2rTQlK2e0mtpeL7ZOGVH8dGZOHwHGBjB2kpqyfHPQ5iS'),
('myh','myh','$2a$10$5eGVsNxOtMiuT/p1h5ToxeijPRH/IoPXvvgI4GPbri4a7Sz6j/iEG');
SELECT id, username, display_name FROM users WHERE username IN ('zhh','myh') ORDER BY id;
