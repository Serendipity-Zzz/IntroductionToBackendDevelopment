-- 在自己的signup_demo库执行。先确认当前库，再逐句运行。
SELECT DATABASE();
SELECT * FROM registrations WHERE event_id=1;
-- 下面只清理活动1的报名，方便重新演示一个名额。
DELETE FROM registrations WHERE event_id=1;
SELECT COUNT(*) AS registered_count FROM registrations WHERE event_id=1;
