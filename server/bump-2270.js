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
    ['android:versionCode="47"', 'android:versionCode="48"'],
    ['android:versionName="2.2.6.0"', 'android:versionName="2.2.7.0"'],
  ]);
}
patch(ROOT + '/android/src/com/chatapp/app/MainActivity.java', [
  ['"App v2.2.6.0"', '"App v2.2.7.0"'],
]);

const home = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';
let h = fs.readFileSync(home, 'utf8');
h = h.split('"关于 (App v2.2.6.0)"').join('"关于 (App v2.2.7.0)"');
h = h.split('"Chatapp v2.2.6.0\\n" + I18n.t("current_server")').join('"Chatapp v2.2.7.0\\n" + I18n.t("current_server")');
const oldEntry = '        addVersion(list, "v2.2.6.0", new String[]{"加载动画改为边累积点亮全亮后一起熄灭", "白昼模式移除按钮阴影", "地区/生日选择器文字颜色修复, 已选地点变蓝", "发图片支持拍照(可旋转/裁剪/重拍/取消)与相册选择", "联系人弹窗新增扫一扫"});';
const newEntry = '        addVersion(list, "v2.2.7.0", new String[]{"加载动画绿色光效层级修复", "字体大小调节真正生效(含聊天文字)", "聊天搜索框常显", "动态图片上限9张并即时预览", "动态新增收藏/评论/转发/左滑删除", "点赞即时反馈后同步", "我的收藏页(动态+对话)", "聊天消息可收藏并选择发送", "聊天右下角＋进入好友中心(添加好友/好友申请/扫一扫)"});\n' + oldEntry;
if (h.includes(oldEntry)) { h = h.split(oldEntry).join(newEntry); console.log('[HomeActivity.java] changelog v2.2.7.0'); }
else console.log('changelog anchor not found');
fs.writeFileSync(home, h);

patch(ROOT + '/server/server.js', [["version: '2.2.6.0'", "version: '2.2.7.0'"]]);
patch(ROOT + '/outputs/server.js', [["version: '2.2.6.0'", "version: '2.2.7.0'"]]);
console.log('DONE');
