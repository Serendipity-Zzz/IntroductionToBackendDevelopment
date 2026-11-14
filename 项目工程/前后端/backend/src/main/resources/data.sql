-- 密码均为demo123；数据库存储的是密码哈希。
INSERT IGNORE INTO users(id,username,display_name,password_hash) VALUES
(1,'alice','林同学','$2a$10$Kyre8Dvtf2rTQlK2e0mtpeL7ZOGVH8dGZOHwHGBjB2kpqyfHPQ5iS'),
(2,'bob','周同学','$2a$10$5eGVsNxOtMiuT/p1h5ToxeijPRH/IoPXvvgI4GPbri4a7Sz6j/iEG');
INSERT IGNORE INTO events(id,title,capacity) VALUES
(1,'后端开发体验课',1),
(10,'接口练习场',3);

-- 新增演示登录账号；保留原账号，数字ID由MySQL生成。密码均为demo123。
INSERT IGNORE INTO users(username,display_name,password_hash) VALUES
('zhh','zhh','$2a$10$Kyre8Dvtf2rTQlK2e0mtpeL7ZOGVH8dGZOHwHGBjB2kpqyfHPQ5iS'),
('myh','myh','$2a$10$5eGVsNxOtMiuT/p1h5ToxeijPRH/IoPXvvgI4GPbri4a7Sz6j/iEG');
