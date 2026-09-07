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

// Utils: 全局字体缩放
patch(ROOT + '/android/src/com/chatapp/app/Utils.java', [
  ['    /** 液态玻璃开关: 开启后所有按钮/卡片/提示框使用透明玻璃质感 */\n    public static boolean liquid = false;\n\n', '    /** 全局字体缩放系数 */\n    public static float fontScale = 1.0f;\n\n'],
  ['    public static int sp(Context c, float v) {\n        return (int) (c.getResources().getDisplayMetrics().scaledDensity * v + 0.5f);\n    }',
   '    public static int sp(Context c, float v) {\n        return (int) (c.getResources().getDisplayMetrics().scaledDensity * v * fontScale + 0.5f);\n    }\n\n    /** 从偏好读取字体缩放 */\n    public static void loadFontScale(Context c) {\n        try {\n            fontScale = c.getSharedPreferences("chatapp_font", Context.MODE_PRIVATE).getFloat("scale", 1.0f);\n        } catch (Exception ignored) {\n        }\n    }'],
]);

// Notifier: 免打扰
patch(ROOT + '/android/src/com/chatapp/app/Notifier.java', [
  ['import android.content.pm.PackageManager;\nimport android.os.Build;',
   'import android.content.SharedPreferences;\nimport android.content.pm.PackageManager;\nimport android.os.Build;\nimport java.util.Calendar;'],
  ['    public static void notifyMessage(Context ctx, String title, String text) {\n        if (!canNotify(ctx)) return;',
   '    /** 免打扰时段内不提醒 */\n    public static boolean dndAllowed(Context ctx) {\n        try {\n            SharedPreferences p = ctx.getSharedPreferences("chatapp_dnd", Context.MODE_PRIVATE);\n            if (!p.getBoolean("enabled", false)) return true;\n            int start = p.getInt("start", 23);\n            int end = p.getInt("end", 7);\n            int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);\n            if (start <= end) return !(h >= start && h < end);\n            return !(h >= start || h < end);\n        } catch (Exception ignored) {\n            return true;\n        }\n    }\n\n    public static void notifyMessage(Context ctx, String title, String text) {\n        if (!canNotify(ctx)) return;\n        if (!dndAllowed(ctx)) return;'],
]);

// 服务器 typing 支持群
patch(ROOT + '/server/server.js', [
  ['    case \'typing\': {\n      if (!me) return needAuth(sock);\n      const peer = String(msg.with || \'\').trim();\n      if (online(peer)) send(sockets[peer], { type: \'peer_typing\', from: me });\n      break;\n    }',
   '    case \'typing\': {\n      if (!me) return needAuth(sock);\n      const t = String(msg.with || \'\').trim();\n      if (t.indexOf(\'g\') === 0 && groups[t] && groups[t].members.includes(me)) {\n        for (const m of groups[t].members) {\n          if (m !== me && online(m)) send(sockets[m], { type: \'peer_typing\', from: me });\n        }\n      } else if (online(t)) {\n        send(sockets[t], { type: \'peer_typing\', from: me });\n      }\n      break;\n    }'],
]);

// I18n 键
patch(ROOT + '/android/src/com/chatapp/app/I18n.java', [
  ['        put("email_invalid", "当前邮箱不合法", "目前郵箱不合法", "Invalid email address");',
   '        put("email_invalid", "当前邮箱不合法", "目前郵箱不合法", "Invalid email address");\n        put("read", "已读", "已讀", "Read");\n        put("typing_hint", "对方正在输入...", "對方正在輸入...", "Typing...");\n        put("search_msg", "搜索消息", "搜尋訊息", "Search messages");\n        put("export_chat", "导出聊天记录", "匯出聊天紀錄", "Export chat");\n        put("pin_msg", "置顶消息", "置頂訊息", "Pin message");\n        put("unpin_msg", "取消置顶", "取消置頂", "Unpin message");\n        put("font_size", "字体大小", "字體大小", "Font size");\n        put("font_small", "小", "小", "Small");\n        put("font_normal", "标准", "標準", "Normal");\n        put("font_large", "大", "大", "Large");\n        put("font_xlarge", "特大", "特大", "Extra large");\n        put("dnd", "免打扰", "勿擾", "Do not disturb");\n        put("dnd_start", "开始", "開始", "Start");\n        put("dnd_end", "结束", "結束", "End");'],
]);
console.log('DONE');
