'use strict';
/*
 * v2.1.1.2.1.2 协议自检: 群会话列表 / 隐藏会话 / 转发消息 / 清空群记录 / 群已读
 */
const net = require('net');
const fs = require('fs');
const HOST = '127.0.0.1';
const PORT = 8897;
const sleep = ms => new Promise(r => setTimeout(r, ms));

function client(tag) {
  return new Promise((resolve, reject) => {
    const c = net.createConnection({ host: HOST, port: PORT }, () => {
      c.tag = tag;
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
  const b = await client('B');
  send(a, { type: 'register', username: 'gt_c1', password: '1234', nickname: '测甲' });
  await sleep(300);
  check('注册A', has(a, 'login_ok'));
  send(b, { type: 'register', username: 'gt_c2', password: '1234', nickname: '测乙' });
  await sleep(300);
  check('注册B', has(b, 'login_ok'));
  clear(a); clear(b);

  send(a, { type: 'add_friend', username: 'gt_c2' });
  await sleep(400);
  clear(a);
  send(b, { type: 'respond_request', from: 'gt_c1', accept: true });
  await sleep(400);
  clear(a); clear(b);

  // 建群 + 拉人
  send(a, { type: 'create_group', name: '测试群2' });
  await sleep(400);
  const cg = get(a, 'group_created');
  const gid = cg.group.id;
  clear(a);
  send(a, { type: 'invite_group', id: gid, username: 'gt_c2' });
  await sleep(500);
  clear(a); clear(b);

  // A 发群消息 -> 会话列表应包含群
  send(a, { type: 'send_group_message', id: gid, text: '群消息1' });
  await sleep(500);
  clear(a); clear(b);
  send(a, { type: 'get_conversations' });
  await sleep(400);
  const convsA = get(a, 'conversations');
  const gconv = convsA.conversations.find(c => c.type === 'group' && c.with === gid);
  check('会话列表包含群聊', !!gconv && gconv.nickname === '测试群2', gconv && gconv.lastText);

  // A 隐藏群会话
  clear(a);
  send(a, { type: 'hide_conversation', with: gid });
  await sleep(400);
  clear(a);
  send(a, { type: 'get_conversations' });
  await sleep(400);
  const convsA2 = get(a, 'conversations');
  check('隐藏后群会话消失', !convsA2.conversations.some(c => c.with === gid));

  // B 发群消息 -> A 的会话自动重新出现
  clear(a); clear(b);
  send(b, { type: 'send_group_message', id: gid, text: '唤醒' });
  await sleep(500);
  clear(a);
  send(a, { type: 'get_conversations' });
  await sleep(400);
  const convsA3 = get(a, 'conversations');
  check('收到群消息后会话重新出现', convsA3.conversations.some(c => c.type === 'group' && c.with === gid && c.unread >= 1));

  // mark_group_read
  clear(a);
  send(a, { type: 'mark_group_read', id: gid });
  await sleep(300);
  clear(a);
  send(a, { type: 'get_conversations' });
  await sleep(400);
  const convsA4 = get(a, 'conversations');
  const gconv4 = convsA4.conversations.find(c => c.with === gid);
  check('群已读后 unread=0', gconv4 && gconv4.unread === 0);

  // 转发消息 (1:1 文本)
  clear(a); clear(b);
  send(a, { type: 'send_message', to: 'gt_c2', text: '原始消息' });
  await sleep(400);
  clear(a); clear(b);
  const hist = await new Promise(res => {
    send(a, { type: 'get_history', with: 'gt_c2' });
    setTimeout(() => {
      const h = get(a, 'history');
      res(h ? h.messages.find(m => m.text === '原始消息') : null);
    }, 400);
  });
  check('找到原始消息', !!hist, hist && hist.id);
  clear(a); clear(b);
  send(a, { type: 'forward_message', to: 'gt_c2', id: hist.id });
  await sleep(500);
  check('转发成功(A收到)', has(a, 'new_message', o => o.from === 'gt_c1' && o.text === '原始消息'));
  check('转发成功(B收到)', has(b, 'new_message', o => o.from === 'gt_c1' && o.text === '原始消息'));
  clear(a); clear(b);

  // 清空群记录
  send(a, { type: 'clear_group_history', id: gid });
  await sleep(400);
  check('清空群记录A', has(a, 'group_history_cleared', o => o.id === gid));
  check('清空群记录B收到', has(b, 'group_history_cleared', o => o.id === gid));
  clear(a); clear(b);
  send(a, { type: 'get_group_history', id: gid });
  await sleep(400);
  const gh = get(a, 'group_history', o => o.id === gid);
  check('群历史已清空', gh && gh.messages.length === 0);

  // 清理
  const usersFile = 'C:/Users/cheng/Documents/Codex/Chatapp/outputs/users.json';
  let users = JSON.parse(fs.readFileSync(usersFile, 'utf8'));
  for (const k of ['gt_c1', 'gt_c2']) delete users[k];
  fs.writeFileSync(usersFile, JSON.stringify(users));
  const msgsFile = 'C:/Users/cheng/Documents/Codex/Chatapp/outputs/messages.json';
  let msgs = JSON.parse(fs.readFileSync(msgsFile, 'utf8'));
  msgs = msgs.filter(m => m.from !== 'gt_c1' && m.from !== 'gt_c2' && m.to !== 'gt_c1' && m.to !== 'gt_c2');
  fs.writeFileSync(msgsFile, JSON.stringify(msgs));
  const groupsFile = 'C:/Users/cheng/Documents/Codex/Chatapp/outputs/groups.json';
  let groups = JSON.parse(fs.readFileSync(groupsFile, 'utf8'));
  for (const k of Object.keys(groups)) {
    if (groups[k].owner === 'gt_c1' || groups[k].owner === 'gt_c2') delete groups[k];
  }
  fs.writeFileSync(groupsFile, JSON.stringify(groups));
  const gmsgsFile = 'C:/Users/cheng/Documents/Codex/Chatapp/outputs/groupMessages.json';
  let gmsgs = JSON.parse(fs.readFileSync(gmsgsFile, 'utf8'));
  for (const k of Object.keys(gmsgs)) {
    if (gmsgs[k].some(m => m.from === 'gt_c1' || m.from === 'gt_c2')) delete gmsgs[k];
  }
  fs.writeFileSync(gmsgsFile, JSON.stringify(gmsgs));
  console.log('测试账号已清理');

  a.destroy();
  b.destroy();
  console.log('====================================');
  console.log('PASS=' + pass + ' FAIL=' + fail);
  process.exit(fail === 0 ? 0 : 1);
})().catch(e => {
  console.error('ERROR: ' + e.stack);
  process.exit(2);
});
