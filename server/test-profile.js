'use strict';
const net = require('net');
const fs = require('fs');
const HOST = '127.0.0.1';
const PORT = 8897;
const sleep = ms => new Promise(r => setTimeout(r, ms));

function client(tag) {
  return new Promise((resolve, reject) => {
    const c = net.createConnection({ host: HOST, port: PORT }, () => {
      c.buf = '';
      c.all = [];
      c.on('data', d => {
        c.buf += d.toString('utf8');
        let idx;
        while ((idx = c.buf.indexOf('\n')) >= 0) {
          const line = c.buf.slice(0, idx).trim();
          c.buf = c.buf.slice(idx + 1);
          if (!line) continue;
          try { c.all.push(JSON.parse(line)); } catch (e) {}
        }
      });
      c.on('error', () => {});
      resolve(c);
    });
    c.on('error', reject);
  });
}
function send(c, obj) { c.write(JSON.stringify(obj) + '\n'); }
function clear(c) { c.all = []; }
function has(c, type, pred) { return c.all.some(o => o.type === type && (!pred || pred(o))); }
function get(c, type, pred) { return c.all.find(o => o.type === type && (!pred || pred(o))); }

let pass = 0, fail = 0;
function check(name, ok, extra) {
  if (ok) { pass++; console.log('PASS ' + name + (extra ? '  ' + extra : '')); }
  else { fail++; console.log('FAIL ' + name + (extra ? '  ' + extra : '')); }
}

(async () => {
  const a = await client('A');
  send(a, { type: 'register', username: 'gt_p1', password: '1234', nickname: '测档' });
  await sleep(300);
  check('注册', has(a, 'login_ok'));
  clear(a);

  send(a, { type: 'get_profile' });
  await sleep(300);
  const p1 = get(a, 'profile');
  check('获取默认档案', !!p1 && p1.profile && Array.isArray(p1.profile.tags), JSON.stringify(p1 && p1.profile).slice(0, 100));

  clear(a);
  send(a, { type: 'set_profile', gender: '男', birthday: '1995-06-15', job: '教育', company: '测试公司', location: '广东-深圳-南山', birthplace: '湖南-长沙', email: 'abc@gmail.com', tags: ['#睡觉', '#音乐'] });
  await sleep(300);
  check('设置档案', has(a, 'profile_updated'));
  clear(a);
  send(a, { type: 'get_profile' });
  await sleep(300);
  const p2 = get(a, 'profile');
  check('档案字段保存', p2 && p2.profile.gender === '男' && p2.profile.birthday === '1995-06-15' && p2.profile.tags.length === 2 && p2.profile.email === 'abc@gmail.com', JSON.stringify(p2 && p2.profile));

  // 照片
  const tiny = Buffer.from('89504e470d0a1a0a0000000d4948445200000001000000010806000000' +
    '1f15c4890000000d49444154789c626001000000ffff03000006000557bfabd40000000049454e44ae426082', 'hex');
  clear(a);
  send(a, { type: 'add_profile_photo', data: tiny.toString('base64') });
  await sleep(400);
  const pu = get(a, 'profile_updated');
  check('添加照片', pu && pu.profile.photos.length === 1, pu && pu.profile.photos[0]);
  clear(a);
  send(a, { type: 'remove_profile_photo', index: 0 });
  await sleep(300);
  const pr = get(a, 'profile_updated');
  check('删除照片', pr && pr.profile.photos.length === 0);

  // 清理
  const usersFile = 'C:/Users/cheng/Documents/Codex/Chatapp/outputs/users.json';
  let users = JSON.parse(fs.readFileSync(usersFile, 'utf8'));
  delete users['gt_p1'];
  fs.writeFileSync(usersFile, JSON.stringify(users));
  const msgsFile = 'C:/Users/cheng/Documents/Codex/Chatapp/outputs/messages.json';
  let msgs = JSON.parse(fs.readFileSync(msgsFile, 'utf8'));
  msgs = msgs.filter(m => m.from !== 'gt_p1' && m.to !== 'gt_p1');
  fs.writeFileSync(msgsFile, JSON.stringify(msgs));
  console.log('测试账号已清理');
  a.destroy();
  console.log('PASS=' + pass + ' FAIL=' + fail);
  process.exit(fail === 0 ? 0 : 1);
})().catch(e => { console.error('ERROR: ' + e.stack); process.exit(2); });
