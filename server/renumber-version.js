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

// 1) 清单版本号
for (const m of [ROOT + '/android/AndroidManifest.xml', ROOT + '/build/AndroidManifest.xml']) {
  patch(m, [
    ['android:versionCode="36"', 'android:versionCode="37"'],
    ['android:versionName="2.1.1.2.1.2.2.1"', 'android:versionName="2.1.4.1"'],
  ]);
}
patch(ROOT + '/android/src/com/chatapp/app/MainActivity.java', [
  ['"App v2.1.1.2.1.2.2.1"', '"App v2.1.4.1"'],
]);

// 2) HomeActivity 关于 + 更新日志重写
const home = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';
let h = fs.readFileSync(home, 'utf8');
h = h.split('"关于 (App v2.1.1.2.1.2.2.1)"').join('"关于 (App v2.1.4.1)"');
h = h.split('"Chatapp v2.1.1.2.1.2.2.1\\n" + I18n.t("current_server")').join('"Chatapp v2.1.4.1\\n" + I18n.t("current_server")');

const startMark = '        addVersion(list, "v2.1.1.2.1.2.2.1", new String[]{"修复更新下载后无法跳转安装(引导开启安装权限并自动重试)"});';
const endMark = '        addVersion(list, "v2.0.10.2",';
const si = h.indexOf(startMark);
const ei = h.indexOf(endMark);
if (si >= 0 && ei > si) {
  const newLog = [
    '        addVersion(list, "v2.1.4.1", new String[]{"修复更新下载后无法跳转安装(引导开启安装权限并自动重试)", "应用包名更改为 com.chatapp.app"});',
    '        addVersion(list, "v2.1.4.0", new String[]{"群聊右上角三点直接进入群聊信息", "移除群聊信息页的\\u201C进入群聊\\u201D按钮", "聊天气泡新增头像(群聊含发送者姓名)"});',
    '        addVersion(list, "v2.1.3.1", new String[]{"修复主题/液态玻璃/语言切换时的闪屏问题", "液态玻璃标记为测试中并加入开启确认"});',
    '        addVersion(list, "v2.1.3.0", new String[]{"长按图片/文件支持转发、引用、删除", "更新下载显示进度条并自动跳转安装", "聊天/群聊顶底栏改为圆角", "群聊新增删除群聊天记录", "会话左滑隐藏(收到新消息后重新出现)"});',
    '        addVersion(list, "v2.1.2.2", new String[]{"修复\\u201C自己\\u201D页点击开关后页面回弹到顶部", "登录/注册界面布局居中优化"});',
    '        addVersion(list, "v2.1.2.1", new String[]{"删除消息提示改为应用内顶部弹窗"});',
    '        addVersion(list, "v2.1.2.0", new String[]{"新增振动反馈开关(导航栏/开关按键振动)", "新增消息通知权限与消息提醒", "移除聊天页搜索框放大镜"});',
    '        addVersion(list, "v2.1.1.1", new String[]{"修复群聊消息重复发送", "地区选择即时高亮显示", "移除多余的系统提示", "补全繁體/English语言覆盖"});',
    '        addVersion(list, "v2.1.1.0", new String[]{"连接状态提示移至更新页右上角", "新增\\u201C液态玻璃\\u201D透明玻璃质感开关", "聊天页联系人图标位置优化"});',
    '        addVersion(list, "v2.1.0.0", new String[]{"开放社群功能，支持群聊/建群/群主管理", "聊天列表长按可置顶/删除记录/删除好友/拉黑", "点击好友头像查看好友名片", "新增语言与地区设置(简/繁/英)", "修复白昼模式下卡片容器不可见", "应用更名 Chatapp"});',
  ].join('\n');
  h = h.slice(0, si) + newLog + '\n' + h.slice(ei);
  fs.writeFileSync(home, h);
  console.log('[HomeActivity.java] changelog rewritten');
} else {
  console.log('changelog anchors not found si=' + si + ' ei=' + ei);
}

// 3) 服务器版本号
patch(ROOT + '/server/server.js', [["version: '2.1.1.2.1.2.2.1'", "version: '2.1.4.1'"]]);
patch(ROOT + '/outputs/server.js', [["version: '2.1.1.2.1.2.2.1'", "version: '2.1.4.1'"]]);
console.log('DONE');
