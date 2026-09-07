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

const GA = ROOT + '/android/src/com/chatapp/app/GroupChatActivity.java';
const CA = ROOT + '/android/src/com/chatapp/app/ChatActivity.java';
const GD = ROOT + '/android/src/com/chatapp/app/GroupDetailActivity.java';
const HA = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';

// ============ GroupChatActivity ============
// 1) 右上角 … 直接进群聊信息
patch(GA, [
  ['        infoBtn.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                showGroupMenu();\n            }\n        });',
   '        infoBtn.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                openDetail();\n            }\n        });'],
  // 2) 气泡行: 横向布局 + 头像 + 昵称
  ['        LinearLayout row = new LinearLayout(this);\n        row.setOrientation(LinearLayout.VERTICAL);\n        row.setGravity(m.mine ? Gravity.END : Gravity.START);\n        row.setPadding(Utils.dp(this, 4), Utils.dp(this, 2), Utils.dp(this, 4), Utils.dp(this, 2));\n\n        // 非本人消息显示发送者昵称\n        if (!m.mine) {\n            String sender = (m.nickname != null && !m.nickname.isEmpty()) ? m.nickname : m.from;\n            TextView sn = Utils.tv(this, sender, 11, Utils.TEXT_DIM, Gravity.START);\n            sn.setPadding(Utils.dp(this, 8), 0, 0, Utils.dp(this, 2));\n            row.addView(sn);\n        }',
   '        LinearLayout row = new LinearLayout(this);\n        row.setOrientation(LinearLayout.HORIZONTAL);\n        row.setGravity(m.mine ? Gravity.END : Gravity.START);\n        row.setPadding(Utils.dp(this, 4), Utils.dp(this, 2), Utils.dp(this, 4), Utils.dp(this, 2));\n\n        // 非本人消息: 左侧头像\n        if (!m.mine) {\n            String sender = (m.nickname != null && !m.nickname.isEmpty()) ? m.nickname : m.from;\n            row.addView(AvatarManager.avatarView(this, m.from, sender, m.senderHasAvatar, 36, 14));\n            row.addView(Utils.vSpace(this, 8));\n        }\n\n        LinearLayout col = new LinearLayout(this);\n        col.setOrientation(LinearLayout.VERTICAL);\n        col.setGravity(m.mine ? Gravity.END : Gravity.START);\n\n        // 非本人消息: 昵称显示在气泡上方\n        if (!m.mine) {\n            String sender = (m.nickname != null && !m.nickname.isEmpty()) ? m.nickname : m.from;\n            TextView sn = Utils.tv(this, sender, 11, Utils.TEXT_DIM, Gravity.START);\n            sn.setPadding(Utils.dp(this, 6), 0, Utils.dp(this, 6), Utils.dp(this, 2));\n            col.addView(sn);\n        }'],
  ['        row.addView(bubble, new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));',
   '        col.addView(bubble, new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));'],
  ['        TextView time = Utils.tv(this, Utils.timeText(m.time), 10, Utils.TEXT_DIM, Gravity.END);\n        time.setPadding(0, Utils.dp(this, 2), m.mine ? Utils.dp(this, 4) : 0, 0);\n        row.addView(time);',
   '        TextView time = Utils.tv(this, Utils.timeText(m.time), 10, Utils.TEXT_DIM, Gravity.END);\n        time.setPadding(0, Utils.dp(this, 2), m.mine ? Utils.dp(this, 4) : 0, 0);\n        col.addView(time);\n\n        row.addView(col);\n\n        // 本人消息: 右侧头像\n        if (m.mine) {\n            row.addView(Utils.vSpace(this, 8));\n            row.addView(AvatarManager.avatarView(this, Session.username, Session.nickname, Session.hasAvatar, 36, 14));\n        }'],
]);

// 删除 showGroupMenu + confirmClearGroupHistory 方法 (保留 openDetail)
{
  let t = fs.readFileSync(GA, 'utf8');
  const startMark = '    private void showGroupMenu() {';
  const endMark = '    // ---------- 转发 ----------';
  const si = t.indexOf(startMark);
  const ei = t.indexOf(endMark);
  if (si >= 0 && ei > si) {
    t = t.slice(0, si) + t.slice(ei);
    fs.writeFileSync(GA, t);
    console.log('[GroupChatActivity.java] removed group menu methods');
  } else {
    console.log('[GroupChatActivity.java] menu methods anchors not found si=' + si + ' ei=' + ei);
  }
}

// ============ ChatActivity: 头像 + peerHasAvatar ============
patch(CA, [
  ['    private String peer;\n    private String peerNickname;',
   '    private String peer;\n    private String peerNickname;\n    private boolean peerHasAvatar = false;'],
  ['        if (peerNickname == null) peerNickname = peer;',
   '        if (peerNickname == null) peerNickname = peer;\n        peerHasAvatar = it.getBooleanExtra("hasAvatar", false);'],
  ['        LinearLayout row = new LinearLayout(this);\n        row.setOrientation(LinearLayout.VERTICAL);\n        row.setGravity(m.mine ? Gravity.END : Gravity.START);\n        row.setPadding(Utils.dp(this, 4), Utils.dp(this, 2), Utils.dp(this, 4), Utils.dp(this, 2));',
   '        LinearLayout row = new LinearLayout(this);\n        row.setOrientation(LinearLayout.HORIZONTAL);\n        row.setGravity(m.mine ? Gravity.END : Gravity.START);\n        row.setPadding(Utils.dp(this, 4), Utils.dp(this, 2), Utils.dp(this, 4), Utils.dp(this, 2));\n\n        // 非本人消息: 左侧头像\n        if (!m.mine) {\n            row.addView(AvatarManager.avatarView(this, peer, peerNickname, peerHasAvatar, 36, 14));\n            row.addView(Utils.vSpace(this, 8));\n        }\n\n        LinearLayout col = new LinearLayout(this);\n        col.setOrientation(LinearLayout.VERTICAL);\n        col.setGravity(m.mine ? Gravity.END : Gravity.START);'],
  ['        row.addView(bubble, new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));',
   '        col.addView(bubble, new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));'],
  ['        TextView time = Utils.tv(this, Utils.timeText(m.time), 10, Utils.TEXT_DIM, Gravity.END);\n        time.setPadding(0, Utils.dp(this, 2), m.mine ? Utils.dp(this, 4) : 0, 0);\n        row.addView(time);',
   '        TextView time = Utils.tv(this, Utils.timeText(m.time), 10, Utils.TEXT_DIM, Gravity.END);\n        time.setPadding(0, Utils.dp(this, 2), m.mine ? Utils.dp(this, 4) : 0, 0);\n        col.addView(time);\n\n        row.addView(col);\n\n        // 本人消息: 右侧头像\n        if (m.mine) {\n            row.addView(Utils.vSpace(this, 8));\n            row.addView(AvatarManager.avatarView(this, Session.username, Session.nickname, Session.hasAvatar, 36, 14));\n        }'],
]);

// ============ GroupDetailActivity: 移除"进入群聊"按钮 ============
patch(GD, [
  ['        Button enterChat = new Button(this);\n        enterChat.setText(I18n.t("enter_group_chat"));\n        enterChat.setTextSize(16);\n        enterChat.setTextColor(Color.WHITE);\n        enterChat.setBackground(Utils.bg(this, Utils.ACCENT, 24));\n        enterChat.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                Intent i = new Intent(GroupDetailActivity.this, GroupChatActivity.class);\n                i.putExtra("groupId", groupId);\n                i.putExtra("groupName", group == null ? "" : group.name);\n                i.putExtra("isOwner", group != null && group.owner.equals(Session.username));\n                startActivity(i);\n            }\n        });\n        body.addView(enterChat);\n\n        sc.addView(body);',
   '        sc.addView(body);'],
]);

// ============ HomeActivity: 传 hasAvatar ============
patch(HA, [
  ['    private void openChat(String username, String nickname) {\n        Intent i = new Intent(this, ChatActivity.class);\n        i.putExtra("peer", username);\n        i.putExtra("nickname", nickname);\n        startActivity(i);\n    }',
   '    private void openChat(String username, String nickname, boolean hasAvatar) {\n        Intent i = new Intent(this, ChatActivity.class);\n        i.putExtra("peer", username);\n        i.putExtra("nickname", nickname);\n        i.putExtra("hasAvatar", hasAvatar);\n        startActivity(i);\n    }'],
  ['            Intent i = new Intent(this, ChatActivity.class);\n            i.putExtra("peer", c.with);\n            i.putExtra("nickname", c.nickname);\n            startActivity(i);',
   '            Intent i = new Intent(this, ChatActivity.class);\n            i.putExtra("peer", c.with);\n            i.putExtra("nickname", c.nickname);\n            i.putExtra("hasAvatar", c.hasAvatar);\n            startActivity(i);'],
  ['                openChat(c.username, c.nickname);', '                openChat(c.username, c.nickname, c.hasAvatar);'],
  ['                openChat(c.with, c.nickname);', '                openChat(c.with, c.nickname, c.hasAvatar);'],
  ['                    openChat(username, nickname);', '                    openChat(username, nickname, false);'],
]);
console.log('DONE');
