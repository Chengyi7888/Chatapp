'use strict';
/*
 * Chatapp 服务器
 * 运行: node server.js   (Windows 下也可以双击 start-server.bat)
 * 协议: TCP 长连接, 每行一个 JSON 对象
 * 日志: 写入 server.log (与 server.js 同目录)
 */
const net = require('net');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const PORT = parseInt(process.env.PORT || '8899', 10);
const DATA_DIR = __dirname;
const USERS_FILE = path.join(DATA_DIR, 'users.json');
const MESSAGES_FILE = path.join(DATA_DIR, 'messages.json');
const LOG_FILE = path.join(DATA_DIR, 'server.log');
const VERBOSE_IO_LOG = process.env.VERBOSE_IO_LOG === '1';

let users = {};      // username -> { username, salt, hash, nickname, friends[], pending[], lastRead{}, blocked[] }
let messages = [];   // { id, from, to, text, time, kind, url, name, size, reply }
let sockets = {};    // username -> socket
let groups = {};         // id -> {id,name,owner,notice,members[],hasAvatar,createdAt}
let groupMessages = {};  // id -> [msgs]
let posts = [];          // {id, from, text, images[], time, likes[]}
let activities = [];       // {id, from, title, text, time, joiners[]}

function log(msg) {
  const line = '[' + new Date().toLocaleString() + '] ' + msg;
  console.log(line);
  try { fs.appendFileSync(LOG_FILE, line + '\n'); } catch (e) { /* ignore */ }
}

function load() {
  try { users = JSON.parse(fs.readFileSync(USERS_FILE, 'utf8')); } catch (e) { users = {}; }
  try { messages = JSON.parse(fs.readFileSync(MESSAGES_FILE, 'utf8')); } catch (e) { messages = []; }
  for (const k of Object.keys(users)) {
    const u = users[k];
    u.friends = u.friends || [];
    u.pending = u.pending || [];
    u.lastRead = u.lastRead || {};
    u.status = u.status || 'Hello,world';
    u.hasAvatar = !!u.hasAvatar;
    u.blocked = u.blocked || [];
    u.hidden = u.hidden || [];
    u.hiddenMessages = u.hiddenMessages || [];
    u.lastReadGroup = u.lastReadGroup || {};
    u.profile = u.profile || { gender: '', birthday: '', tags: [], photos: [], job: '', company: '', location: '', birthplace: '', email: '' };
  }
}

function loadGroups() {
  try { groups = JSON.parse(fs.readFileSync(path.join(DATA_DIR, 'groups.json'), 'utf8')); } catch (e) { groups = {}; }
  try { groupMessages = JSON.parse(fs.readFileSync(path.join(DATA_DIR, 'groupMessages.json'), 'utf8')); } catch (e) { groupMessages = {}; }
  try { posts = JSON.parse(fs.readFileSync(path.join(DATA_DIR, 'posts.json'), 'utf8')); } catch (e) { posts = []; }
  try { activities = JSON.parse(fs.readFileSync(path.join(DATA_DIR, 'activities.json'), 'utf8')); } catch (e) { activities = []; }
}

function seedActivities() {
  if (!activities.some(a => a && a.game === 'jump')) {
    activities.unshift({
      id: 'sys-jump-jump',
      from: 'system',
      title: '跳一跳',
      text: '微信小程序同款「跳一跳」小游戏，长按蓄力、松开跳跃，快来挑战最高分吧！',
      time: Date.now(),
      joiners: [],
      game: 'jump'
    });
    scheduleGroupSave();
  }
}
let gSaveTimer = null;
function scheduleGroupSave() {
  if (gSaveTimer) return;
  gSaveTimer = setTimeout(() => { gSaveTimer = null; saveGroups(); }, 300);
}
function saveGroups() {
  try {
    fs.writeFileSync(path.join(DATA_DIR, 'groups.json') + '.tmp', JSON.stringify(groups));
    fs.renameSync(path.join(DATA_DIR, 'groups.json') + '.tmp', path.join(DATA_DIR, 'groups.json'));
  } catch (e) { /* ignore */ }
  try {
    fs.writeFileSync(path.join(DATA_DIR, 'groupMessages.json') + '.tmp', JSON.stringify(groupMessages));
    fs.renameSync(path.join(DATA_DIR, 'groupMessages.json') + '.tmp', path.join(DATA_DIR, 'groupMessages.json'));
  } catch (e) { /* ignore */ }
  try {
    fs.writeFileSync(path.join(DATA_DIR, 'posts.json') + '.tmp', JSON.stringify(posts));
    fs.renameSync(path.join(DATA_DIR, 'posts.json') + '.tmp', path.join(DATA_DIR, 'posts.json'));
  try {
    fs.writeFileSync(path.join(DATA_DIR, 'activities.json') + '.tmp', JSON.stringify(activities));
    fs.renameSync(path.join(DATA_DIR, 'activities.json') + '.tmp', path.join(DATA_DIR, 'activities.json'));
  } catch (e) { /* ignore */ }
  } catch (e) { /* ignore */ }
}

let saveTimer = null;
function scheduleSave() {
  if (saveTimer) return;
  saveTimer = setTimeout(() => { saveTimer = null; save(); }, 300);
}
function save() {
  try {
    fs.writeFileSync(USERS_FILE + '.tmp', JSON.stringify(users));
    fs.renameSync(USERS_FILE + '.tmp', USERS_FILE);
  } catch (e) { log('保存用户数据失败: ' + e.message); }
  try {
    fs.writeFileSync(MESSAGES_FILE + '.tmp', JSON.stringify(messages));
    fs.renameSync(MESSAGES_FILE + '.tmp', MESSAGES_FILE);
  } catch (e) { log('保存消息数据失败: ' + e.message); }
}

function hash(pw, salt) { return crypto.createHash('sha256').update(salt + ':' + pw).digest('hex'); }
function newSalt() { return crypto.randomBytes(8).toString('hex'); }
function randomNickname() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  let n = '';
  for (let i = 0; i < 8; i++) n += chars[crypto.randomInt(chars.length)];
  return n;
}

function broadcast(obj) {
  for (const k of Object.keys(sockets)) {
    try { send(sockets[k], obj); } catch (e) { /* ignore */ }
  }
}

function send(sock, obj) {
  if (!sock || sock.destroyed) return;
  try { sock.write(JSON.stringify(obj) + '\n'); } catch (e) { /* ignore */ }
}
function online(username) { return !!sockets[username]; }
function isFriend(a, b) { const u = users[a]; return !!(u && u.friends.indexOf(b) >= 0); }
function isBlocked(a, b) { const u = users[a]; return !!(u && (u.blocked || []).indexOf(b) >= 0); }
function convKey(a, b) { return a < b ? a + '|' + b : b + '|' + a; }

function convMessages(a, b) {
  return messages.filter(m => (m.from === a && m.to === b) || (m.from === b && m.to === a));
}

function unreadCount(me, peer) {
  const u = users[me]; if (!u) return 0;
  const since = u.lastRead[peer] || 0;
  return messages.filter(m => m.from === peer && m.to === me && m.time > since).length;
}

function conversationSummary(me, peer) {
  const p = users[peer];
  const hidden = users[me].hiddenMessages || [];
  const list = convMessages(me, peer);
  let last = null;
  for (let i = list.length - 1; i >= 0; i--) {
    if (hidden.indexOf(list[i].id) < 0) { last = list[i]; break; }
  }
  return {
    with: peer,
    nickname: p ? p.nickname : peer,
    hasAvatar: !!(p && p.hasAvatar),
    lastText: last ? last.text : '',
    lastKind: last ? (last.kind || 'text') : 'text',
    lastTime: last ? last.time : 0,
    unread: unreadCount(me, peer),
    blocked: isBlocked(me, peer),
    type: 'peer'
  };
}

function groupConversationSummary(me, g) {
  const hidden = users[me].hiddenMessages || [];
  const list = groupMessages[g.id] || [];
  let last = null;
  for (let i = list.length - 1; i >= 0; i--) {
    if (hidden.indexOf(list[i].id) < 0) { last = list[i]; break; }
  }
  const since = (users[me].lastReadGroup || {})[g.id] || 0;
  const unread = list.filter(m => m.from !== me && m.time > since).length;
  return {
    with: g.id,
    nickname: g.name,
    hasAvatar: !!g.hasAvatar,
    lastText: last ? (last.text || '') : '',
    lastKind: last ? (last.kind || 'text') : 'text',
    lastTime: last ? last.time : 0,
    unread: unread,
    type: 'group',
    memberCount: g.members.length
  };
}

function allConversations(me) {
  const hidden = users[me].hidden || [];
  let convs = users[me].friends.map(f => conversationSummary(me, f));
  for (const id of Object.keys(groups)) {
    const g = groups[id];
    if (g.members.includes(me)) convs.push(groupConversationSummary(me, g));
  }
  convs = convs.filter(c => hidden.indexOf(c.with) < 0);
  return convs.sort((a, b) => b.lastTime - a.lastTime);
}

function getContactsData(username) {
  const u = users[username];
  return {
    contacts: u.friends.map(f => ({
      username: f,
      nickname: users[f] ? users[f].nickname : f,
      status: users[f] ? (users[f].status || '') : '',
      hasAvatar: !!(users[f] && users[f].hasAvatar),
      online: online(f),
      blocked: isBlocked(username, f)
    })),
    pending: (u.pending || []).map(f => ({ username: f, nickname: users[f] ? users[f].nickname : f }))
  };
}

function notifyContactsChanged(username) {
  const u = users[username]; if (!u) return;
  for (const f of u.friends) {
    if (online(f)) send(sockets[f], { type: 'contacts_changed' });
  }
}

// ============ 群聊 ============
function myGroups(username) {
  return Object.keys(groups).map(id => groups[id]).filter(g => g.members.includes(username)).map(g => ({
    id: g.id, name: g.name, owner: g.owner, notice: g.notice || '',
    memberCount: g.members.length, hasAvatar: !!g.hasAvatar, members: g.members
  }));
}
function pushToGroup(g, obj) {
  for (const m of g.members) if (online(m)) send(sockets[m], obj);
}
function pushGroupChanged(g) {
  pushToGroup(g, { type: 'group_changed', group: groupObj(g) });
}
function findGroupByName(name) {
  for (const id of Object.keys(groups)) if (groups[id].name === name) return groups[id];
  return null;
}
function groupObj(g) {
  return { id: g.id, name: g.name, owner: g.owner, notice: g.notice || '', memberCount: g.members.length, hasAvatar: !!g.hasAvatar, members: g.members };
}
function getGroupFor(me, id) {
  const g = groups[id];
  if (!g || !g.members.includes(me)) return null;
  return g;
}

function needAuth(sock) { send(sock, { type: 'error', message: '请先登录' }); }

function attachUser(sock, username, welcome) {
  sock.user = username;
  sockets[username] = sock;
  send(sock, { type: 'login_ok', username, nickname: users[username].nickname, token: users[username].token || '', status: users[username].status || '愿你每天都有好心情', hasAvatar: !!users[username].hasAvatar, welcome });
  const data = getContactsData(username);
  data.type = 'contacts';
  send(sock, data);
  send(sock, { type: 'conversations', conversations: allConversations(username) });
  send(sock, { type: 'groups', groups: myGroups(username) });
  notifyContactsChanged(username);
  log(username + ' 上线 (nickname=' + users[username].nickname + ')');
}

function handle(sock, line) {
  let msg;
  try { msg = JSON.parse(line); } catch (e) { log('收到无法解析的数据: ' + line.slice(0, 200)); return; }
  if (!msg || typeof msg !== 'object') return;
  const type = msg.type || '';
  const me = sock.user;

  switch (type) {
    case 'ping':
      send(sock, { type: 'pong' });
      break;

    case 'register': {
      const username = String(msg.username || '').trim();
      const password = String(msg.password || '');
      if (!/^[A-Za-z0-9\-_\/~@]{2,20}$/.test(username) || !/[A-Za-z]/.test(username) || !/[0-9]/.test(username)) {
        log('注册失败: 用户账号不合法 [' + username + ']');
        return send(sock, { type: 'error', code: 'bad_username', message: '用户账号需为英文+数字（2-20位），符号只能使用 - _ / ~ @' });
      }
      if (password.length < 8 || password.length > 16 || !/[A-Za-z]/.test(password) || !/[0-9]/.test(password)) {
        log('注册失败: 密码不合规 [' + username + ']');
        return send(sock, { type: 'error', code: 'bad_password', message: '登录密码需 8-16 位，且同时包含字母和数字' });
      }
      if (users[username]) {
        log('注册失败: 用户账号已存在 [' + username + ']');
        return send(sock, { type: 'error', code: 'exists', message: '用户账号已被注册' });
      }
      const salt = newSalt();
      const nickname = randomNickname();
      users[username] = {
        username, salt, hash: hash(password, salt), nickname,
        friends: [], pending: [], lastRead: {}, blocked: [], hidden: [], hiddenMessages: [], lastReadGroup: {}, createdAt: Date.now(), profile: { gender: '', birthday: '', tags: [], photos: [], job: '', company: '', location: '', birthplace: '', email: '' },
        token: crypto.randomBytes(16).toString('hex'),
        status: 'Hello,world',
        hasAvatar: false
      };
      scheduleSave();
      log('注册成功: ' + username + ' (随机昵称=' + nickname + ')');
      attachUser(sock, username, '注册成功，欢迎使用 Chatapp');
      break;
    }

    case 'login': {
      const username = String(msg.username || '').trim();
      const password = String(msg.password || '');
      const u = users[username];
      if (!u || u.hash !== hash(password, u.salt)) {
        log('登录失败: ' + username + ' (密码错误或用户不存在)');
        return send(sock, { type: 'error', code: 'bad_login', message: '用户名或密码错误' });
      }
      if (!u.token) { u.token = crypto.randomBytes(16).toString('hex'); scheduleSave(); }
      if (online(username) && sockets[username] !== sock) {
        const old = sockets[username];
        delete sockets[username];
        send(old, { type: 'kicked', message: '你的账号在另一台设备上登录' });
        try { old.destroy(); } catch (e) { /* ignore */ }
        log('账号 ' + username + ' 在其他设备登录，已踢出旧连接');
      }
      log('登录成功: ' + username);
      attachUser(sock, username, '登录成功');
      break;
    }

    case 'auth': {
      const token = String(msg.token || '');
      let found = '';
      for (const name of Object.keys(users)) {
        if (users[name].token && users[name].token === token) { found = name; break; }
      }
      if (!found) return send(sock, { type: 'error', message: '登录状态已失效，请重新登录' });
      if (online(found) && sockets[found] !== sock) {
        const old = sockets[found];
        delete sockets[found];
        send(old, { type: 'kicked', message: '你的账号在另一台设备上登录' });
        try { old.destroy(); } catch (e) { /* ignore */ }
        log('账号 ' + found + ' 自动登录，已踢出旧连接');
      }
      log('令牌自动登录: ' + found);
      attachUser(sock, found, '欢迎回来');
      break;
    }

    case 'set_nickname': {
      if (!me) return needAuth(sock);
      const nickname = String(msg.nickname || '').trim();
      if (!nickname || nickname.length > 20) {
        return send(sock, { type: 'error', message: '昵称不能为空且不超过 20 字' });
      }
      users[me].nickname = nickname;
      scheduleSave();
      log('修改昵称: ' + me + ' -> ' + nickname);
      send(sock, { type: 'nickname_updated', nickname });
      notifyContactsChanged(me);
      break;
    }

    case 'search_user': {
      if (!me) return needAuth(sock);
      const target = String(msg.username || '').trim();
      const tu = users[target];
      if (!tu) return send(sock, { type: 'search_result', status: 'not_found', username: target });
      let status = 'none';
      if (target === me) status = 'self';
      else if (isFriend(me, target)) status = 'friend';
      else if ((users[me].pending || []).indexOf(target) >= 0) status = 'pending_in';
      else if ((tu.pending || []).indexOf(me) >= 0) status = 'pending_out';
      send(sock, { type: 'search_result', status, username: target, nickname: tu.nickname });
      break;
    }

    case 'add_friend': {
      if (!me) return needAuth(sock);
      const target = String(msg.username || '').trim();
      if (target === me) return send(sock, { type: 'toast', message: '不能添加自己为好友' });
      const tu = users[target];
      if (!tu) return send(sock, { type: 'toast', message: '用户不存在' });
      if (isFriend(me, target)) return send(sock, { type: 'toast', message: '你们已经是好友了' });
      if ((users[me].pending || []).indexOf(target) >= 0) {
        return send(sock, { type: 'toast', message: '对方已向你发送请求，去接受吧' });
      }
      if ((tu.pending || []).indexOf(me) >= 0) {
        return send(sock, { type: 'toast', message: '请求已发送，等待对方同意' });
      }
      tu.pending.push(me);
      scheduleSave();
      if (online(target)) {
        send(sockets[target], { type: 'friend_request', from: me, nickname: users[me].nickname });
        send(sockets[target], { type: 'contacts_changed' });
      }
      send(sock, { type: 'toast', message: '好友请求已发送' });
      break;
    }

    case 'respond_request': {
      if (!me) return needAuth(sock);
      const from = String(msg.from || '').trim();
      const accept = !!msg.accept;
      const u = users[me];
      if (!u) return;
      const idx = (u.pending || []).indexOf(from);
      if (idx < 0) return send(sock, { type: 'toast', message: '没有来自该用户的请求' });
      u.pending.splice(idx, 1);
      log('好友请求处理: ' + from + ' -> ' + me + ', accept=' + accept);
      const fu = users[from];
      if (!fu) return send(sock, { type: 'toast', message: '用户不存在' });
      if (accept) {
        if (u.friends.indexOf(from) < 0) u.friends.push(from);
        if (fu.friends.indexOf(me) < 0) fu.friends.push(me);
        if (online(from)) {
          send(sockets[from], { type: 'friend_response', from: me, nickname: u.nickname, accepted: true });
          send(sockets[from], { type: 'contacts_changed' });
        }
        send(sock, { type: 'friend_response', from, nickname: fu.nickname, accepted: true, mine: true });
        send(sock, { type: 'toast', message: '已添加好友' });
      } else {
        if (online(from)) send(sockets[from], { type: 'friend_response', from: me, nickname: u.nickname, accepted: false });
        send(sock, { type: 'toast', message: '已拒绝请求' });
      }
      scheduleSave();
      send(sock, { type: 'contacts_changed' });
      break;
    }

    case 'get_contacts': {
      if (!me) return needAuth(sock);
      const data = getContactsData(me);
      data.type = 'contacts';
      send(sock, data);
      break;
    }

    case 'get_conversations': {
      if (!me) return needAuth(sock);
      send(sock, { type: 'conversations', conversations: allConversations(me) });
      break;
    }

    case 'get_history': {
      if (!me) return needAuth(sock);
      const peer = String(msg.with || '').trim();
      if (!users[peer]) return send(sock, { type: 'history', with: peer, messages: [] });
      const hidden = users[me].hiddenMessages || [];
      const list = convMessages(me, peer).slice(-200)
        .filter(m => hidden.indexOf(m.id) < 0)
        .map(m => {
          const o = { id: m.id, from: m.from, to: m.to, text: m.text || '', time: m.time };
          if (m.kind) o.kind = m.kind;
          if (m.url) o.url = m.url;
          if (m.name) o.name = m.name;
          if (m.size) o.size = m.size;
          if (m.reply) o.reply = m.reply;
          return o;
        });
      send(sock, { type: 'history', with: peer, messages: list, peerReadTime: users[peer].lastRead[me] || 0, myReadTime: users[me].lastRead[peer] || 0 });
      break;
    }

    case 'clear_history': {
      if (!me) return needAuth(sock);
      const peer = String(msg.with || '').trim();
      messages = messages.filter(m => !((m.from === me && m.to === peer) || (m.from === peer && m.to === me)));
      scheduleSave();
      send(sock, { type: 'history_cleared', with: peer });
      send(sock, { type: 'conversations_changed' });
      if (online(peer)) send(sockets[peer], { type: 'conversations_changed' });
      log('清空聊天记录: ' + me + ' <-> ' + peer);
      break;
    }

    case 'unfriend': {
      if (!me) return needAuth(sock);
      const target = String(msg.with || '').trim();
      const u = users[me], tu = users[target];
      if (u) {
        const i = u.friends.indexOf(target);
        if (i >= 0) u.friends.splice(i, 1);
        delete u.lastRead[target];
      }
      if (tu) {
        const j = tu.friends.indexOf(me);
        if (j >= 0) tu.friends.splice(j, 1);
        delete tu.lastRead[me];
      }
      scheduleSave();
      send(sock, { type: 'unfriended', with: target });
      send(sock, { type: 'conversations_changed' });
      notifyContactsChanged(me);
      if (online(target)) {
        send(sockets[target], { type: 'conversations_changed' });
        notifyContactsChanged(target);
      }
      log('删除好友: ' + me + ' -> ' + target);
      break;
    }

    case 'block_user': {
      if (!me) return needAuth(sock);
      const target = String(msg.with || '').trim();
      if (target === me) return send(sock, { type: 'error', message: '不能拉黑自己' });
      const u = users[me];
      if (!u) return;
      if ((u.blocked || []).indexOf(target) < 0) u.blocked.push(target);
      scheduleSave();
      send(sock, { type: 'blocked', with: target });
      send(sock, { type: 'conversations_changed' });
      notifyContactsChanged(me);
      log('拉黑用户: ' + me + ' -> ' + target);
      break;
    }

    case 'unblock_user': {
      if (!me) return needAuth(sock);
      const target = String(msg.with || '').trim();
      const u = users[me];
      if (u) {
        const i = (u.blocked || []).indexOf(target);
        if (i >= 0) u.blocked.splice(i, 1);
      }
      scheduleSave();
      send(sock, { type: 'unblocked', with: target });
      send(sock, { type: 'conversations_changed' });
      notifyContactsChanged(me);
      log('取消拉黑: ' + me + ' -> ' + target);
      break;
    }

    case 'send_message': {
      if (!me) return needAuth(sock);
      const to = String(msg.to || '').trim();
      const text = String(msg.text || '');
      if (!users[to]) return send(sock, { type: 'toast', message: '用户不存在' });
      if (!isFriend(me, to)) return send(sock, { type: 'toast', message: '还不是好友，先添加好友吧' });
      if (isBlocked(to, me)) return send(sock, { type: 'toast', message: '消息发送失败：对方已将你加入黑名单' });
      const time = Date.now();
      const kind = String(msg.kind || 'text');
      if (kind === 'text' && !text.trim()) return;
      const replyText = (msg.reply && String(msg.reply.text || '').trim()) ? String(msg.reply.text).slice(0, 50) : '';
      const mObj = { id: crypto.randomBytes(8).toString('hex'), from: me, to, time, kind: kind };
      if (kind === 'text') {
        mObj.text = text;
        if (replyText) mObj.reply = replyText;
      } else if (kind === 'image' || kind === 'file') {
        const data = String(msg.data || '');
        const name = String(msg.name || '').slice(0, 80);
        const limit = kind === 'image' ? 2500000 : 5000000;
        if (data.length < 16 || data.length > limit * 1.4) {
          return send(sock, { type: 'error', message: '文件数据不合法或过大' });
        }
        const buf = Buffer.from(data, 'base64');
        if (buf.length < 1 || buf.length > limit) {
          return send(sock, { type: 'error', message: '文件数据不合法或过大' });
        }
        const ext = kind === 'image' ? '.jpg' : (String(name).match(/\.[A-Za-z0-9]{1,10}$/) || ['.bin'])[0];
        const fname = mObj.id + ext;
        const fdir = path.join(DATA_DIR, 'files');
        try { fs.mkdirSync(fdir, { recursive: true }); } catch (e) { /* ignore */ }
        fs.writeFileSync(path.join(fdir, fname), buf);
        mObj.url = '/files/' + fname;
        mObj.name = name || fname;
        mObj.size = buf.length;
      }
      messages.push(mObj);
      if (messages.length > 20000) messages = messages.slice(-20000);
      scheduleSave();
      const obj = { type: 'new_message', id: mObj.id, from: me, to, text: mObj.text || '', time, nickname: users[me].nickname, kind: kind };
      if (mObj.url) obj.url = mObj.url;
      if (mObj.name) obj.name = mObj.name;
      if (mObj.size) obj.size = mObj.size;
      if (mObj.reply) obj.reply = mObj.reply;
      users[me].hidden = (users[me].hidden || []).filter(x => x !== to);
      if (users[to]) users[to].hidden = (users[to].hidden || []).filter(x => x !== me);
      scheduleSave();
      send(sock, obj);                 // 回执给发送方
      if (online(to)) send(sockets[to], obj); // 转发给接收方
      break;
    }

    case 'mark_read': {
      if (!me) return needAuth(sock);
      const peer = String(msg.with || '').trim();
      if (!users[me] || !users[peer]) break;
      users[me].lastRead[peer] = Date.now();
      scheduleSave();
      if (online(peer)) send(sockets[peer], { type: 'peer_read', with: me, time: users[me].lastRead[peer] });
      break;
    }

    case 'change_password': {
      if (!me) return needAuth(sock);
      const oldPw = String(msg.old || '');
      const newPw = String(msg.next || '');
      const u = users[me];
      if (u.hash !== hash(oldPw, u.salt)) return send(sock, { type: 'error', message: '原密码错误' });
      if (newPw.length < 8 || newPw.length > 16 || !/[A-Za-z]/.test(newPw) || !/[0-9]/.test(newPw)) return send(sock, { type: 'error', message: '新密码需 8-16 位且同时包含字母和数字' });
      u.salt = newSalt();
      u.hash = hash(newPw, u.salt);
      scheduleSave();
      send(sock, { type: 'password_changed' });
      log('修改密码: ' + me);
      break;
    }

    case 'verify_account': {
      if (!me) return needAuth(sock);
      const u0 = users[me];
      if (String(msg.username || '').trim() !== me) return send(sock, { type: 'error', message: '用户名错误' });
      if (String(msg.nickname || '').trim() !== u0.nickname) return send(sock, { type: 'error', message: '用户昵称错误' });
      if (u0.hash !== hash(String(msg.password || ''), u0.salt)) return send(sock, { type: 'error', message: '用户密码错误' });
      send(sock, { type: 'account_verify_ok' });
      break;
    }

    case 'delete_account': {
      if (!me) return needAuth(sock);
      const u = users[me];
      if (String(msg.username || '').trim() !== me) return send(sock, { type: 'error', message: '用户名错误' });
      if (String(msg.nickname || '').trim() !== u.nickname) return send(sock, { type: 'error', message: '用户昵称错误' });
      if (u.hash !== hash(String(msg.password || ''), u.salt)) return send(sock, { type: 'error', message: '用户密码错误' });
      delete users[me];
      for (const k of Object.keys(users)) {
        const u = users[k];
        u.friends = (u.friends || []).filter(f => f !== me);
        u.pending = (u.pending || []).filter(f => f !== me);
        u.blocked = (u.blocked || []).filter(f => f !== me);
      }
      for (const id of Object.keys(groups)) {
        const g = groups[id];
        const idx = g.members.indexOf(me);
        if (idx >= 0) g.members.splice(idx, 1);
        if (g.owner === me || g.members.length === 0) delete groups[id];
      }
      scheduleSave();
      scheduleGroupSave();
      send(sock, { type: 'account_deleted' });
      delete sockets[me];
      log('注销账号: ' + me);
      try { sock.destroy(); } catch (e) { /* ignore */ }
      break;
    }

    case 'forward_group_message': {
      if (!me) return needAuth(sock);
      const g = getGroupFor(me, String(msg.group || ''));
      if (!g) return send(sock, { type: 'error', message: '群聊不存在或你已不在群中' });
      const id = String(msg.messageId || '');
      let src = messages.find(m => m.id === id);
      if (!src) {
        for (const gid of Object.keys(groupMessages)) {
          src = (groupMessages[gid] || []).find(m => m.id === id);
          if (src) break;
        }
      }
      if (!src) return send(sock, { type: 'error', message: '消息不存在或已被撤回' });
      const time = Date.now();
      const mObj = { id: crypto.randomBytes(8).toString('hex'), from: me, to: g.id, time, kind: src.kind || 'text', nickname: users[me] ? users[me].nickname : me };
      if (src.kind === 'text') {
        mObj.text = src.text || '';
        if (src.reply) mObj.reply = src.reply;
      } else {
        mObj.url = src.url;
        mObj.name = src.name;
        mObj.size = src.size;
      }
      if (!groupMessages[g.id]) groupMessages[g.id] = [];
      groupMessages[g.id].push(mObj);
      if (groupMessages[g.id].length > 10000) groupMessages[g.id] = groupMessages[g.id].slice(-10000);
      scheduleGroupSave();
      const obj = { type: 'new_group_message', id: mObj.id, from: me, group: g.id, text: mObj.text || '', time, nickname: mObj.nickname, kind: mObj.kind, senderHasAvatar: !!(users[me] && users[me].hasAvatar) };
      if (mObj.url) obj.url = mObj.url;
      if (mObj.name) obj.name = mObj.name;
      if (mObj.size) obj.size = mObj.size;
      if (mObj.reply) obj.reply = mObj.reply;
      pushToGroup(g, obj);
      for (const m of g.members) users[m].hidden = (users[m].hidden || []).filter(x => x !== g.id);
      scheduleSave();
      break;
    }

    case 'get_posts': {
      if (!me) return needAuth(sock);
      const mine = (users[me] && users[me].friends) || [];
      const visible = posts.filter(p => p.from === me || mine.indexOf(p.from) >= 0);
      const out = visible.slice().sort((a, b) => b.time - a.time).slice(0, 200).map(p => ({
        id: p.id,
        from: p.from,
        nickname: users[p.from] ? users[p.from].nickname : p.from,
        hasAvatar: !!(users[p.from] && users[p.from].hasAvatar),
        text: p.text || '',
        images: p.images || [],
        time: p.time,
        likes: p.likes || [],
        favorites: p.favorites || [],
        comments: p.comments || [],
        forwardFrom: p.forwardFrom || ''
      }));
      send(sock, { type: 'posts', posts: out });
      break;
    }

    case 'add_post': {
      if (!me) return needAuth(sock);
      const text = String(msg.text || '').slice(0, 500);
      const images = [];
      const dataArr = msg.images;
      if (dataArr && Array.isArray(dataArr)) {
        for (const data of dataArr.slice(0, 9)) {
          try {
            const buf = Buffer.from(String(data), 'base64');
            if (buf.length < 100 || buf.length > 900000) continue;
            const name = 'm_' + crypto.randomBytes(6).toString('hex') + '.jpg';
            fs.writeFileSync(path.join(DATA_DIR, 'files', name), buf);
            images.push('/files/' + name);
          } catch (e) { /* ignore */ }
        }
      }
      if (!text.trim() && images.length === 0) return send(sock, { type: 'error', message: '动态内容不能为空' });
      const pid = crypto.randomBytes(8).toString('hex');
      let forwardFrom = '';
      if (msg.forward && msg.forward === true) {
        const orig = posts.find(x => x.id === String(msg.forwardId || ''));
        if (orig) {
          forwardFrom = orig.from;
          if (!text.trim()) text = orig.text || '';
          if (images.length === 0) images = orig.images || [];
        }
      }
      posts.push({ id: pid, from: me, text, images, time: Date.now(), likes: [], favorites: [], comments: [], forwardFrom });
      if (posts.length > 2000) posts = posts.slice(-2000);
      scheduleGroupSave();
      send(sock, { type: 'post_added' });
      log('发布动态: ' + me);
      break;
    }

    case 'get_activities': {
      if (!me) return needAuth(sock);
      const out = activities.slice().sort((a, b) => b.time - a.time).slice(0, 200).map(a => ({
        id: a.id,
        from: a.from,
        nickname: a.from === 'system' ? 'Chatapp 官方' : (users[a.from] ? users[a.from].nickname : a.from),
        hasAvatar: !!(users[a.from] && users[a.from].hasAvatar),
        title: a.title || '',
        text: a.text || '',
        game: a.game || '',
        time: a.time,
        joiners: a.joiners || [],
        joined: (a.joiners || []).indexOf(me) >= 0
      }));
      send(sock, { type: 'activities', activities: out });
      break;
    }

    case 'add_activity': {
      if (!me) return needAuth(sock);
      send(sock, { type: 'error', message: '仅系统可发布活动' });
      break;
    }

    case 'join_activity': {
      if (!me) return needAuth(sock);
      const id = String(msg.id || '');
      const a = activities.find(x => x.id === id);
      if (!a) break;
      a.joiners = a.joiners || [];
      const idx = a.joiners.indexOf(me);
      if (idx >= 0) a.joiners.splice(idx, 1); else a.joiners.push(me);
      scheduleGroupSave();
      broadcast({ type: 'activities_changed' });
      break;
    }

    case 'remove_activity': {
      if (!me) return needAuth(sock);
      const id = String(msg.id || '');
      const idx = activities.findIndex(x => x.id === id && x.from === me);
      if (idx < 0) break;
      activities.splice(idx, 1);
      scheduleGroupSave();
      broadcast({ type: 'activities_changed' });
      break;
    }

    case 'favorite_post': {
      if (!me) return needAuth(sock);
      const id = String(msg.id || '');
      const p = posts.find(x => x.id === id);
      if (!p) break;
      p.favorites = p.favorites || [];
      const idx = p.favorites.indexOf(me);
      if (idx >= 0) p.favorites.splice(idx, 1); else p.favorites.push(me);
      scheduleGroupSave();
      send(sock, { type: 'posts_changed' });
      break;
    }

    case 'add_comment': {
      if (!me) return needAuth(sock);
      const id = String(msg.id || '');
      const p = posts.find(x => x.id === id);
      if (!p) break;
      const text = String(msg.text || '').trim().slice(0, 200);
      if (!text) break;
      p.comments = p.comments || [];
      p.comments.push({ from: me, nickname: users[me] ? users[me].nickname : me, text, time: Date.now() });
      scheduleGroupSave();
      send(sock, { type: 'posts_changed' });
      break;
    }

    case 'repost_post': {
      if (!me) return needAuth(sock);
      const id = String(msg.id || '');
      const orig = posts.find(x => x.id === id);
      if (!orig) break;
      posts.push({ id: crypto.randomBytes(8).toString('hex'), from: me, text: orig.text || '', images: (orig.images || []).slice(), time: Date.now(), likes: [], favorites: [], comments: [], forwardFrom: orig.from });
      if (posts.length > 2000) posts = posts.slice(-2000);
      scheduleGroupSave();
      send(sock, { type: 'post_added' });
      break;
    }

    case 'delete_post': {
      if (!me) return needAuth(sock);
      const id = String(msg.id || '');
      const idx = posts.findIndex(x => x.id === id && x.from === me);
      if (idx >= 0) posts.splice(idx, 1);
      scheduleGroupSave();
      send(sock, { type: 'posts_changed' });
      break;
    }

    case 'get_my_favorites': {
      if (!me) return needAuth(sock);
      const out = posts.filter(p => (p.favorites || []).indexOf(me) >= 0)
        .sort((a, b) => b.time - a.time)
        .slice(0, 200)
        .map(p => ({
          id: p.id, from: p.from, nickname: users[p.from] ? users[p.from].nickname : p.from,
          hasAvatar: !!(users[p.from] && users[p.from].hasAvatar),
          text: p.text || '', images: p.images || [], time: p.time,
          likes: p.likes || [], favorites: p.favorites || [], comments: p.comments || [], forwardFrom: p.forwardFrom || ''
        }));
      send(sock, { type: 'my_favorites', posts: out });
      break;
    }

    case 'like_post': {
      if (!me) return needAuth(sock);
      const id = String(msg.id || '');
      const p = posts.find(x => x.id === id);
      if (!p) break;
      p.likes = p.likes || [];
      const idx = p.likes.indexOf(me);
      if (idx >= 0) p.likes.splice(idx, 1); else p.likes.push(me);
      scheduleGroupSave();
      send(sock, { type: 'posts_changed' });
      break;
    }

    case 'hide_message': {
      if (!me) return needAuth(sock);
      const id = String(msg.id || '');
      if (!id) break;
      users[me].hiddenMessages = users[me].hiddenMessages || [];
      if (users[me].hiddenMessages.indexOf(id) < 0) users[me].hiddenMessages.push(id);
      scheduleSave();
      send(sock, { type: 'conversations_changed' });
      break;
    }

    case 'typing': {
      if (!me) return needAuth(sock);
      const t = String(msg.with || '').trim();
      if (t.indexOf('g') === 0 && groups[t] && groups[t].members.includes(me)) {
        for (const m of groups[t].members) {
          if (m !== me && online(m)) send(sockets[m], { type: 'peer_typing', from: me });
        }
      } else if (online(t)) {
        send(sockets[t], { type: 'peer_typing', from: me });
      }
      break;
    }

    case 'set_status': {
      if (!me) return needAuth(sock);
      const status = String(msg.status || '').trim();
      if (!status || status.length > 50) {
        return send(sock, { type: 'error', message: '文案不能为空且不超过 50 字' });
      }
      users[me].status = status;
      scheduleSave();
      send(sock, { type: 'status_updated', status: status });
      notifyContactsChanged(me);
      break;
    }

    case 'recall_message': {
      if (!me) return needAuth(sock);
      const id = String(msg.id || '');
      const idx = messages.findIndex(m => m.id === id);
      if (idx < 0) return send(sock, { type: 'error', message: '消息不存在或已被撤回' });
      const m = messages[idx];
      if (m.from !== me) return send(sock, { type: 'error', message: '只能撤回自己的消息' });
      if (Date.now() - m.time > 120000) return send(sock, { type: 'error', message: '超过两分钟，无法撤回' });
      messages.splice(idx, 1);
      scheduleSave();
      const evt = { type: 'message_recalled', id: id };
      send(sock, evt);
      if (online(m.to)) send(sockets[m.to], evt);
      send(sock, { type: 'conversations_changed' });
      if (online(m.to)) send(sockets[m.to], { type: 'conversations_changed' });
      log('撤回消息: ' + me + ' -> ' + m.to);
      break;
    }

    case 'set_avatar': {
      if (!me) return needAuth(sock);
      const data = String(msg.data || '');
      if (data.length < 100 || data.length > 800000) {
        return send(sock, { type: 'error', message: '头像数据不合法' });
      }
      try {
        const buf = Buffer.from(data, 'base64');
        if (buf.length < 100 || buf.length > 600000) {
          return send(sock, { type: 'error', message: '头像数据不合法' });
        }
        const dir = path.join(DATA_DIR, 'avatars');
        try { fs.mkdirSync(dir, { recursive: true }); } catch (e) { /* ignore */ }
        fs.writeFileSync(path.join(dir, me + '.jpg'), buf);
        users[me].hasAvatar = true;
        scheduleSave();
        send(sock, { type: 'avatar_updated', ok: true });
        notifyContactsChanged(me);
        log('头像已更新: ' + me);
      } catch (e) {
        send(sock, { type: 'error', message: '头像保存失败' });
      }
      break;
    }

    // ================= 群聊协议 =================

    case 'get_groups': {
      if (!me) return needAuth(sock);
      send(sock, { type: 'groups', groups: myGroups(me) });
      break;
    }

    case 'create_group': {
      if (!me) return needAuth(sock);
      const name = String(msg.name || '').trim();
      if (!name || name.length > 24) {
        return send(sock, { type: 'error', message: '群名称不能为空且不超过 24 字' });
      }
      if (findGroupByName(name)) {
        return send(sock, { type: 'error', message: '已存在同名群聊' });
      }
      const id = 'g' + crypto.randomBytes(6).toString('hex');
      groups[id] = {
        id, name, owner: me, notice: '', members: [me],
        hasAvatar: false, createdAt: Date.now()
      };
      groupMessages[id] = [];
      scheduleGroupSave();
      send(sock, { type: 'group_created', group: groupObj(groups[id]) });
      send(sock, { type: 'groups', groups: myGroups(me) });
      log('创建群聊: ' + me + ' 创建了 ' + name + ' (' + id + ')');
      break;
    }

    case 'set_group_info': {
      if (!me) return needAuth(sock);
      const g = getGroupFor(me, String(msg.id || ''));
      if (!g) return send(sock, { type: 'error', message: '群聊不存在或你已不在群中' });
      if (g.owner !== me) return send(sock, { type: 'error', message: '仅群主可以修改群资料' });
      const name = String(msg.name || '').trim();
      if (name) {
        if (name.length > 24) return send(sock, { type: 'error', message: '群名称不超过 24 字' });
        const other = findGroupByName(name);
        if (other && other.id !== g.id) return send(sock, { type: 'error', message: '已存在同名群聊' });
        g.name = name;
      }
      const avatarData = String(msg.avatarData || '');
      if (avatarData.length > 100) {
        try {
          const buf = Buffer.from(avatarData, 'base64');
          if (buf.length >= 100 && buf.length <= 600000) {
            const dir = path.join(DATA_DIR, 'avatars');
            try { fs.mkdirSync(dir, { recursive: true }); } catch (e) { /* ignore */ }
            fs.writeFileSync(path.join(dir, 'g_' + g.id + '.jpg'), buf);
            g.hasAvatar = true;
          }
        } catch (e) { /* ignore */ }
      }
      scheduleGroupSave();
      pushGroupChanged(g);
      send(sock, { type: 'group_updated', group: groupObj(g) });
      log('修改群资料: ' + g.id);
      break;
    }

    case 'set_group_notice': {
      if (!me) return needAuth(sock);
      const g = getGroupFor(me, String(msg.id || ''));
      if (!g) return send(sock, { type: 'error', message: '群聊不存在或你已不在群中' });
      if (g.owner !== me) return send(sock, { type: 'error', message: '仅群主可以编辑群公告' });
      const notice = String(msg.notice || '').trim().slice(0, 200);
      g.notice = notice;
      scheduleGroupSave();
      pushGroupChanged(g);
      send(sock, { type: 'group_updated', group: groupObj(g) });
      log('编辑群公告: ' + g.id);
      break;
    }

    case 'invite_group': {
      if (!me) return needAuth(sock);
      const g = getGroupFor(me, String(msg.id || ''));
      if (!g) return send(sock, { type: 'error', message: '群聊不存在或你已不在群中' });
      if (g.owner !== me) return send(sock, { type: 'error', message: '仅群主可以邀请成员' });
      const target = String(msg.username || '').trim();
      if (!users[target]) return send(sock, { type: 'error', message: '用户不存在' });
      if (!isFriend(me, target)) return send(sock, { type: 'error', message: '只能邀请你的好友进群' });
      if (g.members.includes(target)) return send(sock, { type: 'error', message: '该用户已在群中' });
      g.members.push(target);
      scheduleGroupSave();
      pushGroupChanged(g);
      send(sock, { type: 'group_updated', group: groupObj(g) });
      send(sock, { type: 'groups', groups: myGroups(me) });
      if (online(target)) {
        send(sockets[target], { type: 'group_invited', group: groupObj(g), by: me });
        send(sockets[target], { type: 'groups', groups: myGroups(target) });
      }
      log('群主邀请: ' + me + ' 邀请 ' + target + ' 加入 ' + g.id);
      break;
    }

    case 'kick_group': {
      if (!me) return needAuth(sock);
      const g = getGroupFor(me, String(msg.id || ''));
      if (!g) return send(sock, { type: 'error', message: '群聊不存在或你已不在群中' });
      if (g.owner !== me) return send(sock, { type: 'error', message: '仅群主可以踢人' });
      const target = String(msg.username || '').trim();
      if (target === me) return send(sock, { type: 'error', message: '群主不能踢自己' });
      const idx = g.members.indexOf(target);
      if (idx < 0) return send(sock, { type: 'error', message: '该用户不在群中' });
      g.members.splice(idx, 1);
      scheduleGroupSave();
      pushGroupChanged(g);
      send(sock, { type: 'group_updated', group: groupObj(g) });
      if (online(target)) {
        send(sockets[target], { type: 'group_kicked', group: groupObj(g), by: me });
        send(sockets[target], { type: 'groups', groups: myGroups(target) });
      }
      log('群主踢人: ' + me + ' 踢出 ' + target + ' 于 ' + g.id);
      break;
    }

    case 'transfer_owner': {
      if (!me) return needAuth(sock);
      const g = getGroupFor(me, String(msg.id || ''));
      if (!g) return send(sock, { type: 'error', message: '群聊不存在或你已不在群中' });
      if (g.owner !== me) return send(sock, { type: 'error', message: '仅群主可以转让群主' });
      const target = String(msg.username || '').trim();
      if (!g.members.includes(target)) return send(sock, { type: 'error', message: '对方不在群中' });
      if (target === me) return send(sock, { type: 'error', message: '你已经是群主' });
      g.owner = target;
      scheduleGroupSave();
      pushGroupChanged(g);
      send(sock, { type: 'group_updated', group: groupObj(g) });
      log('转让群主: ' + g.id + ' -> ' + target);
      break;
    }

    case 'leave_group': {
      if (!me) return needAuth(sock);
      const g = getGroupFor(me, String(msg.id || ''));
      if (!g) return send(sock, { type: 'error', message: '群聊不存在或你已不在群中' });
      if (g.owner === me) return send(sock, { type: 'error', message: '群主不能退出，请先转让群主或解散群聊' });
      const idx = g.members.indexOf(me);
      if (idx >= 0) g.members.splice(idx, 1);
      scheduleGroupSave();
      pushGroupChanged(g);
      send(sock, { type: 'group_left', group: groupObj(g) });
      send(sock, { type: 'groups', groups: myGroups(me) });
      log('成员退群: ' + me + ' 退出 ' + g.id);
      break;
    }

    case 'dissolve_group': {
      if (!me) return needAuth(sock);
      const g = getGroupFor(me, String(msg.id || ''));
      if (!g) return send(sock, { type: 'error', message: '群聊不存在或你已不在群中' });
      if (g.owner !== me) return send(sock, { type: 'error', message: '仅群主可以解散群聊' });
      const evt = { type: 'group_dissolved', group: groupObj(g) };
      for (const m of g.members) {
        if (online(m)) {
          send(sockets[m], evt);
          send(sockets[m], { type: 'groups', groups: myGroups(m) });
        }
      }
      delete groups[g.id];
      delete groupMessages[g.id];
      scheduleGroupSave();
      send(sock, { type: 'group_dissolved_ok', id: g.id });
      log('解散群聊: ' + me + ' 解散 ' + g.id);
      break;
    }

    case 'get_group_members': {
      if (!me) return needAuth(sock);
      const g = getGroupFor(me, String(msg.id || ''));
      if (!g) return send(sock, { type: 'error', message: '群聊不存在或你已不在群中' });
      const members = g.members.map(m => ({
        username: m,
        nickname: users[m] ? users[m].nickname : m,
        hasAvatar: !!(users[m] && users[m].hasAvatar),
        online: online(m),
        isOwner: m === g.owner
      }));
      send(sock, { type: 'group_members', id: g.id, members });
      break;
    }

    case 'get_group_history': {
      if (!me) return needAuth(sock);
      const g = getGroupFor(me, String(msg.id || ''));
      if (!g) return send(sock, { type: 'error', message: '群聊不存在或你已不在群中' });
      const list = (groupMessages[g.id] || []).slice(-300).map(m => {
        const o = { id: m.id, from: m.from, to: g.id, text: m.text || '', time: m.time, nickname: m.nickname || (users[m.from] ? users[m.from].nickname : m.from), senderHasAvatar: !!(users[m.from] && users[m.from].hasAvatar) };
        if (m.kind) o.kind = m.kind;
        if (m.url) o.url = m.url;
        if (m.name) o.name = m.name;
        if (m.size) o.size = m.size;
        if (m.reply) o.reply = m.reply;
        return o;
      });
      send(sock, { type: 'group_history', id: g.id, messages: list });
      break;
    }

    case 'send_group_message': {
      if (!me) return needAuth(sock);
      const g = getGroupFor(me, String(msg.id || ''));
      if (!g) return send(sock, { type: 'error', message: '群聊不存在或你已不在群中' });
      const text = String(msg.text || '');
      const kind = String(msg.kind || 'text');
      if (kind === 'text' && !text.trim()) return;
      const atAll = (kind === 'text' && text.indexOf('@所有人') >= 0);
      const time = Date.now();
      const mObj = {
        id: crypto.randomBytes(8).toString('hex'), from: me, to: g.id, time, kind,
        nickname: users[me] ? users[me].nickname : me
      };
      if (kind === 'text') {
        mObj.text = text;
        const replyText = (msg.reply && String(msg.reply.text || '').trim()) ? String(msg.reply.text).slice(0, 50) : '';
        if (replyText) mObj.reply = replyText;
      } else if (kind === 'image' || kind === 'file') {
        const data = String(msg.data || '');
        const name = String(msg.name || '').slice(0, 80);
        const limit = kind === 'image' ? 2500000 : 5000000;
        if (data.length < 16 || data.length > limit * 1.4) {
          return send(sock, { type: 'error', message: '文件数据不合法或过大' });
        }
        const buf = Buffer.from(data, 'base64');
        if (buf.length < 1 || buf.length > limit) {
          return send(sock, { type: 'error', message: '文件数据不合法或过大' });
        }
        const ext = kind === 'image' ? '.jpg' : (String(name).match(/\.[A-Za-z0-9]{1,10}$/) || ['.bin'])[0];
        const fname = mObj.id + ext;
        const fdir = path.join(DATA_DIR, 'files');
        try { fs.mkdirSync(fdir, { recursive: true }); } catch (e) { /* ignore */ }
        fs.writeFileSync(path.join(fdir, fname), buf);
        mObj.url = '/files/' + fname;
        mObj.name = name || fname;
        mObj.size = buf.length;
      }
      if (!groupMessages[g.id]) groupMessages[g.id] = [];
      groupMessages[g.id].push(mObj);
      if (groupMessages[g.id].length > 10000) groupMessages[g.id] = groupMessages[g.id].slice(-10000);
      scheduleGroupSave();
      const obj = { type: 'new_group_message', id: mObj.id, from: me, group: g.id, text: mObj.text || '', time, nickname: mObj.nickname, kind: kind, senderHasAvatar: !!(users[me] && users[me].hasAvatar) };
      if (mObj.url) obj.url = mObj.url;
      if (mObj.name) obj.name = mObj.name;
      if (mObj.size) obj.size = mObj.size;
      if (mObj.reply) obj.reply = mObj.reply;
      pushToGroup(g, obj);
      if (atAll) pushToGroup(g, { type: 'group_at_all', group: g.id, from: me, nickname: users[me] ? users[me].nickname : me });
      for (const m of g.members) {
        users[m].hidden = (users[m].hidden || []).filter(x => x !== g.id);
      }
      scheduleSave();
      break;
    }

    case 'mark_group_read': {
      if (!me) return needAuth(sock);
      const g = getGroupFor(me, String(msg.id || ''));
      if (!g) break;
      users[me].lastReadGroup = users[me].lastReadGroup || {};
      users[me].lastReadGroup[g.id] = Date.now();
      scheduleSave();
      break;
    }

    case 'clear_group_history': {
      if (!me) return needAuth(sock);
      const g = getGroupFor(me, String(msg.id || ''));
      if (!g) return send(sock, { type: 'error', message: '群聊不存在或你已不在群中' });
      groupMessages[g.id] = [];
      scheduleGroupSave();
      pushToGroup(g, { type: 'group_history_cleared', id: g.id });
      send(sock, { type: 'group_history_cleared', id: g.id });
      log('清空群聊天记录: ' + g.id + ' by ' + me);
      break;
    }

    case 'hide_conversation': {
      if (!me) return needAuth(sock);
      const key = String(msg.with || '').trim();
      if (!key) break;
      users[me].hidden = users[me].hidden || [];
      if (users[me].hidden.indexOf(key) < 0) users[me].hidden.push(key);
      scheduleSave();
      send(sock, { type: 'conversations_changed' });
      break;
    }

    case 'get_profile': {
      if (!me) return needAuth(sock);
      send(sock, { type: 'profile', profile: users[me].profile || {}, nickname: users[me].nickname, username: me, hasAvatar: !!users[me].hasAvatar });
      break;
    }

    case 'get_user_profile': {
      if (!me) return needAuth(sock);
      const target = String(msg.username || '').trim();
      const tu = users[target];
      if (!tu) return send(sock, { type: 'error', message: '用户不存在' });
      send(sock, { type: 'user_profile', username: target, nickname: tu.nickname, hasAvatar: !!tu.hasAvatar, status: tu.status || '', profile: tu.profile || {} });
      break;
    }

    case 'set_profile': {
      if (!me) return needAuth(sock);
      const p = users[me].profile = users[me].profile || {};
      const fields = ['gender', 'birthday', 'job', 'company', 'location', 'birthplace', 'email'];
      for (const f of fields) {
        if (msg[f] !== undefined) p[f] = String(msg[f]).slice(0, 80);
      }
      if (msg.tags !== undefined) {
        let tags = [];
        try {
          if (Array.isArray(msg.tags)) tags = msg.tags.slice(0, 12).map(t => String(t).slice(0, 20));
        } catch (e) { /* ignore */ }
        p.tags = tags;
      }
      scheduleSave();
      send(sock, { type: 'profile_updated', profile: p });
      break;
    }

    case 'add_profile_photo': {
      if (!me) return needAuth(sock);
      const data = String(msg.data || '');
      if (data.length < 100 || data.length > 1400000) {
        return send(sock, { type: 'error', message: '图片数据不合法' });
      }
      try {
        const buf = Buffer.from(data, 'base64');
        if (buf.length < 100 || buf.length > 900000) {
          return send(sock, { type: 'error', message: '图片数据不合法' });
        }
        const p = users[me].profile = users[me].profile || {};
        p.photos = p.photos || [];
        if (p.photos.length >= 9) return send(sock, { type: 'toast', message: '最多9张照片' });
        const name = 'p_' + crypto.randomBytes(6).toString('hex') + '.jpg';
        fs.writeFileSync(path.join(DATA_DIR, 'files', name), buf);
        p.photos.push('/files/' + name);
        scheduleSave();
        send(sock, { type: 'profile_updated', profile: p });
        log('添加名片照片: ' + me);
      } catch (e) {
        send(sock, { type: 'error', message: '照片保存失败' });
      }
      break;
    }

    case 'remove_profile_photo': {
      if (!me) return needAuth(sock);
      const idx = parseInt(msg.index, 10);
      const p = users[me].profile = users[me].profile || {};
      p.photos = p.photos || [];
      if (idx >= 0 && idx < p.photos.length) {
        p.photos.splice(idx, 1);
        scheduleSave();
        send(sock, { type: 'profile_updated', profile: p });
      }
      break;
    }

    case 'forward_message': {
      if (!me) return needAuth(sock);
      const to = String(msg.to || '').trim();
      if (!users[to]) return send(sock, { type: 'toast', message: '用户不存在' });
      if (!isFriend(me, to)) return send(sock, { type: 'toast', message: '还不是好友，先添加好友吧' });
      if (isBlocked(to, me)) return send(sock, { type: 'toast', message: '消息发送失败：对方已将你加入黑名单' });
      const id = String(msg.id || '');
      let src = messages.find(m => m.id === id);
      if (!src) {
        for (const gid of Object.keys(groupMessages)) {
          src = (groupMessages[gid] || []).find(m => m.id === id);
          if (src) break;
        }
      }
      if (!src) return send(sock, { type: 'error', message: '消息不存在或已被撤回' });
      const time = Date.now();
      const mObj = { id: crypto.randomBytes(8).toString('hex'), from: me, to, time, kind: src.kind || 'text' };
      if (src.kind === 'text') {
        mObj.text = src.text || '';
        if (src.reply) mObj.reply = src.reply;
      } else {
        mObj.url = src.url;
        mObj.name = src.name;
        mObj.size = src.size;
      }
      messages.push(mObj);
      if (messages.length > 20000) messages = messages.slice(-20000);
      users[me].hidden = (users[me].hidden || []).filter(x => x !== to);
      if (users[to]) users[to].hidden = (users[to].hidden || []).filter(x => x !== me);
      scheduleSave();
      const obj = { type: 'new_message', id: mObj.id, from: me, to, text: mObj.text || '', time, nickname: users[me].nickname, kind: mObj.kind };
      if (mObj.url) obj.url = mObj.url;
      if (mObj.name) obj.name = mObj.name;
      if (mObj.size) obj.size = mObj.size;
      if (mObj.reply) obj.reply = mObj.reply;
      send(sock, obj);
      if (online(to)) send(sockets[to], obj);
      break;
    }

    case 'recall_group_message': {
      if (!me) return needAuth(sock);
      const g = getGroupFor(me, String(msg.id || ''));
      if (!g) return send(sock, { type: 'error', message: '群聊不存在或你已不在群中' });
      const id = String(msg.messageId || '');
      const arr = groupMessages[g.id] || [];
      const idx = arr.findIndex(m => m.id === id);
      if (idx < 0) return send(sock, { type: 'error', message: '消息不存在或已被撤回' });
      const m = arr[idx];
      if (m.from !== me) return send(sock, { type: 'error', message: '只能撤回自己的消息' });
      if (Date.now() - m.time > 120000) return send(sock, { type: 'error', message: '超过两分钟，无法撤回' });
      arr.splice(idx, 1);
      scheduleGroupSave();
      pushToGroup(g, { type: 'group_message_recalled', group: g.id, id: id });
      log('撤回群消息: ' + me + ' @ ' + g.id);
      break;
    }

    case 'crash': {
      log('手机端崩溃上报: ' + String(msg.stack || '').replace(/\n/g, ' | ').slice(0, 2000));
      break;
    }

    default:
      log('未知指令: ' + type + ' 来自 ' + (sock.user || '未登录'));
      send(sock, { type: 'error', message: '未知指令: ' + type });
  }
}

const server = net.createServer(sock => {
  sock.connectedAt = Date.now();
  const remote = sock.remoteAddress || 'unknown';
  log('新连接来自 ' + remote);
  sock.setKeepAlive(true, 5000);
  sock.setNoDelay(true);
  let buf = Buffer.alloc(0);

  sock.on('data', chunk => {
    if (VERBOSE_IO_LOG) {
      log('收到数据 ' + chunk.length + 'B: ' + chunk.toString('utf8').replace(/\r/g, '\\r').replace(/\n/g, '\\n').slice(0, 200));
    }
    buf = Buffer.concat([buf, chunk]);
    let idx;
    while ((idx = buf.indexOf(0x0a)) >= 0) {
      const line = buf.slice(0, idx).toString('utf8').trim();
      buf = buf.slice(idx + 1);
      if (line) {
        try { handle(sock, line); } catch (e) { log('处理消息出错: ' + e.stack); }
      }
    }
    if (buf.length > 1024 * 1024) buf = Buffer.alloc(0);
  });

  sock.on('close', () => {
    const dur = sock.connectedAt ? (Date.now() - sock.connectedAt) + 'ms' : '?ms';
    if (sock.user && sockets[sock.user] === sock) {
      delete sockets[sock.user];
      notifyContactsChanged(sock.user);
    }
    if (buf.length > 0) log('关闭时仍有未处理数据: ' + buf.toString('utf8').slice(0, 200));
    log('连接关闭 ' + remote + ' (持续 ' + dur + (sock.user ? ', 用户=' + sock.user : '') + ')');
  });
  sock.on('error', e => {
    log('连接错误 ' + remote + ': ' + e.message);
  });
});

server.on('error', e => {
  if (e.code === 'EADDRINUSE') {
    console.error('端口 ' + PORT + ' 已被占用！服务器可能已经在运行。');
    console.error('请关闭其他 start-server.bat 窗口后重新启动。');
    try { fs.appendFileSync(LOG_FILE, '[' + new Date().toLocaleString() + '] 启动失败: 端口 ' + PORT + ' 被占用\n'); } catch (err) { /* ignore */ }
    process.exit(1);
  }
  log('服务器错误: ' + e.stack);
});

load();
loadGroups();
seedActivities();
try { fs.mkdirSync(path.join(DATA_DIR, 'avatars'), { recursive: true }); } catch (e) { /* ignore */ }
try { fs.mkdirSync(path.join(DATA_DIR, 'files'), { recursive: true }); } catch (e) { /* ignore */ }

// ============ update service (HTTP) ============
// keep version in sync with the APK versionName
const http = require('http');
const os = require('os');
const UPDATE_PORT = parseInt(process.env.UPDATE_PORT || '8900', 10);

const updateSrv = http.createServer((req, res) => {
  const url = String(req.url || '').split('?')[0];
  if (url.indexOf('/avatar/') === 0) {
    const name = decodeURIComponent(url.replace('/avatar/', '').replace(/\.jpg$/, ''));
    const file = path.join(DATA_DIR, 'avatars', name + '.jpg');
    if (fs.existsSync(file)) {
      const stat = fs.statSync(file);
      res.writeHead(200, { 'Content-Type': 'image/jpeg', 'Content-Length': stat.size });
      fs.createReadStream(file).pipe(res);
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('not found');
    }
    return;
  }

  if (url.indexOf('/files/') === 0) {
    const fname = decodeURIComponent(url.replace('/files/', ''));
    if (fname.indexOf('..') >= 0) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      return res.end('not found');
    }
    const file = path.join(DATA_DIR, 'files', fname);
    if (fs.existsSync(file)) {
      const stat = fs.statSync(file);
      res.writeHead(200, { 'Content-Type': 'application/octet-stream', 'Content-Length': stat.size });
      fs.createReadStream(file).pipe(res);
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('not found');
    }
    return;
  }
  if (url === '/version.json') {
    const apk = path.join(DATA_DIR, 'Chatapp.apk');
    let size = 0;
    try { size = fs.statSync(apk).size; } catch (e) { size = 0; }
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify({ version: '1.0.2', file: 'Chatapp.apk', size: size }));
  } else if (url === '/Chatapp.apk') {
    const apk = path.join(DATA_DIR, 'Chatapp.apk');
    if (fs.existsSync(apk)) {
      const stat = fs.statSync(apk);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': stat.size
      });
      fs.createReadStream(apk).pipe(res);
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('apk not found');
    }
  } else {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('not found');
  }
}).listen(UPDATE_PORT, () => {
  log('update service on port ' + UPDATE_PORT);
});
updateSrv.on('error', e => {
  if (e.code === 'EADDRINUSE') {
    console.error('更新服务端口 ' + UPDATE_PORT + ' 已被占用！服务器可能已经在运行。');
    try { fs.appendFileSync(LOG_FILE, '[' + new Date().toLocaleString() + '] 启动失败: 更新端口 ' + UPDATE_PORT + ' 被占用\n'); } catch (err) { /* ignore */ }
    process.exit(1);
  }
  log('更新服务错误: ' + e.stack);
});
server.listen(PORT, () => {
  const nets = os.networkInterfaces();
  for (const n of Object.keys(nets)) {
    for (const info of nets[n]) {
      if (info.family === 'IPv4' && !info.internal) log('  本机IPv4: ' + info.address);
    }
  }
  log('==========================================');
  log('  Chatapp 服务器已启动  端口: ' + PORT);
  log('  数据目录: ' + DATA_DIR);
  log('  日志文件: ' + LOG_FILE);
  log('  手机端登录页填写: 电脑IP:' + PORT + '  (ipconfig 查看 IPv4 地址)');
  log('==========================================');
});
