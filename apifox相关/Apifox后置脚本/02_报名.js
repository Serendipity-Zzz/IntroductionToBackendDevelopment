pm.test('首次报名应返回201',function(){pm.response.to.have.status(201);});
if(pm.response.code===201){
    const data=pm.response.json();
    pm.test('报名属于当前账号和活动',function(){
        pm.expect(data.userId).to.eql(Number(pm.environment.get('user_id')));
        pm.expect(data.eventId).to.eql(Number(pm.environment.get('event_id')));
    });
    pm.environment.set('registration_id',String(data.id));
}
// 重复报名应另存409用例。失败响应不会覆盖已有报名ID。
