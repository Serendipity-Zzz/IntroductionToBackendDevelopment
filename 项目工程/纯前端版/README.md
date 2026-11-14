# 纯前端对照

VS Code打开本目录，用Live Server启动index.html，配置端口5501。演示账号zhh、myh，密码demo123，也可注册新账号。

新注册账号保存在当前来源的localStorage，包含随机盐和PBKDF2派生值。普通浏览器刷新或重启后可以再次读取。同一浏览器用户配置的同源普通页面共享localStorage，普通窗口与无痕会话隔离，Chrome与Edge分别保存。直接双击本地HTML时使用file协议，存储行为取决于浏览器，课堂使用Live Server的HTTP地址。

报名记录放在当前页面的JavaScript内存，刷新后清空。不同页面各自计算名额，两个页面可能都显示抢到最后一个名额。

本版与后端版使用相近的界面，app.js包含本地账号和报名实现。后端版则通过接口交由Java处理，不能只替换本版config.js就认为两套脚本完全相同。
