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

const GD = ROOT + '/android/src/com/chatapp/app/GroupDetailActivity.java';

patch(GD, [
  // import JSONArray
  ['import org.json.JSONObject;', 'import org.json.JSONArray;\nimport org.json.JSONObject;'],
  // 群头像居中
  ['        avatarBox = new LinearLayout(this);\n        avatarBox.setOrientation(LinearLayout.VERTICAL);\n        head.addView(avatarBox);',
   '        avatarBox = new LinearLayout(this);\n        avatarBox.setOrientation(LinearLayout.VERTICAL);\n        avatarBox.setGravity(Gravity.CENTER_HORIZONTAL);\n        avatarBox.setLayoutParams(new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));\n        head.addView(avatarBox);'],
  // 成员头像可点击 -> 名片
  ['            row.addView(AvatarManager.avatarView(this, m.username, m.nickname, m.hasAvatar, 40, 15));',
   '            final View memAv = AvatarManager.avatarView(this, m.username, m.nickname, m.hasAvatar, 40, 15);\n            memAv.setOnClickListener(new View.OnClickListener() {\n                @Override\n                public void onClick(View v) {\n                    showMemberCard(m);\n                }\n            });\n            row.addView(memAv);'],
  // handle: user_profile
  ['        } else if ("group_history_cleared".equals(type)) {\n            if (groupId.equals(o.optString("id"))) {\n                Ui.banner(this, I18n.t("clear_group_history_confirm"));\n            }\n        } else if ("group_left".equals(type)) {',
   '        } else if ("group_history_cleared".equals(type)) {\n            if (groupId.equals(o.optString("id"))) {\n                Ui.banner(this, I18n.t("clear_group_history_confirm"));\n            }\n        } else if ("user_profile".equals(type)) {\n            fillMemberCard(o);\n        } else if ("group_left".equals(type)) {'],
]);

// 追加成员名片方法 (插到 confirmClearHistory 前)
{
  let t = fs.readFileSync(GD, 'utf8');
  const anchor = '    private void confirmClearHistory() {';
  const methods = `    private Dialog memberCardDialog;
    private LinearLayout memberCardBox;
    private boolean memberCardLoaded = false;
    private String memberCardUsername = "";

    private void showMemberCard(final Models.GroupMember m) {
        memberCardLoaded = false;
        memberCardUsername = m.username;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8), Utils.dp(this, 6));
        box.addView(AvatarManager.avatarView(this, m.username, m.nickname, m.hasAvatar, 64, 26));
        box.addView(Utils.hSpace(this, 8));
        TextView nm = Utils.tv(this, m.nickname, 17, Utils.TEXT, Gravity.CENTER);
        nm.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(nm);
        box.addView(Utils.tv(this, "@" + m.username, 13, Utils.TEXT_DIM, Gravity.CENTER));
        boolean isSelf = m.username.equals(Session.username);
        boolean isFriend = false;
        for (Models.Contact c : contacts) {
            if (c.username.equals(m.username)) isFriend = true;
        }
        if (!isSelf && !isFriend) {
            Button addBtn = new Button(this);
            addBtn.setText(I18n.t("add_friend"));
            addBtn.setTextSize(14);
            addBtn.setTextColor(Color.WHITE);
            addBtn.setBackground(Utils.bg(this, Utils.ACCENT, 22));
            addBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        JSONObject o = new JSONObject();
                        o.put("type", "add_friend");
                        o.put("username", m.username);
                        Net.get().send(o);
                    } catch (Exception ignored) {
                    }
                    ((Button) v).setText(I18n.t("request_sent"));
                    v.setEnabled(false);
                }
            });
            box.addView(addBtn);
        }
        memberCardBox = box;
        memberCardDialog = Ui.showSingle(this, I18n.t("friend_card"), box, I18n.t("close"), null);
        try {
            JSONObject o = new JSONObject();
            o.put("type", "get_user_profile");
            o.put("username", m.username);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    private void fillMemberCard(JSONObject o) {
        if (memberCardDialog == null || !memberCardDialog.isShowing() || memberCardLoaded) return;
        if (!memberCardUsername.equals(o.optString("username"))) return;
        memberCardLoaded = true;
        JSONObject p = o.optJSONObject("profile");
        if (p != null) appendProfileFields(memberCardBox, p);
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

    private void confirmClearHistory() {`;
  if (t.includes(anchor)) {
    t = t.split(anchor).join(methods);
    fs.writeFileSync(GD, t);
    console.log('[GroupDetailActivity.java] member card methods added');
  } else {
    console.log('anchor not found');
  }
}
console.log('DONE');
