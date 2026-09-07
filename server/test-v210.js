'use strict';
/*
 * v2.1.0 协议自检: 群聊 + 好友管理 (线性 sleep + 消息数组检查)
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
function has(c, type, pred) {
  return c.all.some(o => o.type === type && (!pred || pred(o)));
}
function get(c, type, pred) {
  return c.all.find(o => o.type === type && (!pred || pred(o)));
}

let pass = 0, fail = 0;
function check(name, ok, extra) {
  if (ok) { pass++; console.log('PASS ' + name + (extra ? '  ' + extra : '')); }
  else { fail++; console.log('FAIL ' + name + (extra ? '  ' + extra : '')); }
}

(async () => {
  const a = await client('A');
  const b = await client('B');

  send(a, { type: 'register', username: 'gt_a1', password: '1234', nickname: '群测A' });
  await sleep(300);
  check('注册A', has(a, 'login_ok'));
  send(b, { type: 'register', username: 'gt_b1', password: '1234', nickname: '群测B' });
  await sleep(300);
  check('注册B', has(b, 'login_ok'));
  clear(a); clear(b);

  // 加好友
  send(a, { type: 'add_friend', username: 'gt_b1' });
  await sleep(500);
  check('A收到toast', has(a, 'toast'));
  check('B收到好友请求', has(b, 'friend_request', o => o.from === 'gt_a1'));
  clear(a); clear(b);
  send(b, { type: 'respond_request', from: 'gt_a1', accept: true });
  await sleep(500);
  check('B接受后成为好友', has(b, 'friend_response', o => o.accepted && o.mine));
  clear(a); clear(b);

  // 创建群聊
  send(a, { type: 'create_group', name: '测试群' });
  await sleep(500);
  const cg = get(a, 'group_created');
  check('创建群聊', !!cg && cg.group && cg.group.owner === 'gt_a1', cg && cg.group && cg.group.id);
  const gid = cg.group.id;
  clear(a);

  // 拉 B 进群
  send(a, { type: 'invite_group', id: gid, username: 'gt_b1' });
  await sleep(600);
  check('A收到group_updated含B', has(a, 'group_updated', o => o.group && o.group.id === gid && o.group.members.includes('gt_b1')));
  check('B收到入群通知', has(b, 'group_invited', o => o.group && o.group.id === gid));
  clear(a); clear(b);

  // 群公告
  send(a, { type: 'set_group_notice', id: gid, notice: '欢迎加入测试群' });
  await sleep(500);
  check('编辑公告', has(a, 'group_updated', o => o.group && o.group.id === gid && o.group.notice === '欢迎加入测试群'));
  clear(a); clear(b);

  // 群消息
  send(a, { type: 'send_group_message', id: gid, text: '大家好' });
  await sleep(600);
  check('A收到自己群消息', has(a, 'new_group_message', o => o.group === gid && o.text === '大家好'));
  check('B收到群消息', has(b, 'new_group_message', o => o.group === gid && o.text === '大家好'));
  clear(a); clear(b);

  // 群历史
  send(b, { type: 'get_group_history', id: gid });
  await sleep(500);
  const hist = get(b, 'group_history', o => o.id === gid);
  check('B拉群历史', !!hist && hist.messages.length >= 1);
  clear(b);

  // 撤回群消息
  send(b, { type: 'send_group_message', id: gid, text: '撤回我' });
  await sleep(500);
  const gm = get(b, 'new_group_message', o => o.group === gid && o.text === '撤回我');
  clear(b); clear(a);
  send(b, { type: 'recall_group_message', id: gid, messageId: gm.id });
  await sleep(500);
  check('撤回群消息', has(b, 'group_message_recalled'));
  check('A收到撤回通知', has(a, 'group_message_recalled'));
  clear(a); clear(b);

  // 成员列表
  send(b, { type: 'get_group_members', id: gid });
  await sleep(500);
  const members = get(b, 'group_members', o => o.id === gid);
  check('成员列表2人', !!members && members.members.length === 2);
  clear(b);

  // 非群主踢人被拒
  send(b, { type: 'kick_group', id: gid, username: 'gt_a1' });
  await sleep(400);
  check('非群主踢人被拒', has(b, 'error'));
  clear(b);

  // 转让群主
  send(a, { type: 'transfer_owner', id: gid, username: 'gt_b1' });
  await sleep(600);
  check('转让群主给B', has(a, 'group_updated', o => o.group && o.group.id === gid && o.group.owner === 'gt_b1'));
  check('B收到群变更(owner)', has(b, 'group_changed', o => o.group && o.group.id === gid && o.group.owner === 'gt_b1'));
  clear(a); clear(b);

  // B 踢 A
  send(b, { type: 'kick_group', id: gid, username: 'gt_a1' });
  await sleep(600);
  check('群主踢人成功', has(b, 'group_updated', o => o.group && o.group.id === gid && !o.group.members.includes('gt_a1')));
  check('A被踢通知', has(a, 'group_kicked', o => o.group && o.group.id === gid));
  clear(a); clear(b);

  // 解散群
  send(a, { type: 'create_group', name: '解散测试' });
  await sleep(400);
  const cg2 = get(a, 'group_created');
  clear(a);
  send(a, { type: 'dissolve_group', id: cg2.group.id });
  await sleep(500);
  check('解散群聊', has(a, 'group_dissolved_ok'));
  clear(a);

  // 好友管理
  send(b, { type: 'send_message', to: 'gt_a1', text: 'hello' });
  await sleep(500);
  clear(a); clear(b);
  send(b, { type: 'clear_history', with: 'gt_a1' });
  await sleep(400);
  check('清空记录', has(b, 'history_cleared'));
  clear(b);
  send(b, { type: 'block_user', with: 'gt_a1' });
  await sleep(400);
  check('拉黑', has(b, 'blocked'));
  clear(b);
  send(a, { type: 'send_message', to: 'gt_b1', text: '被拉黑' });
  await sleep(400);
  check('拉黑后消息被拒', has(a, 'toast'));
  clear(a); clear(b);
  send(b, { type: 'unblock_user', with: 'gt_a1' });
  await sleep(400);
  check('取消拉黑', has(b, 'unblocked'));
  clear(b);
  send(b, { type: 'unfriend', with: 'gt_a1' });
  await sleep(400);
  check('删除好友', has(b, 'unfriended'));
  clear(a); clear(b);

  // 清理测试账号
  const usersFile = 'C:/Users/cheng/Documents/Codex/Chatapp/outputs/users.json';
  let users = JSON.parse(fs.readFileSync(usersFile, 'utf8'));
  for (const k of ['gt_a1', 'gt_b1']) delete users[k];
  fs.writeFileSync(usersFile, JSON.stringify(users));
  const msgsFile = 'C:/Users/cheng/Documents/Codex/Chatapp/outputs/messages.json';
  let msgs = JSON.parse(fs.readFileSync(msgsFile, 'utf8'));
  msgs = msgs.filter(m => m.from !== 'gt_a1' && m.from !== 'gt_b1' && m.to !== 'gt_a1' && m.to !== 'gt_b1');
  fs.writeFileSync(msgsFile, JSON.stringify(msgs));
  const groupsFile = 'C:/Users/cheng/Documents/Codex/Chatapp/outputs/groups.json';
  if (fs.existsSync(groupsFile)) {
    let groups = JSON.parse(fs.readFileSync(groupsFile, 'utf8'));
    for (const k of Object.keys(groups)) {
      if (groups[k].owner === 'gt_a1' || groups[k].owner === 'gt_b1') delete groups[k];
    }
    fs.writeFileSync(groupsFile, JSON.stringify(groups));
  }
  const gmsgsFile = 'C:/Users/cheng/Documents/Codex/Chatapp/outputs/groupMessages.json';
  if (fs.existsSync(gmsgsFile)) {
    let gmsgs = JSON.parse(fs.readFileSync(gmsgsFile, 'utf8'));
    for (const k of Object.keys(gmsgs)) {
      if (gmsgs[k].some(m => m.from === 'gt_a1' || m.from === 'gt_b1')) delete gmsgs[k];
    }
    fs.writeFileSync(gmsgsFile, JSON.stringify(gmsgs));
  }
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
