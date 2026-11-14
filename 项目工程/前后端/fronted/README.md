# 报名系统前端联调页

VS Code打开本目录，通过Live Server启动index.html，项目配置端口5500。config.js中的apiBase为http://127.0.0.1:8080/api，eventId为1。

IDEA打开相邻的../backend/pom.xml并启动Java后端。数据库准备和运行配置见backend/README.md。前端页面访问5500，API请求8080。改变前端端口时需要同时调整Java的CORS配置。

网页账号zhh、myh，密码demo123。网页与Apifox均使用活动1“后端开发体验课”，刷新活动或页面后可核对同一组数据。账号与报名保存到MySQL，Java重启后需要重新登录，业务记录保留。

API前端使用Java返回的登录凭证，不包含纯前端版的本地账号验证逻辑。前端界面风格一致，交互脚本的存储实现不同。
