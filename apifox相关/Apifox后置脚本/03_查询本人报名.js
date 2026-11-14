pm.test('查询应返回200',function(){pm.response.to.have.status(200);});
if(pm.response.code===200){
    const data=pm.response.json();
    pm.test('ID与报名响应一致',function(){pm.expect(data.id).to.eql(Number(pm.environment.get('registration_id')));});
    pm.test('记录属于当前账号',function(){pm.expect(data.userId).to.eql(Number(pm.environment.get('user_id')));});
}
