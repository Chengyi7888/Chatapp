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

patch(ROOT + '/android/src/com/chatapp/app/HomeActivity.java', [
  ['import android.app.Activity;\nimport android.content.Intent;',
   'import android.app.Activity;\nimport android.app.Dialog;\nimport android.content.Intent;'],
  ['import org.json.JSONObject;', 'import org.json.JSONArray;\nimport org.json.JSONObject;'],
]);
patch(ROOT + '/android/src/com/chatapp/app/GroupDetailActivity.java', [
  ['import android.app.Activity;\nimport android.content.Intent;',
   'import android.app.Activity;\nimport android.app.Dialog;\nimport android.content.Intent;'],
]);
console.log('DONE');
