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

// HomeActivity: 补回 group2 (关于/退出) + 修改密码/注销
patch(ROOT + '/android/src/com/chatapp/app/HomeActivity.java', [
  ['        themeGroup.addView(vibrateRow);\n        meLayout.addView(themeGroup);\n    }\n\n    private String langLabel() {',
   `        themeGroup.addView(vibrateRow);
        meLayout.addView(themeGroup);

        meLayout.addView(Utils.hSpace(this, 16));

        LinearLayout group2 = new LinearLayout(this);
        group2.setOrientation(LinearLayout.VERTICAL);
        group2.setBackground(Utils.bg(this, Utils.CARD, 16));
        group2.addView(menuRow(I18n.t("change_password"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangePasswordDialog();
            }
        }));
        group2.addView(Utils.divider(this));
        group2.addView(menuRow(I18n.t("delete_account"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDeleteAccount();
            }
        }));
        group2.addView(Utils.divider(this));
        group2.addView(menuRow(I18n.t("about") + " (App v2.2.3.0)", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAboutDialog();
            }
        }));
        group2.addView(Utils.divider(this));
        group2.addView(menuRow(I18n.t("logout"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmLogout();
            }
        }));
        meLayout.addView(group2);
    }

    private String langLabel() {`],
]);

// ChatActivity: handle 'groups' 解析转发群
patch(ROOT + '/android/src/com/chatapp/app/ChatActivity.java', [
  ['        } else if ("contacts".equals(type)) {\n            forwardContacts = Models.parseContacts(o.optJSONArray("contacts"));\n            if (forwardSheet != null && forwardSheet.isShowing()) renderForwardList();\n            if (pendingMultiForward != null) {',
   '        } else if ("groups".equals(type)) {\n            forwardGroups = Models.parseGroups(o.optJSONArray("groups"));\n            if (forwardSheet != null && forwardSheet.isShowing()) renderForwardList();\n        } else if ("contacts".equals(type)) {\n            forwardContacts = Models.parseContacts(o.optJSONArray("contacts"));\n            if (forwardSheet != null && forwardSheet.isShowing()) renderForwardList();\n            if (pendingMultiForward != null) {'],
]);

// GroupChatActivity: 转发到群
const GA = ROOT + '/android/src/com/chatapp/app/GroupChatActivity.java';
patch(GA, [
  ['    private List<Models.Contact> forwardContacts = new ArrayList<>();',
   '    private List<Models.Contact> forwardContacts = new ArrayList<>();\n    private List<Models.Group> forwardGroups = new ArrayList<>();'],
  ['        try {\n            JSONObject o = new JSONObject();\n            o.put("type", "get_contacts");\n            Net.get().send(o);\n        } catch (Exception ignored) {\n        }\n    }\n\n    private void renderForwardList() {',
   '        try {\n            JSONObject o = new JSONObject();\n            o.put("type", "get_contacts");\n            Net.get().send(o);\n            JSONObject g = new JSONObject();\n            g.put("type", "get_groups");\n            Net.get().send(g);\n        } catch (Exception ignored) {\n        }\n    }\n\n    private void renderForwardList() {'],
  ['            forwardListContainer.addView(row);\n            forwardListContainer.addView(Utils.divider(this));\n        }\n    }\n\n    private void confirmForward(final Models.Contact c) {',
   '            forwardListContainer.addView(row);\n            forwardListContainer.addView(Utils.divider(this));\n        }\n        if (!forwardGroups.isEmpty()) {\n            TextView gh = Utils.tv(this, I18n.t("forward_to_group"), 13, Utils.ACCENT, Gravity.START);\n            gh.setPadding(0, Utils.dp(this, 10), 0, Utils.dp(this, 4));\n            forwardListContainer.addView(gh);\n            for (final Models.Group g : forwardGroups) {\n                LinearLayout row = new LinearLayout(this);\n                row.setOrientation(LinearLayout.HORIZONTAL);\n                row.setGravity(Gravity.CENTER_VERTICAL);\n                row.setPadding(Utils.dp(this, 6), Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8));\n                row.addView(Utils.letterAvatar(this, g.name, 40, 15));\n                row.addView(Utils.vSpace(this, 10));\n                row.addView(Utils.tv(this, g.name + " (" + g.memberCount + ")", 15, Utils.TEXT, Gravity.START), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));\n                row.setOnClickListener(new View.OnClickListener() {\n                    @Override\n                    public void onClick(View v) {\n                        confirmForwardGroup(g);\n                    }\n                });\n                forwardListContainer.addView(row);\n                forwardListContainer.addView(Utils.divider(this));\n            }\n        }\n    }\n\n    private void confirmForwardGroup(final Models.Group g) {\n        try {\n            JSONObject o = new JSONObject();\n            o.put("type", "forward_group_message");\n            o.put("group", g.id);\n            o.put("messageId", forwardTarget.id);\n            Net.get().send(o);\n            Ui.banner(this, I18n.f("forwarded_ok", g.name));\n        } catch (Exception ignored) {\n        }\n        if (forwardSheet != null && forwardSheet.isShowing()) forwardSheet.dismiss();\n    }\n\n    private void confirmForward(final Models.Contact c) {'],
  ['        } else if ("contacts".equals(type)) {\n            forwardContacts = Models.parseContacts(o.optJSONArray("contacts"));\n            if (forwardSheet != null && forwardSheet.isShowing()) renderForwardList();\n            if (pendingMultiForward != null) {',
   '        } else if ("groups".equals(type)) {\n            forwardGroups = Models.parseGroups(o.optJSONArray("groups"));\n            if (forwardSheet != null && forwardSheet.isShowing()) renderForwardList();\n        } else if ("contacts".equals(type)) {\n            forwardContacts = Models.parseContacts(o.optJSONArray("contacts"));\n            if (forwardSheet != null && forwardSheet.isShowing()) renderForwardList();\n            if (pendingMultiForward != null) {'],
]);
console.log('DONE');
