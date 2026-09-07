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

patch(ROOT + '/android/src/com/chatapp/app/I18n.java', [
  ['        server("动态内容不能为空", "srv_moment_empty");',
   '        server("动态内容不能为空", "srv_moment_empty");\n        server("用户名错误", "srv_username_wrong");\n        server("用户昵称错误", "srv_nickname_wrong");\n        server("用户密码错误", "srv_password_wrong");'],
  ['        put("srv_moment_empty", "动态内容不能为空", "動態內容不能為空", "Content cannot be empty");',
   '        put("srv_moment_empty", "动态内容不能为空", "動態內容不能為空", "Content cannot be empty");\n        put("srv_username_wrong", "用户名错误", "用戶名錯誤", "Username is wrong");\n        put("srv_nickname_wrong", "用户昵称错误", "用戶暱稱錯誤", "Nickname is wrong");\n        put("srv_password_wrong", "用户密码错误", "用戶密碼錯誤", "Password is wrong");'],
  // 新文案
  ['        put("friend_center", "好友中心", "好友中心", "Friends");',
   '        put("friend_center", "添加与请求", "添加與請求", "Add & Requests");'],
  ['        put("scan_add_hint", "扫码识别待完善，可手动输入用户名添加", "掃碼識別待完善，可手動輸入用戶名新增", "Scan pending, enter username to add");',
   '        put("scan_add_hint", "扫码识别待完善，可手动输入用户名添加", "掃碼識別待完善，可手動輸入用戶名新增", "Scan pending, enter username to add");\n        put("added", "已添加", "已添加", "Added");\n        put("upgrade", "升级到最新版本", "升級到最新版本", "Upgrade");\n        put("current_latest", "已是最新版本", "已是最新版本", "Up to date");\n        put("checking_update", "检测中...", "檢測中...", "Checking...");\n        put("verify_identity", "验证身份", "驗證身份", "Verify identity");\n        put("verify_nickname", "输入用户昵称", "輸入用戶暱稱", "Enter nickname");\n        put("verify_username", "输入用户名", "輸入用戶名", "Enter username");\n        put("verify_password", "输入用户密码", "輸入用戶密碼", "Enter password");\n        put("confirm_delete2", "再次确认注销？此操作不可恢复", "再次確認註銷？此操作不可恢復", "Confirm delete again? Cannot be undone");\n        put("fav_delete", "删除收藏", "刪除收藏", "Remove favorite");\n        put("fav_forward", "转发", "轉發", "Forward");'],
]);
console.log('DONE');
