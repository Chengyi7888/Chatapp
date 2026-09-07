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

const CA = ROOT + '/android/src/com/chatapp/app/ChatActivity.java';
const GA = ROOT + '/android/src/com/chatapp/app/GroupChatActivity.java';

for (const [f, cls] of [[CA, 'ChatActivity'], [GA, 'GroupChatActivity']]) {
  patch(f, [
    ['    private void buildSelectBar() {\n        selectBar = new LinearLayout(this);',
     '    private void buildSelectBar(final LinearLayout root) {\n        selectBar = new LinearLayout(this);'],
    ['        root.addView(selectBar, sbp);\n    }\n\n    private void copySelected() {',
     '        root.addView(selectBar, sbp);\n    }\n\n    private void copySelected() {'],
    ['        root.addView(replyBar, rlp);\n        buildSelectBar();',
     '        root.addView(replyBar, rlp);\n        buildSelectBar(root);'],
    ['                    exitMultiSelect();\n                    Ui.banner(this, I18n.f("forwarded_ok", c.nickname));',
     '                    exitMultiSelect();\n                    Ui.banner(' + cls + '.this, I18n.f("forwarded_ok", c.nickname));'],
  ]);
}
console.log('DONE');
