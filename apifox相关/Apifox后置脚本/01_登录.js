pm.test('登录应返回200',function(){pm.response.to.have.status(200);});
if(pm.response.code===200){
    const data=pm.response.json();
    pm.test('登录响应包含身份',function(){pm.expect(data.token).to.be.a('string').and.not.empty;pm.expect(data.user.id).to.be.a('number');});
    // 切换账号后清掉旧报名ID；同一账号重登时保留，便于重启服务后继续查询。
    if(String(pm.environment.get('user_id'))!==String(data.user.id)){pm.environment.unset('registration_id');}
    pm.environment.set('token',data.token);
    pm.environment.set('user_id',String(data.user.id));
}else{
    pm.environment.unset('token');pm.environment.unset('user_id');pm.environment.unset('registration_id');
}
