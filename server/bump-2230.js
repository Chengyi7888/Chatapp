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
    ['android:versionCode="42"', 'android:versionCode="43"'],
    ['android:versionName="2.2.2.0"', 'android:versionName="2.2.3.0"'],
  ]);
}
patch(ROOT + '/android/src/com/chatapp/app/MainActivity.java', [
  ['"App v2.2.2.0"', '"App v2.2.3.0"'],
]);

const home = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';
let h = fs.readFileSync(home, 'utf8');
h = h.split('"关于 (App v2.2.2.0)"').join('"关于 (App v2.2.3.0)"');
h = h.split('"Chatapp v2.2.2.0\\n" + I18n.t("current_server")').join('"Chatapp v2.2.3.0\\n" + I18n.t("current_server")');
const oldEntry = '        addVersion(list, "v2.2.2.0", new String[]{"聊天/群聊/群信息加载增加居中转圈动画", "名片选中项(性别/生日/职业)绿色高亮", "所在地/出生地改为省-市-区三级滑轮选择", "邮箱输入增加合法性校验", "移除液态玻璃功能"});';
const newEntry = '        addVersion(list, "v2.2.3.0", new String[]{"消息已读回执(单聊显示已读)", "对方正在输入提示", "聊天内消息搜索", "导出聊天记录", "单条消息置顶", "字体大小调节", "消息草稿自动保存", "免打扰时段(夜间不提醒)"});\n' + oldEntry;
if (h.includes(oldEntry)) { h = h.split(oldEntry).join(newEntry); console.log('[HomeActivity.java] changelog v2.2.3.0'); }
else console.log('changelog anchor not found');
fs.writeFileSync(home, h);

patch(ROOT + '/server/server.js', [["version: '2.2.2.0'", "version: '2.2.3.0'"]]);
patch(ROOT + '/outputs/server.js', [["version: '2.2.2.0'", "version: '2.2.3.0'"]]);
console.log('DONE');
