'use strict';
const fs = require('fs');
const ROOT = 'C:/Users/cheng/Documents/Codex/Chatapp';
for (const f of ['GroupChatActivity.java', 'GroupDetailActivity.java']) {
  const p = ROOT + '/android/src/com/chatapp/app/' + f;
  let t = fs.readFileSync(p, 'utf8');
  const old = 'import android.widget.EditText;';
  const neu = 'import android.widget.EditText;\nimport android.widget.FrameLayout;';
  if (t.includes(old)) { t = t.split(old).join(neu); fs.writeFileSync(p, t); console.log(f + ': FrameLayout import added'); }
  else console.log(f + ': import anchor not found');
}
