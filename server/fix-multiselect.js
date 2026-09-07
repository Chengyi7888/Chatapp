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

// I18n 多选
patch(ROOT + '/android/src/com/chatapp/app/I18n.java', [
  ['        put("hide", "隐藏", "隱藏", "Hide");',
   '        put("hide", "隐藏", "隱藏", "Hide");\n        put("multi_select", "多选", "多選", "Multi-select");'],
]);

const methods = `    // ---------- 多选 ----------

    private void enterMultiSelect() {
        multiSelect = true;
        selectedIds.clear();
        selectBar.setVisibility(View.VISIBLE);
        updateSelectBar();
        rerender();
    }

    private void exitMultiSelect() {
        multiSelect = false;
        selectedIds.clear();
        selectBar.setVisibility(View.GONE);
        rerender();
    }

    private void toggleSelect(Models.ChatMsg m) {
        if (m.id == null || m.id.isEmpty()) return;
        if (selectedIds.contains(m.id)) selectedIds.remove(m.id);
        else selectedIds.add(m.id);
        updateSelectBar();
        rerender();
    }

    private void updateSelectBar() {
        selectCountText.setText(String.valueOf(selectedIds.size()));
    }

    private List<Models.ChatMsg> selectedMessages() {
        List<Models.ChatMsg> list = new ArrayList<>();
        for (Models.ChatMsg m : messages) {
            if (selectedIds.contains(m.id)) list.add(m);
        }
        return list;
    }

    private TextView selectActionBtn(String text, View.OnClickListener onClick) {
        TextView t = Utils.tv(this, text, 14, Utils.TEXT, Gravity.CENTER);
        t.setBackground(Utils.bg(this, Utils.CARD2, 14));
        t.setPadding(Utils.dp(this, 12), Utils.dp(this, 7), Utils.dp(this, 12), Utils.dp(this, 7));
        t.setOnClickListener(onClick);
        return t;
    }

    private void buildSelectBar() {
        selectBar = new LinearLayout(this);
        selectBar.setOrientation(LinearLayout.HORIZONTAL);
        selectBar.setGravity(Gravity.CENTER_VERTICAL);
        selectBar.setBackground(Utils.bg(this, Utils.CARD, 18));
        LinearLayout.LayoutParams sbp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sbp.setMargins(Utils.dp(this, 8), 0, Utils.dp(this, 8), 0);
        TextView close = Utils.tv(this, "✕", 18, Utils.TEXT_DIM, Gravity.CENTER);
        close.setPadding(Utils.dp(this, 10), 0, Utils.dp(this, 6), 0);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitMultiSelect();
            }
        });
        selectBar.addView(close);
        selectCountText = Utils.tv(this, "0", 13, Utils.ACCENT, Gravity.CENTER);
        selectCountText.setPadding(Utils.dp(this, 4), 0, Utils.dp(this, 10), 0);
        selectBar.addView(selectCountText);
        selectBar.addView(selectActionBtn(I18n.t("forward"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forwardSelected();
            }
        }));
        selectBar.addView(Utils.vSpace(this, 6));
        selectBar.addView(selectActionBtn(I18n.t("copy"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copySelected();
            }
        }));
        selectBar.addView(Utils.vSpace(this, 6));
        selectBar.addView(selectActionBtn(I18n.t("reply"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                replySelected();
            }
        }));
        selectBar.addView(Utils.vSpace(this, 6));
        selectBar.addView(selectActionBtn(I18n.t("delete"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSelected();
            }
        }));
        selectBar.setVisibility(View.GONE);
        root.addView(selectBar, sbp);
    }

    private void copySelected() {
        StringBuilder sb = new StringBuilder();
        for (Models.ChatMsg m : selectedMessages()) {
            if (sb.length() > 0) sb.append('\\n');
            sb.append(mediaLabel(m));
        }
        if (sb.length() == 0) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("msgs", sb.toString()));
        exitMultiSelect();
        Ui.banner(this, I18n.t("copied"));
    }

    private void replySelected() {
        StringBuilder sb = new StringBuilder();
        for (Models.ChatMsg m : selectedMessages()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(truncate8(mediaLabel(m)));
        }
        pendingReply = sb.toString();
        replyBarText.setText(I18n.t("reply_prefix") + truncate8(pendingReply));
        replyBar.setVisibility(View.VISIBLE);
        exitMultiSelect();
    }

    private void deleteSelected() {
        for (Models.ChatMsg m : selectedMessages()) {
            if (m.id == null || m.id.isEmpty()) continue;
            Set<String> set = new HashSet<>(localDel.getStringSet("DEL_KEY", new HashSet<String>()));
            set.add(m.id);
            localDel.edit().putStringSet("DEL_KEY", set).apply();
        }
        exitMultiSelect();
        Ui.banner(this, I18n.t("deleted_self"));
    }

    private void forwardSelected() {
        List<Models.ChatMsg> sel = selectedMessages();
        if (sel.isEmpty()) return;
        if (forwardContacts.isEmpty()) {
            pendingMultiForward = sel;
            try {
                JSONObject o = new JSONObject();
                o.put("type", "get_contacts");
                Net.get().send(o);
            } catch (Exception ignored) {
            }
            return;
        }
        showMultiForwardList(sel);
    }

    private void showMultiForwardList(final List<Models.ChatMsg> sel) {
        String[] names = new String[forwardContacts.size()];
        Ui.Click[] clicks = new Ui.Click[forwardContacts.size()];
        for (int i = 0; i < forwardContacts.size(); i++) {
            final Models.Contact c = forwardContacts.get(i);
            names[i] = c.nickname + " (@" + c.username + ")";
            clicks[i] = new Ui.Click() {
                @Override
                public void onClick() {
                    for (Models.ChatMsg m : sel) {
                        try {
                            JSONObject o = new JSONObject();
                            if ("image".equals(m.kind) || "file".equals(m.kind)) {
                                o.put("type", "forward_message");
                                o.put("to", c.username);
                                o.put("id", m.id);
                            } else {
                                o.put("type", "send_message");
                                o.put("to", c.username);
                                o.put("text", m.text);
                            }
                            Net.get().send(o);
                        } catch (Exception ignored) {
                        }
                    }
                    exitMultiSelect();
                    Ui.banner(this, I18n.f("forwarded_ok", c.nickname));
                }
            };
        }
        Ui.list(this, I18n.t("forward_to"), names, clicks);
    }

`;

// ============ ChatActivity ============
const CA = ROOT + '/android/src/com/chatapp/app/ChatActivity.java';
let ca = fs.readFileSync(CA, 'utf8');
let caOk = 0, caMiss = [];

function rep(h, old, neu) {
  if (h.includes(old)) { h = h.split(old).join(neu); caOk++; }
  else caMiss.push(old.slice(0, 60));
  return h;
}

// 字段
ca = rep(ca, '    private Models.ChatMsg forwardTarget;',
  '    private Models.ChatMsg forwardTarget;\n    private boolean multiSelect = false;\n    private final Set<String> selectedIds = new HashSet<>();\n    private LinearLayout selectBar;\n    private TextView selectCountText;\n    private List<Models.ChatMsg> pendingMultiForward = null;');
// 菜单加多选
ca = rep(ca, '        items.add(I18n.t("forward"));\n        items.add(I18n.t("reply"));',
  '        items.add(I18n.t("forward"));\n        items.add(I18n.t("multi_select"));\n        items.add(I18n.t("reply"));');
ca = rep(ca, '                    else if (I18n.t("forward").equals(act)) openForwardSheet(m);\n                    else if (I18n.t("reply").equals(act)) setReply(m);',
  '                    else if (I18n.t("forward").equals(act)) openForwardSheet(m);\n                    else if (I18n.t("multi_select").equals(act)) enterMultiSelect();\n                    else if (I18n.t("reply").equals(act)) setReply(m);');
// 气泡选中样式
ca = rep(ca, '        bubble.setBackground(Utils.bg(this, m.mine ? Utils.ACCENT : Color.WHITE, 8));',
  '        boolean sel = multiSelect && selectedIds.contains(m.id);\n        int bColor = m.mine ? Utils.ACCENT : Color.WHITE;\n        if (sel) bubble.setBackground(Utils.ring(this, bColor, Utils.ACCENT, 2, 8));\n        else bubble.setBackground(Utils.bg(this, bColor, 8));');
// 气泡点击切换选中
ca = rep(ca, '        bubble.setOnLongClickListener(new View.OnLongClickListener() {\n            @Override\n            public boolean onLongClick(View v) {\n                showActionMenu(m);\n                return true;\n            }\n        });\n        col.addView(bubble,',
  '        bubble.setOnLongClickListener(new View.OnLongClickListener() {\n            @Override\n            public boolean onLongClick(View v) {\n                if (multiSelect) { toggleSelect(m); return true; }\n                showActionMenu(m);\n                return true;\n            }\n        });\n        bubble.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                if (multiSelect) toggleSelect(m);\n            }\n        });\n        col.addView(bubble,');
// 图片点击
ca = rep(ca, '                @Override\n                public void onClick(View v) {\n                    showImagePreview(m);\n                }',
  '                @Override\n                public void onClick(View v) {\n                    if (multiSelect) toggleSelect(m);\n                    else showImagePreview(m);\n                }');
// 选中栏
ca = rep(ca, '        root.addView(replyBar, rlp);',
  '        root.addView(replyBar, rlp);\n        buildSelectBar();');
// 多选方法 (插到转发区前)
ca = rep(ca, '    // ---------- 转发 ----------',
  methods.replace('DEL_KEY', '"deleted_" + peer') + '    // ---------- 转发 ----------');
// contacts 处理 pendingMultiForward
ca = rep(ca, '            if (forwardSheet != null && forwardSheet.isShowing()) renderForwardList();\n            if (pendingForward != null) {',
  '            if (forwardSheet != null && forwardSheet.isShowing()) renderForwardList();\n            if (pendingMultiForward != null) {\n                List<Models.ChatMsg> pf = pendingMultiForward;\n                pendingMultiForward = null;\n                showMultiForwardList(pf);\n            }\n            if (pendingForward != null) {');
fs.writeFileSync(CA, ca);
console.log('[ChatActivity.java] ' + caOk + ' patched' + (caMiss.length ? '  MISS: ' + caMiss.join(' | ') : ''));

// ============ GroupChatActivity ============
const GA = ROOT + '/android/src/com/chatapp/app/GroupChatActivity.java';
let ga = fs.readFileSync(GA, 'utf8');
let gaOk = 0, gaMiss = [];
function repg(h, old, neu) {
  if (h.includes(old)) { h = h.split(old).join(neu); gaOk++; }
  else gaMiss.push(old.slice(0, 60));
  return h;
}
ga = repg(ga, '    private List<Models.Contact> forwardContacts = new ArrayList<>();',
  '    private List<Models.Contact> forwardContacts = new ArrayList<>();\n    private boolean multiSelect = false;\n    private final Set<String> selectedIds = new HashSet<>();\n    private LinearLayout selectBar;\n    private TextView selectCountText;\n    private List<Models.ChatMsg> pendingMultiForward = null;');
ga = repg(ga, '        items.add(I18n.t("forward"));\n        items.add(I18n.t("reply"));',
  '        items.add(I18n.t("forward"));\n        items.add(I18n.t("multi_select"));\n        items.add(I18n.t("reply"));');
ga = repg(ga, '                    else if (I18n.t("forward").equals(act)) openForwardSheet(m);\n                    else if (I18n.t("reply").equals(act)) setReply(m);',
  '                    else if (I18n.t("forward").equals(act)) openForwardSheet(m);\n                    else if (I18n.t("multi_select").equals(act)) enterMultiSelect();\n                    else if (I18n.t("reply").equals(act)) setReply(m);');
ga = repg(ga, '        bubble.setBackground(Utils.bg(this, m.mine ? Utils.ACCENT : Color.WHITE, 8));',
  '        boolean sel = multiSelect && selectedIds.contains(m.id);\n        int bColor = m.mine ? Utils.ACCENT : Color.WHITE;\n        if (sel) bubble.setBackground(Utils.ring(this, bColor, Utils.ACCENT, 2, 8));\n        else bubble.setBackground(Utils.bg(this, bColor, 8));');
ga = repg(ga, '        bubble.setOnLongClickListener(new View.OnLongClickListener() {\n            @Override\n            public boolean onLongClick(View v) {\n                showActionMenu(m);\n                return true;\n            }\n        });\n        col.addView(bubble,',
  '        bubble.setOnLongClickListener(new View.OnLongClickListener() {\n            @Override\n            public boolean onLongClick(View v) {\n                if (multiSelect) { toggleSelect(m); return true; }\n                showActionMenu(m);\n                return true;\n            }\n        });\n        bubble.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                if (multiSelect) toggleSelect(m);\n            }\n        });\n        col.addView(bubble,');
ga = repg(ga, '                @Override\n                public void onClick(View v) {\n                    MediaHelper.showImagePreview(GroupChatActivity.this, m, null);\n                }',
  '                @Override\n                public void onClick(View v) {\n                    if (multiSelect) toggleSelect(m);\n                    else MediaHelper.showImagePreview(GroupChatActivity.this, m, null);\n                }');
ga = repg(ga, '        root.addView(replyBar, rlp);',
  '        root.addView(replyBar, rlp);\n        buildSelectBar();');
ga = repg(ga, '    // ---------- 转发 ----------',
  methods.replace('DEL_KEY', '"deleted_" + groupId') + '    // ---------- 转发 ----------');
ga = repg(ga, '            if (forwardSheet != null && forwardSheet.isShowing()) renderForwardList();\n        } else if ("error".equals(type)) {',
  '            if (forwardSheet != null && forwardSheet.isShowing()) renderForwardList();\n            if (pendingMultiForward != null) {\n                List<Models.ChatMsg> pf = pendingMultiForward;\n                pendingMultiForward = null;\n                showMultiForwardList(pf);\n            }\n        } else if ("error".equals(type)) {');
fs.writeFileSync(GA, ga);
console.log('[GroupChatActivity.java] ' + gaOk + ' patched' + (gaMiss.length ? '  MISS: ' + gaMiss.join(' | ') : ''));
console.log('DONE');
