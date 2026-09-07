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

// 1) 三角形加载动画替换系统圆圈
for (const f of ['ChatActivity.java', 'GroupChatActivity.java', 'GroupDetailActivity.java', 'ProfileActivity.java']) {
  const p = ROOT + '/android/src/com/chatapp/app/' + f;
  patch(p, [
    ['    private ProgressBar loading;', '    private View loading;'],
    ['        loading = new ProgressBar(this);', '        loading = new TriangleLoadingView(this);'],
  ]);
}

// 2) 服务器: 隐藏消息(仅自己) -> 预览/历史过滤
const S = ROOT + '/server/server.js';
patch(S, [
  ['    u.hidden = u.hidden || [];\n    u.lastReadGroup = u.lastReadGroup || {};',
   '    u.hidden = u.hidden || [];\n    u.hiddenMessages = u.hiddenMessages || [];\n    u.lastReadGroup = u.lastReadGroup || {};'],
  ['        friends: [], pending: [], lastRead: {}, blocked: [], hidden: [], lastReadGroup: {}, createdAt: Date.now(),',
   '        friends: [], pending: [], lastRead: {}, blocked: [], hidden: [], hiddenMessages: [], lastReadGroup: {}, createdAt: Date.now(),'],
  // conversationSummary: 跳过已隐藏消息
  ['function conversationSummary(me, peer) {\n  const p = users[peer];\n  const list = convMessages(me, peer);\n  const last = list[list.length - 1] || null;',
   'function conversationSummary(me, peer) {\n  const p = users[peer];\n  const hidden = users[me].hiddenMessages || [];\n  const list = convMessages(me, peer);\n  let last = null;\n  for (let i = list.length - 1; i >= 0; i--) {\n    if (hidden.indexOf(list[i].id) < 0) { last = list[i]; break; }\n  }'],
  // groupConversationSummary 同样
  ['function groupConversationSummary(me, g) {\n  const list = groupMessages[g.id] || [];\n  const last = list[list.length - 1] || null;',
   'function groupConversationSummary(me, g) {\n  const hidden = users[me].hiddenMessages || [];\n  const list = groupMessages[g.id] || [];\n  let last = null;\n  for (let i = list.length - 1; i >= 0; i--) {\n    if (hidden.indexOf(list[i].id) < 0) { last = list[i]; break; }\n  }'],
  // get_history 过滤
  ['      const list = convMessages(me, peer).slice(-200)\n        .map(m => {',
   '      const hidden = users[me].hiddenMessages || [];\n      const list = convMessages(me, peer).slice(-200)\n        .filter(m => hidden.indexOf(m.id) < 0)\n        .map(m => {'],
  // hide_message 协议 (插到 typing 前)
  ['    case \'typing\': {',
   '    case \'hide_message\': {\n      if (!me) return needAuth(sock);\n      const id = String(msg.id || \'\');\n      if (!id) break;\n      users[me].hiddenMessages = users[me].hiddenMessages || [];\n      if (users[me].hiddenMessages.indexOf(id) < 0) users[me].hiddenMessages.push(id);\n      scheduleSave();\n      send(sock, { type: \'conversations_changed\' });\n      break;\n    }\n\n    case \'typing\': {'],
]);

// 3) 客户端删除消息时同步隐藏
patch(ROOT + '/android/src/com/chatapp/app/ChatActivity.java', [
  ['    private void deleteMsg(Models.ChatMsg m) {\n        if (m.id == null || m.id.isEmpty()) return;\n        Set<String> set = new HashSet<>(localDel.getStringSet("deleted_" + peer, new HashSet<String>()));\n        set.add(m.id);\n        localDel.edit().putStringSet("deleted_" + peer, set).apply();\n        rerender();\n        Ui.banner(this, I18n.t("deleted_self"));\n    }',
   '    private void deleteMsg(Models.ChatMsg m) {\n        if (m.id == null || m.id.isEmpty()) return;\n        Set<String> set = new HashSet<>(localDel.getStringSet("deleted_" + peer, new HashSet<String>()));\n        set.add(m.id);\n        localDel.edit().putStringSet("deleted_" + peer, set).apply();\n        hideOnServer(m.id);\n        rerender();\n        Ui.banner(this, I18n.t("deleted_self"));\n    }\n\n    private void hideOnServer(String id) {\n        try {\n            JSONObject o = new JSONObject();\n            o.put("type", "hide_message");\n            o.put("id", id);\n            Net.get().send(o);\n        } catch (Exception ignored) {\n        }\n    }'],
  ['        for (Models.ChatMsg m : selectedMessages()) {\n            if (m.id == null || m.id.isEmpty()) continue;\n            Set<String> set = new HashSet<>(localDel.getStringSet("deleted_" + peer, new HashSet<String>()));\n            set.add(m.id);\n            localDel.edit().putStringSet("deleted_" + peer, set).apply();\n            hideOnServer(m.id);\n        }',
   '        for (Models.ChatMsg m : selectedMessages()) {\n            if (m.id == null || m.id.isEmpty()) continue;\n            Set<String> set = new HashSet<>(localDel.getStringSet("deleted_" + peer, new HashSet<String>()));\n            set.add(m.id);\n            localDel.edit().putStringSet("deleted_" + peer, set).apply();\n            hideOnServer(m.id);\n        }'],
]);
patch(ROOT + '/android/src/com/chatapp/app/GroupChatActivity.java', [
  ['    private void deleteMsg(Models.ChatMsg m) {\n        if (m.id == null || m.id.isEmpty()) return;\n        Set<String> set = new HashSet<>(localDel.getStringSet("deleted_" + groupId, new HashSet<String>()));\n        set.add(m.id);\n        localDel.edit().putStringSet("deleted_" + groupId, set).apply();\n        rerender();\n        Ui.banner(this, I18n.t("deleted_self"));\n    }',
   '    private void deleteMsg(Models.ChatMsg m) {\n        if (m.id == null || m.id.isEmpty()) return;\n        Set<String> set = new HashSet<>(localDel.getStringSet("deleted_" + groupId, new HashSet<String>()));\n        set.add(m.id);\n        localDel.edit().putStringSet("deleted_" + groupId, set).apply();\n        hideOnServer(m.id);\n        rerender();\n        Ui.banner(this, I18n.t("deleted_self"));\n    }\n\n    private void hideOnServer(String id) {\n        try {\n            JSONObject o = new JSONObject();\n            o.put("type", "hide_message");\n            o.put("id", id);\n            Net.get().send(o);\n        } catch (Exception ignored) {\n        }\n    }'],
]);
console.log('DONE');
