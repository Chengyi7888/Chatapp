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

for (const f of ['ChatActivity.java', 'GroupChatActivity.java']) {
  const p = ROOT + '/android/src/com/chatapp/app/' + f;
  patch(p, [
    // + 号略微缩小
    ['        int ps = Utils.dp(this, 44);', '        int ps = Utils.dp(this, 40);'],
    // 发送键与输入框同高
    ['        sendBtn.setBackground(Utils.bg(this, Utils.ACCENT, 16));\n        sendBtn.setElevation(0);',
     '        sendBtn.setBackground(Utils.bg(this, Utils.ACCENT, 16));\n        sendBtn.setElevation(0);\n        sendBtn.setMinHeight(0);\n        sendBtn.setHeight(Utils.dp(this, 40));\n        sendBtn.setPadding(Utils.dp(this, 14), 0, Utils.dp(this, 14), 0);'],
  ]);
}
console.log('DONE');
