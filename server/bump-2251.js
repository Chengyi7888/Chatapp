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
    ['android:versionCode="45"', 'android:versionCode="46"'],
    ['android:versionName="2.2.5.0"', 'android:versionName="2.2.5.1"'],
  ]);
}
patch(ROOT + '/android/src/com/chatapp/app/MainActivity.java', [
  ['"App v2.2.5.0"', '"App v2.2.5.1"'],
]);

const home = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';
let h = fs.readFileSync(home, 'utf8');
h = h.split('"关于 (App v2.2.5.0)"').join('"关于 (App v2.2.5.1)"');
h = h.split('"Chatapp v2.2.5.0\\n" + I18n.t("current_server")').join('"Chatapp v2.2.5.1\\n" + I18n.t("current_server")');
const oldEntry = '        addVersion(list, "v2.2.5.0", new String[]{"更新提示确认后自动关闭并定时后台检测", "频道按语言/地区筛选并可跳转对应应用", "白昼模式文字对比度优化", "三角形加载动画改为整条边顺序点亮(白昼变蓝)"});';
const newEntry = '        addVersion(list, "v2.2.5.1", new String[]{"好友/成员名片上下统一布局", "登录注册页优化: 默认服务器100.100.1.100:1100并记忆上次域名", "副标题改为Safe Reliable Simple", "新用户默认英文界面"});\n' + oldEntry;
if (h.includes(oldEntry)) { h = h.split(oldEntry).join(newEntry); console.log('[HomeActivity.java] changelog v2.2.5.1'); }
else console.log('changelog anchor not found');
fs.writeFileSync(home, h);

patch(ROOT + '/server/server.js', [["version: '2.2.5.0'", "version: '2.2.5.1'"]]);
patch(ROOT + '/outputs/server.js', [["version: '2.2.5.0'", "version: '2.2.5.1'"]]);
console.log('DONE');
