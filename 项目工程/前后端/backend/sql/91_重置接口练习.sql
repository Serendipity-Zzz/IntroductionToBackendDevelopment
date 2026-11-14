-- 在自己的signup_lab库执行。只清理活动10，重跑Apifox练习。
SELECT DATABASE();
SELECT * FROM registrations WHERE event_id=10;
DELETE FROM registrations WHERE event_id=10;
