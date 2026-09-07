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

const S = ROOT + '/server/server.js';
patch(S, [
  // 已读回执: mark_read 后推送
  ['    case \'mark_read\': {\n      if (!me) return needAuth(sock);\n      const peer = String(msg.with || \'\').trim();\n      if (!users[me] || !users[peer]) break;\n      users[me].lastRead[peer] = Date.now();\n      scheduleSave();\n      break;\n    }',
   '    case \'mark_read\': {\n      if (!me) return needAuth(sock);\n      const peer = String(msg.with || \'\').trim();\n      if (!users[me] || !users[peer]) break;\n      users[me].lastRead[peer] = Date.now();\n      scheduleSave();\n      if (online(peer)) send(sockets[peer], { type: \'peer_read\', with: me, time: users[me].lastRead[peer] });\n      break;\n    }'],
  // 正在输入
  ['    case \'set_status\': {',
   '    case \'typing\': {\n      if (!me) return needAuth(sock);\n      const peer = String(msg.with || \'\').trim();\n      if (online(peer)) send(sockets[peer], { type: \'peer_typing\', from: me });\n      break;\n    }\n\n    case \'set_status\': {'],
  // 历史带已读时间
  ['      send(sock, { type: \'history\', with: peer, messages: list });\n      break;\n    }',
   '      send(sock, { type: \'history\', with: peer, messages: list, peerReadTime: users[peer].lastRead[me] || 0, myReadTime: users[me].lastRead[peer] || 0 });\n      break;\n    }'],
]);
console.log('DONE');
