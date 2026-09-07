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
    ['android:versionCode="39"', 'android:versionCode="40"'],
    ['android:versionName="2.2.0.0"', 'android:versionName="2.2.1.0"'],
  ]);
}
patch(ROOT + '/android/src/com/chatapp/app/MainActivity.java', [
  ['"App v2.2.0.0"', '"App v2.2.1.0"'],
]);

const home = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';
let h = fs.readFileSync(home, 'utf8');
h = h.split('"关于 (App v2.2.0.0)"').join('"关于 (App v2.2.1.0)"');
h = h.split('"Chatapp v2.2.0.0\\n" + I18n.t("current_server")').join('"Chatapp v2.2.1.0\\n" + I18n.t("current_server")');
const oldEntry = '        addVersion(list, "v2.2.0.0", new String[]{"新增个人名片(头像/昵称/性别/生日/标签/照片/职业/公司/所在地/出生地/邮箱)", "聊天消息新增多选(转发/复制/回复/删除)", "修复连接不稳定(增加心跳保活)", "默认文案改为Hello,world", "社群页移除重复的新建群聊按钮", "＋面板图标改为Ima/Fil/Fav"});';
const newEntry = '        addVersion(list, "v2.2.1.0", new String[]{"修复切换标签后连接状态显示延迟", "优化后台恢复立即重连与图片缓存, 提升响应速度", "更新页动态头像不再支持更换", "好友名片同步完整个人资料", "群聊信息头像居中, 群成员可查看名片并添加好友"});\n' + oldEntry;
if (h.includes(oldEntry)) { h = h.split(oldEntry).join(newEntry); console.log('[HomeActivity.java] changelog v2.2.1.0'); }
else console.log('changelog anchor not found');
fs.writeFileSync(home, h);

patch(ROOT + '/server/server.js', [["version: '2.2.0.0'", "version: '2.2.1.0'"]]);
patch(ROOT + '/outputs/server.js', [["version: '2.2.0.0'", "version: '2.2.1.0'"]]);
console.log('DONE');
