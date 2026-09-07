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
  // get_posts 输出带 comments/favorites
  ['        time: p.time,\n        likes: p.likes || []\n      }));\n      send(sock, { type: \'posts\', posts: out });',
   '        time: p.time,\n        likes: p.likes || [],\n        favorites: p.favorites || [],\n        comments: p.comments || [],\n        forwardFrom: p.forwardFrom || \'\'\n      }));\n      send(sock, { type: \'posts\', posts: out });'],
  // add_post 默认 favorites/comments
  ['      posts.push({ id: crypto.randomBytes(8).toString(\'hex\'), from: me, text, images, time: Date.now(), likes: [] });',
   '      const pid = crypto.randomBytes(8).toString(\'hex\');\n      let forwardFrom = \'\';\n      if (msg.forward && msg.forward === true) {\n        const orig = posts.find(x => x.id === String(msg.forwardId || \'\'));\n        if (orig) {\n          forwardFrom = orig.from;\n          if (!text.trim()) text = orig.text || \'\';\n          if (images.length === 0) images = orig.images || [];\n        }\n      }\n      posts.push({ id: pid, from: me, text, images, time: Date.now(), likes: [], favorites: [], comments: [], forwardFrom });'],
  // 新协议: 收藏/评论/转发/删除/我的收藏
  ['    case \'like_post\': {',
   '    case \'favorite_post\': {\n      if (!me) return needAuth(sock);\n      const id = String(msg.id || \'\');\n      const p = posts.find(x => x.id === id);\n      if (!p) break;\n      p.favorites = p.favorites || [];\n      const idx = p.favorites.indexOf(me);\n      if (idx >= 0) p.favorites.splice(idx, 1); else p.favorites.push(me);\n      scheduleGroupSave();\n      send(sock, { type: \'posts_changed\' });\n      break;\n    }\n\n    case \'add_comment\': {\n      if (!me) return needAuth(sock);\n      const id = String(msg.id || \'\');\n      const p = posts.find(x => x.id === id);\n      if (!p) break;\n      const text = String(msg.text || \'\').trim().slice(0, 200);\n      if (!text) break;\n      p.comments = p.comments || [];\n      p.comments.push({ from: me, nickname: users[me] ? users[me].nickname : me, text, time: Date.now() });\n      scheduleGroupSave();\n      send(sock, { type: \'posts_changed\' });\n      break;\n    }\n\n    case \'repost_post\': {\n      if (!me) return needAuth(sock);\n      const id = String(msg.id || \'\');\n      const orig = posts.find(x => x.id === id);\n      if (!orig) break;\n      posts.push({ id: crypto.randomBytes(8).toString(\'hex\'), from: me, text: orig.text || \'\', images: (orig.images || []).slice(), time: Date.now(), likes: [], favorites: [], comments: [], forwardFrom: orig.from });\n      if (posts.length > 2000) posts = posts.slice(-2000);\n      scheduleGroupSave();\n      send(sock, { type: \'post_added\' });\n      break;\n    }\n\n    case \'delete_post\': {\n      if (!me) return needAuth(sock);\n      const id = String(msg.id || \'\');\n      const idx = posts.findIndex(x => x.id === id && x.from === me);\n      if (idx >= 0) posts.splice(idx, 1);\n      scheduleGroupSave();\n      send(sock, { type: \'posts_changed\' });\n      break;\n    }\n\n    case \'get_my_favorites\': {\n      if (!me) return needAuth(sock);\n      const out = posts.filter(p => (p.favorites || []).indexOf(me) >= 0)\n        .sort((a, b) => b.time - a.time)\n        .slice(0, 200)\n        .map(p => ({\n          id: p.id, from: p.from, nickname: users[p.from] ? users[p.from].nickname : p.from,\n          hasAvatar: !!(users[p.from] && users[p.from].hasAvatar),\n          text: p.text || \'\', images: p.images || [], time: p.time,\n          likes: p.likes || [], favorites: p.favorites || [], comments: p.comments || [], forwardFrom: p.forwardFrom || \'\'\n        }));\n      send(sock, { type: \'my_favorites\', posts: out });\n      break;\n    }\n\n    case \'like_post\': {'],
]);
console.log('DONE');
