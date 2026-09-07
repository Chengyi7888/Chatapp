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

// 移除 retryPendingInstall 调用
patch(ROOT + '/android/src/com/chatapp/app/HomeActivity.java', [
  ['        if (Theme.apply(this)) reapplyTheme();\n        UpdateChecker.retryPendingInstall(this);',
   '        if (Theme.apply(this)) reapplyTheme();'],
]);

// 版本 2.1.4.1 -> 2.1.4.2 (修Bug)
for (const m of [ROOT + '/android/AndroidManifest.xml', ROOT + '/build/AndroidManifest.xml']) {
  patch(m, [
    ['android:versionCode="37"', 'android:versionCode="38"'],
    ['android:versionName="2.1.4.1"', 'android:versionName="2.1.4.2"'],
  ]);
}
patch(ROOT + '/android/src/com/chatapp/app/MainActivity.java', [
  ['"App v2.1.4.1"', '"App v2.1.4.2"'],
]);

const home = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';
let h = fs.readFileSync(home, 'utf8');
h = h.split('"关于 (App v2.1.4.1)"').join('"关于 (App v2.1.4.2)"');
h = h.split('"Chatapp v2.1.4.1\\n" + I18n.t("current_server")').join('"Chatapp v2.1.4.2\\n" + I18n.t("current_server")');
const oldEntry = '        addVersion(list, "v2.1.4.1", new String[]{"修复更新下载后无法跳转安装(引导开启安装权限并自动重试)", "应用包名更改为 com.chatapp.app"});';
const newEntry = '        addVersion(list, "v2.1.4.2", new String[]{"更新方式恢复为系统下载管理器+通知安装"});\n' + oldEntry;
if (h.includes(oldEntry)) { h = h.split(oldEntry).join(newEntry); console.log('[HomeActivity.java] changelog added v2.1.4.2'); }
else console.log('changelog anchor not found');
fs.writeFileSync(home, h);

patch(ROOT + '/server/server.js', [["version: '2.1.4.1'", "version: '2.1.4.2'"]]);
patch(ROOT + '/outputs/server.js', [["version: '2.1.4.1'", "version: '2.1.4.2'"]]);
console.log('DONE');
