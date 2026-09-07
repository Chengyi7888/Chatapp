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

const HA = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';

// 1) 状态即时刷新: renderUpdates 末尾更新状态灯
patch(HA, [
  ['        updatesLayout.addView(channel);\n    }',
   '        updatesLayout.addView(channel);\n        updateStatusUi();\n    }'],
  // 2) 后台恢复: 断线立即重连
  ['            Net.get().setListener(this);\n            if (Net.get().isRunning()) {\n                if (Net.get().isConnected()) refreshAll();\n            } else {',
   '            Net.get().setListener(this);\n            if (Net.get().isRunning()) {\n                if (Net.get().isConnected()) refreshAll();\n                else {\n                    // 进程在但已断线: 立即重连, 不等默认3秒\n                    String[] sp = Session.serverParts();\n                    Net.get().stop();\n                    Net.get().start(sp[0], Integer.parseInt(sp[1]));\n                }\n            } else {'],
  // 3) 更新页动态头像不再支持换头像
  ['        final View dynAvatar = AvatarManager.avatarView(this, Session.username, Session.nickname, Session.hasAvatar, 52, 26);\n        dynAvatar.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                pickAvatar();\n            }\n        });\n        dyn.addView(dynAvatar);',
   '        final View dynAvatar = AvatarManager.avatarView(this, Session.username, Session.nickname, Session.hasAvatar, 52, 26);\n        dyn.addView(dynAvatar);'],
]);

// 4) 好友名片同步资料: 替换 showFriendCard
{
  let h = fs.readFileSync(HA, 'utf8');
  const start = '    private void showFriendCard(final Models.Conversation c) {';
  const end = '\n    // ================= 自己页 =================';
  const si = h.indexOf(start);
  const ei = h.indexOf(end);
  if (si >= 0 && ei > si) {
    const fields = `    private Dialog friendCardDialog;
    private LinearLayout friendCardBox;
    private boolean friendCardLoaded = false;
    private String friendCardUsername = "";

    private void showFriendCard(final Models.Conversation c) {
        friendCardLoaded = false;
        friendCardUsername = c.with;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8), Utils.dp(this, 6));
        box.addView(AvatarManager.avatarView(this, c.with, c.nickname, c.hasAvatar, 72, 30));
        box.addView(Utils.hSpace(this, 10));
        TextView nm = Utils.tv(this, c.nickname, 18, Utils.TEXT, Gravity.CENTER);
        nm.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(nm);
        TextView un = Utils.tv(this, "@" + c.with, 13, Utils.TEXT_DIM, Gravity.CENTER);
        box.addView(un);
        String sub = c.blocked ? I18n.t("blocked_status") : I18n.t("friend_status");
        TextView st = Utils.tv(this, sub, 12, c.blocked ? Utils.DANGER : Utils.ACCENT, Gravity.CENTER);
        box.addView(st);
        box.addView(Utils.hSpace(this, 12));
        Button msg = new Button(this);
        msg.setText(I18n.t("send_message"));
        msg.setTextSize(14);
        msg.setTextColor(Color.WHITE);
        msg.setBackground(Utils.bg(this, Utils.ACCENT, 22));
        msg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChat(c.with, c.nickname, c.hasAvatar);
            }
        });
        box.addView(msg);
        friendCardBox = box;
        friendCardDialog = Ui.showSingle(this, I18n.t("friend_card"), box, I18n.t("close"), null);
        // 请求对方完整名片
        try {
            JSONObject o = new JSONObject();
            o.put("type", "get_user_profile");
            o.put("username", c.with);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    private void fillFriendCard(JSONObject o) {
        if (friendCardDialog == null || !friendCardDialog.isShowing() || friendCardLoaded) return;
        if (!friendCardUsername.equals(o.optString("username"))) return;
        friendCardLoaded = true;
        JSONObject p = o.optJSONObject("profile");
        if (p != null) appendProfileFields(friendCardBox, p);
    }

    private void appendProfileFields(LinearLayout box, JSONObject p) {
        addField(box, I18n.t("gender"), p.optString("gender"));
        addField(box, I18n.t("birthday"), p.optString("birthday"));
        StringBuilder tagStr = new StringBuilder();
        JSONArray tags = p.optJSONArray("tags");
        if (tags != null) {
            for (int i = 0; i < tags.length(); i++) {
                if (tagStr.length() > 0) tagStr.append(" ");
                tagStr.append(tags.optString(i));
            }
        }
        if (tagStr.length() > 0) addField(box, I18n.t("tags"), tagStr.toString());
        addField(box, I18n.t("job"), p.optString("job"));
        addField(box, I18n.t("company"), p.optString("company"));
        addField(box, I18n.t("location"), p.optString("location"));
        addField(box, I18n.t("birthplace"), p.optString("birthplace"));
        addField(box, I18n.t("email"), p.optString("email"));
        JSONArray photos = p.optJSONArray("photos");
        int photoCount = photos == null ? 0 : photos.length();
        if (photoCount > 0) addField(box, I18n.t("my_photos"), I18n.f("photo_count", photoCount));
    }

    private void addField(LinearLayout box, String label, String value) {
        if (value == null || value.isEmpty()) return;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Utils.dp(this, 4), 0, Utils.dp(this, 4));
        row.addView(Utils.tv(this, label + "：", 13, Utils.TEXT, Gravity.START));
        TextView v = Utils.tv(this, value, 13, Utils.TEXT_DIM, Gravity.START);
        v.setPadding(Utils.dp(this, 8), 0, 0, 0);
        row.addView(v, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(row);
    }
`;
    h = h.slice(0, si) + fields + h.slice(ei);
    fs.writeFileSync(HA, h);
    console.log('[HomeActivity.java] showFriendCard replaced');
  } else {
    console.log('showFriendCard anchors not found');
  }
}

// 5) handle: user_profile 分支
patch(HA, [
  ['        } else if ("group_history_cleared".equals(type)) {\n            refreshAll();\n        } else if ("friend_request".equals(type)) {',
   '        } else if ("group_history_cleared".equals(type)) {\n            refreshAll();\n        } else if ("user_profile".equals(type)) {\n            fillFriendCard(o);\n        } else if ("friend_request".equals(type)) {'],
]);
console.log('DONE');
