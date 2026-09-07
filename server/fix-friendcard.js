'use strict';
const fs = require('fs');
const ROOT = 'C:/Users/cheng/Documents/Codex/Chatapp';

const HA = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';
let h = fs.readFileSync(HA, 'utf8');
const start = '    private Dialog friendCardDialog;';
const end = '\n    // ================= 自己页 =================';
const si = h.indexOf(start);
const ei = h.indexOf(end);
if (si >= 0 && ei > si) {
  const block = `    private Dialog friendCardDialog;
    private LinearLayout friendCardBox;
    private boolean friendCardLoaded = false;
    private String friendCardUsername = "";
    private String friendCardWith = "";
    private String friendCardNickname = "";
    private boolean friendCardHasAvatar = false;
    private boolean friendCardBlocked = false;

    private void showFriendCard(final Models.Conversation c) {
        friendCardLoaded = false;
        friendCardUsername = c.with;
        friendCardWith = c.with;
        friendCardNickname = c.nickname;
        friendCardHasAvatar = c.hasAvatar;
        friendCardBlocked = c.blocked;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8), Utils.dp(this, 6));
        box.addView(AvatarManager.avatarView(this, c.with, c.nickname, c.hasAvatar, 72, 30));
        box.addView(Utils.hSpace(this, 10));
        TextView nm = Utils.tv(this, c.nickname, 18, Utils.TEXT, Gravity.CENTER);
        nm.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(nm);
        box.addView(Utils.tv(this, "@" + c.with, 13, Utils.TEXT_DIM, Gravity.CENTER));
        String sub = c.blocked ? I18n.t("blocked_status") : I18n.t("friend_status");
        box.addView(Utils.tv(this, sub, 12, c.blocked ? Utils.DANGER : Utils.ACCENT, Gravity.CENTER));
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
        rebuildFriendCard(o.optJSONObject("profile"));
    }

    private void rebuildFriendCard(JSONObject p) {
        if (friendCardBox == null) return;
        friendCardBox.removeAllViews();
        friendCardBox.setGravity(Gravity.CENTER_HORIZONTAL);
        friendCardBox.addView(AvatarManager.avatarView(this, friendCardWith, friendCardNickname, friendCardHasAvatar, 72, 30));
        friendCardBox.addView(Utils.hSpace(this, 10));
        TextView nm = Utils.tv(this, friendCardNickname, 18, Utils.TEXT, Gravity.CENTER);
        nm.setTypeface(Typeface.DEFAULT_BOLD);
        friendCardBox.addView(nm);
        friendCardBox.addView(Utils.tv(this, "@" + friendCardWith, 13, Utils.TEXT_DIM, Gravity.CENTER));
        String sub = friendCardBlocked ? I18n.t("blocked_status") : I18n.t("friend_status");
        friendCardBox.addView(Utils.tv(this, sub, 12, friendCardBlocked ? Utils.DANGER : Utils.ACCENT, Gravity.CENTER));
        friendCardBox.addView(Utils.hSpace(this, 10));
        friendCardBox.addView(Utils.divider(this));
        friendCardBox.addView(Utils.hSpace(this, 6));
        if (p != null) appendProfileFields(friendCardBox, p);
        friendCardBox.addView(Utils.hSpace(this, 10));
        Button msg = new Button(this);
        msg.setText(I18n.t("send_message"));
        msg.setTextSize(14);
        msg.setTextColor(Color.WHITE);
        msg.setBackground(Utils.bg(this, Utils.ACCENT, 22));
        msg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChat(friendCardWith, friendCardNickname, friendCardHasAvatar);
            }
        });
        friendCardBox.addView(msg);
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
  h = h.slice(0, si) + block + h.slice(ei);
  fs.writeFileSync(HA, h);
  console.log('[HomeActivity.java] friend card unified');
} else {
  console.log('friend card anchors not found');
}
