package com.chatapp.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 邀请好友进群: 全屏界面展示所有好友, 已在群中的显示灰色已邀请, 加载完成整体显示 */
public class GroupInviteActivity extends Activity implements Net.Listener {

    private String groupId;
    private final Set<String> members = new HashSet<>();
    private final List<Models.Contact> contacts = new ArrayList<>();
    private LinearLayout list;
    private LinearLayout rootView;
    private TriangleLoadingView loading;
    private boolean loaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setBackgroundColor(Utils.BG);
        Theme.init(this);
        Theme.apply(this);
        groupId = getIntent().getStringExtra("groupId");
        if (groupId == null) groupId = "";
        ArrayList<String> ms = getIntent().getStringArrayListExtra("members");
        if (ms != null) members.addAll(ms);
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
        TextView back = Utils.tv(this, "<", 26, Utils.TEXT, Gravity.CENTER);
        back.setPadding(Utils.dp(this, 10), 0, Utils.dp(this, 10), Utils.dp(this, 4));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        top.addView(back);
        TextView title = Utils.tv(this, I18n.t("invite"), 17, Utils.TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        topLp.setMargins(Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8), 0);
        root.addView(top, topLp);

        ScrollView sc = new ScrollView(this);
        sc.setBackgroundColor(Utils.BG);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Utils.dp(this, 16), Utils.dp(this, 18), Utils.dp(this, 16), Utils.dp(this, 20));
        body.setBackgroundColor(Utils.BG);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        body.addView(list);
        sc.addView(body);
        root.addView(sc, Utils.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Utils.BG);
        rootView = root;
        root.setVisibility(View.GONE);
        frame.addView(root, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        loading = new TriangleLoadingView(this);
        frame.addView(loading, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        setContentView(frame);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Net.get().setListener(this);
        try {
            JSONObject o = new JSONObject();
            o.put("type", "get_contacts");
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
    }

    @Override
    public void onMessage(final JSONObject obj) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    String type = obj.optString("type");
                    if ("contacts".equals(type)) {
                        contacts.clear();
                        contacts.addAll(Models.parseContacts(obj.optJSONArray("contacts")));
                        loaded = true;
                        render();
                        if (loading != null) loading.setVisibility(View.GONE);
                        if (rootView != null) rootView.setVisibility(View.VISIBLE);
                    }
                } catch (Throwable ignored) {
                }
            }
        });
    }

    @Override
    public void onDisconnected(String reason) {
    }

    private void render() {
        list.removeAllViews();
        if (contacts.isEmpty()) {
            list.addView(Utils.tv(this, I18n.t("no_friends_invite"), 14, Utils.TEXT_DIM, Gravity.CENTER));
            return;
        }
        for (final Models.Contact c : contacts) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, Utils.dp(this, 8), 0, Utils.dp(this, 8));
            row.addView(AvatarManager.avatarView(this, c.username, c.nickname, c.hasAvatar, 40, 15));
            row.addView(Utils.vSpace(this, 10));
            row.addView(Utils.tv(this, c.nickname, 15, Utils.TEXT, Gravity.START), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            final boolean inGroup = members.contains(c.username);
            TextView btn = Utils.tv(this, inGroup ? I18n.t("已邀请", "已邀請", "Invited") : I18n.t("邀请", "邀請", "Invite"),
                    13, inGroup ? Utils.TEXT_DIM : Color.WHITE, Gravity.CENTER);
            btn.setPadding(Utils.dp(this, 16), Utils.dp(this, 7), Utils.dp(this, 16), Utils.dp(this, 7));
            btn.setBackground(Utils.bg(this, inGroup ? Utils.CARD2 : Utils.ACCENT, 16));
            if (!inGroup) {
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            JSONObject o = new JSONObject();
                            o.put("type", "invite_group");
                            o.put("id", groupId);
                            o.put("username", c.username);
                            Net.get().send(o);
                        } catch (Exception ignored) {
                        }
                        members.add(c.username);
                        render();
                    }
                });
            }
            row.addView(btn);
            list.addView(row);
            list.addView(Utils.divider(this));
        }
    }
}
