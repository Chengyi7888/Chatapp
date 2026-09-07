'use strict';
const fs = require('fs');
const ROOT = 'C:/Users/cheng/Documents/Codex/Chatapp';

const GD = ROOT + '/android/src/com/chatapp/app/GroupDetailActivity.java';
let t = fs.readFileSync(GD, 'utf8');
const start = '    private Dialog memberCardDialog;';
const end = '\n    private void confirmClearHistory() {';
const si = t.indexOf(start);
const ei = t.indexOf(end);
if (si >= 0 && ei > si) {
  const block = `    private Dialog memberCardDialog;
    private LinearLayout memberCardBox;
    private boolean memberCardLoaded = false;
    private String memberCardUsername = "";
    private String memberCardNickname = "";
    private boolean memberCardHasAvatar = false;

    private void showMemberCard(final Models.GroupMember m) {
        memberCardLoaded = false;
        memberCardUsername = m.username;
        memberCardNickname = m.nickname;
        memberCardHasAvatar = m.hasAvatar;
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
        rebuildMemberCard(o.optJSONObject("profile"));
    }

    private void rebuildMemberCard(JSONObject p) {
        if (memberCardBox == null) return;
        memberCardBox.removeAllViews();
        memberCardBox.setGravity(Gravity.CENTER_HORIZONTAL);
        memberCardBox.addView(AvatarManager.avatarView(this, memberCardUsername, memberCardNickname, memberCardHasAvatar, 64, 26));
        memberCardBox.addView(Utils.hSpace(this, 8));
        TextView nm = Utils.tv(this, memberCardNickname, 17, Utils.TEXT, Gravity.CENTER);
        nm.setTypeface(Typeface.DEFAULT_BOLD);
        memberCardBox.addView(nm);
        memberCardBox.addView(Utils.tv(this, "@" + memberCardUsername, 13, Utils.TEXT_DIM, Gravity.CENTER));
        memberCardBox.addView(Utils.hSpace(this, 10));
        memberCardBox.addView(Utils.divider(this));
        memberCardBox.addView(Utils.hSpace(this, 6));
        if (p != null) appendProfileFields(memberCardBox, p);
        // 非好友且不是自己 -> 添加好友
        boolean isSelf = memberCardUsername.equals(Session.username);
        boolean isFriend = false;
        for (Models.Contact c : contacts) {
            if (c.username.equals(memberCardUsername)) isFriend = true;
        }
        if (!isSelf && !isFriend) {
            memberCardBox.addView(Utils.hSpace(this, 10));
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
                        o.put("username", memberCardUsername);
                        Net.get().send(o);
                    } catch (Exception ignored) {
                    }
                    ((Button) v).setText(I18n.t("request_sent"));
                    v.setEnabled(false);
                }
            });
            memberCardBox.addView(addBtn);
        }
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
        addField(box, I18n.t("tags"), tagStr.length() > 0 ? tagStr.toString() : "");
        addField(box, I18n.t("job"), p.optString("job"));
        addField(box, I18n.t("company"), p.optString("company"));
        addField(box, I18n.t("location"), p.optString("location"));
        addField(box, I18n.t("birthplace"), p.optString("birthplace"));
        addField(box, I18n.t("email"), p.optString("email"));
        JSONArray photos = p.optJSONArray("photos");
        int photoCount = photos == null ? 0 : photos.length();
        addField(box, I18n.t("my_photos"), photoCount > 0 ? I18n.f("photo_count", photoCount) : "");
    }

    private void addField(LinearLayout box, String label, String value) {
        if (value == null || value.isEmpty()) value = "—";
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, Utils.dp(this, 4), 0, Utils.dp(this, 4));
        row.addView(Utils.tv(this, label + "：", 13, Utils.TEXT, Gravity.START));
        TextView v = Utils.tv(this, value, 13, Utils.TEXT_DIM, Gravity.START);
        v.setPadding(Utils.dp(this, 8), 0, 0, 0);
        row.addView(v, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(row);
    }

`;
  t = t.slice(0, si) + block + t.slice(ei);
  fs.writeFileSync(GD, t);
  console.log('[GroupDetailActivity.java] member card unified');
} else {
  console.log('member card anchors not found si=' + si + ' ei=' + ei);
}
