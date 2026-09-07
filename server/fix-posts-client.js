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

// 字段: 预览网格
patch(HA, [
  ['    private LinearLayout postsContainer;\n    private final List<String> pendingPostImages = new ArrayList<>();',
   '    private LinearLayout postsContainer;\n    private final List<String> pendingPostImages = new ArrayList<>();\n    private GridLayout postPreviewGrid;'],
  // 自己页: 我的收藏 行 (放在 utilCard 前或 group1 后; 插到 utilCard 前)
  ['        LinearLayout utilCard = new LinearLayout(this);', '        LinearLayout favCard = new LinearLayout(this);\n        favCard.setOrientation(LinearLayout.VERTICAL);\n        favCard.setBackground(Utils.bg(this, Utils.CARD, 16));\n        favCard.addView(menuRow(I18n.t("my_favorites"), new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                startActivity(new Intent(HomeActivity.this, FavoritesActivity.class));\n            }\n        }));\n        meLayout.addView(favCard);\n        meLayout.addView(Utils.hSpace(this, 16));\n\n        LinearLayout utilCard = new LinearLayout(this);'],
  // fab: 进入好友中心
  ['        fab.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                showFabMenu();\n            }\n        });',
   '        fab.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                startActivity(new Intent(HomeActivity.this, FriendCenterActivity.class));\n            }\n        });'],
  // 移除联系人弹窗里的扫一扫
  ['        Button scanBtn = new Button(this);\n        scanBtn.setText(I18n.t("qr_scan"));\n        scanBtn.setTextSize(14);\n        scanBtn.setTextColor(Color.WHITE);\n        scanBtn.setBackground(Utils.bg(this, Utils.ACCENT, 16));\n        scanBtn.setElevation(0);\n        scanBtn.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                MediaPick.scan(HomeActivity.this, 9101, new MediaPick.Callback() {\n                    @Override\n                    public void onBitmap(Bitmap bmp) {\n                        showScanResult(bmp);\n                    }\n                });\n            }\n        });\n        list.addView(scanBtn);\n        list.addView(Utils.divider(this));\n',
   ''],
]);

// 替换 postCard + 新增动态操作/评论
{
  let t = fs.readFileSync(HA, 'utf8');
  const startMark = '    private View postCard(final Models.Post p) {';
  const endMark = '\n    private void showChangePasswordDialog() {';
  const si = t.indexOf(startMark);
  const ei = t.indexOf(endMark);
  if (si >= 0 && ei > si) {
    const neu = `    private View postCard(final Models.Post p) {
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
        if (p.forwardFrom != null && !p.forwardFrom.isEmpty()) {
            TextView fw = Utils.tv(this, I18n.t("forward") + " @", 12, Utils.ACCENT, Gravity.START);
            fw.setPadding(0, Utils.dp(this, 4), 0, 0);
            card.addView(fw);
        }
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
        // 操作行: 点赞 / 收藏 / 评论 / 转发
        final boolean liked = p.likes.contains(Session.username);
        final boolean favored = p.favorites.contains(Session.username);
        LinearLayout acts = new LinearLayout(this);
        acts.setOrientation(LinearLayout.HORIZONTAL);
        acts.setGravity(Gravity.CENTER);
        Button likeBtn = smallFlatBtn((liked ? I18n.t("liked") : I18n.t("like")) + " " + p.likes.size(), liked ? Utils.ACCENT : Utils.TEXT);
        likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 乐观更新: 先出特效, 数据稍后同步
                if (p.likes.contains(Session.username)) p.likes.remove(Session.username);
                else p.likes.add(Session.username);
                renderPosts();
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "like_post");
                    o.put("id", p.id);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
        acts.addView(likeBtn, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        acts.addView(Utils.vSpace(this, 6));
        Button favBtn = smallFlatBtn((favored ? I18n.t("favorited") : I18n.t("favorite")) + " " + p.favorites.size(), favored ? Utils.ACCENT : Utils.TEXT);
        favBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (p.favorites.contains(Session.username)) p.favorites.remove(Session.username);
                else p.favorites.add(Session.username);
                renderPosts();
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "favorite_post");
                    o.put("id", p.id);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
        acts.addView(favBtn, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        acts.addView(Utils.vSpace(this, 6));
        Button cmtBtn = smallFlatBtn(I18n.t("comment") + " " + p.comments.size(), Utils.TEXT);
        cmtBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCommentInput(p);
            }
        });
        acts.addView(cmtBtn, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        acts.addView(Utils.vSpace(this, 6));
        Button fwdBtn = smallFlatBtn(I18n.t("forward"), Utils.TEXT);
        fwdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "repost_post");
                    o.put("id", p.id);
                    Net.get().send(o);
                    Ui.banner(HomeActivity.this, I18n.t("reposted"));
                } catch (Exception ignored) {
                }
            }
        });
        acts.addView(fwdBtn, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        al.topMargin = Utils.dp(this, 8);
        card.addView(acts, al);

        // 评论列表
        if (!p.comments.isEmpty()) {
            LinearLayout cl = new LinearLayout(this);
            cl.setOrientation(LinearLayout.VERTICAL);
            cl.setPadding(Utils.dp(this, 6), Utils.dp(this, 4), Utils.dp(this, 6), 0);
            for (Models.Comment c : p.comments) {
                cl.addView(Utils.tv(this, c.nickname + "： " + c.text, 12, Utils.TEXT_DIM, Gravity.START));
            }
            card.addView(cl);
        }

        // 自己的动态: 左滑删除
        if (p.from.equals(Session.username)) {
            int screenW = getResources().getDisplayMetrics().widthPixels - Utils.dp(this, 28);
            HorizontalScrollView hsv = new HorizontalScrollView(this);
            hsv.setHorizontalScrollBarEnabled(false);
            hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.HORIZONTAL);
            card.setLayoutParams(new LinearLayout.LayoutParams(screenW, LinearLayout.LayoutParams.WRAP_CONTENT));
            inner.addView(card);
            TextView del = Utils.tv(this, I18n.t("delete"), 14, Color.WHITE, Gravity.CENTER);
            del.setBackground(Utils.bg(this, Utils.DANGER, 16));
            del.setLayoutParams(new LinearLayout.LayoutParams(Utils.dp(this, 76), LinearLayout.LayoutParams.MATCH_PARENT));
            del.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        JSONObject o = new JSONObject();
                        o.put("type", "delete_post");
                        o.put("id", p.id);
                        Net.get().send(o);
                    } catch (Exception ignored) {
                    }
                }
            });
            inner.addView(del);
            hsv.addView(inner);
            return hsv;
        }
        return card;
    }

    private Button smallFlatBtn(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(11);
        b.setTextColor(color);
        b.setBackground(Utils.bg(this, Utils.CARD2, 12));
        b.setElevation(0);
        return b;
    }

    private void showCommentInput(final Models.Post p) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final EditText input = Ui.input(this, null, I18n.t("comment_hint"));
        box.addView(input);
        Ui.show(this, I18n.t("comment"), box, I18n.t("send"), new Ui.Click() {
            @Override
            public void onClick() {
                String text = input.getText().toString().trim();
                if (text.isEmpty()) return;
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "add_comment");
                    o.put("id", p.id);
                    o.put("text", text);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

`;
    t = t.slice(0, si) + neu + t.slice(ei);
    fs.writeFileSync(HA, t);
    console.log('[HomeActivity.java] postCard replaced');
  } else {
    console.log('postCard anchors not found');
  }
}

// 发布弹窗: 9图 + 预览
{
  let t = fs.readFileSync(HA, 'utf8');
  const old = `        Button addImg = new Button(this);
        addImg.setText(I18n.t("moment_add_photo") + " (0/3)");
        addImg.setTextSize(12);
        addImg.setTextColor(Utils.ACCENT);
        addImg.setBackground(Utils.bg(this, Utils.CARD2, 14));
        addImg.setElevation(0);
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
        box.addView(addImg);`;
  const neu = `        postPreviewGrid = new GridLayout(this);
        postPreviewGrid.setColumnCount(3);
        box.addView(postPreviewGrid);
        Button addImg = new Button(this);
        addImg.setText(I18n.t("moment_add_photo") + " (0/9)");
        addImg.setTextSize(12);
        addImg.setTextColor(Utils.ACCENT);
        addImg.setBackground(Utils.bg(this, Utils.CARD2, 14));
        addImg.setElevation(0);
        addImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pendingPostImages.size() >= 9) {
                    Ui.banner(HomeActivity.this, "最多9张");
                    return;
                }
                pickPostImage();
            }
        });
        box.addView(addImg);
        refreshPostPreview();`;
  if (t.includes(old)) { t = t.split(old).join(neu); fs.writeFileSync(HA, t); console.log('[HomeActivity.java] post dialog 9-image'); }
  else console.log('post dialog anchor not found');
}

// addPostBitmap: 上限9 + 预览刷新
{
  let t = fs.readFileSync(HA, 'utf8');
  const old = '                            pendingPostImages.add(b64);\n                            Ui.banner(HomeActivity.this, I18n.t("moment_add_photo") + " (" + pendingPostImages.size() + "/3)");';
  const neu = '                            pendingPostImages.add(b64);\n                            Ui.banner(HomeActivity.this, I18n.t("moment_add_photo") + " (" + pendingPostImages.size() + "/9)");\n                            refreshPostPreview();';
  if (t.includes(old)) { t = t.split(old).join(neu); fs.writeFileSync(HA, t); console.log('[HomeActivity.java] addPostBitmap updated'); }
  else console.log('addPostBitmap anchor not found');
}

// refreshPostPreview 方法
{
  let t = fs.readFileSync(HA, 'utf8');
  const anchor = '    private void showScanResult(final Bitmap bmp) {';
  const methods = `    private void refreshPostPreview() {
        if (postPreviewGrid == null) return;
        postPreviewGrid.removeAllViews();
        int cell = (getResources().getDisplayMetrics().widthPixels - Utils.dp(this, 36 + 60)) / 3;
        for (String b64 : pendingPostImages) {
            try {
                byte[] data = Base64.decode(b64, Base64.NO_WRAP);
                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                ImageView iv = new ImageView(this);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setBackground(Utils.bg(this, Utils.CARD2, 8));
                GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
                glp.width = cell;
                glp.height = cell;
                glp.setMargins(Utils.dp(this, 2), Utils.dp(this, 2), Utils.dp(this, 2), Utils.dp(this, 2));
                iv.setLayoutParams(glp);
                iv.setImageBitmap(bmp);
                postPreviewGrid.addView(iv);
            } catch (Exception ignored) {
            }
        }
    }

    private void showScanResult(final Bitmap bmp) {`;
  if (t.includes(anchor)) { t = t.split(anchor).join(methods); fs.writeFileSync(HA, t); console.log('[HomeActivity.java] refreshPostPreview added'); }
  else console.log('showScanResult anchor not found');
}
console.log('DONE');
