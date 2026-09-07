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

// Session: 默认服务器 + 默认英文
patch(ROOT + '/android/src/com/chatapp/app/Session.java', [
  ['server = prefs.getString("server", "192.168.1.100:8899");', 'server = prefs.getString("server", "100.100.1.100:1100");'],
  ['lang = prefs.getString("lang", I18n.ZH_HANS);', 'lang = prefs.getString("lang", I18n.EN);'],
  ['        if (s.isEmpty()) s = "127.0.0.1:8899";', '        if (s.isEmpty()) s = "100.100.1.100:1100";'],
  ['        if (!s.contains(":")) s = s + ":8899";', '        if (!s.contains(":")) s = s + ":1100";'],
  ['        return new String[]{p[0].trim(), p.length > 1 && !p[1].trim().isEmpty() ? p[1].trim() : "8899"};',
   '        return new String[]{p[0].trim(), p.length > 1 && !p[1].trim().isEmpty() ? p[1].trim() : "1100"};'],
]);

// I18n: 默认英文 + 副标题
patch(ROOT + '/android/src/com/chatapp/app/I18n.java', [
  ['    private static String lang = ZH_HANS;', '    private static String lang = EN;'],
  ['        lang = prefs.getString("lang", ZH_HANS);', '        lang = prefs.getString("lang", EN);'],
  ['put("app_subtitle", "简单好用的聊天软件", "簡單好用的聊天軟體", "Simple chat app");',
   'put("app_subtitle", "Safe Reliable Simple", "Safe Reliable Simple", "Safe Reliable Simple");'],
]);

// 登录/注册页优化: 间距/按钮
patch(ROOT + '/android/src/com/chatapp/app/MainActivity.java', [
  ['        title.setPadding(0, 0, 0, Utils.dp(this, 4));', '        title.setPadding(0, 0, 0, Utils.dp(this, 10));'],
  ['        lp.topMargin = Utils.dp(this, 10);', '        lp.topMargin = Utils.dp(this, 14);'],
  ['        loginBtn.setPadding(0, Utils.dp(this, 6), 0, Utils.dp(this, 6));', '        loginBtn.setPadding(0, Utils.dp(this, 12), 0, Utils.dp(this, 12));'],
  ['        registerBtn.setPadding(0, Utils.dp(this, 6), 0, Utils.dp(this, 6));', '        registerBtn.setPadding(0, Utils.dp(this, 12), 0, Utils.dp(this, 12));'],
  ['        toggle.setPadding(0, Utils.dp(this, 8), 0, Utils.dp(this, 8));', '        toggle.setPadding(0, Utils.dp(this, 12), 0, Utils.dp(this, 12));'],
]);
console.log('DONE');
