(() => {
  'use strict';
  const config = window.APP_CONFIG;
  const root = document.getElementById('app');
  const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'}[c]));
  const emptyForm = () => ({displayName:'', username:'', password:'', confirm:''});
  const state = {user:null, event:null, items:[], tab:'events', authMode:'login', form:emptyForm(), notice:'', error:false, busy:false, reveal:false};
  const accountKey = 'signup-classroom-accounts-v1';
  const iterations = 120000;
  function failure(status, code, message) { return Object.assign(new Error(message), {status, code}); }

  function apiClient() {
    let token = sessionStorage.getItem('signup-classroom-token') || '';
    async function call(path, method='GET', body) {
      const headers = {};
      if (token) headers.Authorization = `Bearer ${token}`;
      if (body !== undefined) headers['Content-Type'] = 'application/json';
      const response = await fetch(config.apiBase + path, {method, headers, body:body === undefined ? undefined : JSON.stringify(body)});
      if (response.status === 204) return null;
      const data = await response.json();
      if (!response.ok) throw failure(response.status, data.code, data.message || '请求失败，请稍后重试');
      return data;
    }
    return {
      hasSession:() => Boolean(token),
      createAccount:body => call('/auth/register', 'POST', body),
      login:async (username,password) => {
        const data = await call('/auth/login','POST',{username,password});
        token=data.token; sessionStorage.setItem('signup-classroom-token',token); return data.user;
      },
      me:() => call('/me'),
      event:() => call(`/events/${config.eventId}`),
      mine:async () => (await call('/me/registrations')).items,
      register:() => call(`/events/${config.eventId}/registrations`,'POST',{}),
      logout:async () => { try { if(token) await call('/auth/logout','POST'); } finally {token=''; sessionStorage.removeItem('signup-classroom-token');} }
    };
  }

  function memoryClient() {
    const demos=[{id:1,username:'alice',displayName:'林同学'},{id:2,username:'bob',displayName:'周同学'},{id:3,username:'zhh',displayName:'zhh'},{id:4,username:'myh',displayName:'myh'}];
    let current=null,records=[],nextId=1;
    const event={id:1,title:'后端开发体验课',capacity:1};
    const publicUser=u => ({id:u.id,username:u.username,displayName:u.displayName});
    const hex=bytes => Array.from(bytes,b=>b.toString(16).padStart(2,'0')).join('');
    function accounts() {
      try {
        const raw=localStorage.getItem(accountKey);
        if(!raw)return [];
        const data=JSON.parse(raw);
        if(data.version!==1 || !Array.isArray(data.accounts) || data.accounts.some(u=>!Number.isSafeInteger(u.id) || u.id<3 || !/^[a-z][a-z0-9_]{2,31}$/.test(u.username) || typeof u.displayName!=='string' || !/^[a-f0-9]{32}$/.test(u.salt) || !/^[a-f0-9]{64}$/.test(u.passwordHash)))throw new Error('invalid local accounts');
        return data.accounts;
      } catch {throw failure(503,'LOCAL_ACCOUNTS_UNAVAILABLE','无法读取当前浏览器的账号，请检查本地存储权限');}
    }
    function demoUsers(stored) {
      const used=new Set([1,2,...stored.map(u=>u.id)]);
      return demos.map(u=>{
        if(u.id<=2)return u;
        let id=3;while(used.has(id))id++;
        used.add(id);return {...u,id};
      });
    }
    async function derive(password,salt) {
      if(!globalThis.crypto?.subtle)throw failure(503,'CRYPTO_UNAVAILABLE','当前浏览器不支持账号加密，请使用新版Edge或Chrome');
      const key=await crypto.subtle.importKey('raw',new TextEncoder().encode(password),'PBKDF2',false,['deriveBits']);
      const bits=await crypto.subtle.deriveBits({name:'PBKDF2',salt:new Uint8Array(salt.match(/../g).map(x=>parseInt(x,16))),iterations,hash:'SHA-256'},key,256);
      return hex(new Uint8Array(bits));
    }
    return {
      hasSession:() => false,
      createAccount:async body => {
        const salt=hex(crypto.getRandomValues(new Uint8Array(16)));
        const passwordHash=await derive(body.password,salt);
        const stored=accounts();
        if([...demos,...stored].some(u=>u.username===body.username))throw failure(409,'USERNAME_TAKEN','这个账号已有人使用，请换一个账号');
        const user={id:Math.max(2,...stored.map(u=>u.id),...demoUsers(stored).map(u=>u.id))+1,username:body.username,displayName:body.displayName,salt,passwordHash};
        try {localStorage.setItem(accountKey,JSON.stringify({version:1,accounts:[...stored,user]}));}
        catch {throw failure(503,'LOCAL_ACCOUNTS_UNAVAILABLE','当前浏览器无法保存新账号，请允许本地存储后重试');}
        return publicUser(user);
      },
      login:async (username,password) => {
        username=username.trim().toLowerCase();
        const stored=accounts();
        const demo=demoUsers(stored).find(u=>u.username===username);
        const user=demo || stored.find(u=>u.username===username);
        const valid=user && (demo ? password==='demo123' : await derive(password,user.salt)===user.passwordHash);
        if(!valid){current=null;throw failure(401,'UNAUTHENTICATED','账号或密码不正确');}
        current=publicUser(user); return {...current};
      },
      me:async () => current,
      event:async () => ({...event,registeredCount:records.length,remainingSeats:Math.max(0,1-records.length),myRegistrationId:records.find(r=>r.userId===current.id)?.id ?? null}),
      mine:async () => records.filter(r=>r.userId===current.id).map(r=>({...r})),
      register:async () => {
        if(records.some(r=>r.userId===current.id))throw failure(409,'ALREADY_REGISTERED','你已经报名了这场活动');
        if(records.length>=1)throw failure(409,'EVENT_FULL','名额已满，请关注下一场活动');
        const row={id:nextId++,eventId:1,eventTitle:event.title,userId:current.id,username:current.username,displayName:current.displayName,createdAt:new Date().toISOString()};
        records.push(row);return {...row};
      },
      logout:async () => {current=null;}
    };
  }

  const client=config.mode==='memory' ? memoryClient() : apiClient();
  function noticeHtml() {return state.notice ? `<div class="notice ${state.error?'error':''}" role="${state.error?'alert':'status'}" tabindex="-1">${esc(state.notice)}</div>` : '';}
  function captureForm(form) { for(const name of Object.keys(state.form))if(form.elements[name])state.form[name]=form.elements[name].value; }
  function registrationError() {
    const f=state.form;
    if(!f.displayName.trim() || f.displayName.trim().length>32)return '请填写称呼，最多32个字符';
    if(!/^[a-z][a-z0-9_]{2,31}$/.test(f.username.trim().toLowerCase()))return '账号须为3至32位字母、数字或下划线，以字母开头';
    if(!f.password.trim() || f.password.length<8 || f.password.length>64)return '密码须为8至64个字符';
    if(new TextEncoder().encode(f.password).length>72)return '密码的UTF-8长度不能超过72字节，请缩短密码';
    if(f.password!==f.confirm)return '两次密码不一致，请重新确认';
    return '';
  }
  function switchAuth(mode) {
    if(state.busy)return;
    const username=state.form.username;
    state.authMode=mode;state.form={...emptyForm(),username};state.reveal=false;state.notice='';state.error=false;renderAuth();
    root.querySelector(`[data-auth="${mode}"]`).focus();
  }
  function renderAuth() {
    const signup=state.authMode==='signup',f=state.form;
    root.innerHTML=`<div class="login-layout"><section class="intro"><h1>后端开发<br>体验课</h1><p>认识一个报名按钮背后的流程。<br>注册或登录，查看活动与报名结果。</p><div class="intro-art" aria-hidden="true"><div class="art-top"><span>本期活动</span><span>开放报名</span></div><div class="art-visual"><div class="art-title">后端开发<br>体验课</div><div class="art-seat"><strong>1</strong><span>个报名名额</span></div></div></div></section><section class="auth-surface"><nav class="auth-tabs" role="tablist" aria-label="账号入口"><button type="button" role="tab" id="login-tab" data-auth="login" aria-selected="${!signup}" aria-controls="auth-panel" tabindex="${signup?-1:0}">登录</button><button type="button" role="tab" id="signup-tab" data-auth="signup" aria-selected="${signup}" aria-controls="auth-panel" tabindex="${signup?0:-1}">注册账号</button></nav><div id="auth-panel" role="tabpanel" aria-labelledby="${signup?'signup':'login'}-tab"><h2>${signup?'创建你的课堂账号':'欢迎回来'}</h2><p class="lead">${signup?'填好信息，注册后即可登录报名。':'登录后查看活动名额和自己的报名。'}</p><form id="${signup?'signup':'login'}-form">${signup?`<label for="displayName">怎么称呼你</label><input id="displayName" name="displayName" autocomplete="nickname" maxlength="32" required placeholder="例如：陈同学" value="${esc(f.displayName)}">`:''}<label for="username">账号</label><input id="username" name="username" autocomplete="username" autocapitalize="none" spellcheck="false" maxlength="32" ${signup?'minlength="3" pattern="[A-Za-z][A-Za-z0-9_]{2,31}" aria-describedby="username-hint"':''} required placeholder="${signup?'给自己起一个英文账号':'输入账号'}" value="${esc(f.username)}">${signup?'<p class="field-hint" id="username-hint">3至32位字母、数字或下划线，以字母开头。</p>':''}<label for="password">密码</label><div class="password-field"><input id="password" name="password" type="${state.reveal?'text':'password'}" autocomplete="${signup?'new-password':'current-password'}" maxlength="64" ${signup?'minlength="8" aria-describedby="password-hint"':''} required placeholder="${signup?'设置至少8位密码':'输入密码'}" value="${esc(f.password)}"><button type="button" class="password-toggle" aria-label="${state.reveal?'隐藏':'显示'}密码" aria-pressed="${state.reveal}">${state.reveal?'隐藏':'显示'}</button></div>${signup?`<p class="field-hint" id="password-hint">8至64个字符，中文等多字节字符请勿过长。</p><label for="confirm">确认密码</label><input id="confirm" name="confirm" type="${state.reveal?'text':'password'}" autocomplete="new-password" maxlength="64" minlength="8" required placeholder="再输入一次密码" value="${esc(f.confirm)}">`:''}${noticeHtml()}<button class="primary login-submit" type="submit">${signup?'创建账号':'登录'}</button></form>${!signup?'<div class="demo-hint">也可以使用演示账号，密码均为demo123<div class="demo-buttons"><button type="button" class="ghost" data-fill="zhh">填入 zhh</button><button type="button" class="ghost" data-fill="myh">填入 myh</button></div></div>':''}</div></section></div>`;
    root.querySelectorAll('[data-auth]').forEach(button=>{
      button.addEventListener('click',()=>{captureForm(root.querySelector('form'));switchAuth(button.dataset.auth);});
      button.addEventListener('keydown',event=>{if(['ArrowLeft','ArrowRight'].includes(event.key)){event.preventDefault();captureForm(root.querySelector('form'));switchAuth(signup?'login':'signup');}});
    });
    root.querySelectorAll('[data-fill]').forEach(button=>button.addEventListener('click',()=>{root.querySelector('#username').value=button.dataset.fill;root.querySelector('#password').value='demo123';root.querySelector('#password').focus();}));
    root.querySelector('.password-toggle').addEventListener('click',event=>{
      state.reveal=!state.reveal;
      root.querySelectorAll('#password,#confirm').forEach(input=>input.type=state.reveal?'text':'password');
      event.currentTarget.textContent=state.reveal?'隐藏':'显示';event.currentTarget.setAttribute('aria-pressed',String(state.reveal));event.currentTarget.setAttribute('aria-label',`${state.reveal?'隐藏':'显示'}密码`);
    });
    root.querySelector('form').addEventListener('submit',async event=>{
      event.preventDefault();if(state.busy)return;
      captureForm(event.currentTarget);state.notice='';state.error=false;
      const error=signup?registrationError():'';
      if(error){state.notice=error;state.error=true;renderAuth();root.querySelector('.notice').focus();return;}
      state.busy=true;const submit=root.querySelector('[type="submit"]');submit.disabled=true;submit.textContent=signup?'正在创建…':'正在登录…';
      root.querySelectorAll('input,[data-auth],[data-fill],.password-toggle').forEach(control=>control.disabled=true);
      try {
        if(signup){
          const user=await client.createAccount({username:state.form.username.trim().toLowerCase(),displayName:state.form.displayName.trim(),password:state.form.password});
          state.authMode='login';state.form={...emptyForm(),username:user.username};state.reveal=false;state.notice='账号已创建，请使用新账号登录。';state.busy=false;renderAuth();root.querySelector('#password').focus();
        }else{
          state.user=await client.login(state.form.username,state.form.password);state.form=emptyForm();location.hash=`/account/${state.user.username}`;
          await reloadData();state.busy=false;renderDashboard();
        }
      }catch(error){
        // SQL练习断点可能在登录成功后出现；返回表单前清掉该次登录状态。
        if(state.user){try{await client.logout();}catch{}state.user=null;}
        state.busy=false;state.notice=error.message || '暂时无法连接，请检查服务后重试';state.error=true;renderAuth();root.querySelector('.notice').focus();
      }
    });
  }
  async function reloadData(){[state.event,state.items]=await Promise.all([client.event(),client.mine()]);}
  function renderDashboard(){
    const e=state.event,u=state.user,own=Boolean(e.myRegistrationId);
    const content=state.tab==='events'
      ? `<div class="event-layout"><section class="card event-card"><div class="event-cover"><h2>${esc(e.title)}</h2><p>现场体验 · 名额有限</p></div><div class="event-body"><h3>关于活动</h3><p>从一个报名按钮开始，看看请求怎样被处理，报名记录又保存在哪里。</p><div class="event-tags"><span class="tag">新生可参加</span><span class="tag">现场讲解</span></div></div></section><aside class="card booking-card"><div class="booking-caption">当前剩余</div><div class="seat-count"><strong data-testid="remaining">${e.remainingSeats}</strong><span>个名额</span></div><div class="capacity">活动总名额 ${e.capacity} · 已报名 ${e.registeredCount}</div><button class="primary" id="register" ${own||e.remainingSeats===0||state.busy?'disabled':''}>${state.busy?'正在提交…':own?'已报名':e.remainingSeats===0?'名额已满':'立即报名'}</button><button class="refresh" id="refresh">刷新名额</button></aside></div>`
      : `<section class="card mine-card"><h2>我的报名</h2>${state.items.length?state.items.map(r=>`<article class="registration"><div><h3>${esc(r.eventTitle)}</h3><p>报名编号 #${esc(String(r.id).padStart(4,'0'))} · ${esc(r.displayName)}（${esc(r.username)}）</p></div><span class="status">已确认</span></article>`).join(''):'<p class="empty">你还没有报名记录。可以先到活动页看看。</p>'}</section>`;
    root.innerHTML=`<div class="dashboard-top"><div class="welcome"><h1>活动报名</h1><p>你好，${esc(u.displayName)}。报名结果会显示在“我的报名”。</p></div><div class="account"><div class="avatar">${esc(u.username[0].toUpperCase())}</div><div class="account-name">${esc(u.displayName)}<small>${esc(u.username)}</small></div><button class="logout" id="logout">退出</button></div></div><nav class="tabs" aria-label="账户页面"><button class="tab ${state.tab==='events'?'active':''}" data-tab="events">活动报名</button><button class="tab ${state.tab==='mine'?'active':''}" data-tab="mine">我的报名</button></nav>${noticeHtml()}${content}`;
    root.querySelectorAll('[data-tab]').forEach(button=>button.addEventListener('click',async()=>{state.tab=button.dataset.tab;state.notice='';await refresh();}));
    root.querySelector('#logout').addEventListener('click',async()=>{try{await client.logout();}catch{}state.user=null;state.form=emptyForm();state.tab='events';state.notice='';state.error=false;location.hash='';renderAuth();});
    root.querySelector('#refresh')?.addEventListener('click',refresh);
    root.querySelector('#register')?.addEventListener('click',async()=>{
      if(state.busy)return;state.busy=true;state.notice='';renderDashboard();
      try{await client.register();state.notice='报名成功，可在“我的报名”查看记录。';state.error=false;}
      catch(error){if(error.status===401)return expired();state.notice=error.message;state.error=true;}
      finally{state.busy=false;}
      try{await reloadData();}catch(error){state.notice=error.message;state.error=true;}
      if(state.user)renderDashboard();
    });
  }
  async function expired(){try{await client.logout();}catch{}state.user=null;state.busy=false;state.form=emptyForm();state.notice='登录已失效，请重新登录。';state.error=true;renderAuth();}
  async function refresh(){try{await reloadData();renderDashboard();}catch(error){if(error.status===401)return expired();state.notice=error.message;state.error=true;renderDashboard();}}
  (async()=>{
    if(!client.hasSession())return renderAuth();
    root.innerHTML='<div class="loading">正在读取账号…</div>';
    try{state.user=await client.me();await reloadData();location.hash=`/account/${state.user.username}`;renderDashboard();}
    catch{await expired();}
  })();
})();
