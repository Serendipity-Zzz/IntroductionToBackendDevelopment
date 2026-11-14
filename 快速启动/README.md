# 报名系统快速启动

1. 安装JDK21、MySQL，启动MySQL服务。
2. 完整解压material，打开快速启动文件夹，双击运行数据库准备.cmd。输入MySQL管理员密码和workshop数据库密码，看到“准备完成”后关闭窗口。已有workshop账号需要填原密码。
3. IDEA打开项目工程/前后端/backend/pom.xml，选JDK21，添加环境变量DB_USER=workshop、DB_PASSWORD=自己的workshop密码、DB_INIT=never，再运行SignupApplication，后端地址为http://127.0.0.1:8080。
4. VS Code打开项目工程/前后端/fronted，用Live Server启动5500，网页请求8080。也可以双击运行完整工程.cmd，使用附带JAR启动后端，再访问http://127.0.0.1:8080/。
5. 内存对照可双击运行内存后端.cmd，访问http://127.0.0.1:8082/。Java重启后，新账号和报名清空。

网页登录账号zhh、myh，密码demo123。数据库账号workshop使用自己设置的密码。

脚本按自身位置寻找../项目工程/前后端/backend/sql，移动材料时保留整个目录结构。在material目录也可以执行下面的命令。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ".\快速启动\课前准备数据库.ps1"
```

Apifox导入../apifox相关/基础接口.openapi.json，查询和报名都使用活动1，与网页共享数据。修改源码后需要重新构建JAR，快速启动中的现成JAR不会自动更新。
