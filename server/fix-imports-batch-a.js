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

patch(ROOT + '/android/src/com/chatapp/app/ChatActivity.java', [
  ['import android.text.TextUtils;', 'import android.text.Editable;\nimport android.text.TextUtils;\nimport android.text.TextWatcher;'],
]);
patch(ROOT + '/android/src/com/chatapp/app/GroupChatActivity.java', [
  ['import android.text.TextUtils;', 'import android.text.Editable;\nimport android.text.TextUtils;\nimport android.text.TextWatcher;'],
]);
patch(ROOT + '/android/src/com/chatapp/app/HomeActivity.java', [
  ['import android.content.Intent;', 'import android.content.Intent;\nimport android.content.SharedPreferences;'],
]);
console.log('DONE');
