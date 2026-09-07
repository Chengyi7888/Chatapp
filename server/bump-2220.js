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
    ['android:versionCode="41"', 'android:versionCode="42"'],
    ['android:versionName="2.2.1.1"', 'android:versionName="2.2.2.0"'],
  ]);
}
patch(ROOT + '/android/src/com/chatapp/app/MainActivity.java', [
  ['"App v2.2.1.1"', '"App v2.2.2.0"'],
]);

const home = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';
let h = fs.readFileSync(home, 'utf8');
h = h.split('"关于 (App v2.2.1.1)"').join('"关于 (App v2.2.2.0)"');
h = h.split('"Chatapp v2.2.1.1\\n" + I18n.t("current_server")').join('"Chatapp v2.2.2.0\\n" + I18n.t("current_server")');
const oldEntry = '        addVersion(list, "v2.2.1.1", new String[]{"好友名片/成员名片显示完整资料, 未填写项显示—", "个人名片页增加居中加载动画"});';
const newEntry = '        addVersion(list, "v2.2.2.0", new String[]{"聊天/群聊/群信息加载增加居中转圈动画", "名片选中项(性别/生日/职业)绿色高亮", "所在地/出生地改为省-市-区三级滑轮选择", "邮箱输入增加合法性校验", "移除液态玻璃功能"});\n' + oldEntry;
if (h.includes(oldEntry)) { h = h.split(oldEntry).join(newEntry); console.log('[HomeActivity.java] changelog v2.2.2.0'); }
else console.log('changelog anchor not found');
fs.writeFileSync(home, h);

patch(ROOT + '/server/server.js', [["version: '2.2.1.1'", "version: '2.2.2.0'"]]);
patch(ROOT + '/outputs/server.js', [["version: '2.2.1.1'", "version: '2.2.2.0'"]]);
console.log('DONE');
