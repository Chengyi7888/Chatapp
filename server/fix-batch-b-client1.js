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

// I18n
patch(ROOT + '/android/src/com/chatapp/app/I18n.java', [
  ['        put("dnd_end", "结束", "結束", "End");',
   '        put("dnd_end", "结束", "結束", "End");\n        put("at_all", "@所有人", "@所有人", "@everyone");\n        put("at_all_msg", "%s @了全体成员", "%s @了全體成員", "%s @ everyone");\n        put("change_password", "修改密码", "修改密碼", "Change password");\n        put("old_password", "原密码", "原密碼", "Old password");\n        put("new_password", "新密码", "新密碼", "New password");\n        put("confirm_password", "确认密码", "確認密碼", "Confirm password");\n        put("password_changed", "密码已修改", "密碼已修改", "Password changed");\n        put("delete_account", "注销账号", "註銷帳號", "Delete account");\n        put("delete_account_confirm", "确定注销账号？此操作不可恢复", "確定註銷帳號？此操作不可恢復", "Delete this account? This cannot be undone");\n        put("forward_to_group", "转发到群聊", "轉發到群聊", "Forward to group");\n        put("qr_card", "二维码名片", "二維碼名片", "QR card");\n        put("publish", "发布", "發佈", "Publish");\n        put("like", "点赞", "點讚", "Like");\n        put("liked", "已赞", "已讚", "Liked");\n        put("moment_hint", "分享你的新鲜事...", "分享你的新鮮事...", "Share something...");\n        put("moment_add_photo", "添加图片", "新增圖片", "Add image");\n        put("moments_empty", "暂无动态，发一条吧", "暫無動態，發一條吧", "No moments yet, post one");'],
  ['        server("不能拉黑自己", "srv_block_self");',
   '        server("不能拉黑自己", "srv_block_self");\n        server("原密码错误", "srv_old_password_wrong");\n        server("新密码至少 4 位", "srv_new_password_short");\n        server("动态内容不能为空", "srv_moment_empty");'],
  ['        put("srv_block_self", "不能拉黑自己", "不能拉黑自己", "Can\'t block yourself");',
   '        put("srv_block_self", "不能拉黑自己", "不能拉黑自己", "Can\'t block yourself");\n        put("srv_old_password_wrong", "原密码错误", "原密碼錯誤", "Old password is wrong");\n        put("srv_new_password_short", "新密码至少 4 位", "新密碼至少 4 位", "New password needs 4+ chars");\n        put("srv_moment_empty", "动态内容不能为空", "動態內容不能為空", "Content cannot be empty");'],
]);

// Models.Post
patch(ROOT + '/android/src/com/chatapp/app/Models.java', [
  ['    public static List<ChatMsg> parseMessages(JSONArray arr, String me) {',
   '    public static class Post {\n        public String id;\n        public String from;\n        public String nickname;\n        public boolean hasAvatar;\n        public String text = "";\n        public List<String> images = new ArrayList<>();\n        public List<String> likes = new ArrayList<>();\n        public long time;\n\n        public static Post fromJson(JSONObject o) {\n            Post p = new Post();\n            p.id = o.optString("id");\n            p.from = o.optString("from");\n            p.nickname = o.optString("nickname", p.from);\n            p.hasAvatar = o.optBoolean("hasAvatar");\n            p.text = o.optString("text");\n            p.time = o.optLong("time");\n            JSONArray ia = o.optJSONArray("images");\n            if (ia != null) for (int i = 0; i < ia.length(); i++) p.images.add(ia.optString(i));\n            JSONArray la = o.optJSONArray("likes");\n            if (la != null) for (int i = 0; i < la.length(); i++) p.likes.add(la.optString(i));\n            return p;\n        }\n    }\n\n    public static List<Post> parsePosts(JSONArray arr) {\n        List<Post> list = new ArrayList<>();\n        if (arr == null) return list;\n        for (int i = 0; i < arr.length(); i++) {\n            JSONObject o = arr.optJSONObject(i);\n            if (o != null) list.add(Post.fromJson(o));\n        }\n        return list;\n    }\n\n    public static List<ChatMsg> parseMessages(JSONArray arr, String me) {'],
]);

// AvatarManager.loadBitmapUrl
patch(ROOT + '/android/src/com/chatapp/app/AvatarManager.java', [
  ['    public static void loadFileImage(final Context ctx, final String urlPath, final ImageView iv) {',
   '    /** 加载任意 http 图片地址 */\n    public static void loadBitmapUrl(final Context ctx, final String url, final BitmapCallback cb) {\n        Thread t = new Thread(new Runnable() {\n            @Override\n            public void run() {\n                Bitmap bmp = null;\n                try {\n                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();\n                    c.setConnectTimeout(6000);\n                    c.setReadTimeout(6000);\n                    InputStream in = c.getInputStream();\n                    bmp = BitmapFactory.decodeStream(in);\n                    in.close();\n                    c.disconnect();\n                } catch (Exception ignored) {\n                }\n                final Bitmap fb = bmp;\n                H.post(new Runnable() {\n                    @Override\n                    public void run() {\n                        if (cb != null) cb.onBitmap(fb);\n                    }\n                });\n            }\n        });\n        t.setDaemon(true);\n        t.start();\n    }\n\n    public static void loadFileImage(final Context ctx, final String urlPath, final ImageView iv) {'],
]);

// 群聊: @所有人 按钮 + group_at_all 处理
const GA = ROOT + '/android/src/com/chatapp/app/GroupChatActivity.java';
patch(GA, [
  ['        inputRow.addView(input, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));\n        inputRow.addView(Utils.vSpace(this, 6));\n\n        Button sendBtn = new Button(this);',
   '        inputRow.addView(input, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));\n        TextView atBtn = Utils.tv(this, "@", 16, Utils.ACCENT, Gravity.CENTER);\n        atBtn.setBackground(Utils.bg(this, Utils.CARD2, 14));\n        atBtn.setPadding(Utils.dp(this, 10), Utils.dp(this, 6), Utils.dp(this, 10), Utils.dp(this, 6));\n        atBtn.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                String cur = input.getText().toString();\n                input.setText(cur + "@所有人 ");\n                input.setSelection(input.length());\n            }\n        });\n        inputRow.addView(Utils.vSpace(this, 6));\n        inputRow.addView(atBtn);\n        inputRow.addView(Utils.vSpace(this, 6));\n\n        Button sendBtn = new Button(this);'],
  ['        } else if ("peer_typing".equals(type)) {',
   '        } else if ("group_at_all".equals(type)) {\n            String from = o.optString("nickname", o.optString("from"));\n            Ui.banner(this, I18n.f("at_all_msg", from));\n        } else if ("peer_typing".equals(type)) {'],
]);

// 个人名片: 二维码名片
const PA = ROOT + '/android/src/com/chatapp/app/ProfileActivity.java';
patch(PA, [
  ['        card1.addView(Utils.divider(this));\n        card1.addView(valueRow(I18n.t("birthday"), birthday.isEmpty() ? "—" : birthday, new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                showBirthdayDialog();\n            }\n        }));\n        content.addView(card1);',
   '        card1.addView(Utils.divider(this));\n        card1.addView(valueRow(I18n.t("birthday"), birthday.isEmpty() ? "—" : birthday, new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                showBirthdayDialog();\n            }\n        }));\n        card1.addView(Utils.divider(this));\n        card1.addView(valueRow(I18n.t("qr_card"), "@" + Session.username, new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                showQrDialog();\n            }\n        }));\n        content.addView(card1);'],
]);

// showQrDialog 方法
{
  let t = fs.readFileSync(PA, 'utf8');
  const anchor = '    private void showBirthdayDialog() {';
  const methods = `    private void showQrDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8), Utils.dp(this, 6));
        final ImageView qr = new ImageView(this);
        int s = Utils.dp(this, 220);
        qr.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        box.addView(qr);
        box.addView(Utils.hSpace(this, 8));
        box.addView(Utils.tv(this, "@" + Session.username, 14, Utils.TEXT, Gravity.CENTER));
        Ui.showSingle(this, I18n.t("qr_card"), box, I18n.t("close"), null);
        String data = "chatapp:" + Session.username;
        String url = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data="
                + java.net.URLEncoder.encode(data, "UTF-8");
        AvatarManager.loadBitmapUrl(this, url, new AvatarManager.BitmapCallback() {
            @Override
            public void onBitmap(Bitmap bmp) {
                if (bmp != null) qr.setImageBitmap(bmp);
            }
        });
    }

    private void showBirthdayDialog() {`;
  if (t.includes(anchor)) { t = t.split(anchor).join(methods); fs.writeFileSync(PA, t); console.log('[ProfileActivity.java] QR dialog added'); }
  else console.log('showBirthdayDialog anchor not found');
}
console.log('DONE');
