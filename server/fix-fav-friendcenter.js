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

// Manifest 注册
for (const m of [ROOT + '/android/AndroidManifest.xml', ROOT + '/build/AndroidManifest.xml']) {
  patch(m, [
    ['        <activity\n            android:name=".ProfileActivity"',
     '        <activity\n            android:name=".FavoritesActivity"\n            android:exported="false"\n            android:screenOrientation="portrait" />\n\n        <activity\n            android:name=".FriendCenterActivity"\n            android:exported="false"\n            android:screenOrientation="portrait" />\n\n        <activity\n            android:name=".ProfileActivity"'],
  ]);
}

// ChatActivity: 长按收藏 + Fav 拾取发送
const CA = ROOT + '/android/src/com/chatapp/app/ChatActivity.java';
patch(CA, [
  ['        items.add(I18n.t("multi_select"));\n        boolean pinned = pinPrefs.getStringSet("pin_" + peer, new HashSet<String>()).contains(m.id);\n        items.add(pinned ? I18n.t("unpin_msg") : I18n.t("pin_msg"));\n        items.add(I18n.t("reply"));',
   '        items.add(I18n.t("multi_select"));\n        boolean pinned = pinPrefs.getStringSet("pin_" + peer, new HashSet<String>()).contains(m.id);\n        items.add(pinned ? I18n.t("unpin_msg") : I18n.t("pin_msg"));\n        items.add(I18n.t("favorite"));\n        items.add(I18n.t("reply"));'],
  ['                    else if (I18n.t("pin_msg").equals(act) || I18n.t("unpin_msg").equals(act)) togglePin(m);\n                    else if (I18n.t("reply").equals(act)) setReply(m);',
   '                    else if (I18n.t("pin_msg").equals(act) || I18n.t("unpin_msg").equals(act)) togglePin(m);\n                    else if (I18n.t("favorite").equals(act)) favoriteMsg(m);\n                    else if (I18n.t("reply").equals(act)) setReply(m);'],
  // Fav 按钮
  ['        attachPanel.addView(attachBtn("Fav", I18n.t("favorite"), new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                Ui.banner(ChatActivity.this, I18n.t("favorite_coming"));\n            }\n        }));',
   '        attachPanel.addView(attachBtn("Fav", I18n.t("favorite"), new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                openFavPicker();\n            }\n        }));'],
]);

// 收藏方法
{
  let t = fs.readFileSync(CA, 'utf8');
  const anchor = '    private void togglePin(Models.ChatMsg m) {';
  const methods = `    private void favoriteMsg(Models.ChatMsg m) {
        try {
            JSONObject j = new JSONObject();
            j.put("id", m.id == null ? "" : m.id);
            j.put("peer", peer);
            j.put("from", m.from);
            j.put("kind", m.kind);
            j.put("text", m.text == null ? "" : m.text);
            j.put("url", m.url == null ? "" : m.url);
            j.put("name", m.name == null ? "" : m.name);
            j.put("time", m.time);
            Set<String> set = new HashSet<>(getSharedPreferences("chatapp_fav_msgs", MODE_PRIVATE)
                    .getStringSet("favs", new HashSet<String>()));
            set.add(j.toString());
            if (set.size() > 100) {
                List<String> list = new ArrayList<>(set);
                while (list.size() > 100) list.remove(0);
                set = new HashSet<>(list);
            }
            getSharedPreferences("chatapp_fav_msgs", MODE_PRIVATE).edit().putStringSet("favs", set).apply();
            Ui.banner(this, I18n.t("favorited"));
        } catch (Exception ignored) {
        }
    }

    private void openFavPicker() {
        final List<String> items = new ArrayList<>();
        final List<Models.ChatMsg> favs = new ArrayList<>();
        try {
            Set<String> set = getSharedPreferences("chatapp_fav_msgs", MODE_PRIVATE)
                    .getStringSet("favs", new HashSet<String>());
            for (String s : set) {
                try {
                    JSONObject j = new JSONObject(s);
                    Models.ChatMsg m = new Models.ChatMsg(j.optString("from"), j.optString("peer"),
                            j.optString("text"), j.optLong("time"), false);
                    m.id = j.optString("id");
                    m.kind = j.optString("kind", "text");
                    m.url = j.optString("url");
                    m.name = j.optString("name");
                    favs.add(m);
                    String label = "image".equals(m.kind) ? I18n.t("media_image")
                            : "file".equals(m.kind) ? (m.name.isEmpty() ? I18n.t("media_file") : m.name)
                            : (m.text == null || m.text.isEmpty() ? "(empty)" : m.text);
                    items.add(label);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        if (favs.isEmpty()) {
            Ui.banner(this, I18n.t("fav_empty"));
            return;
        }
        Ui.Click[] clicks = new Ui.Click[favs.size()];
        for (int i = 0; i < favs.size(); i++) {
            final Models.ChatMsg m = favs.get(i);
            clicks[i] = new Ui.Click() {
                @Override
                public void onClick() {
                    try {
                        JSONObject o = new JSONObject();
                        if ("image".equals(m.kind) || "file".equals(m.kind)) {
                            o.put("type", "forward_message");
                            o.put("to", peer);
                            o.put("id", m.id);
                        } else {
                            o.put("type", "send_message");
                            o.put("to", peer);
                            o.put("text", m.text);
                        }
                        Net.get().send(o);
                        Ui.banner(ChatActivity.this, I18n.t("send"));
                    } catch (Exception ignored) {
                    }
                }
            };
        }
        Ui.list(this, I18n.t("favorite"), items.toArray(new String[0]), clicks);
    }

    private void togglePin(Models.ChatMsg m) {`;
  if (t.includes(anchor)) { t = t.split(anchor).join(methods); fs.writeFileSync(CA, t); console.log('[ChatActivity.java] favorite methods added'); }
  else console.log('togglePin anchor not found');
}

// GroupChatActivity: 长按收藏 + Fav 拾取发送
const GA = ROOT + '/android/src/com/chatapp/app/GroupChatActivity.java';
patch(GA, [
  ['        items.add(I18n.t("forward"));\n        items.add(I18n.t("multi_select"));\n        items.add(I18n.t("reply"));',
   '        items.add(I18n.t("forward"));\n        items.add(I18n.t("multi_select"));\n        items.add(I18n.t("favorite"));\n        items.add(I18n.t("reply"));'],
  ['                    else if (I18n.t("forward").equals(act)) openForwardSheet(m);\n                    else if (I18n.t("multi_select").equals(act)) enterMultiSelect();\n                    else if (I18n.t("reply").equals(act)) setReply(m);',
   '                    else if (I18n.t("forward").equals(act)) openForwardSheet(m);\n                    else if (I18n.t("multi_select").equals(act)) enterMultiSelect();\n                    else if (I18n.t("favorite").equals(act)) favoriteMsg(m);\n                    else if (I18n.t("reply").equals(act)) setReply(m);'],
  ['        attachPanel.addView(attachBtn("Fav", I18n.t("favorite"), new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                Ui.banner(GroupChatActivity.this, I18n.t("favorite_coming"));\n            }\n        }));',
   '        attachPanel.addView(attachBtn("Fav", I18n.t("favorite"), new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                openFavPicker();\n            }\n        }));'],
]);

{
  let t = fs.readFileSync(GA, 'utf8');
  const anchor = '    private void confirmForwardGroup(final Models.Group g) {';
  const methods = `    private void favoriteMsg(Models.ChatMsg m) {
        try {
            JSONObject j = new JSONObject();
            j.put("id", m.id == null ? "" : m.id);
            j.put("peer", groupId);
            j.put("from", m.from);
            j.put("kind", m.kind);
            j.put("text", m.text == null ? "" : m.text);
            j.put("url", m.url == null ? "" : m.url);
            j.put("name", m.name == null ? "" : m.name);
            j.put("time", m.time);
            Set<String> set = new HashSet<>(getSharedPreferences("chatapp_fav_msgs", MODE_PRIVATE)
                    .getStringSet("favs", new HashSet<String>()));
            set.add(j.toString());
            if (set.size() > 100) {
                List<String> list = new ArrayList<>(set);
                while (list.size() > 100) list.remove(0);
                set = new HashSet<>(list);
            }
            getSharedPreferences("chatapp_fav_msgs", MODE_PRIVATE).edit().putStringSet("favs", set).apply();
            Ui.banner(this, I18n.t("favorited"));
        } catch (Exception ignored) {
        }
    }

    private void openFavPicker() {
        final List<String> items = new ArrayList<>();
        final List<Models.ChatMsg> favs = new ArrayList<>();
        try {
            Set<String> set = getSharedPreferences("chatapp_fav_msgs", MODE_PRIVATE)
                    .getStringSet("favs", new HashSet<String>());
            for (String s : set) {
                try {
                    JSONObject j = new JSONObject(s);
                    Models.ChatMsg m = new Models.ChatMsg(j.optString("from"), j.optString("peer"),
                            j.optString("text"), j.optLong("time"), false);
                    m.id = j.optString("id");
                    m.kind = j.optString("kind", "text");
                    m.url = j.optString("url");
                    m.name = j.optString("name");
                    favs.add(m);
                    String label = "image".equals(m.kind) ? I18n.t("media_image")
                            : "file".equals(m.kind) ? (m.name.isEmpty() ? I18n.t("media_file") : m.name)
                            : (m.text == null || m.text.isEmpty() ? "(empty)" : m.text);
                    items.add(label);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        if (favs.isEmpty()) {
            Ui.banner(this, I18n.t("fav_empty"));
            return;
        }
        Ui.Click[] clicks = new Ui.Click[favs.size()];
        for (int i = 0; i < favs.size(); i++) {
            final Models.ChatMsg m = favs.get(i);
            clicks[i] = new Ui.Click() {
                @Override
                public void onClick() {
                    try {
                        JSONObject o = new JSONObject();
                        if ("image".equals(m.kind) || "file".equals(m.kind)) {
                            o.put("type", "forward_group_message");
                            o.put("group", groupId);
                            o.put("messageId", m.id);
                        } else {
                            o.put("type", "send_group_message");
                            o.put("id", groupId);
                            o.put("text", m.text);
                        }
                        Net.get().send(o);
                        Ui.banner(GroupChatActivity.this, I18n.t("send"));
                    } catch (Exception ignored) {
                    }
                }
            };
        }
        Ui.list(this, I18n.t("favorite"), items.toArray(new String[0]), clicks);
    }

    private void confirmForwardGroup(final Models.Group g) {`;
  if (t.includes(anchor)) { t = t.split(anchor).join(methods); fs.writeFileSync(GA, t); console.log('[GroupChatActivity.java] favorite methods added'); }
  else console.log('confirmForwardGroup anchor not found');
}
console.log('DONE');
