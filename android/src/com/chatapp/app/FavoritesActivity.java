package com.chatapp.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 我的收藏: 收藏的动态 + 收藏的对话/图片, 长按可删除或转发 */
public class FavoritesActivity extends Activity implements Net.Listener {

    private LinearLayout content;
    private final List<Models.Post> posts = new ArrayList<>();
    private final List<JSONObject> favMsgs = new ArrayList<>();
    private final List<Models.Contact> forwardContacts = new ArrayList<>();
    private JSONObject pendingForwardMsg = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setBackgroundColor(Utils.BG);
        Theme.init(this);
        Theme.apply(this);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Utils.BG);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackground(Utils.bg(this, Utils.CARD, 18));
        top.setPadding(Utils.dp(this, 4), Utils.dp(this, 8), Utils.dp(this, 12), Utils.dp(this, 8));
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        topLp.setMargins(Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8), 0);
        TextView back = Utils.tv(this, "\u2190", 26, Utils.TEXT, Gravity.CENTER);
        back.setPadding(Utils.dp(this, 10), 0, Utils.dp(this, 10), Utils.dp(this, 4));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        top.addView(back);
        TextView title = Utils.tv(this, I18n.t("my_favorites"), 17, Utils.TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title);
        root.addView(top, topLp);

        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        sc.setBackgroundColor(Utils.BG);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Utils.dp(this, 16), Utils.dp(this, 18), Utils.dp(this, 16), Utils.dp(this, 20));
        content.setBackgroundColor(Utils.BG);
        sc.addView(content);
        root.addView(sc, Utils.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void render() {
        content.removeAllViews();
        TextView pTitle = Utils.tv(this, I18n.t("fav_posts"), 16, Utils.TEXT, Gravity.START);
        pTitle.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(pTitle);
        content.addView(Utils.hSpace(this, 8));
        if (posts.isEmpty()) {
            content.addView(Utils.tv(this, I18n.t("fav_empty"), 13, Utils.TEXT_DIM, Gravity.CENTER));
        } else {
            for (final Models.Post p : posts) {
                content.addView(favPostCard(p));
                content.addView(Utils.hSpace(this, 10));
            }
        }
        content.addView(Utils.hSpace(this, 18));
        TextView mTitle = Utils.tv(this, I18n.t("fav_messages"), 16, Utils.TEXT, Gravity.START);
        mTitle.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(mTitle);
        content.addView(Utils.hSpace(this, 8));
        if (favMsgs.isEmpty()) {
            content.addView(Utils.tv(this, I18n.t("fav_empty"), 13, Utils.TEXT_DIM, Gravity.CENTER));
        } else {
            for (final JSONObject m : favMsgs) {
                content.addView(favMsgRow(m));
            }
        }
    }

    private View favPostCard(final Models.Post p) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Utils.bg(this, Utils.CARD, 16));
        card.setPadding(Utils.dp(this, 12), Utils.dp(this, 12), Utils.dp(this, 12), Utils.dp(this, 12));
        card.addView(Utils.tv(this, p.nickname, 14, Utils.TEXT, Gravity.START));
        if (p.text != null && !p.text.isEmpty()) {
            TextView tx = Utils.tv(this, p.text, 14, Utils.TEXT, Gravity.START);
            tx.setPadding(0, Utils.dp(this, 6), 0, Utils.dp(this, 4));
            card.addView(tx);
        }
        if (!p.images.isEmpty()) {
            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(3);
            int cell = (getResources().getDisplayMetrics().widthPixels - Utils.dp(this, 32 + 48)) / 3;
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
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Ui.list(FavoritesActivity.this, I18n.t("my_favorites"),
                        new String[]{I18n.t("fav_delete"), I18n.t("fav_forward")},
                        new Ui.Click[]{
                                new Ui.Click() {
                                    @Override
                                    public void onClick() {
                                        try {
                                            JSONObject o = new JSONObject();
                                            o.put("type", "favorite_post");
                                            o.put("id", p.id);
                                            Net.get().send(o);
                                            posts.remove(p);
                                            render();
                                        } catch (Exception ignored) {
                                        }
                                    }
                                },
                                new Ui.Click() {
                                    @Override
                                    public void onClick() {
                                        try {
                                            JSONObject o = new JSONObject();
                                            o.put("type", "repost_post");
                                            o.put("id", p.id);
                                            Net.get().send(o);
                                            Ui.popup(FavoritesActivity.this, I18n.t("reposted"));
                                        } catch (Exception ignored) {
                                        }
                                    }
                                }
                        });
                return true;
            }
        });
        return card;
    }

    private View favMsgRow(final JSONObject m) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(Utils.bg(this, Utils.CARD, 16));
        row.setPadding(Utils.dp(this, 12), Utils.dp(this, 10), Utils.dp(this, 12), Utils.dp(this, 10));
        String kind = m.optString("kind", "text");
        if ("image".equals(kind)) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackground(Utils.bg(this, Utils.CARD2, 8));
            iv.setLayoutParams(new LinearLayout.LayoutParams(Utils.dp(this, 48), Utils.dp(this, 48)));
            AvatarManager.loadFileImage(this, m.optString("url"), iv);
            row.addView(iv);
            row.addView(Utils.vSpace(this, 10));
        } else if ("file".equals(kind)) {
            row.addView(Utils.tv(this, "\ud83d\udcc1", 22, Utils.TEXT, Gravity.CENTER));
            row.addView(Utils.vSpace(this, 10));
        }
        String label = "text".equals(kind) ? m.optString("text") : m.optString("name", kind);
        row.addView(Utils.tv(this, label, 14, Utils.TEXT, Gravity.START), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Ui.list(FavoritesActivity.this, I18n.t("my_favorites"),
                        new String[]{I18n.t("fav_delete"), I18n.t("fav_forward")},
                        new Ui.Click[]{
                                new Ui.Click() {
                                    @Override
                                    public void onClick() {
                                        removeFavMsg(m);
                                    }
                                },
                                new Ui.Click() {
                                    @Override
                                    public void onClick() {
                                        forwardMsg(m);
                                    }
                                }
                        });
                return true;
            }
        });
        return row;
    }

    private void removeFavMsg(JSONObject m) {
        try {
            Set<String> set = new HashSet<>(getSharedPreferences("chatapp_fav_msgs", MODE_PRIVATE)
                    .getStringSet("favs", new HashSet<String>()));
            set.remove(m.toString());
            getSharedPreferences("chatapp_fav_msgs", MODE_PRIVATE).edit().putStringSet("favs", set).apply();
            favMsgs.remove(m);
            render();
        } catch (Exception ignored) {
        }
    }

    private void forwardMsg(final JSONObject m) {
        pendingForwardMsg = m;
        try {
            JSONObject o = new JSONObject();
            o.put("type", "get_contacts");
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    private void showForwardPick(JSONObject m) {
        if (forwardContacts.isEmpty()) {
            Ui.popup(this, I18n.t("no_friends_yet"));
            return;
        }
        String[] names = new String[forwardContacts.size()];
        Ui.Click[] clicks = new Ui.Click[forwardContacts.size()];
        for (int i = 0; i < forwardContacts.size(); i++) {
            final Models.Contact c = forwardContacts.get(i);
            names[i] = c.nickname + " (@" + c.username + ")";
            clicks[i] = new Ui.Click() {
                @Override
                public void onClick() {
                    try {
                        String kind = m.optString("kind", "text");
                        JSONObject o = new JSONObject();
                        if ("text".equals(kind)) {
                            o.put("type", "send_message");
                            o.put("to", c.username);
                            o.put("text", m.optString("text"));
                        } else {
                            o.put("type", "forward_message");
                            o.put("to", c.username);
                            o.put("id", m.optString("id"));
                        }
                        Net.get().send(o);
                        Ui.popup(FavoritesActivity.this, I18n.t("forwarded_ok"));
                    } catch (Exception ignored) {
                    }
                }
            };
        }
        Ui.list(this, I18n.t("forward_to"), names, clicks);
    }

    private void loadLocalFavs() {
        favMsgs.clear();
        try {
            Set<String> set = getSharedPreferences("chatapp_fav_msgs", MODE_PRIVATE)
                    .getStringSet("favs", new HashSet<String>());
            for (String s : set) {
                try {
                    favMsgs.add(new JSONObject(s));
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Net.get().setListener(this);
        loadLocalFavs();
        try {
            JSONObject o = new JSONObject();
            o.put("type", "get_my_favorites");
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Net.get().setListener(null);
    }

    @Override
    public void onConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "get_my_favorites");
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public void onMessage(final JSONObject obj) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    String type = obj.optString("type");
                    if ("my_favorites".equals(type)) {
                        posts.clear();
                        posts.addAll(Models.parsePosts(obj.optJSONArray("posts")));
                        render();
                    } else if ("contacts".equals(type)) {
                        forwardContacts.clear();
                        forwardContacts.addAll(Models.parseContacts(obj.optJSONArray("contacts")));
                        if (pendingForwardMsg != null) {
                            JSONObject m = pendingForwardMsg;
                            pendingForwardMsg = null;
                            showForwardPick(m);
                        }
                    } else if ("toast".equals(type) || "error".equals(type)) {
                        Ui.popup(FavoritesActivity.this, I18n.serverMsg(obj.optString("message")));
                    }
                } catch (Throwable t) {
                    try {
                        JSONObject c = new JSONObject();
                        c.put("type", "crash");
                        c.put("stack", t.toString());
                        Net.get().send(c);
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    @Override
    public void onDisconnected(String reason) {
    }
}
