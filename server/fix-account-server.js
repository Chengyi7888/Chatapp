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
  // verify_account + delete_account 验证昵称/用户名/密码
  ['    case \'delete_account\': {\n      if (!me) return needAuth(sock);\n      delete users[me];',
   '    case \'verify_account\': {\n      if (!me) return needAuth(sock);\n      const u0 = users[me];\n      if (String(msg.username || \'\').trim() !== me) return send(sock, { type: \'error\', message: \'用户名错误\' });\n      if (String(msg.nickname || \'\').trim() !== u0.nickname) return send(sock, { type: \'error\', message: \'用户昵称错误\' });\n      if (u0.hash !== hash(String(msg.password || \'\'), u0.salt)) return send(sock, { type: \'error\', message: \'用户密码错误\' });\n      send(sock, { type: \'account_verify_ok\' });\n      break;\n    }\n\n    case \'delete_account\': {\n      if (!me) return needAuth(sock);\n      const u = users[me];\n      if (String(msg.username || \'\').trim() !== me) return send(sock, { type: \'error\', message: \'用户名错误\' });\n      if (String(msg.nickname || \'\').trim() !== u.nickname) return send(sock, { type: \'error\', message: \'用户昵称错误\' });\n      if (u.hash !== hash(String(msg.password || \'\'), u.salt)) return send(sock, { type: \'error\', message: \'用户密码错误\' });\n      delete users[me];'],
  // 服务器消息映射
  ['        server("动态内容不能为空", "srv_moment_empty");',
   '        server("动态内容不能为空", "srv_moment_empty");\n        server("用户名错误", "srv_username_wrong");\n        server("用户昵称错误", "srv_nickname_wrong");\n        server("用户密码错误", "srv_password_wrong");'],
  ['        put("srv_moment_empty", "动态内容不能为空", "動態內容不能為空", "Content cannot be empty");',
   '        put("srv_moment_empty", "动态内容不能为空", "動態內容不能為空", "Content cannot be empty");\n        put("srv_username_wrong", "用户名错误", "用戶名錯誤", "Username is wrong");\n        put("srv_nickname_wrong", "用户昵称错误", "用戶暱稱錯誤", "Nickname is wrong");\n        put("srv_password_wrong", "用户密码错误", "用戶密碼錯誤", "Password is wrong");'],
]);
console.log('DONE');
