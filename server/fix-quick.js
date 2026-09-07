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

// ============ 1) 连接稳定: Net 心跳 ping ============
patch(ROOT + '/android/src/com/chatapp/app/Net.java', [
  ['    private int reconnectCount = 0;\n    private int sessionId = 0;',
   '    private int reconnectCount = 0;\n    private int sessionId = 0;\n    private long lastWrite = 0;'],
  ['                writer = w;\n                connected = true;\n                reconnectCount = 0;',
   '                writer = w;\n                connected = true;\n                reconnectCount = 0;\n                lastWrite = System.currentTimeMillis();'],
  ['                    } catch (SocketTimeoutException te) {\n                        continue; // 超时后继续, 顺便清空发送队列\n                    }',
   '                    } catch (SocketTimeoutException te) {\n                        long now = System.currentTimeMillis();\n                        if (now - lastWrite > 12000) {\n                            try {\n                                w.write("{\\"type\\":\\"ping\\"}\\n");\n                                w.flush();\n                                lastWrite = now;\n                            } catch (Exception ignored) {\n                            }\n                        }\n                        continue; // 超时后继续, 顺便清空发送队列\n                    }'],
  ['                w.write(msg);\n                w.write("\\n");\n                w.flush();\n                Log.i("Net", "sent " + msg.length() + "B");',
   '                w.write(msg);\n                w.write("\\n");\n                w.flush();\n                lastWrite = System.currentTimeMillis();\n                Log.i("Net", "sent " + msg.length() + "B");'],
]);

// ============ 2) 默认文案 Hello,world ============
patch(ROOT + '/android/src/com/chatapp/app/Session.java', [
  ['status = prefs.getString("status", "愿你每天都有好心情");', 'status = prefs.getString("status", "Hello,world");'],
  ['status = (st == null || st.isEmpty()) ? "愿你每天都有好心情" : st;', 'status = (st == null || st.isEmpty()) ? "Hello,world" : st;'],
  ['status = (s == null || s.isEmpty()) ? "愿你每天都有好心情" : s;', 'status = (s == null || s.isEmpty()) ? "Hello,world" : s;'],
  ['status = "愿你每天都有好心情";', 'status = "Hello,world";'],
]);
patch(ROOT + '/server/server.js', [
  ["u.status = u.status || '愿你每天都有好心情';", "u.status = u.status || 'Hello,world';"],
  ["status: '愿你每天都有好心情',", "status: 'Hello,world',"],
]);

// ============ 3) 社群列表: 去掉底部分隔线和新建群聊按钮 ============
patch(ROOT + '/android/src/com/chatapp/app/HomeActivity.java', [
  ['            groupListContainer.addView(row);\n            groupListContainer.addView(Utils.divider(this));\n        }\n    }',
   '            groupListContainer.addView(row);\n            if (groups.indexOf(g) < groups.size() - 1) {\n                groupListContainer.addView(Utils.divider(this));\n            }\n        }\n    }'],
  ['        communitiesLayout.addView(Utils.hSpace(this, 24));\n        Button create = new Button(this);\n        create.setText(I18n.t("create_group"));\n        create.setTextSize(16);\n        create.setTextColor(Color.WHITE);\n        create.setBackground(Utils.bg(this, Utils.ACCENT, 24));\n        create.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                showCreateGroupDialog();\n            }\n        });\n        communitiesLayout.addView(create);\n    }',
   '    }'],
]);
patch(ROOT + '/android/src/com/chatapp/app/I18n.java', [
  ['put("no_groups", "还没有加入任何群聊\\n点下方按钮创建或等群主拉你", "還沒有加入任何群聊\\n點下方按鈕建立或等群主邀請你", "No groups yet\\nCreate one or wait for an invite");',
   'put("no_groups", "还没有加入任何群聊\\n点右上角＋创建或等群主拉你", "還沒有加入任何群聊\\n點右上角＋建立或等群主邀請你", "No groups yet\\nTap ＋ to create or wait for an invite");'],
]);

// ============ 5) +号面板: Ima / Fil / Fav ============
for (const f of ['ChatActivity.java', 'GroupChatActivity.java']) {
  const p = ROOT + '/android/src/com/chatapp/app/' + f;
  patch(p, [
    ['attachBtn("🖼️", "图片", new View.OnClickListener() {', 'attachBtn("Ima", "图片", new View.OnClickListener() {'],
    ['attachBtn("📁", "文件", new View.OnClickListener() {', 'attachBtn("Fil", "文件", new View.OnClickListener() {'],
    ['attachBtn("⭐", "收藏", new View.OnClickListener() {', 'attachBtn("Fav", "收藏", new View.OnClickListener() {'],
    // 圆圈内文字白色自适应
    ['        TextView circle = Utils.tv(this, icon, 22, Utils.TEXT, Gravity.CENTER);\n        int s = Utils.dp(this, 54);',
     '        TextView circle = Utils.tv(this, icon, 15, Color.WHITE, Gravity.CENTER);\n        circle.setTypeface(Typeface.DEFAULT_BOLD);\n        int s = Utils.dp(this, 54);'],
  ]);
}
console.log('DONE');
