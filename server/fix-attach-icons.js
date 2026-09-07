'use strict';
const fs = require('fs');
const ROOT = 'C:/Users/cheng/Documents/Codex/Chatapp';

function patch(file, pairs) {
  let t = fs.readFileSync(file, 'utf8');
  let ok = 0, miss = [];
  for (const [old, neu] of pairs) {
    if (t.includes(old)) { t = t.split(old).join(neu); ok++; }
    else miss.push(old.slice(0, 60));
  }
  fs.writeFileSync(file, t);
  console.log('[' + file.split('/').pop() + '] ' + ok + ' patched' + (miss.length ? '  MISS: ' + miss.join(' | ') : ''));
}

for (const f of ['ChatActivity.java', 'GroupChatActivity.java']) {
  const p = ROOT + '/android/src/com/chatapp/app/' + f;
  patch(p, [
    ['attachBtn("🖼️", I18n.t("image"),', 'attachBtn("Ima", I18n.t("image"),'],
    ['attachBtn("📁", I18n.t("file"),', 'attachBtn("Fil", I18n.t("file"),'],
    ['attachBtn("⭐", I18n.t("favorite"),', 'attachBtn("Fav", I18n.t("favorite"),'],
  ]);
}
console.log('DONE');
