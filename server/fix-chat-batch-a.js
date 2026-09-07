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

const CA = ROOT + '/android/src/com/chatapp/app/ChatActivity.java';

patch(CA, [
  // imports
  ['import android.os.Bundle;', 'import android.os.Bundle;\nimport android.os.Handler;\nimport android.os.Looper;'],
  // 字段
  ['    private LinearLayout replyBar;\n    private LinearLayout attachPanel;\n    private ProgressBar loading;',
   '    private LinearLayout replyBar;\n    private LinearLayout attachPanel;\n    private ProgressBar loading;\n    private long peerReadTime = 0;\n    private TextView typingText;\n    private LinearLayout searchRow;\n    private EditText searchInput;\n    private String searchQuery = "";\n    private final Handler typingHide = new Handler(Looper.getMainLooper());\n    private SharedPreferences pinPrefs;\n    private SharedPreferences draftPrefs;'],
  // pin/draft 初始化
  ['        localDel = getSharedPreferences("chatapp_chat", MODE_PRIVATE);',
   '        localDel = getSharedPreferences("chatapp_chat", MODE_PRIVATE);\n        pinPrefs = getSharedPreferences("chatapp_msg_pin", MODE_PRIVATE);\n        draftPrefs = getSharedPreferences("chatapp_drafts", MODE_PRIVATE);'],
  // 顶栏右侧: 搜索 + 导出
  ['        top.addView(title, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));\n        root.addView(top, topLp);',
   '        top.addView(title, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));\n        TextView searchBtn = Utils.tv(this, "🔍", 18, Utils.TEXT, Gravity.CENTER);\n        searchBtn.setPadding(Utils.dp(this, 8), 0, Utils.dp(this, 4), 0);\n        searchBtn.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                toggleSearch();\n            }\n        });\n        top.addView(searchBtn);\n        TextView exportBtn = Utils.tv(this, I18n.t("export_chat"), 13, Utils.ACCENT, Gravity.CENTER);\n        exportBtn.setPadding(Utils.dp(this, 8), 0, Utils.dp(this, 6), 0);\n        exportBtn.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                exportChat();\n            }\n        });\n        top.addView(exportBtn);\n        root.addView(top, topLp);\n\n        // 消息搜索行\n        searchRow = new LinearLayout(this);\n        searchRow.setOrientation(LinearLayout.HORIZONTAL);\n        searchRow.setGravity(Gravity.CENTER_VERTICAL);\n        searchRow.setBackground(Utils.bg(this, Utils.CARD, 18));\n        LinearLayout.LayoutParams srp = new LinearLayout.LayoutParams(\n                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);\n        srp.setMargins(Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8), 0);\n        searchInput = new EditText(this);\n        searchInput.setHint(I18n.t("search_msg"));\n        searchInput.setSingleLine(true);\n        searchInput.setTextSize(14);\n        searchInput.setTextColor(Utils.TEXT);\n        searchInput.setHintTextColor(Utils.TEXT_DIM);\n        searchInput.setBackground(Utils.bg(this, Utils.CARD2, 14));\n        searchInput.setPadding(Utils.dp(this, 12), Utils.dp(this, 7), Utils.dp(this, 12), Utils.dp(this, 7));\n        searchInput.addTextChangedListener(new TextWatcher() {\n            @Override\n            public void beforeTextChanged(CharSequence s, int a, int b, int c) {\n            }\n\n            @Override\n            public void onTextChanged(CharSequence s, int a, int b, int c) {\n                searchQuery = s.toString();\n                rerender();\n            }\n\n            @Override\n            public void afterTextChanged(Editable s) {\n            }\n        });\n        searchRow.addView(searchInput, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));\n        TextView closeS = Utils.tv(this, "✕", 16, Utils.TEXT_DIM, Gravity.CENTER);\n        closeS.setPadding(Utils.dp(this, 10), 0, Utils.dp(this, 8), 0);\n        closeS.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                searchRow.setVisibility(View.GONE);\n                searchQuery = "";\n                searchInput.setText("");\n                rerender();\n            }\n        });\n        searchRow.addView(closeS);\n        searchRow.setVisibility(View.GONE);\n        root.addView(searchRow, srp);'],
  // 输入中提示条 (scroll 之后)
  ['        root.addView(scroll, Utils.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));\n\n        // 回复指示条',
   '        root.addView(scroll, Utils.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));\n\n        typingText = Utils.tv(this, I18n.t("typing_hint"), 11, Utils.ACCENT, Gravity.START);\n        typingText.setPadding(Utils.dp(this, 12), Utils.dp(this, 4), Utils.dp(this, 12), Utils.dp(this, 4));\n        typingText.setVisibility(View.GONE);\n        root.addView(typingText);\n\n        // 回复指示条'],
  // 输入监听: 草稿 + 输入中
  ['        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {\n            @Override\n            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {\n                if (actionId == EditorInfo.IME_ACTION_SEND) {\n                    sendText();\n                    return true;\n                }\n                return false;\n            }\n        });\n        inputRow.addView(input, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));',
   '        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {\n            @Override\n            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {\n                if (actionId == EditorInfo.IME_ACTION_SEND) {\n                    sendText();\n                    return true;\n                }\n                return false;\n            }\n        });\n        input.addTextChangedListener(new TextWatcher() {\n            @Override\n            public void beforeTextChanged(CharSequence s, int a, int b, int c) {\n            }\n\n            @Override\n            public void onTextChanged(CharSequence s, int a, int b, int c) {\n                draftPrefs.edit().putString("draft_" + peer, s.toString()).apply();\n                if (s.length() > 0) sendTyping();\n            }\n\n            @Override\n            public void afterTextChanged(Editable s) {\n            }\n        });\n        inputRow.addView(input, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));'],
  // onResume 恢复草稿
  ['        Net.get().setListener(this);\n        if (Net.get().isConnected()) {\n            requestHistory();\n        }\n    }',
   '        Net.get().setListener(this);\n        if (Net.get().isConnected()) {\n            requestHistory();\n        }\n        String d = draftPrefs.getString("draft_" + peer, "");\n        if (d != null && !d.isEmpty()) {\n            input.setText(d);\n            input.setSelection(d.length());\n        }\n    }'],
  // 历史: 已读时间
  ['            messages.add(m);\n            }\n            rerender();\n            if (loading != null) loading.setVisibility(View.GONE);\n        } else if ("new_message".equals(type)) {',
   '            messages.add(m);\n            }\n            peerReadTime = o.optLong("peerReadTime", 0);\n            rerender();\n            if (loading != null) loading.setVisibility(View.GONE);\n        } else if ("new_message".equals(type)) {'],
  // peer_read / peer_typing 处理
  ['        } else if ("contacts".equals(type)) {\n            forwardContacts = Models.parseContacts(o.optJSONArray("contacts"));',
   '        } else if ("peer_read".equals(type)) {\n            if (peer.equals(o.optString("with"))) {\n                peerReadTime = o.optLong("time");\n                rerender();\n            }\n        } else if ("peer_typing".equals(type)) {\n            if (peer.equals(o.optString("from"))) {\n                typingText.setVisibility(View.VISIBLE);\n                typingHide.removeCallbacks(typingHideTask);\n                typingHide.postDelayed(typingHideTask, 3000);\n            }\n        } else if ("contacts".equals(type)) {\n            forwardContacts = Models.parseContacts(o.optJSONArray("contacts"));'],
  // 长按菜单: 置顶
  ['        items.add(I18n.t("forward"));\n        items.add(I18n.t("multi_select"));\n        items.add(I18n.t("reply"));',
   '        items.add(I18n.t("forward"));\n        items.add(I18n.t("multi_select"));\n        boolean pinned = pinPrefs.getStringSet("pin_" + peer, new HashSet<String>()).contains(m.id);\n        items.add(pinned ? I18n.t("unpin_msg") : I18n.t("pin_msg"));\n        items.add(I18n.t("reply"));'],
  ['                    else if (I18n.t("multi_select").equals(act)) enterMultiSelect();\n                    else if (I18n.t("reply").equals(act)) setReply(m);',
   '                    else if (I18n.t("multi_select").equals(act)) enterMultiSelect();\n                    else if (I18n.t("pin_msg").equals(act) || I18n.t("unpin_msg").equals(act)) togglePin(m);\n                    else if (I18n.t("reply").equals(act)) setReply(m);'],
]);

// rerender 替换 (置顶+搜索+已读)
{
  let t = fs.readFileSync(CA, 'utf8');
  const startMark = '    private void rerender() {';
  const endMark = '\n    private void addBubble(final Models.ChatMsg m) {';
  const si = t.indexOf(startMark);
  const ei = t.indexOf(endMark);
  if (si >= 0 && ei > si) {
    const neu = `    private void rerender() {
        msgList.removeAllViews();
        Set<String> pins = pinPrefs.getStringSet("pin_" + peer, new HashSet<String>());
        List<Models.ChatMsg> pinnedList = new ArrayList<>();
        List<Models.ChatMsg> normalList = new ArrayList<>();
        Models.ChatMsg lastMine = null;
        for (Models.ChatMsg m : messages) {
            if (isDeleted(m.id)) continue;
            if (pins.contains(m.id)) pinnedList.add(m); else normalList.add(m);
            if (m.mine) lastMine = m;
        }
        List<Models.ChatMsg> ordered = new ArrayList<>(pinnedList);
        ordered.addAll(normalList);
        for (Models.ChatMsg m : ordered) {
            if (!searchQuery.isEmpty()) {
                String hay = m.text == null ? "" : m.text;
                if (!hay.toLowerCase().contains(searchQuery.toLowerCase())) continue;
            }
            addBubble(m, pins.contains(m.id), m == lastMine && peerReadTime >= m.time);
        }
    }
`;
    t = t.slice(0, si) + neu + t.slice(ei);
    fs.writeFileSync(CA, t);
    console.log('[ChatActivity.java] rerender replaced');
  } else {
    console.log('rerender anchors not found');
  }
}

// addBubble 签名 + 置顶徽标 + 已读标签
{
  let t = fs.readFileSync(CA, 'utf8');
  // 签名
  t = t.split('    private void addBubble(final Models.ChatMsg m) {')
        .join('    private void addBubble(final Models.ChatMsg m, final boolean pinned, final boolean showRead) {');
  // appendMsg 调用
  t = t.split('    private void appendMsg(Models.ChatMsg m) {\n        messages.add(m);\n        addBubble(m);\n    }')
        .join('    private void appendMsg(Models.ChatMsg m) {\n        messages.add(m);\n        addBubble(m, false, false);\n    }');
  // 置顶徽标: 插在 col 创建后
  t = t.split('        LinearLayout col = new LinearLayout(this);\n        col.setOrientation(LinearLayout.VERTICAL);\n        col.setGravity(m.mine ? Gravity.END : Gravity.START);\n')
        .join('        LinearLayout col = new LinearLayout(this);\n        col.setOrientation(LinearLayout.VERTICAL);\n        col.setGravity(m.mine ? Gravity.END : Gravity.START);\n\n        if (pinned) {\n            TextView pb = Utils.tv(this, "📌 " + I18n.t("pin_msg"), 10, Utils.ACCENT, Gravity.START);\n            pb.setPadding(Utils.dp(this, 6), 0, Utils.dp(this, 6), Utils.dp(this, 2));\n            col.addView(pb);\n        }\n');
  // 已读标签: 在 time 后
  t = t.split('        time.setPadding(0, Utils.dp(this, 2), m.mine ? Utils.dp(this, 4) : 0, 0);\n        col.addView(time);\n')
        .join('        time.setPadding(0, Utils.dp(this, 2), m.mine ? Utils.dp(this, 4) : 0, 0);\n        col.addView(time);\n        if (showRead && m.mine && peerReadTime >= m.time) {\n            TextView rd = Utils.tv(this, I18n.t("read"), 10, Utils.ACCENT, Gravity.END);\n            rd.setPadding(0, Utils.dp(this, 2), 0, 0);\n            col.addView(rd);\n        }\n');
  fs.writeFileSync(CA, t);
  console.log('[ChatActivity.java] addBubble updated');
}

// 新增方法: typing/toggleSearch/exportChat/togglePin (插到 requestHistory 前)
{
  let t = fs.readFileSync(CA, 'utf8');
  const anchor = '    private void requestHistory() {';
  const methods = `    private final Runnable typingHideTask = new Runnable() {
        @Override
        public void run() {
            typingText.setVisibility(View.GONE);
        }
    };

    private void sendTyping() {
        if (!Net.get().isConnected()) return;
        typingHide.removeCallbacks(typingSendTask);
        typingHide.postDelayed(typingSendTask, 700);
    }

    private final Runnable typingSendTask = new Runnable() {
        @Override
        public void run() {
            try {
                JSONObject o = new JSONObject();
                o.put("type", "typing");
                o.put("with", peer);
                Net.get().send(o);
            } catch (Exception ignored) {
            }
        }
    };

    private void toggleSearch() {
        boolean show = searchRow.getVisibility() != View.VISIBLE;
        searchRow.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            searchInput.requestFocus();
        } else {
            searchQuery = "";
            searchInput.setText("");
            rerender();
        }
    }

    private void exportChat() {
        StringBuilder sb = new StringBuilder();
        sb.append("Chatapp 聊天记录 - ").append(peerNickname).append("\\n\\n");
        for (Models.ChatMsg m : messages) {
            String sender = m.mine ? Session.nickname : peerNickname;
            sb.append("[").append(Utils.timeText(m.time)).append("] ").append(sender).append(": ");
            if ("image".equals(m.kind)) sb.append("[图片]");
            else if ("file".equals(m.kind)) sb.append("[文件] ").append(m.name);
            else sb.append(m.text == null ? "" : m.text);
            sb.append("\\n");
        }
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, sb.toString());
            startActivity(Intent.createChooser(i, I18n.t("export_chat")));
        } catch (Exception e) {
            Utils.toast(this, I18n.t("copy_failed"));
        }
    }

    private void togglePin(Models.ChatMsg m) {
        Set<String> set = new HashSet<>(pinPrefs.getStringSet("pin_" + peer, new HashSet<String>()));
        if (set.contains(m.id)) set.remove(m.id); else set.add(m.id);
        pinPrefs.edit().putStringSet("pin_" + peer, set).apply();
        rerender();
    }

    private void requestHistory() {`;
  if (t.includes(anchor)) {
    t = t.split(anchor).join(methods);
    fs.writeFileSync(CA, t);
    console.log('[ChatActivity.java] methods added');
  } else {
    console.log('requestHistory anchor not found');
  }
}
console.log('DONE');
