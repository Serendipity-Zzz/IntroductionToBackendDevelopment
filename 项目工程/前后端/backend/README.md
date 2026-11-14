# 报名系统Java后端

在IDEA打开本目录pom.xml，使用JDK21，等待Maven同步完成。先双击material/快速启动/运行数据库准备.cmd，准备signup_demo、workshop账号、数据表和演示数据。

运行配置添加DB_USER=workshop、DB_PASSWORD=自己的workshop密码、DB_INIT=never，再运行SignupApplication。默认MySQL地址127.0.0.1:3306，后端端口8080，库名signup_demo。修改默认值时分别添加DB_HOST、DB_PORT、SERVER_PORT或DB_NAME。

前端位于相邻的../fronted文件夹，VS Code Live Server使用5500。后端也提供静态网页，可访问http://127.0.0.1:8080/。网页和Apifox统一操作活动1“后端开发体验课”。zhh、myh密码均为demo123，用户数字ID以本次登录响应为准。

已有接口含my-registration。课堂新增入口registration-status使用课堂文件中的RegistrationStatusController.错误.java.txt，复制为Java类后参与编译；正确答案也在该目录。默认源码没有这个新增入口。

```powershell
.\mvnw.cmd -DskipTests package
```

构建产物target/signup-classroom-1.0.0.jar。源码修改后重新构建、停止旧进程并启动新程序，重新登录获取token。sql目录保留手动建表、数据导入、旧库升级和重置示例；90_重置开场活动.sql会删除活动1的全部报名，执行前确认自己的课堂库。

health只检查Java服务响应。登录、查询活动和查回报名可以验证数据库业务链路。取消报名、调整总名额尚无HTTP接口，可以使用自己的数据库连接执行SQL。
