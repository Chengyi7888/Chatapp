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

// 字段
patch(HA, [
  ['    private List<Models.Group> groups = new ArrayList<>();',
   '    private List<Models.Group> groups = new ArrayList<>();\n    private List<Models.Post> postsList = new ArrayList<>();\n    private LinearLayout postsContainer;\n    private final List<String> pendingPostImages = new ArrayList<>();'],
  ['    private static final int PICK_AVATAR = 1001;', '    private static final int PICK_AVATAR = 1001;\n    private static final int PICK_POST_IMAGE = 1002;'],
  // refreshAll 增加动态
  ['            JSONObject g = new JSONObject();\n            g.put("type", "get_groups");\n            Net.get().send(g);\n        } catch (Exception ignored) {\n        }\n    }',
   '            JSONObject g = new JSONObject();\n            g.put("type", "get_groups");\n            Net.get().send(g);\n            JSONObject p = new JSONObject();\n            p.put("type", "get_posts");\n            Net.get().send(p);\n        } catch (Exception ignored) {\n        }\n    }'],
  // 动态区替换
  ['        TextView dynTitle = Utils.tv(this, I18n.t("dynamics"), 18, Utils.TEXT, Gravity.START);\n        dynTitle.setTypeface(Typeface.DEFAULT_BOLD);\n        dynTitle.setPadding(0, 0, 0, Utils.dp(this, 10));\n        updatesLayout.addView(dynTitle);\n\n        LinearLayout dyn = new LinearLayout(this);\n        dyn.setOrientation(LinearLayout.HORIZONTAL);\n        dyn.setGravity(Gravity.CENTER_VERTICAL);\n        dyn.setBackground(Utils.bg(this, Utils.CARD, 16));\n        dyn.setPadding(Utils.dp(this, 14), Utils.dp(this, 12), Utils.dp(this, 14), Utils.dp(this, 12));\n        final View dynAvatar = AvatarManager.avatarView(this, Session.username, Session.nickname, Session.hasAvatar, 52, 26);\n        dyn.addView(dynAvatar);\n        dyn.addView(Utils.vSpace(this, 12));\n        LinearLayout dynMid = new LinearLayout(this);\n        dynMid.setOrientation(LinearLayout.VERTICAL);\n        dynMid.addView(Utils.tv(this, I18n.t("my_dynamics"), 15, Utils.TEXT, Gravity.START));\n        dynMid.addView(Utils.tv(this, I18n.t("share_moment"), 12, Utils.TEXT_DIM, Gravity.START));\n        dyn.addView(dynMid, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));\n        dyn.addView(roundIconBtn("📷"));\n        dyn.addView(Utils.vSpace(this, 10));\n        dyn.addView(roundIconBtn("🖌️"));\n        updatesLayout.addView(dyn);\n\n        updatesLayout.addView(Utils.hSpace(this, 22));\n\n        TextView chTitle = Utils.tv(this, I18n.t("channels"), 18, Utils.TEXT, Gravity.START);',
   '        LinearLayout dynHead = new LinearLayout(this);\n        dynHead.setOrientation(LinearLayout.HORIZONTAL);\n        dynHead.setGravity(Gravity.CENTER_VERTICAL);\n        TextView dynTitle = Utils.tv(this, I18n.t("dynamics"), 18, Utils.TEXT, Gravity.START);\n        dynTitle.setTypeface(Typeface.DEFAULT_BOLD);\n        dynHead.addView(dynTitle, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));\n        Button pubBtn = new Button(this);\n        pubBtn.setText(I18n.t("publish"));\n        pubBtn.setTextSize(13);\n        pubBtn.setTextColor(Color.WHITE);\n        pubBtn.setBackground(Utils.bg(this, Utils.ACCENT, 16));\n        pubBtn.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                showPostDialog();\n            }\n        });\n        dynHead.addView(pubBtn);\n        updatesLayout.addView(dynHead);\n        updatesLayout.addView(Utils.hSpace(this, 10));\n\n        postsContainer = new LinearLayout(this);\n        postsContainer.setOrientation(LinearLayout.VERTICAL);\n        updatesLayout.addView(postsContainer);\n        renderPosts();\n\n        updatesLayout.addView(Utils.hSpace(this, 22));\n\n        TextView chTitle = Utils.tv(this, I18n.t("channels"), 18, Utils.TEXT, Gravity.START);'],
  // 自己页 group2: 改密码/注销
  ['        group2.addView(menuRow("关于 (App v2.2.3.0)", new View.OnClickListener() {',
   '        group2.addView(menuRow(I18n.t("change_password"), new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                showChangePasswordDialog();\n            }\n        }));\n        group2.addView(Utils.divider(this));\n        group2.addView(menuRow(I18n.t("delete_account"), new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                confirmDeleteAccount();\n            }\n        }));\n        group2.addView(Utils.divider(this));\n        group2.addView(menuRow("关于 (App v2.2.3.0)", new View.OnClickListener() {'],
  // handle: posts / post_added / posts_changed / password_changed / account_deleted
  ['        } else if ("groups".equals(type)) {\n            groups = Models.parseGroups(o.optJSONArray("groups"));\n            if (communitiesScroll.getVisibility() == View.VISIBLE) renderCommunities();\n        }',
   '        } else if ("groups".equals(type)) {\n            groups = Models.parseGroups(o.optJSONArray("groups"));\n            if (communitiesScroll.getVisibility() == View.VISIBLE) renderCommunities();\n        } else if ("posts".equals(type)) {\n            postsList = Models.parsePosts(o.optJSONArray("posts"));\n            renderPosts();\n        } else if ("post_added".equals(type) || "posts_changed".equals(type)) {\n            refreshAll();\n        } else if ("password_changed".equals(type)) {\n            Utils.toast(this, I18n.t("password_changed"));\n        } else if ("account_deleted".equals(type)) {\n            logoutToMain();\n        }'],
]);

// 方法: showPostDialog/pickPostImage/renderPosts/postCard/showChangePasswordDialog/confirmDeleteAccount
{
  let t = fs.readFileSync(HA, 'utf8');
  const anchor = '    private void showChangelogDialog() {';
  const methods = `    private void showPostDialog() {
        pendingPostImages.clear();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final EditText input = Ui.input(this, null, I18n.t("moment_hint"));
        input.setSingleLine(false);
        input.setMinLines(3);
        box.addView(input);
        box.addView(Utils.hSpace(this, 10));
        Button addImg = new Button(this);
        addImg.setText(I18n.t("moment_add_photo") + " (0/3)");
        addImg.setTextSize(12);
        addImg.setTextColor(Utils.ACCENT);
        addImg.setBackground(Utils.bg(this, Utils.CARD2, 14));
        addImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pendingPostImages.size() >= 3) {
                    Ui.banner(HomeActivity.this, "最多3张");
                    return;
                }
                pickPostImage();
            }
        });
        box.addView(addImg);
        Ui.show(this, I18n.t("publish"), box, I18n.t("publish"), new Ui.Click() {
            @Override
            public void onClick() {
                String text = input.getText().toString().trim();
                if (text.isEmpty() && pendingPostImages.isEmpty()) {
                    Utils.toast(HomeActivity.this, I18n.t("srv_moment_empty"));
                    return;
                }
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "add_post");
                    o.put("text", text);
                    JSONArray arr = new JSONArray();
                    for (String b : pendingPostImages) arr.put(b);
                    o.put("images", arr);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void pickPostImage() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        try {
            startActivityForResult(Intent.createChooser(i, I18n.t("moment_add_photo")), PICK_POST_IMAGE);
        } catch (Exception e) {
            Utils.toast(this, I18n.t("cannot_open_picker"));
        }
    }

    private void renderPosts() {
        if (postsContainer == null) return;
        postsContainer.removeAllViews();
        if (postsList.isEmpty()) {
            postsContainer.addView(Utils.tv(this, I18n.t("moments_empty"), 14, Utils.TEXT_DIM, Gravity.CENTER));
            return;
        }
        for (final Models.Post p : postsList) {
            postsContainer.addView(postCard(p));
            postsContainer.addView(Utils.hSpace(this, 10));
        }
    }

    private View postCard(final Models.Post p) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Utils.bg(this, Utils.CARD, 16));
        card.setPadding(Utils.dp(this, 12), Utils.dp(this, 12), Utils.dp(this, 12), Utils.dp(this, 12));
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(AvatarManager.avatarView(this, p.from, p.nickname, p.hasAvatar, 36, 14));
        head.addView(Utils.vSpace(this, 8));
        LinearLayout hi = new LinearLayout(this);
        hi.setOrientation(LinearLayout.VERTICAL);
        hi.addView(Utils.tv(this, p.nickname, 14, Utils.TEXT, Gravity.START));
        hi.addView(Utils.tv(this, Utils.timeText(p.time), 11, Utils.TEXT_DIM, Gravity.START));
        head.addView(hi, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(head);
        if (p.text != null && !p.text.isEmpty()) {
            TextView tx = Utils.tv(this, p.text, 14, Utils.TEXT, Gravity.START);
            tx.setPadding(0, Utils.dp(this, 6), 0, Utils.dp(this, 4));
            card.addView(tx);
        }
        if (!p.images.isEmpty()) {
            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(3);
            int cell = (getResources().getDisplayMetrics().widthPixels - Utils.dp(this, 36 + 48)) / 3;
            for (String url : p.images) {
                ImageView iv = new ImageView(this);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setBackground(Utils.bg(this, Utils.CARD2, 8));
                GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
                glp.width = cell;
                glp.height = cell;
                glp.setMargins(Utils.dp(this, 2), Utils.dp(this, 2), Utils.dp(this, 2), Utils.dp(this, 2));
                iv.setLayoutParams(glp);
                AvatarManager.loadFileImage(this, url, iv);
                grid.addView(iv);
            }
            card.addView(grid);
        }
        final boolean liked = p.likes.contains(Session.username);
        Button likeBtn = new Button(this);
        likeBtn.setText((liked ? I18n.t("liked") : I18n.t("like")) + " " + p.likes.size());
        likeBtn.setTextSize(12);
        likeBtn.setTextColor(liked ? Utils.ACCENT : Utils.TEXT);
        likeBtn.setBackground(Utils.bg(this, Utils.CARD2, 14));
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.topMargin = Utils.dp(this, 8);
        likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "like_post");
                    o.put("id", p.id);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
        card.addView(likeBtn, llp);
        return card;
    }

    private void showChangePasswordDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final EditText oldP = Ui.input(this, null, I18n.t("old_password"));
        final EditText newP = Ui.input(this, null, I18n.t("new_password"));
        final EditText newP2 = Ui.input(this, null, I18n.t("confirm_password"));
        box.addView(oldP);
        box.addView(Utils.hSpace(this, 8));
        box.addView(newP);
        box.addView(Utils.hSpace(this, 8));
        box.addView(newP2);
        Ui.show(this, I18n.t("change_password"), box, I18n.t("confirm"), new Ui.Click() {
            @Override
            public void onClick() {
                String o = oldP.getText().toString();
                String n = newP.getText().toString();
                String n2 = newP2.getText().toString();
                if (n.length() < 4) {
                    Utils.toast(HomeActivity.this, I18n.t("password_min"));
                    return;
                }
                if (!n.equals(n2)) {
                    Utils.toast(HomeActivity.this, I18n.t("confirm_password"));
                    return;
                }
                try {
                    JSONObject j = new JSONObject();
                    j.put("type", "change_password");
                    j.put("old", o);
                    j.put("next", n);
                    Net.get().send(j);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void confirmDeleteAccount() {
        Ui.show(this, I18n.t("delete_account"), Ui.message(this, I18n.t("delete_account_confirm")), I18n.t("confirm"), new Ui.Click() {
            @Override
            public void onClick() {
                try {
                    JSONObject j = new JSONObject();
                    j.put("type", "delete_account");
                    Net.get().send(j);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void showChangelogDialog() {`;
  if (t.includes(anchor)) { t = t.split(anchor).join(methods); fs.writeFileSync(HA, t); console.log('[HomeActivity.java] methods added'); }
  else console.log('showChangelogDialog anchor not found');
}

// onActivityResult 增加 PICK_POST_IMAGE
patch(HA, [
  ['        if (requestCode == PICK_AVATAR && resultCode == RESULT_OK && data != null && data.getData() != null) {\n            uploadAvatar(data.getData());\n        }\n    }',
   '        if (requestCode == PICK_AVATAR && resultCode == RESULT_OK && data != null && data.getData() != null) {\n            uploadAvatar(data.getData());\n        } else if (requestCode == PICK_POST_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {\n            encodePostImage(data.getData());\n        }\n    }\n\n    private void encodePostImage(final android.net.Uri uri) {\n        Thread t = new Thread(new Runnable() {\n            @Override\n            public void run() {\n                try {\n                    InputStream in = getContentResolver().openInputStream(uri);\n                    Bitmap bmp = BitmapFactory.decodeStream(in);\n                    if (in != null) in.close();\n                    if (bmp == null) return;\n                    int w = bmp.getWidth(), h = bmp.getHeight();\n                    int side = Math.min(w, h);\n                    if (side > 0 && (w != side || h != side)) {\n                        Bitmap c = Bitmap.createBitmap(bmp, (w - side) / 2, (h - side) / 2, side, side);\n                        if (c != bmp) bmp.recycle();\n                        bmp = c;\n                    }\n                    int target = 512;\n                    if (bmp.getWidth() > target) {\n                        Bitmap s = Bitmap.createScaledBitmap(bmp, target, target, true);\n                        if (s != bmp) bmp.recycle();\n                        bmp = s;\n                    }\n                    ByteArrayOutputStream bos = new ByteArrayOutputStream();\n                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);\n                    final String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);\n                    bmp.recycle();\n                    runOnUiThread(new Runnable() {\n                        @Override\n                        public void run() {\n                            pendingPostImages.add(b64);\n                            Ui.banner(HomeActivity.this, I18n.t("moment_add_photo") + " (" + pendingPostImages.size() + "/3)");\n                        }\n                    });\n                } catch (Exception ignored) {\n                }\n            }\n        });\n        t.setDaemon(true);\n        t.start();\n    }'],
]);

// 转发到群: ChatActivity
const CA = ROOT + '/android/src/com/chatapp/app/ChatActivity.java';
patch(CA, [
  ['    private List<Models.Contact> forwardContacts = new ArrayList<>();', '    private List<Models.Contact> forwardContacts = new ArrayList<>();\n    private List<Models.Group> forwardGroups = new ArrayList<>();'],
  ['        try {\n            JSONObject o = new JSONObject();\n            o.put("type", "get_contacts");\n            Net.get().send(o);\n        } catch (Exception ignored) {\n        }\n    }\n\n    private void renderForwardList() {',
   '        try {\n            JSONObject o = new JSONObject();\n            o.put("type", "get_contacts");\n            Net.get().send(o);\n            JSONObject g = new JSONObject();\n            g.put("type", "get_groups");\n            Net.get().send(g);\n        } catch (Exception ignored) {\n        }\n    }\n\n    private void renderForwardList() {'],
  ['            forwardListContainer.addView(row);\n            forwardListContainer.addView(Utils.divider(this));\n        }\n    }\n\n    private void confirmForward(final Models.Contact c) {',
   '            forwardListContainer.addView(row);\n            forwardListContainer.addView(Utils.divider(this));\n        }\n        if (!forwardGroups.isEmpty()) {\n            TextView gh = Utils.tv(this, I18n.t("forward_to_group"), 13, Utils.ACCENT, Gravity.START);\n            gh.setPadding(0, Utils.dp(this, 10), 0, Utils.dp(this, 4));\n            forwardListContainer.addView(gh);\n            for (final Models.Group g : forwardGroups) {\n                LinearLayout row = new LinearLayout(this);\n                row.setOrientation(LinearLayout.HORIZONTAL);\n                row.setGravity(Gravity.CENTER_VERTICAL);\n                row.setPadding(Utils.dp(this, 6), Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8));\n                row.addView(Utils.letterAvatar(this, g.name, 40, 15));\n                row.addView(Utils.vSpace(this, 10));\n                row.addView(Utils.tv(this, g.name + " (" + g.memberCount + ")", 15, Utils.TEXT, Gravity.START), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));\n                row.setOnClickListener(new View.OnClickListener() {\n                    @Override\n                    public void onClick(View v) {\n                        confirmForwardGroup(g);\n                    }\n                });\n                forwardListContainer.addView(row);\n                forwardListContainer.addView(Utils.divider(this));\n            }\n        }\n    }\n\n    private void confirmForwardGroup(final Models.Group g) {\n        try {\n            JSONObject o = new JSONObject();\n            o.put("type", "forward_group_message");\n            o.put("group", g.id);\n            o.put("messageId", forwardTarget.id);\n            Net.get().send(o);\n            Ui.banner(this, I18n.f("forwarded_ok", g.name));\n        } catch (Exception ignored) {\n        }\n        if (forwardSheet != null && forwardSheet.isShowing()) forwardSheet.dismiss();\n    }\n\n    private void confirmForward(final Models.Contact c) {'],
  ['        } else if ("contacts".equals(type)) {\n            forwardContacts = Models.parseContacts(o.optJSONArray("contacts"));\n            if (forwardSheet != null && forwardSheet.isShowing()) renderForwardList();\n            if (pendingMultiForward != null) {',
   '        } else if ("contacts".equals(type)) {\n            forwardContacts = Models.parseContacts(o.optJSONArray("contacts"));\n            if (forwardSheet != null && forwardSheet.isShowing()) renderForwardList();\n            if (pendingMultiForward != null) {'],
  ['            if (pendingForward != null) {', '            if (pendingForward != null) {'],
]);
console.log('DONE');
