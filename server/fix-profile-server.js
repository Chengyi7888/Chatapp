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
  // load: 补 profile
  ['    u.hidden = u.hidden || [];\n    u.lastReadGroup = u.lastReadGroup || {};',
   '    u.hidden = u.hidden || [];\n    u.lastReadGroup = u.lastReadGroup || {};\n    u.profile = u.profile || { gender: \'\', birthday: \'\', tags: [], photos: [], job: \'\', company: \'\', location: \'\', birthplace: \'\', email: \'\' };'],
  // 注册默认 profile
  ['        friends: [], pending: [], lastRead: {}, blocked: [], hidden: [], lastReadGroup: {}, createdAt: Date.now(),',
   '        friends: [], pending: [], lastRead: {}, blocked: [], hidden: [], lastReadGroup: {}, createdAt: Date.now(), profile: { gender: \'\', birthday: \'\', tags: [], photos: [], job: \'\', company: \'\', location: \'\', birthplace: \'\', email: \'\' },'],
  // 协议: 在 hide_conversation 之后插入个人名片协议
  ['      send(sock, { type: \'conversations_changed\' });\n      break;\n    }\n\n    case \'forward_message\': {',
   '      send(sock, { type: \'conversations_changed\' });\n      break;\n    }\n\n    case \'get_profile\': {\n      if (!me) return needAuth(sock);\n      send(sock, { type: \'profile\', profile: users[me].profile || {}, nickname: users[me].nickname, username: me, hasAvatar: !!users[me].hasAvatar });\n      break;\n    }\n\n    case \'set_profile\': {\n      if (!me) return needAuth(sock);\n      const p = users[me].profile = users[me].profile || {};\n      const fields = [\'gender\', \'birthday\', \'job\', \'company\', \'location\', \'birthplace\', \'email\'];\n      for (const f of fields) {\n        if (msg[f] !== undefined) p[f] = String(msg[f]).slice(0, 80);\n      }\n      if (msg.tags !== undefined) {\n        let tags = [];\n        try {\n          if (Array.isArray(msg.tags)) tags = msg.tags.slice(0, 12).map(t => String(t).slice(0, 20));\n        } catch (e) { /* ignore */ }\n        p.tags = tags;\n      }\n      scheduleSave();\n      send(sock, { type: \'profile_updated\', profile: p });\n      break;\n    }\n\n    case \'add_profile_photo\': {\n      if (!me) return needAuth(sock);\n      const data = String(msg.data || \'\');\n      if (data.length < 100 || data.length > 1400000) {\n        return send(sock, { type: \'error\', message: \'图片数据不合法\' });\n      }\n      try {\n        const buf = Buffer.from(data, \'base64\');\n        if (buf.length < 100 || buf.length > 900000) {\n          return send(sock, { type: \'error\', message: \'图片数据不合法\' });\n        }\n        const p = users[me].profile = users[me].profile || {};\n        p.photos = p.photos || [];\n        if (p.photos.length >= 9) return send(sock, { type: \'toast\', message: \'最多9张照片\' });\n        const name = \'p_\' + crypto.randomBytes(6).toString(\'hex\') + \'.jpg\';\n        fs.writeFileSync(path.join(DATA_DIR, \'files\', name), buf);\n        p.photos.push(\'/files/\' + name);\n        scheduleSave();\n        send(sock, { type: \'profile_updated\', profile: p });\n        log(\'添加名片照片: \' + me);\n      } catch (e) {\n        send(sock, { type: \'error\', message: \'照片保存失败\' });\n      }\n      break;\n    }\n\n    case \'remove_profile_photo\': {\n      if (!me) return needAuth(sock);\n      const idx = parseInt(msg.index, 10);\n      const p = users[me].profile = users[me].profile || {};\n      p.photos = p.photos || [];\n      if (idx >= 0 && idx < p.photos.length) {\n        p.photos.splice(idx, 1);\n        scheduleSave();\n        send(sock, { type: \'profile_updated\', profile: p });\n      }\n      break;\n    }\n\n    case \'forward_message\': {'],
]);
console.log('DONE');
