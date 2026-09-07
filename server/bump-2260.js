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

patch(ROOT + '/android/src/com/chatapp/app/MainActivity.java', [
  ['        loginBtn.setBackground(Utils.bg(this, Utils.ACCENT, 10));', '        loginBtn.setBackground(Utils.bg(this, Utils.ACCENT, 10));\n        loginBtn.setElevation(0);'],
  ['        registerBtn.setBackground(Utils.bg(this, Utils.ACCENT_DARK, 10));', '        registerBtn.setBackground(Utils.bg(this, Utils.ACCENT_DARK, 10));\n        registerBtn.setElevation(0);'],
]);
patch(ROOT + '/android/src/com/chatapp/app/GroupDetailActivity.java', [
  ['            addBtn.setBackground(Utils.bg(this, Utils.ACCENT, 22));', '            addBtn.setBackground(Utils.bg(this, Utils.ACCENT, 22));\n            addBtn.setElevation(0);'],
]);

// 版本 2.2.5.1 -> 2.2.6.0
for (const m of [ROOT + '/android/AndroidManifest.xml', ROOT + '/build/AndroidManifest.xml']) {
  patch(m, [
    ['android:versionCode="46"', 'android:versionCode="47"'],
    ['android:versionName="2.2.5.1"', 'android:versionName="2.2.6.0"'],
  ]);
}
patch(ROOT + '/android/src/com/chatapp/app/MainActivity.java', [
  ['"App v2.2.5.1"', '"App v2.2.6.0"'],
]);
const home = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';
let h = fs.readFileSync(home, 'utf8');
h = h.split('"关于 (App v2.2.5.1)"').join('"关于 (App v2.2.6.0)"');
h = h.split('"Chatapp v2.2.5.1\\n" + I18n.t("current_server")').join('"Chatapp v2.2.6.0\\n" + I18n.t("current_server")');
const oldEntry = '        addVersion(list, "v2.2.5.1", new String[]{"好友/成员名片上下统一布局", "登录注册页优化: 默认服务器100.100.1.100:1100并记忆上次域名", "副标题改为Safe Reliable Simple", "新用户默认英文界面"});';
const newEntry = '        addVersion(list, "v2.2.6.0", new String[]{"加载动画改为边累积点亮全亮后一起熄灭", "白昼模式移除按钮阴影", "地区/生日选择器文字颜色修复, 已选地点变蓝", "发图片支持拍照(可旋转/裁剪/重拍/取消)与相册选择", "联系人弹窗新增扫一扫"});\n' + oldEntry;
if (h.includes(oldEntry)) { h = h.split(oldEntry).join(newEntry); console.log('[HomeActivity.java] changelog v2.2.6.0'); }
else console.log('changelog anchor not found');
fs.writeFileSync(home, h);

patch(ROOT + '/server/server.js', [["version: '2.2.5.1'", "version: '2.2.6.0'"]]);
patch(ROOT + '/outputs/server.js', [["version: '2.2.5.1'", "version: '2.2.6.0'"]]);
console.log('DONE');
