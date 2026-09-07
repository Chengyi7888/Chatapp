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

// Manifest 注册 ProfileActivity + 版本 2.2.0.0
for (const m of [ROOT + '/android/AndroidManifest.xml', ROOT + '/build/AndroidManifest.xml']) {
  patch(m, [
    ['        <provider\n            android:name=".ApkProvider"',
     '        <activity\n            android:name=".ProfileActivity"\n            android:exported="false"\n            android:screenOrientation="portrait" />\n\n        <provider\n            android:name=".ApkProvider"'],
    ['android:versionCode="38"', 'android:versionCode="39"'],
    ['android:versionName="2.1.4.2"', 'android:versionName="2.2.0.0"'],
  ]);
}
patch(ROOT + '/android/src/com/chatapp/app/MainActivity.java', [
  ['"App v2.1.4.2"', '"App v2.2.0.0"'],
]);

const home = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';
let h = fs.readFileSync(home, 'utf8');
let ok = 0, miss = [];
function rep(old, neu) {
  if (h.includes(old)) { h = h.split(old).join(neu); ok++; }
  else miss.push(old.slice(0, 60));
}
// 头像点击 -> 个人名片
rep('        avatar.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                pickAvatar();\n            }\n        });',
    '        avatar.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                Intent i = new Intent(HomeActivity.this, ProfileActivity.class);\n                startActivity(i);\n            }\n        });');
// 移除 修改昵称 行
rep('        group1.addView(menuRow(I18n.t("edit_nickname"), new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                showNicknameDialog();\n            }\n        }));\n        group1.addView(Utils.divider(this));\n',
    '');
// 版本显示
rep('"关于 (App v2.1.4.2)"', '"关于 (App v2.2.0.0)"');
rep('"Chatapp v2.1.4.2\\n" + I18n.t("current_server")', '"Chatapp v2.2.0.0\\n" + I18n.t("current_server")');
rep('        addVersion(list, "v2.1.4.2", new String[]{"更新方式恢复为系统下载管理器+通知安装"});',
    '        addVersion(list, "v2.2.0.0", new String[]{"新增个人名片(头像/昵称/性别/生日/标签/照片/职业/公司/所在地/出生地/邮箱)", "聊天消息新增多选(转发/复制/回复/删除)", "修复连接不稳定(增加心跳保活)", "默认文案改为Hello,world", "社群页移除重复的新建群聊按钮", "＋面板图标改为Ima/Fil/Fav"});\n        addVersion(list, "v2.1.4.2", new String[]{"更新方式恢复为系统下载管理器+通知安装"});');
fs.writeFileSync(home, h);
console.log('[HomeActivity.java] ' + ok + ' patched' + (miss.length ? '  MISS: ' + miss.join(' | ') : ''));

patch(ROOT + '/server/server.js', [["version: '2.1.4.2'", "version: '2.2.0.0'"]]);
patch(ROOT + '/outputs/server.js', [["version: '2.1.4.2'", "version: '2.2.0.0'"]]);
console.log('DONE');
