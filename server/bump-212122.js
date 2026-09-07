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

for (const m of [ROOT + '/android/AndroidManifest.xml', ROOT + '/build/AndroidManifest.xml']) {
  patch(m, [
    ['android:versionCode="34"', 'android:versionCode="35"'],
    ['android:versionName="2.1.1.2.1.2.1"', 'android:versionName="2.1.1.2.1.2.2"'],
  ]);
}
patch(ROOT + '/android/src/com/chatapp/app/MainActivity.java', [
  ['"App v2.1.1.2.1.2.1"', '"App v2.1.1.2.1.2.2"'],
]);

const home = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';
let h = fs.readFileSync(home, 'utf8');
const pairs = [
  ['"关于 (App v2.1.1.2.1.2.1)"', '"关于 (App v2.1.1.2.1.2.2)"'],
  ['"Chatapp v2.1.1.2.1.2.1\\n" + I18n.t("current_server")', '"Chatapp v2.1.1.2.1.2.2\\n" + I18n.t("current_server")'],
  ['        addVersion(list, "v2.1.1.2.1.2.1", new String[]{"修复主题/液态玻璃/语言切换时的闪屏问题", "液态玻璃标记为测试中并加入开启确认"});',
   '        addVersion(list, "v2.1.1.2.1.2.2", new String[]{"群聊右上角三点直接进入群聊信息", "移除群聊信息页的\\u201C进入群聊\\u201D按钮", "聊天气泡新增头像(群聊含发送者姓名)"});\n        addVersion(list, "v2.1.1.2.1.2.1", new String[]{"修复主题/液态玻璃/语言切换时的闪屏问题", "液态玻璃标记为测试中并加入开启确认"});'],
];
let ok = 0, miss = [];
for (const [old, neu] of pairs) {
  if (h.includes(old)) { h = h.split(old).join(neu); ok++; }
  else miss.push(old.slice(0, 60));
}
fs.writeFileSync(home, h);
console.log('[HomeActivity.java] ' + ok + ' patched' + (miss.length ? '  MISS: ' + miss.join(' | ') : ''));

patch(ROOT + '/server/server.js', [["version: '2.1.1.2.1.2.1'", "version: '2.1.1.2.1.2.2'"]]);
patch(ROOT + '/outputs/server.js', [["version: '2.1.1.2.1.2.1'", "version: '2.1.1.2.1.2.2'"]]);
console.log('DONE');
