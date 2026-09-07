'use strict';
const fs = require('fs');
const ROOT = 'C:/Users/cheng/Documents/Codex/Chatapp';

function patch(file, pairs) {
  let t = fs.readFileSync(file, 'utf8');
  let ok = 0, miss = [];
  for (const [old, neu] of pairs) {
    if (t.includes(old)) { t = t.split(old).join(neu); ok++; }
    else miss.push(old.slice(0, 70));
  }
  fs.writeFileSync(file, t);
  console.log('[' + file.split('/').pop() + '] ' + ok + ' patched' + (miss.length ? '  MISS: ' + miss.join(' | ') : ''));
}

// 服务器: 群消息/群历史带发送者头像标记
patch(ROOT + '/server/server.js', [
  ['      const obj = { type: \'new_group_message\', id: mObj.id, from: me, group: g.id, text: mObj.text || \'\', time, nickname: mObj.nickname, kind: kind };',
   '      const obj = { type: \'new_group_message\', id: mObj.id, from: me, group: g.id, text: mObj.text || \'\', time, nickname: mObj.nickname, kind: kind, senderHasAvatar: !!(users[me] && users[me].hasAvatar) };'],
  ['      const list = (groupMessages[g.id] || []).slice(-300).map(m => {\n        const o = { id: m.id, from: m.from, to: g.id, text: m.text || \'\', time: m.time, nickname: m.nickname || (users[m.from] ? users[m.from].nickname : m.from) };',
   '      const list = (groupMessages[g.id] || []).slice(-300).map(m => {\n        const o = { id: m.id, from: m.from, to: g.id, text: m.text || \'\', time: m.time, nickname: m.nickname || (users[m.from] ? users[m.from].nickname : m.from), senderHasAvatar: !!(users[m.from] && users[m.from].hasAvatar) };'],
]);

// 模型: senderHasAvatar
patch(ROOT + '/android/src/com/chatapp/app/Models.java', [
  ['        public String nickname = "";', '        public String nickname = "";\n        public boolean senderHasAvatar;'],
  ['            c.nickname = o.optString("nickname");', '            c.nickname = o.optString("nickname");\n            c.senderHasAvatar = o.optBoolean("senderHasAvatar");'],
]);
console.log('DONE');
