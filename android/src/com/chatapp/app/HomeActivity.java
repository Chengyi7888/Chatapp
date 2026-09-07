package com.chatapp.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 主界�? 底部 5 标签导航 (更新 / 通话 / 社群 / 聊天 / 自己)
 * 社群: 群聊列表 + 新建群聊
 * 聊天: 长按会话可置�?删除记录/删除好友/拉黑, 点头像看好友名片
 * 自己: 语言与地区设�? */
public class HomeActivity extends Activity implements Net.Listener {

    private static final int PICK_AVATAR = 1001;
    private static final int PICK_POST_IMAGE = 1002;
    private static int pendingTab = 3;
    private static int savedMeScroll = 0;

    private List<Models.Conversation> conversations = new ArrayList<>();
    private List<Models.Contact> contacts = new ArrayList<>();
    private List<Models.Contact> pending = new ArrayList<>();
    private List<Models.Group> groups = new ArrayList<>();
    private List<Models.Post> postsList = new ArrayList<>();
    private List<JSONObject> activities = new ArrayList<>();
    private boolean activitiesLoaded = false;
    private LinearLayout postsContainer;
    private final List<String> pendingPostImages = new ArrayList<>();
    private GridLayout postPreviewGrid;

    private FrameLayout content;
    private LinearLayout root, tabRow;
    private LinearLayout tabsBar;
    private LinearLayout updatesLayout, callsLayout, communitiesLayout, chatsLayout, meLayout;
    private LinearLayout chatListContainer;
    private LinearLayout groupListContainer;
    private ScrollView updatesScroll, callsScroll, communitiesScroll, chatsScroll, meScroll;
    private TextView tabUpdates, tabCalls, tabCommunities, tabChats, tabMe;
    private TextView statusDot, statusText;
    private EditText chatSearch;
    private TextView filterAll, filterUnread;
    private String chatFilter = "all";
    private TextView fab;
    private LinearLayout notifPrompt;
    private LinearLayout updateTipCard;
    private TextView updateTipTitle, updateTipSub;
    private TextView channelDropdownText;
    private String selectedChannelPkg = "";
    private final Handler updateCheckHandler = new Handler(Looper.getMainLooper());
    private final Runnable updateCheckTask = new Runnable() {
        @Override
        public void run() {
            if (updatesScroll != null && updatesScroll.getVisibility() == View.VISIBLE) {
                UpdateChecker.checkAsync(HomeActivity.this, new UpdateChecker.Result() {
                    @Override
                    public void onResult(boolean hasUpdate, String version, String file) {
                        if (updateTipCard != null) {
                            if (hasUpdate) {
                                updateTipTitle.setText(I18n.t("new_version_available") + " (v" + version + ")");
                                updateTipSub.setText(I18n.t("click_confirm_update"));
                                updateTipCard.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
            }
            updateCheckHandler.postDelayed(this, 300000);
        }
    };

    private LinearLayout searchResultBox;
    private String searchState = "idle";
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            if (Net.get().isConnected()) {
                refreshAll();
                updateStatusUi();
                refreshHandler.postDelayed(this, 15000);
            }
        }
    };
    private final Runnable searchTimeout = new Runnable() {
        @Override
        public void run() {
            if ("waiting".equals(searchState)) {
                searchState = "idle";
                setSearchBox(I18n.t("search_no_response"), Utils.DANGER);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setBackgroundColor(Utils.BG);
        Theme.init(this);
        Theme.apply(this);
        I18n.init(this);
        Utils.loadFontScale(this);
        Session.init(this);
        try {
            buildUi();
            selectTab(pendingTab); // 默认进入聊天
            if (savedMeScroll > 0) {
                final int target = savedMeScroll;
                savedMeScroll = 0;
                meScroll.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        meScroll.scrollTo(0, target);
                    }
                }, 60);
            }
        } catch (Throwable t) {
            reportCrash(t);
            showFatalScreen(t);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!UpdateChecker.isCheckedOnce()) {
            UpdateChecker.setCheckedOnce();
            UpdateChecker.check(this, true);
        }
        if (Theme.apply(this)) reapplyTheme();
        try {
            Net.get().setListener(this);
            if (Net.get().isRunning()) {
                if (Net.get().isConnected()) refreshAll();
                else {
                                        String[] sp = Session.serverParts();
                    Net.get().stop();
                    Net.get().start(sp[0], Integer.parseInt(sp[1]));
                }
            } else {
                String[] sp = Session.serverParts();
                AvatarManager.setHost(sp[0]);
                Net.get().start(sp[0], Integer.parseInt(sp[1]));
            }
            updateStatusUi();
            refreshHandler.removeCallbacks(refreshTask);
            refreshHandler.postDelayed(refreshTask, 15000);
            updateCheckHandler.removeCallbacks(updateCheckTask);
            updateCheckHandler.postDelayed(updateCheckTask, 300000);
        } catch (Throwable t) {
            reportCrash(t);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Net.get().setListener(null);
        refreshHandler.removeCallbacks(refreshTask);
        refreshHandler.removeCallbacks(searchTimeout);
        updateCheckHandler.removeCallbacks(updateCheckTask);
    }

    private void reportCrash(Throwable t) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(t.toString()).append('\n');
            for (StackTraceElement e : t.getStackTrace()) {
                sb.append("  at ").append(e.toString()).append('\n');
            }
            JSONObject o = new JSONObject();
            o.put("type", "crash");
            o.put("stack", sb.toString());
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    private void showFatalScreen(Throwable t) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setBackgroundColor(Utils.BG);
        l.setPadding(Utils.dp(this, 30), Utils.dp(this, 60), Utils.dp(this, 30), Utils.dp(this, 30));
        TextView msg = Utils.tv(this, I18n.t("fatal_init"), 15, Utils.DANGER, Gravity.CENTER);
        msg.setPadding(0, 0, 0, Utils.dp(this, 24));
        l.addView(msg);
        Button btn = Utils.button(this);
        btn.setText(I18n.t("logout"));
        btn.setTextColor(Color.WHITE);
        btn.setBackground(Utils.bg(this, Utils.DANGER, 8));
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutToMain();
            }
        });
        l.addView(btn);
        setContentView(l);
    }

    private void logoutToMain() {
        Net.get().stop();
        Session.logout();
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    // ================= UI 构建 =================

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Utils.BG);

        content = new FrameLayout(this);
        content.setBackgroundColor(Utils.BG);
        root.addView(content, Utils.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        updatesLayout = makeScrollView();
        updatesScroll = (ScrollView) updatesLayout.getParent();
        content.addView(updatesScroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        callsLayout = makeScrollView();
        callsScroll = (ScrollView) callsLayout.getParent();
        callsScroll.setVisibility(View.GONE);
        content.addView(callsScroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        communitiesLayout = makeScrollView();
        communitiesScroll = (ScrollView) communitiesLayout.getParent();
        communitiesScroll.setVisibility(View.GONE);
        content.addView(communitiesScroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        chatsLayout = makeScrollView();
        chatsScroll = (ScrollView) chatsLayout.getParent();
        chatsScroll.setVisibility(View.GONE);
        content.addView(chatsScroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        meLayout = makeScrollView();
        meScroll = (ScrollView) meLayout.getParent();
        meScroll.setVisibility(View.GONE);
        content.addView(meScroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        fab = new TextView(this);
        fab.setText("+");
        fab.setTextSize(26);
        fab.setTextColor(Color.WHITE);
        fab.setGravity(Gravity.CENTER);
        int fabSize = Utils.dp(this, 56);
        fab.setLayoutParams(new FrameLayout.LayoutParams(fabSize, fabSize));
        ((FrameLayout.LayoutParams) fab.getLayoutParams()).gravity = Gravity.BOTTOM | Gravity.END;
        ((FrameLayout.LayoutParams) fab.getLayoutParams()).setMargins(0, 0, Utils.dp(this, 20), Utils.dp(this, 24));
        GradientDrawable fabBg = new GradientDrawable();
        fabBg.setShape(GradientDrawable.OVAL);
        fabBg.setColor(Utils.ACCENT);
        fab.setBackground(fabBg);
        fab.setVisibility(View.GONE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, FriendCenterActivity.class));
            }
        });
        content.addView(fab);

        tabsBar = new LinearLayout(this);
        tabsBar.setOrientation(LinearLayout.HORIZONTAL);
        tabsBar.setBackground(Utils.ring(this, Utils.CARD, Utils.CARD2, 1, 24));
        tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabUpdates = tab(I18n.t("updates"), 0);
        tabCalls = tab(I18n.t("activities"), 1);
        tabCommunities = tab(I18n.t("communities"), 2);
        tabChats = tab(I18n.t("chats"), 3);
        tabMe = tab(I18n.t("me"), 4);
        tabRow.addView(tabUpdates, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tabRow.addView(tabCalls, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tabRow.addView(tabCommunities, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tabRow.addView(tabChats, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tabRow.addView(tabMe, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tabsBar.addView(tabRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams tabsLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tabsLp.setMargins(Utils.dp(this, 14), Utils.dp(this, 2), Utils.dp(this, 14), Utils.dp(this, 28));
        root.addView(tabsBar, tabsLp);

        setContentView(root);
        initChatsHeader();
        renderUpdates();
        renderActivities();
        renderCommunities();
        renderMe();
    }

    private LinearLayout makeScrollView() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundColor(Utils.BG);
        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        sc.setBackgroundColor(Utils.BG);
        sc.addView(l);
        return l;
    }

    private TextView tab(String text, final int index) {
        TextView t = Utils.tv(this, text, 15, Utils.TEXT_DIM, Gravity.CENTER);
        t.setPadding(0, Utils.dp(this, 12), 0, Utils.dp(this, 12));
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(index);
            }
        });
        return t;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    private void selectTab(int i) {
        pendingTab = i;
        tabUpdates.setTextColor(i == 0 ? Utils.ACCENT : Utils.TEXT_DIM);
        tabUpdates.setTypeface(null, i == 0 ? Typeface.BOLD : Typeface.NORMAL);
        tabCalls.setTextColor(i == 1 ? Utils.ACCENT : Utils.TEXT_DIM);
        tabCalls.setTypeface(null, i == 1 ? Typeface.BOLD : Typeface.NORMAL);
        tabCommunities.setTextColor(i == 2 ? Utils.ACCENT : Utils.TEXT_DIM);
        tabCommunities.setTypeface(null, i == 2 ? Typeface.BOLD : Typeface.NORMAL);
        tabChats.setTextColor(i == 3 ? Utils.ACCENT : Utils.TEXT_DIM);
        tabChats.setTypeface(null, i == 3 ? Typeface.BOLD : Typeface.NORMAL);
        tabMe.setTextColor(i == 4 ? Utils.ACCENT : Utils.TEXT_DIM);
        tabMe.setTypeface(null, i == 4 ? Typeface.BOLD : Typeface.NORMAL);

        updatesScroll.setVisibility(i == 0 ? View.VISIBLE : View.GONE);
        callsScroll.setVisibility(i == 1 ? View.VISIBLE : View.GONE);
        communitiesScroll.setVisibility(i == 2 ? View.VISIBLE : View.GONE);
        chatsScroll.setVisibility(i == 3 ? View.VISIBLE : View.GONE);
        meScroll.setVisibility(i == 4 ? View.VISIBLE : View.GONE);
        fab.setVisibility(i == 3 ? View.VISIBLE : View.GONE);

        if (i == 0) renderUpdates();
        if (i == 2) renderCommunities();
        if (i == 3) renderChats();
        if (i == 4) renderMe();
        updateChatTabBadge();
    }

    private void updateChatTabBadge() {
        int unread = 0;
        for (Models.Conversation c : conversations) unread += c.unread;
        int total = unread + pending.size();
        if (total > 0) {
            tabChats.setText(I18n.t("chats") + " " + total);
            tabChats.setTextColor(Utils.ACCENT);
        } else {
            tabChats.setText(I18n.t("chats"));
        }
    }

    // ================= 更新�?=================

    private void renderUpdates() {
        updatesLayout.removeAllViews();
        updatesLayout.setPadding(Utils.dp(this, 18), Utils.dp(this, 22), Utils.dp(this, 18), Utils.dp(this, 20));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(0, 0, 0, Utils.dp(this, 18));
        TextView title = Utils.tv(this, I18n.t("updates"), 28, Utils.TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(title, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        statusDot = new TextView(this);
        int sd = Utils.dp(this, 10);
        statusDot.setLayoutParams(new LinearLayout.LayoutParams(sd, sd));
        statusDot.setBackground(circle(0xFF8E8E93));
        head.addView(statusDot);
        head.addView(Utils.vSpace(this, 5));
        statusText = Utils.tv(this, I18n.t("connecting"), 12, Utils.TEXT_DIM, Gravity.CENTER);
        head.addView(statusText);
        updatesLayout.addView(head);

                updateTipCard = new LinearLayout(this);
        updateTipCard.setOrientation(LinearLayout.VERTICAL);
        updateTipCard.setBackground(Utils.bg(this, Utils.CARD, 16));
        updateTipCard.setPadding(Utils.dp(this, 16), Utils.dp(this, 14), Utils.dp(this, 16), Utils.dp(this, 14));
        updateTipTitle = Utils.tv(this, I18n.t("new_version_available"), 15, Utils.TEXT, Gravity.START);
        updateTipCard.addView(updateTipTitle);
        updateTipCard.addView(Utils.hSpace(this, 10));
        updateTipSub = Utils.tv(this, I18n.t("click_confirm_update"), 12, Utils.TEXT_DIM, Gravity.START);
        updateTipCard.addView(updateTipSub);

        LinearLayout tipBtns = new LinearLayout(this);
        tipBtns.setOrientation(LinearLayout.HORIZONTAL);
        Button okBtn = Utils.button(this);
        okBtn.setText(I18n.t("confirm"));
        okBtn.setTextSize(14);
        okBtn.setTextColor(Color.WHITE);
        okBtn.setBackground(Utils.bg(this, Utils.ACCENT, 22));
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateChecker.downloadLatest(HomeActivity.this);
                if (updateTipCard != null) updateTipCard.setVisibility(View.GONE);
            }
        });
        Button cancelBtn = Utils.button(this);
        cancelBtn.setText(I18n.t("cancel"));
        cancelBtn.setTextSize(14);
        cancelBtn.setTextColor(Utils.TEXT);
        cancelBtn.setBackground(Utils.bg(this, Utils.CARD2, 22));
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTipCard.setVisibility(View.GONE);
            }
        });
        tipBtns.addView(okBtn, Utils.lp(0, Utils.dp(this, 42), 1f));
        tipBtns.addView(Utils.vSpace(this, 10));
        tipBtns.addView(cancelBtn, Utils.lp(0, Utils.dp(this, 42), 1f));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Utils.dp(this, 12);
        updateTipCard.addView(tipBtns, tlp);
        updateTipCard.setVisibility(View.GONE);
        updatesLayout.addView(updateTipCard);

        UpdateChecker.checkAsync(this, new UpdateChecker.Result() {
            @Override
            public void onResult(boolean hasUpdate, String version, String file) {
                if (hasUpdate) {
                    updateTipTitle.setText(I18n.t("new_version_available") + " (v" + version + ")");
                    updateTipSub.setText(I18n.t("click_confirm_update"));
                    updateTipCard.setVisibility(View.VISIBLE);
                } else {
                    updateTipCard.setVisibility(View.GONE);
                }
            }
        });

        updatesLayout.addView(Utils.hSpace(this, 22));

        LinearLayout dynHead = new LinearLayout(this);
        dynHead.setOrientation(LinearLayout.HORIZONTAL);
        dynHead.setGravity(Gravity.CENTER_VERTICAL);
        TextView dynTitle = Utils.tv(this, I18n.t("dynamics"), 18, Utils.TEXT, Gravity.START);
        dynTitle.setTypeface(Typeface.DEFAULT_BOLD);
        dynHead.addView(dynTitle, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button pubBtn = Utils.button(this);
        pubBtn.setText(I18n.t("publish"));
        pubBtn.setTextSize(13);
        pubBtn.setTextColor(Color.WHITE);
        pubBtn.setBackground(Utils.bg(this, Utils.ACCENT, 16));
        pubBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, PostActivity.class));
            }
        });
        dynHead.addView(pubBtn);
        updatesLayout.addView(dynHead);
        updatesLayout.addView(Utils.hSpace(this, 10));

        postsContainer = new LinearLayout(this);
        postsContainer.setOrientation(LinearLayout.VERTICAL);
        updatesLayout.addView(postsContainer);
        renderPosts();

        updatesLayout.addView(Utils.hSpace(this, 22));

        TextView chTitle = Utils.tv(this, I18n.t("channels"), 18, Utils.TEXT, Gravity.START);
        chTitle.setTypeface(Typeface.DEFAULT_BOLD);
        chTitle.setPadding(0, 0, 0, Utils.dp(this, 4));
        updatesLayout.addView(chTitle);
        updatesLayout.addView(Utils.tv(this, I18n.t("browse_channels"), 12, Utils.TEXT_DIM, Gravity.START));
        updatesLayout.addView(Utils.hSpace(this, 12));

        LinearLayout channel = new LinearLayout(this);
        channel.setOrientation(LinearLayout.VERTICAL);
        channel.setBackground(Utils.bg(this, Utils.CARD, 16));
        channel.setPadding(Utils.dp(this, 14), Utils.dp(this, 12), Utils.dp(this, 14), Utils.dp(this, 12));
        LinearLayout dropdown = new LinearLayout(this);
        dropdown.setOrientation(LinearLayout.HORIZONTAL);
        dropdown.setGravity(Gravity.CENTER_VERTICAL);
        dropdown.setBackground(Utils.bg(this, Utils.CARD2, 12));
        dropdown.setPadding(Utils.dp(this, 12), Utils.dp(this, 10), Utils.dp(this, 12), Utils.dp(this, 10));
        channelDropdownText = Utils.tv(this, I18n.t("all_channels"), 14, Utils.TEXT, Gravity.START);
        dropdown.addView(channelDropdownText, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        dropdown.addView(Utils.tv(this, " v", 14, Utils.ACCENT, Gravity.END));
        dropdown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChannelDialog();
            }
        });
        channel.addView(dropdown);
        channel.addView(Utils.hSpace(this, 12));
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        Button c1 = Utils.button(this);
        c1.setText(I18n.t("goto"));
        c1.setTextSize(14);
        c1.setTextColor(Color.WHITE);
        c1.setBackground(Utils.bg(this, Utils.ACCENT, 12));
        c1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChannel();
            }
        });
        Button c2 = darkBtn(I18n.t("recommend"));
        btnRow.addView(c1, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        btnRow.addView(Utils.vSpace(this, 10));
        btnRow.addView(c2, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        channel.addView(btnRow);
        updatesLayout.addView(channel);
        updateStatusUi();
    }

    private TextView roundIconBtn(String emoji) {
        TextView t = Utils.tv(this, emoji, 18, Utils.TEXT, Gravity.CENTER);
        int s = Utils.dp(this, 40);
        t.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(Utils.CARD2);
        t.setBackground(g);
        return t;
    }

    private Button darkBtn(String text) {
        Button b = Utils.button(this);
        b.setText(text);
        b.setTextSize(14);
        b.setTextColor(Utils.TEXT);
        b.setBackground(Utils.bg(this, Utils.CARD2, 12));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ui.popup(HomeActivity.this, I18n.t("channel_coming_soon"));
            }
        });
        return b;
    }

    // ================= 通话�?=================

    private void renderActivities() {
        callsLayout.removeAllViews();
        callsLayout.setPadding(Utils.dp(this, 18), Utils.dp(this, 22), Utils.dp(this, 18), Utils.dp(this, 20));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = Utils.tv(this, I18n.t("activities"), 28, Utils.TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(title, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        callsLayout.addView(head);
        callsLayout.addView(Utils.hSpace(this, 14));

        if (activities.isEmpty()) {
            callsLayout.addView(Utils.hSpace(this, 40));
            if (!activitiesLoaded) {
                LinearLayout.LayoutParams triLp = new LinearLayout.LayoutParams(Utils.dp(this, 56), Utils.dp(this, 56));
                triLp.gravity = Gravity.CENTER_HORIZONTAL;
                callsLayout.addView(new TriangleLoadingView(this), triLp);
            } else {
                callsLayout.addView(Utils.tv(this, I18n.t("activities_empty"), 14, Utils.TEXT_DIM, Gravity.CENTER));
            }
        } else {
            for (final JSONObject a : activities) {
                callsLayout.addView(activityCard(a));
                callsLayout.addView(Utils.hSpace(this, 12));
            }
        }
    }

    private View activityCard(final JSONObject a) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Utils.bg(this, Utils.CARD, 16));
        card.setPadding(Utils.dp(this, 14), Utils.dp(this, 12), Utils.dp(this, 14), Utils.dp(this, 12));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(AvatarManager.avatarView(this, a.optString("from"), a.optString("nickname"), a.optBoolean("hasAvatar"), 40, 15));
        head.addView(Utils.vSpace(this, 10));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(Utils.tv(this, a.optString("nickname"), 14, Utils.TEXT, Gravity.START));
        col.addView(Utils.tv(this, Utils.timeText(a.optLong("time")), 11, Utils.TEXT_DIM, Gravity.START));
        head.addView(col, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(head);
        card.addView(Utils.hSpace(this, 8));

        TextView t = Utils.tv(this, a.optString("title"), 16, Utils.TEXT, Gravity.START);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(t);
        String txt = a.optString("text");
        if (!txt.isEmpty()) {
            TextView tx = Utils.tv(this, txt, 14, Utils.TEXT_DIM, Gravity.START);
            tx.setPadding(0, Utils.dp(this, 6), 0, Utils.dp(this, 4));
            card.addView(tx);
        }
        card.addView(Utils.hSpace(this, 8));

        LinearLayout foot = new LinearLayout(this);
        foot.setOrientation(LinearLayout.HORIZONTAL);
        foot.setGravity(Gravity.CENTER_VERTICAL);
        final boolean isGame = "jump".equals(a.optString("game"));
        if (isGame) {
            TextView startBtn = Utils.tv(this, I18n.t("jump_start"), 13, Color.WHITE, Gravity.CENTER);
            startBtn.setBackground(Utils.bg(this, Utils.ACCENT, 18));
            startBtn.setPadding(Utils.dp(this, 14), Utils.dp(this, 7), Utils.dp(this, 14), Utils.dp(this, 7));
            startBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(HomeActivity.this, JumpGameActivity.class));
                }
            });
            foot.addView(startBtn);
        } else {
            JSONArray joiners = a.optJSONArray("joiners");
            int joinedCount = joiners == null ? 0 : joiners.length();
            boolean joined = a.optBoolean("joined");
            TextView joinBtn = Utils.tv(this, (joined ? I18n.t("activity_joined") : I18n.t("activity_join")) + " (" + joinedCount + ")", 13,
                    joined ? Utils.TEXT_DIM : Color.WHITE, Gravity.CENTER);
            joinBtn.setBackground(Utils.bg(this, joined ? Utils.CARD2 : Utils.ACCENT, 18));
            joinBtn.setPadding(Utils.dp(this, 14), Utils.dp(this, 7), Utils.dp(this, 14), Utils.dp(this, 7));
            joinBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        JSONObject o = new JSONObject();
                        o.put("type", "join_activity");
                        o.put("id", a.optString("id"));
                        Net.get().send(o);
                    } catch (Exception ignored) {
                    }
                }
            });
            foot.addView(joinBtn);
        }
        card.addView(foot);
        if (isGame) {
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(HomeActivity.this, JumpGameActivity.class));
                }
            });
        }
        return card;
    }

    private TextView greenPlus() {
        TextView t = Utils.tv(this, "+", 22, Color.WHITE, Gravity.CENTER);
        int s = Utils.dp(this, 40);
        t.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(Utils.ACCENT);
        t.setBackground(g);
        return t;
    }

    // ================= 社群�?=================

    private void renderCommunities() {
        communitiesLayout.removeAllViews();
        communitiesLayout.setPadding(Utils.dp(this, 18), Utils.dp(this, 22), Utils.dp(this, 18), Utils.dp(this, 20));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = Utils.tv(this, I18n.t("communities"), 28, Utils.TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(title, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView plus = greenPlus();
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateGroupDialog();
            }
        });
        head.addView(plus);
        communitiesLayout.addView(head);

        communitiesLayout.addView(Utils.hSpace(this, 20));

        TextView intro = Utils.tv(this, I18n.t("group_intro"), 14, Utils.TEXT_DIM, Gravity.START);
        communitiesLayout.addView(intro);
        communitiesLayout.addView(Utils.hSpace(this, 14));

        groupListContainer = new LinearLayout(this);
        groupListContainer.setOrientation(LinearLayout.VERTICAL);
        groupListContainer.setBackground(Utils.bg(this, Utils.CARD, 16));
        communitiesLayout.addView(groupListContainer);
        renderGroupList();

    }

    private void renderGroupList() {
        if (groupListContainer == null) return;
        groupListContainer.removeAllViews();
        if (groups.isEmpty()) {
            groupListContainer.setBackground(Utils.bg(this, Utils.CARD, 16));
            groupListContainer.addView(Utils.tv(this, I18n.t("no_groups"), 14, Utils.TEXT_DIM, Gravity.CENTER));
            return;
        }
        for (final Models.Group g : groups) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Utils.dp(this, 14), Utils.dp(this, 10), Utils.dp(this, 14), Utils.dp(this, 10));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openGroup(g);
                }
            });
            row.addView(g.hasAvatar
                    ? AvatarManager.avatarView(this, "g_" + g.id, g.name, true, 46, 16)
                    : Utils.letterAvatar(this, g.name, 46, 16));
            row.addView(Utils.vSpace(this, 12));
            LinearLayout mid = new LinearLayout(this);
            mid.setOrientation(LinearLayout.VERTICAL);
            TextView nm = Utils.tv(this, g.name, 16, Utils.TEXT, Gravity.START);
            nm.setTypeface(Typeface.DEFAULT_BOLD);
            nm.setSingleLine(true);
            nm.setEllipsize(TextUtils.TruncateAt.END);
            mid.addView(nm);
            String sub = g.owner.equals(Session.username) ? String.format(I18n.t("group_owner_count"), g.memberCount) : String.format(I18n.t("people_count"), g.memberCount);
            mid.addView(Utils.tv(this, sub, 12, g.owner.equals(Session.username) ? Utils.ACCENT : Utils.TEXT_DIM, Gravity.START));
            row.addView(mid, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            groupListContainer.addView(row);
            if (groups.indexOf(g) < groups.size() - 1) {
                groupListContainer.addView(Utils.divider(this));
            }
        }
    }

    private void openGroup(final Models.Group g) {
        Intent i = new Intent(this, GroupChatActivity.class);
        i.putExtra("groupId", g.id);
        i.putExtra("groupName", g.name);
        i.putExtra("isOwner", g.owner.equals(Session.username));
        startActivity(i);
    }

    private static final String[][] CHANNELS = {
            {"bilibili", "com.bilibili.app"},
            {"youtube", "com.google.android.youtube"},
            {"douyin", "com.ss.android.ugc.aweme"},
            {"tiktok", "com.zhiliaoapp.musically"},
            {"xiaohongshu", "com.xingin.xhs"},
            {"facebook", "com.facebook.katana"},
            {"weibo", "com.sina.weibo"},
            {"instagram", "com.instagram.android"}
    };

    private String channelName(String key) {
        if ("bilibili".equals(key)) return I18n.t("哔哩哔哩", "嗶哩嗶哩", "Bilibili");
        if ("youtube".equals(key)) return I18n.t("YouTube", "YouTube", "YouTube");
        if ("douyin".equals(key)) return I18n.t("抖音", "抖音", "Douyin");
        if ("tiktok".equals(key)) return I18n.t("TikTok", "TikTok", "TikTok");
        if ("xiaohongshu".equals(key)) return I18n.t("小红书", "小紅書", "Xiaohongshu");
        if ("facebook".equals(key)) return I18n.t("Facebook", "Facebook", "Facebook");
        if ("weibo".equals(key)) return I18n.t("微博", "微博", "Weibo");
        return I18n.t("Instagram", "Instagram", "Instagram");
    }

    private List<String[]> availableChannels() {
        List<String[]> list = new ArrayList<>();
        String lang = I18n.lang();
        String region = Session.region == null ? "" : Session.region;
        if (I18n.ZH_HANS.equals(lang) && "中国".equals(region)) {
            addChannel(list, "bilibili");
            addChannel(list, "douyin");
            addChannel(list, "xiaohongshu");
            addChannel(list, "weibo");
        } else if (I18n.ZH_HANT.equals(lang)
                && (region.contains("香港") || region.contains("澳门") || region.contains("台湾"))) {
            for (String[] c : CHANNELS) list.add(c);
        } else {
            addChannel(list, "youtube");
            addChannel(list, "tiktok");
            addChannel(list, "facebook");
            addChannel(list, "instagram");
        }
        return list;
    }

    private void addChannel(List<String[]> list, String key) {
        for (String[] c : CHANNELS) {
            if (c[0].equals(key)) list.add(c);
        }
    }

    private void showChannelDialog() {
        final List<String[]> list = availableChannels();
        String[] names = new String[list.size()];
        final Ui.Click[] clicks = new Ui.Click[list.size()];
        for (int i = 0; i < list.size(); i++) {
            final String[] c = list.get(i);
            names[i] = channelName(c[0]);
            clicks[i] = new Ui.Click() {
                @Override
                public void onClick() {
                    selectedChannelPkg = c[1];
                    if (channelDropdownText != null) channelDropdownText.setText(channelName(c[0]));
                }
            };
        }
        Ui.list(this, I18n.t("all_channels"), names, clicks);
    }

    private void openChannel() {
        if (selectedChannelPkg == null || selectedChannelPkg.isEmpty()) {
            Ui.popup(this, I18n.t("channel_pick_first"));
            return;
        }
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(selectedChannelPkg);
            if (i == null) {
                Ui.popup(this, I18n.t("cannot_open_channel"));
                return;
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Ui.popup(this, I18n.t("cannot_open_channel"));
        }
    }

    private void showCreateGroupDialog() {
        final EditText input = Ui.input(this, null, I18n.t("input_group_name"));
        Ui.show(this, I18n.t("create_group"), input, I18n.t("create"), new Ui.Click() {
            @Override
            public void onClick() {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) {
                    Utils.toast(HomeActivity.this, I18n.t("enter_group_name"));
                    return;
                }
                if (!Net.get().isConnected()) {
                    Utils.toast(HomeActivity.this, I18n.t("not_connected"));
                    return;
                }
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "create_group");
                    o.put("name", name);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    // ================= 聊天�?=================

    private void initChatsHeader() {
        chatsLayout.setPadding(Utils.dp(this, 14), Utils.dp(this, 18), Utils.dp(this, 14), Utils.dp(this, 10));

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        chatSearch = new EditText(this);
        chatSearch.setHint(I18n.t("search"));
        chatSearch.setSingleLine(true);
        chatSearch.setTextSize(15);
        chatSearch.setTextColor(Utils.TEXT);
        chatSearch.setHintTextColor(Utils.TEXT_DIM);
        chatSearch.setBackground(Utils.bg(this, Utils.CARD, 20));
        chatSearch.setPadding(Utils.dp(this, 14), Utils.dp(this, 10), Utils.dp(this, 14), Utils.dp(this, 10));
        chatSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                renderChats();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        searchRow.addView(chatSearch, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.82f));

        TextView contactsBtn = Utils.tv(this, "👥", 20, Utils.TEXT, Gravity.CENTER);
        contactsBtn.setPadding(0, 0, Utils.dp(this, 14), 0);
        contactsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFriendListDialog();
            }
        });
        searchRow.addView(Utils.vSpace(this, 14));
        searchRow.addView(contactsBtn);
        chatsLayout.addView(searchRow);

        notifPrompt = new LinearLayout(this);
        notifPrompt.setOrientation(LinearLayout.HORIZONTAL);
        notifPrompt.setGravity(Gravity.CENTER_VERTICAL);
        notifPrompt.setBackground(Utils.bg(this, Utils.CARD, 16));
        notifPrompt.setPadding(Utils.dp(this, 14), Utils.dp(this, 12), Utils.dp(this, 12), Utils.dp(this, 12));
        notifPrompt.addView(Utils.tv(this, I18n.t("enable_notifications"), 13, Utils.TEXT, Gravity.START), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button promptBtn = Utils.button(this);
        promptBtn.setText(I18n.t("enable"));
        promptBtn.setTextSize(12);
        promptBtn.setTextColor(Color.WHITE);
        promptBtn.setBackground(Utils.bg(this, Utils.ACCENT, 16));
        promptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 2001);
                } else {
                    notifPrompt.setVisibility(View.GONE);
                }
            }
        });
        notifPrompt.addView(promptBtn);
        LinearLayout.LayoutParams promptLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        promptLp.topMargin = Utils.dp(this, 12);
        if (Notifier.canNotify(this)) notifPrompt.setVisibility(View.GONE);
        chatsLayout.addView(notifPrompt, promptLp);

        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setPadding(0, Utils.dp(this, 12), 0, Utils.dp(this, 6));
        filterAll = Utils.tv(this, I18n.t("all"), 14, Utils.ACCENT, Gravity.CENTER);
        filterAll.setTypeface(Typeface.DEFAULT_BOLD);
        filterAll.setPadding(Utils.dp(this, 16), Utils.dp(this, 6), Utils.dp(this, 16), Utils.dp(this, 6));
        filterUnread = Utils.tv(this, I18n.t("unread"), 14, Utils.TEXT_DIM, Gravity.CENTER);
        filterUnread.setPadding(Utils.dp(this, 16), Utils.dp(this, 6), Utils.dp(this, 16), Utils.dp(this, 6));
        filterAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatFilter = "all";
                updateFilterStyle();
                renderChats();
            }
        });
        filterUnread.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chatFilter = "unread";
                updateFilterStyle();
                renderChats();
            }
        });
        filterRow.addView(filterAll);
        filterRow.addView(Utils.vSpace(this, 8));
        filterRow.addView(filterUnread);
        chatsLayout.addView(filterRow);

        chatListContainer = new LinearLayout(this);
        chatListContainer.setOrientation(LinearLayout.VERTICAL);
        chatListContainer.setBackgroundColor(Utils.BG);
        chatsLayout.addView(chatListContainer);

        TextView hint = Utils.tv(this, I18n.t("contacts_sync_hint"), 11, Utils.TEXT_DIM, Gravity.CENTER);
        hint.setPadding(0, Utils.dp(this, 16), 0, Utils.dp(this, 6));
        chatsLayout.addView(hint);
    }

    private void updateFilterStyle() {
        filterAll.setTextColor("all".equals(chatFilter) ? Utils.ACCENT : Utils.TEXT_DIM);
        filterAll.setTypeface(null, "all".equals(chatFilter) ? Typeface.BOLD : Typeface.NORMAL);
        filterUnread.setTextColor("unread".equals(chatFilter) ? Utils.ACCENT : Utils.TEXT_DIM);
        filterUnread.setTypeface(null, "unread".equals(chatFilter) ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void renderChats() {
        chatListContainer.removeAllViews();

        if (!pending.isEmpty()) {
            LinearLayout banner = new LinearLayout(this);
            banner.setOrientation(LinearLayout.HORIZONTAL);
            banner.setGravity(Gravity.CENTER_VERTICAL);
            banner.setBackground(Utils.bg(this, Utils.CARD, 14));
            banner.setPadding(Utils.dp(this, 14), Utils.dp(this, 12), Utils.dp(this, 14), Utils.dp(this, 12));
            banner.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPendingDialog();
                }
            });
            banner.addView(Utils.tv(this, "👋", 18, Utils.TEXT, Gravity.CENTER));
            banner.addView(Utils.vSpace(this, 10));
            banner.addView(Utils.tv(this, String.format(I18n.t("new_friend_requests_count"), pending.size()), 14, Utils.TEXT, Gravity.START));
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            blp.topMargin = Utils.dp(this, 4);
            blp.bottomMargin = Utils.dp(this, 8);
            chatListContainer.addView(banner, blp);
        }

        if (searchResultBox != null) {
            if (searchResultBox.getParent() != null) {
                ((android.view.ViewGroup) searchResultBox.getParent()).removeView(searchResultBox);
            }
            chatListContainer.addView(searchResultBox);
        }

        String query = chatSearch.getText().toString().trim().toLowerCase();
        List<Models.Conversation> shown = new ArrayList<>();
        for (Models.Conversation c : conversations) {
            if ("unread".equals(chatFilter) && c.unread <= 0) continue;
            if (!query.isEmpty()) {
                String hay = (c.nickname + " " + c.lastText).toLowerCase();
                if (!hay.contains(query)) continue;
            }
            shown.add(c);
        }
                Collections.sort(shown, new Comparator<Models.Conversation>() {
            @Override
            public int compare(Models.Conversation a, Models.Conversation b) {
                if (a.pinned != b.pinned) return a.pinned ? -1 : 1;
                return Long.compare(b.lastTime, a.lastTime);
            }
        });

        if (shown.isEmpty()) {
            TextView empty = Utils.tv(this, I18n.t("no_conversations_hint"), 14, Utils.TEXT_DIM, Gravity.CENTER);
            empty.setPadding(0, Utils.dp(this, 60), 0, 0);
            chatListContainer.addView(empty);
            return;
        }

        for (final Models.Conversation c : shown) {
            chatListContainer.addView(chatRow(c));
            chatListContainer.addView(Utils.divider(this));
        }
    }

    private View chatRow(final Models.Conversation c) {
        int screenW = getResources().getDisplayMetrics().widthPixels - Utils.dp(this, 28);
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setLayoutParams(new HorizontalScrollView.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Utils.dp(this, 14), Utils.dp(this, 10), Utils.dp(this, 14), Utils.dp(this, 10));
        row.setBackgroundColor(Utils.BG);
        row.setLayoutParams(new LinearLayout.LayoutParams(screenW, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChat(c);
            }
        });
        row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showConversationMenu(c);
                return true;
            }
        });
        final View av = AvatarManager.avatarView(this, c.with, c.nickname, c.hasAvatar, 48, 17);
        av.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("group".equals(c.type)) {
                    openGroupDetail(c);
                } else {
                    showFriendCard(c);
                }
            }
        });
        row.addView(av);
        row.addView(Utils.vSpace(this, 12));

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        TextView name = Utils.tv(this, (c.pinned ? "📌 " : "") + c.nickname, 16, Utils.TEXT, Gravity.START);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setSingleLine(true);
        mid.addView(name);
        String lastText = c.lastText;
        if (lastText == null || lastText.isEmpty()) {
            if ("image".equals(c.lastKind)) lastText = I18n.t("media_image");
            else if ("file".equals(c.lastKind)) lastText = I18n.t("media_file");
            else lastText = I18n.t("start_chat");
        }
        TextView last = Utils.tv(this, lastText, 13, Utils.TEXT_DIM, Gravity.START);
        last.setSingleLine(true);
        last.setEllipsize(TextUtils.TruncateAt.END);
        mid.addView(last);
        row.addView(mid, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(Utils.vSpace(this, 8));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END | Gravity.TOP);
        TextView time = Utils.tv(this, c.lastTime > 0 ? Utils.timeText(c.lastTime) : "", 11, Utils.TEXT_DIM, Gravity.END);
        right.addView(time);
        if (c.unread > 0) {
            TextView badge = new TextView(this);
            badge.setText(String.valueOf(c.unread));
            badge.setTextSize(11);
            badge.setTextColor(Color.WHITE);
            badge.setGravity(Gravity.CENTER);
            int b = Utils.dp(this, 20);
            badge.setMinWidth(b);
            badge.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, b));
            badge.setBackground(circle(Utils.ACCENT));
            LinearLayout.LayoutParams blp = (LinearLayout.LayoutParams) badge.getLayoutParams();
            blp.topMargin = Utils.dp(this, 4);
            right.addView(badge);
        }
        row.addView(right);
        inner.addView(row);

        TextView hide = Utils.tv(this, I18n.t("hide"), 14, Color.WHITE, Gravity.CENTER);
        hide.setBackground(Utils.bg(this, Utils.DANGER, 16));
        hide.setLayoutParams(new LinearLayout.LayoutParams(Utils.dp(this, 76), LinearLayout.LayoutParams.MATCH_PARENT));
        hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideConversation(c);
            }
        });
        inner.addView(hide);

        hsv.addView(inner);
        return hsv;
    }

    // ---------- 会话长按管理 ----------

    private void showConversationMenu(final Models.Conversation c) {
        boolean pinned = PinnedPrefs.isPinned(this, c.with);
        final List<String> items = new ArrayList<>();
        items.add(pinned ? I18n.t("unpin") : I18n.t("pin"));
        if ("group".equals(c.type)) {
            items.add(I18n.t("clear_group_history"));
            items.add(I18n.t("leave_group"));
            boolean isOwner = false;
            for (Models.Group g : groups) {
                if (g.id.equals(c.with) && g.owner.equals(Session.username)) isOwner = true;
            }
            if (isOwner) items.add(I18n.t("dissolve_group"));
        } else {
            items.add(I18n.t("delete_friend"));
            items.add(c.blocked ? I18n.t("unblock") : I18n.t("block"));
        }
        Ui.Click[] clicks = new Ui.Click[items.size()];
        for (int i = 0; i < items.size(); i++) {
            final String act = items.get(i);
            clicks[i] = new Ui.Click() {
                @Override
                public void onClick() {
                    if (act.equals(I18n.t("pin")) || act.equals(I18n.t("unpin"))) {
                        boolean np = !PinnedPrefs.isPinned(HomeActivity.this, c.with);
                        PinnedPrefs.setPinned(HomeActivity.this, c.with, np);
                        c.pinned = np;
                        renderChats();
                    } else if (act.equals(I18n.t("delete_friend"))) {
                        confirmUnfriend(c);
                    } else if (act.equals(I18n.t("block")) || act.equals(I18n.t("unblock"))) {
                        sendBlock(c, !c.blocked);
                    } else if (act.equals(I18n.t("clear_group_history"))) {
                        confirmClearGroupHistory(c);
                    } else if (act.equals(I18n.t("leave_group"))) {
                        confirmLeaveGroup(c);
                    } else if (act.equals(I18n.t("dissolve_group"))) {
                        confirmDissolveGroup(c);
                    }
                }
            };
        }
        Ui.list(this, c.nickname, items.toArray(new String[0]), clicks);
    }

    private void confirmUnfriend(final Models.Conversation c) {
        Ui.show(this, I18n.t("delete_friend"), Ui.message(this, String.format(I18n.t("confirm_unfriend_msg"), c.nickname)), I18n.t("confirm"), new Ui.Click() {
            @Override
            public void onClick() {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "unfriend");
                    o.put("with", c.with);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void sendBlock(final Models.Conversation c, final boolean block) {
        try {
            JSONObject o = new JSONObject();
            o.put("type", block ? "block_user" : "unblock_user");
            o.put("with", c.with);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    // ---------- 好友名片 ----------

    private Dialog friendCardDialog;
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
        friendCardBox = new LinearLayout(this);
        friendCardBox.setOrientation(LinearLayout.VERTICAL);
        friendCardBox.setGravity(Gravity.CENTER);
        friendCardBox.setPadding(Utils.dp(this, 24), Utils.dp(this, 34), Utils.dp(this, 24), Utils.dp(this, 34));
        friendCardBox.addView(new TriangleLoadingView(this));
        friendCardDialog = Ui.showSingle(this, I18n.t("friend_card"), friendCardBox, I18n.t("close"), null);
        try {
            JSONObject o = new JSONObject();
            o.put("type", "get_user_profile");
            o.put("username", c.with);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    private void fillFriendCard(JSONObject o) {
        if (friendCardLoaded) return;
        if (!friendCardUsername.equals(o.optString("username"))) return;
        friendCardLoaded = true;
        rebuildFriendCard(o.optJSONObject("profile"));
    }

    private void rebuildFriendCard(JSONObject p) {
        if (friendCardBox == null) {
            friendCardBox = new LinearLayout(this);
            friendCardBox.setOrientation(LinearLayout.VERTICAL);
        }
        friendCardBox.removeAllViews();
        friendCardBox.setPadding(Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8), Utils.dp(this, 6));
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
        Button msg = Utils.button(this);
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
        if (friendCardDialog != null && friendCardDialog.getWindow() != null) {
            friendCardDialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.86f),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
        if (friendCardDialog == null || !friendCardDialog.isShowing()) {
            friendCardDialog = Ui.showSingle(this, I18n.t("friend_card"), friendCardBox, I18n.t("close"), null);
        }
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
        if (value == null || value.isEmpty()) value = "-";
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, Utils.dp(this, 4), 0, Utils.dp(this, 4));
        row.addView(Utils.tv(this, label + ": ", 13, Utils.TEXT, Gravity.START));
        TextView v = Utils.tv(this, value, 13, Utils.TEXT_DIM, Gravity.START);
        v.setPadding(Utils.dp(this, 8), 0, 0, 0);
        row.addView(v, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(row);
    }

    // ================= 自己�?=================

    private void renderMe() {
        meLayout.removeAllViews();
        meLayout.setPadding(Utils.dp(this, 18), Utils.dp(this, 22), Utils.dp(this, 18), Utils.dp(this, 20));

        LinearLayout centerBox = new LinearLayout(this);
        centerBox.setOrientation(LinearLayout.VERTICAL);
        centerBox.setGravity(Gravity.CENTER_HORIZONTAL);

        final TextView bubble = Utils.tv(this, Session.status, 12, Utils.TEXT_DIM, Gravity.CENTER);
        bubble.setBackground(Utils.bg(this, Utils.CARD, 14));
        bubble.setPadding(Utils.dp(this, 14), Utils.dp(this, 8), Utils.dp(this, 14), Utils.dp(this, 8));
        bubble.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showStatusDialog();
            }
        });
        centerBox.addView(bubble);
        centerBox.addView(Utils.hSpace(this, 16));

        final View avatar = AvatarManager.avatarView(this, Session.username, Session.nickname, Session.hasAvatar, 84, 40);
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HomeActivity.this, ProfileActivity.class);
                startActivity(i);
            }
        });
        centerBox.addView(avatar);
        centerBox.addView(Utils.hSpace(this, 12));

        TextView nick = Utils.tv(this, Session.nickname, 24, Utils.TEXT, Gravity.CENTER);
        nick.setTypeface(Typeface.DEFAULT_BOLD);
        centerBox.addView(nick);
        TextView uname = Utils.tv(this, "@" + Session.username, 13, Utils.TEXT_DIM, Gravity.CENTER);
        uname.setPadding(0, Utils.dp(this, 2), 0, 0);
        centerBox.addView(uname);
        meLayout.addView(centerBox);

        meLayout.addView(Utils.hSpace(this, 30));

        LinearLayout group1 = new LinearLayout(this);
        group1.setOrientation(LinearLayout.VERTICAL);
        group1.setBackground(Utils.bg(this, Utils.CARD, 16));
        group1.addView(menuRow(I18n.t("server_settings"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showServerDialog();
            }
        }));
        group1.addView(Utils.divider(this));
        group1.addView(menuRow(I18n.t("changelog"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangelogDialog();
            }
        }));
        meLayout.addView(group1);

        meLayout.addView(Utils.hSpace(this, 16));

        LinearLayout favCard = new LinearLayout(this);
        favCard.setOrientation(LinearLayout.VERTICAL);
        favCard.setBackground(Utils.bg(this, Utils.CARD, 16));
        favCard.addView(menuRow(I18n.t("my_favorites"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, FavoritesActivity.class));
            }
        }));
        meLayout.addView(favCard);
        meLayout.addView(Utils.hSpace(this, 16));

        LinearLayout utilCard = new LinearLayout(this);
        utilCard.setOrientation(LinearLayout.VERTICAL);
        utilCard.setBackground(Utils.bg(this, Utils.CARD, 16));
        utilCard.addView(menuRow(I18n.t("font_size"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFontDialog();
            }
        }));
        utilCard.addView(Utils.divider(this));
        utilCard.addView(menuRow(I18n.t("dnd"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDndDialog();
            }
        }));
        meLayout.addView(utilCard);

        meLayout.addView(Utils.hSpace(this, 16));

        // 语言与地�?(位于更新日志下方)
        LinearLayout langGroup = new LinearLayout(this);
        langGroup.setOrientation(LinearLayout.VERTICAL);
        langGroup.setBackground(Utils.bg(this, Utils.CARD, 16));
        langGroup.addView(menuRow(I18n.t("language_region") + " · " + langLabel(), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLanguageRegionDialog();
            }
        }));
        langGroup.addView(Utils.divider(this));
        LinearLayout regionRow = new LinearLayout(this);
        regionRow.setOrientation(LinearLayout.HORIZONTAL);
        regionRow.setGravity(Gravity.CENTER_VERTICAL);
        regionRow.setPadding(Utils.dp(this, 16), Utils.dp(this, 14), Utils.dp(this, 16), Utils.dp(this, 14));
        regionRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegionDialog();
            }
        });
        regionRow.addView(Utils.tv(this, I18n.t("region"), 15, Utils.TEXT, Gravity.START), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        regionRow.addView(Utils.tv(this, Session.region == null || Session.region.isEmpty() ? "中国" : Session.region, 13, Utils.TEXT_DIM, Gravity.END));
        regionRow.addView(Utils.vSpace(this, 6));
        langGroup.addView(regionRow);
        meLayout.addView(langGroup);

        meLayout.addView(Utils.hSpace(this, 16));

        // 外观: 白天/黑夜切换 + 跟随系统 (位于语言与地区下�?
        LinearLayout themeGroup = new LinearLayout(this);
        themeGroup.setOrientation(LinearLayout.VERTICAL);
        themeGroup.setBackground(Utils.bg(this, Utils.CARD, 16));
        final String themeLabel = Theme.isDark(this) ? I18n.t("theme_switch_day") : I18n.t("theme_switch_night");
        themeGroup.addView(menuRow(themeLabel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTheme();
            }
        }));
        themeGroup.addView(Utils.divider(this));
        LinearLayout followRow = new LinearLayout(this);
        followRow.setOrientation(LinearLayout.HORIZONTAL);
        followRow.setGravity(Gravity.CENTER_VERTICAL);
        followRow.setPadding(Utils.dp(this, 16), Utils.dp(this, 14), Utils.dp(this, 12), Utils.dp(this, 14));
        followRow.addView(Utils.tv(this, I18n.t("follow_system"), 15, Utils.TEXT, Gravity.START), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        android.widget.Switch sysSwitch = new android.widget.Switch(this);
        sysSwitch.setChecked(Theme.followSystem);
        sysSwitch.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                Theme.setFollow(isChecked);
                Utils.vibrate(HomeActivity.this);
                if (Theme.apply(HomeActivity.this)) reapplyTheme();
            }
        });
        followRow.addView(sysSwitch);
        themeGroup.addView(followRow);
        themeGroup.addView(Utils.divider(this));
        LinearLayout vibrateRow = new LinearLayout(this);
        vibrateRow.setOrientation(LinearLayout.HORIZONTAL);
        vibrateRow.setGravity(Gravity.CENTER_VERTICAL);
        vibrateRow.setPadding(Utils.dp(this, 16), Utils.dp(this, 14), Utils.dp(this, 12), Utils.dp(this, 14));
        vibrateRow.addView(Utils.tv(this, I18n.t("vibrate_feedback"), 15, Utils.TEXT, Gravity.START), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        android.widget.Switch vibrateSwitch = new android.widget.Switch(this);
        vibrateSwitch.setChecked(Theme.vibrate);
        vibrateSwitch.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                Theme.setVibrate(isChecked);
                Utils.vibrate(HomeActivity.this);
            }
        });
        vibrateRow.addView(vibrateSwitch);
        themeGroup.addView(vibrateRow);
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
        group2.addView(menuRow(I18n.t("about") + " (App v" + UpdateChecker.currentVersion(this) + ")", new View.OnClickListener() {
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

    private String langLabel() {
        String l = I18n.lang();
        if (I18n.ZH_HANT.equals(l)) return I18n.t("zh_hant");
        if (I18n.EN.equals(l)) return I18n.t("english");
        return I18n.t("zh_hans");
    }

    private void showFontDialog() {
        Ui.list(this, I18n.t("font_size"), new String[]{
                        I18n.t("font_small"), I18n.t("font_normal"), I18n.t("font_large"), I18n.t("font_xlarge")
                },
                new Ui.Click[]{
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                setFont(0.85f);
                            }
                        },
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                setFont(1.0f);
                            }
                        },
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                setFont(1.15f);
                            }
                        },
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                setFont(1.3f);
                            }
                        }
                });
    }

    private void setFont(float s) {
        getSharedPreferences("chatapp_font", MODE_PRIVATE).edit().putFloat("scale", s).apply();
        Utils.fontScale = s;
        reapplyTheme();
    }

    private void showDndDialog() {
        final SharedPreferences p = getSharedPreferences("chatapp_dnd", MODE_PRIVATE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final android.widget.Switch sw = new android.widget.Switch(this);
        sw.setText(I18n.t("dnd"));
        sw.setChecked(p.getBoolean("enabled", false));
        box.addView(sw);
        box.addView(Utils.hSpace(this, 12));
        final android.widget.NumberPicker start = new android.widget.NumberPicker(this);
        start.setMinValue(0);
        start.setMaxValue(23);
        start.setValue(p.getInt("start", 0));
        start.setWrapSelectorWheel(false);
        stylePicker(start);
        final android.widget.NumberPicker end = new android.widget.NumberPicker(this);
        end.setMinValue(0);
        end.setMaxValue(23);
        end.setValue(p.getInt("end", 0));
        end.setWrapSelectorWheel(false);
        stylePicker(end);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(Utils.tv(this, I18n.t("dnd_start"), 14, Utils.TEXT, Gravity.CENTER), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(start, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(Utils.tv(this, I18n.t("dnd_end"), 14, Utils.TEXT, Gravity.CENTER), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(end, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(row);
        Ui.show(this, I18n.t("dnd"), box, I18n.t("confirm"), new Ui.Click() {
            @Override
            public void onClick() {
                p.edit().putBoolean("enabled", sw.isChecked())
                        .putInt("start", start.getValue())
                        .putInt("end", end.getValue()).apply();
                Utils.toast(HomeActivity.this, I18n.t("saved"));
            }
        });
    }

    private void stylePicker(android.widget.NumberPicker p) {
        final boolean dark = Theme.isDark(this);
        final int wheelColor = dark ? Color.WHITE : Color.BLACK;
        final int dividerColor = dark ? Utils.DIVIDER : Color.BLACK;
        try {
            p.setTextColor(wheelColor);
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Field fd = android.widget.NumberPicker.class.getDeclaredField("mSelectionDivider");
            fd.setAccessible(true);
            android.graphics.drawable.Drawable d = (android.graphics.drawable.Drawable) fd.get(p);
            if (d != null) d.setTint(dividerColor);
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Field f = android.widget.NumberPicker.class.getDeclaredField("mIncrementButton");
            f.setAccessible(true);
            android.widget.ImageButton ib = (android.widget.ImageButton) f.get(p);
            if (ib != null && ib.getDrawable() != null) ib.getDrawable().setTint(wheelColor);
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Field f2 = android.widget.NumberPicker.class.getDeclaredField("mDecrementButton");
            f2.setAccessible(true);
            android.widget.ImageButton ib2 = (android.widget.ImageButton) f2.get(p);
            if (ib2 != null && ib2.getDrawable() != null) ib2.getDrawable().setTint(wheelColor);
        } catch (Exception ignored) {
        }
        p.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < p.getChildCount(); i++) {
                    android.view.View v = p.getChildAt(i);
                    if (v instanceof android.widget.EditText) {
                        ((android.widget.EditText) v).setTextColor(wheelColor);
                    } else if (v instanceof android.widget.ImageButton) {
                        android.widget.ImageButton ib3 = (android.widget.ImageButton) v;
                        if (ib3.getDrawable() != null) ib3.getDrawable().setTint(wheelColor);
                    }
                }
            }
        });
    }
    private void showLanguageRegionDialog() {
        Ui.list(this, I18n.t("language"), new String[]{
                        I18n.t("zh_hans"), I18n.t("zh_hant"), I18n.t("english")
                },
                new Ui.Click[]{
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                Session.updateLang(I18n.ZH_HANS);
                                reapplyTheme();
                            }
                        },
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                Session.updateLang(I18n.ZH_HANT);
                                reapplyTheme();
                            }
                        },
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                Session.updateLang(I18n.EN);
                                reapplyTheme();
                            }
                        }
                });
    }

    private void showRegionDialog() {
        final List<String> allRegions = RegionList.all();
        final LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        final List<TextView> names = new ArrayList<>();
        for (int i = 0; i < allRegions.size(); i++) {
            final String r = allRegions.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Utils.dp(this, 8), Utils.dp(this, 12), Utils.dp(this, 8), Utils.dp(this, 12));
            final TextView name = Utils.tv(this, r, 15, Utils.TEXT, Gravity.START);
            boolean sel = r.equals(Session.region);
            name.setTextColor(sel ? Utils.ACCENT : Utils.TEXT);
            row.addView(name, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            names.add(name);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Session.updateRegion(r);
                    for (int j = 0; j < names.size(); j++) {
                        boolean isSel = allRegions.get(j).equals(Session.region);
                        names.get(j).setTextColor(isSel ? Utils.ACCENT : Utils.TEXT);
                    }
                }
            });
            list.addView(row);
            list.addView(Utils.divider(this));
        }
        ScrollView sc = new ScrollView(this);
        sc.addView(list);
        sc.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Utils.dp(this, 420)));
        Ui.showSingle(this, I18n.t("region"), sc, I18n.t("close"), null);
    }

    private View menuRow(String label, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Utils.dp(this, 16), Utils.dp(this, 14), Utils.dp(this, 16), Utils.dp(this, 14));
        row.setOnClickListener(onClick);
        row.addView(Utils.tv(this, label, 15, Utils.TEXT, Gravity.START), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    // ================= 弹窗交互 =================

    private void toggleTheme() {
        boolean curDark = Theme.isDark(this);
        Theme.setFollow(false);
        Theme.setDark(!curDark);
        Theme.apply(this);
        reapplyTheme();
    }

    private void reapplyTheme() {
        Theme.apply(this);
        getWindow().getDecorView().setBackgroundColor(Utils.BG);
        if (root != null) root.setBackgroundColor(Utils.BG);
        if (content != null) content.setBackgroundColor(Utils.BG);
        if (tabsBar != null) tabsBar.setBackground(Utils.ring(this, Utils.CARD, Utils.CARD2, 1, 24));
        if (fab != null) {
            GradientDrawable fabBg = new GradientDrawable();
            fabBg.setShape(GradientDrawable.OVAL);
            fabBg.setColor(Utils.ACCENT);
            fab.setBackground(fabBg);
        }
        View[] scrolls = {updatesScroll, callsScroll, communitiesScroll, chatsScroll, meScroll};
        for (View s : scrolls) {
            if (s != null) s.setBackgroundColor(Utils.BG);
        }
        LinearLayout[] lays = {updatesLayout, callsLayout, communitiesLayout, chatsLayout, meLayout};
        for (LinearLayout l : lays) {
            if (l != null) l.setBackgroundColor(Utils.BG);
        }
        if (chatListContainer != null) chatListContainer.setBackgroundColor(Utils.BG);
        tabUpdates.setText(I18n.t("updates"));
        tabCalls.setText(I18n.t("calls"));
        tabCommunities.setText(I18n.t("communities"));
        tabChats.setText(I18n.t("chats"));
        tabMe.setText(I18n.t("me"));
        // 聊天页头部重建以刷新颜色
        if (chatsLayout != null) {
            chatsLayout.removeAllViews();
            initChatsHeader();
        }
        renderUpdates();
        renderActivities();
        renderCommunities();
        renderChats();
        renderMe();
        selectTab(pendingTab);
        updateStatusUi();
    }

    private void refreshPostPreview() {
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

    private void showScanResult(final Bitmap bmp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout box = new LinearLayout(HomeActivity.this);
                box.setOrientation(LinearLayout.VERTICAL);
                box.setGravity(Gravity.CENTER_HORIZONTAL);
                box.setPadding(Utils.dp(HomeActivity.this, 8), Utils.dp(HomeActivity.this, 6), Utils.dp(HomeActivity.this, 8), Utils.dp(HomeActivity.this, 6));
                ImageView qr = new ImageView(HomeActivity.this);
                int s = Utils.dp(HomeActivity.this, 180);
                qr.setLayoutParams(new LinearLayout.LayoutParams(s, s));
                qr.setImageBitmap(bmp);
                box.addView(qr);
                box.addView(Utils.tv(HomeActivity.this, I18n.t("scan_manual_hint"), 12, Utils.TEXT_DIM, Gravity.CENTER));
                box.addView(Utils.hSpace(HomeActivity.this, 8));
                final EditText input = Ui.input(HomeActivity.this, null, "username");
                box.addView(input);
                Button add = Utils.button(HomeActivity.this);
                add.setText(I18n.t("add_friend"));
                add.setTextSize(14);
                add.setTextColor(Color.WHITE);
                add.setBackground(Utils.bg(HomeActivity.this, Utils.ACCENT, 22));
                add.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) return;
                        try {
                            JSONObject o = new JSONObject();
                            o.put("type", "add_friend");
                            o.put("username", name);
                            Net.get().send(o);
                        } catch (Exception ignored) {
                        }
                    }
                });
                box.addView(add);
                Ui.showSingle(HomeActivity.this, I18n.t("qr_scan"), box, I18n.t("close"), null);
            }
        });
    }

    private void showPostDialog() {
        pendingPostImages.clear();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final EditText input = Ui.input(this, null, I18n.t("moment_hint"));
        input.setSingleLine(false);
        input.setMinLines(3);
        box.addView(input);
        box.addView(Utils.hSpace(this, 10));
        postPreviewGrid = new GridLayout(this);
        postPreviewGrid.setColumnCount(3);
        box.addView(postPreviewGrid);
        Button addImg = Utils.button(this);
        addImg.setText(I18n.t("moment_add_photo") + " (0/9)");
        addImg.setTextSize(12);
        addImg.setTextColor(Utils.ACCENT);
        addImg.setBackground(Utils.bg(this, Utils.CARD2, 14));
        addImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pendingPostImages.size() >= 9) {
                    Ui.popup(HomeActivity.this, "Max 9");
                    return;
                }
                pickPostImage();
            }
        });
        box.addView(addImg);
        refreshPostPreview();
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
        MediaPick.choose(this, PICK_POST_IMAGE, new MediaPick.Callback() {
            @Override
            public void onBitmap(Bitmap bmp) {
                addPostBitmap(bmp);
            }
        });
    }

    private void addPostBitmap(final Bitmap src) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap bmp = src;
                    int w = bmp.getWidth(), h = bmp.getHeight();
                    int side = Math.min(w, h);
                    if (side > 0 && (w != side || h != side)) {
                        Bitmap cropped = Bitmap.createBitmap(bmp, (w - side) / 2, (h - side) / 2, side, side);
                        if (cropped != bmp && !bmp.isRecycled()) bmp.recycle();
                        bmp = cropped;
                    }
                    int target = 512;
                    if (bmp.getWidth() > target) {
                        Bitmap scaled = Bitmap.createScaledBitmap(bmp, target, target, true);
                        if (scaled != bmp && !bmp.isRecycled()) bmp.recycle();
                        bmp = scaled;
                    }
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                    final String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
                    if (!bmp.isRecycled()) bmp.recycle();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pendingPostImages.add(b64);
                            Ui.popup(HomeActivity.this, "Max 9");
                            refreshPostPreview();
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        });
        t.setDaemon(true);
        t.start();
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
        // 操作�? 点赞 / 收藏 / 评论 / 转发
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
                    Ui.popup(HomeActivity.this, "Max 9");
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
                cl.addView(Utils.tv(this, c.nickname + ": " + c.text, 12, Utils.TEXT_DIM, Gravity.START));
            }
            card.addView(cl);
        }

        // 自己的动�? 左滑删除
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
        Button b = Utils.button(this);
        b.setText(text);
        b.setTextSize(11);
        b.setTextColor(color);
        b.setBackground(Utils.bg(this, Utils.CARD2, 12));
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
                if (n.length() < 8 || n.length() > 16 || !n.matches(".*[A-Za-z].*") || !n.matches(".*[0-9].*")) {
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
                startActivity(new Intent(HomeActivity.this, AccountDeleteActivity.class));
            }
        });
    }

    private void showChangelogDialog() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        addVersion(list, "v1.0.2", new String[]{"修复新的好友弹窗界面与交互问题"});
        addVersion(list, "v1.0.1", new String[]{"跳一跳：修复向左跳跃动作与向右不一致的问题"});
        addVersion(list, "V1.0.0", new String[]{"Chatapp 上线", "欢迎使用 Chatapp"});

        ScrollView sc = new ScrollView(this);
        sc.addView(list);
        sc.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Utils.dp(this, 430)));
        Ui.showSingle(this, I18n.t("changelog"), sc, I18n.t("close"), null);
    }

    private void addVersion(LinearLayout parent, String ver, String[] items) {
        TextView h = Utils.tv(this, ver, 15, Utils.ACCENT, Gravity.START);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        h.setPadding(0, Utils.dp(this, 8), 0, Utils.dp(this, 4));
        parent.addView(h);
        for (String item : items) {
            TextView t = Utils.tv(this, "·" + item, 13, Utils.TEXT_DIM, Gravity.START);
            t.setPadding(Utils.dp(this, 6), 0, 0, Utils.dp(this, 2));
            parent.addView(t);
        }
    }

    private void showFabMenu() {
        Ui.list(this, I18n.t("add_friend"), new String[]{I18n.t("add_friend"), I18n.t("friend_requests") + " (" + pending.size() + ")", I18n.t("contacts")},
                new Ui.Click[]{
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                showSearchDialog();
                            }
                        },
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                showPendingDialog();
                            }
                        },
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                showFriendListDialog();
                            }
                        }
                });
    }

    private void showStatusDialog() {
        final EditText input = Ui.input(this, Session.status, null);
        Ui.show(this, I18n.t("edit_status_title"), input, I18n.t("confirm"), new Ui.Click() {
            @Override
            public void onClick() {
                final String st = input.getText().toString().trim();
                if (st.isEmpty()) {
                    Utils.toast(HomeActivity.this, I18n.t("status_empty"));
                    return;
                }
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "set_status");
                    o.put("status", st);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void showNicknameDialog() {
        final EditText input = Ui.input(this, Session.nickname, null);
        Ui.show(this, I18n.t("edit_nickname"), input, I18n.t("save"), new Ui.Click() {
            @Override
            public void onClick() {
                final String nick = input.getText().toString().trim();
                if (nick.isEmpty()) {
                    Utils.toast(HomeActivity.this, I18n.t("nickname_empty"));
                    return;
                }
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "set_nickname");
                    o.put("nickname", nick);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void showServerDialog() {
        final EditText input = Ui.input(this, Session.server, null);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(Ui.message(this, I18n.t("current_server") + Session.server));
        box.addView(Utils.hSpace(this, 10));
        box.addView(input);
        Ui.show(this, I18n.t("server_settings"), box, I18n.t("save"), new Ui.Click() {
            @Override
            public void onClick() {
                String srv = input.getText().toString().trim();
                if (srv.isEmpty()) return;
                Session.save(Session.username, Session.nickname, srv);
                Utils.toast(HomeActivity.this, I18n.t("saved_reconnecting"));
                String[] sp = Session.serverParts();
                Net.get().stop();
                Net.get().start(sp[0], Integer.parseInt(sp[1]));
            }
        });
    }

    private void showAboutDialog() {
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(Ui.message(this, "Chatapp v" + UpdateChecker.currentVersion(this) + "\n"
                + I18n.t("current_server") + " " + Session.server + "\n"
                + I18n.t("status_label") + " " + (Net.get().isConnected() ? I18n.t("connected") : I18n.t("disconnected"))));
        final Button up = Utils.button(this);
        up.setText(I18n.t("checking_update"));
        up.setTextSize(14);
        up.setTextColor(Color.WHITE);
        up.setBackground(Utils.bg(this, Utils.ACCENT, 22));
        up.setEnabled(false);
        box.addView(Utils.hSpace(this, 10));
        box.addView(up);
        Ui.showSingle(this, I18n.t("about"), box, I18n.t("close"), null);
        UpdateChecker.checkAsync(this, new UpdateChecker.Result() {
            @Override
            public void onResult(boolean hasUpdate, String version, String file) {
                if (hasUpdate) {
                    up.setText(I18n.t("upgrade") + " (v" + version + ")");
                    up.setEnabled(true);
                    up.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            UpdateChecker.downloadLatest(HomeActivity.this);
                        }
                    });
                } else {
                    up.setText(I18n.t("current_latest"));
                    up.setTextColor(Utils.TEXT_DIM);
                    up.setBackground(Utils.bg(HomeActivity.this, Utils.CARD2, 22));
                }
            }
        });
    }

    private void confirmLogout() {
        Ui.show(this, I18n.t("logout"), Ui.message(this, I18n.t("confirm_logout_msg")), I18n.t("confirm"), new Ui.Click() {
            @Override
            public void onClick() {
                logoutToMain();
            }
        });
    }

    private void showPendingDialog() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(Utils.dp(this, 8), 0, Utils.dp(this, 8), 0);
        if (pending.isEmpty()) {
            list.addView(Utils.tv(this, I18n.t("no_pending"), 14, Utils.TEXT_DIM, Gravity.CENTER));
        } else {
            for (final Models.Contact p : pending) {
                list.addView(pendingRow(p, list));
            }
        }
        ScrollView sc = new ScrollView(this);
        sc.addView(list);
        sc.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Utils.dp(this, 360)));
        Ui.showSingle(this, I18n.t("new_friends"), sc, I18n.t("close"), null);
    }

    private void showFriendListDialog() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(Utils.dp(this, 8), 0, Utils.dp(this, 8), 0);
        if (contacts.isEmpty()) {
            list.addView(Utils.tv(this, I18n.t("no_friends"), 14, Utils.TEXT_DIM, Gravity.CENTER));
        } else {
            for (final Models.Contact c : contacts) {
                list.addView(contactRow(c));
            }
        }
        ScrollView sc = new ScrollView(this);
        sc.addView(list);
        sc.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Utils.dp(this, 360)));
        Ui.showSingle(this, I18n.t("contacts"), sc, I18n.t("close"), null);
    }

    private void showSearchDialog() {
        final EditText input = Ui.input(this, null, I18n.t("search_username_hint"));
        Ui.show(this, I18n.t("add_friend"), input, I18n.t("search"), new Ui.Click() {
            @Override
            public void onClick() {
                String name = input.getText().toString().trim();
                if (name.isEmpty()) return;
                if (!Net.get().isConnected()) {
                    setSearchBox(I18n.t("not_connected_check"), Utils.DANGER);
                    Utils.toast(HomeActivity.this, I18n.t("not_connected"));
                    return;
                }
                showSearching();
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "search_user");
                    o.put("username", name);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void pickAvatar() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        try {
            startActivityForResult(Intent.createChooser(i, I18n.t("choose_avatar")), PICK_AVATAR);
        } catch (Exception e) {
            Utils.toast(this, I18n.t("cannot_open_picker"));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2001) {
            if (notifPrompt != null) notifPrompt.setVisibility(View.GONE);
        }
        MediaPick.onPermissionResult(this, requestCode, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (MediaPick.handleResult(this, requestCode, resultCode, data)) return;
        if (requestCode == PICK_AVATAR && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uploadAvatar(data.getData());
        }
    }

    private void encodePostImage(final android.net.Uri uri) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream in = getContentResolver().openInputStream(uri);
                    Bitmap bmp = BitmapFactory.decodeStream(in);
                    if (in != null) in.close();
                    if (bmp == null) return;
                    int w = bmp.getWidth(), h = bmp.getHeight();
                    int side = Math.min(w, h);
                    if (side > 0 && (w != side || h != side)) {
                        Bitmap c = Bitmap.createBitmap(bmp, (w - side) / 2, (h - side) / 2, side, side);
                        if (c != bmp) bmp.recycle();
                        bmp = c;
                    }
                    int target = 512;
                    if (bmp.getWidth() > target) {
                        Bitmap s = Bitmap.createScaledBitmap(bmp, target, target, true);
                        if (s != bmp) bmp.recycle();
                        bmp = s;
                    }
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                    final String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
                    bmp.recycle();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pendingPostImages.add(b64);
                            Ui.popup(HomeActivity.this, "Max 9");
                            refreshPostPreview();
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void uploadAvatar(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (is != null) is.close();
            if (bmp == null) {
                Utils.toast(this, I18n.t("cannot_read_image"));
                return;
            }
            int w = bmp.getWidth();
            int h = bmp.getHeight();
            int side = Math.min(w, h);
            if (side > 0 && (w != side || h != side)) {
                Bitmap cropped = Bitmap.createBitmap(bmp, (w - side) / 2, (h - side) / 2, side, side);
                if (cropped != bmp) bmp.recycle();
                bmp = cropped;
            }
            int target = 256;
            if (bmp.getWidth() > target) {
                Bitmap scaled = Bitmap.createScaledBitmap(bmp, target, target, true);
                if (scaled != bmp) bmp.recycle();
                bmp = scaled;
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
            bmp.recycle();
            JSONObject o = new JSONObject();
            o.put("type", "set_avatar");
            o.put("data", b64);
            Net.get().send(o);
            Utils.toast(this, I18n.t("avatar_uploading"));
        } catch (Exception e) {
            Utils.toast(this, I18n.t("avatar_upload_failed"));
        }
    }

    private View pendingRow(final Models.Contact p, final LinearLayout list) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Utils.dp(this, 8), Utils.dp(this, 8), Utils.dp(this, 8), Utils.dp(this, 8));
        row.addView(Utils.letterAvatar(this, p.nickname, 40, 15));
        row.addView(Utils.vSpace(this, 10));
        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        TextView name = Utils.tv(this, p.nickname, 15, Utils.TEXT, Gravity.START);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        mid.addView(name);
        TextView sub = Utils.tv(this, "@" + p.username + " " + I18n.t("request_add_friend"), 12, Utils.TEXT_DIM, Gravity.START);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        mid.addView(sub);
        row.addView(mid, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(Utils.vSpace(this, 8));
        Button accept = smallBtn(I18n.t("accept"), Utils.ACCENT);
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                respond(p.username, true);
                removePendingRow(list, row, p);
            }
        });
        row.addView(accept);
        row.addView(Utils.vSpace(this, 6));
        Button deny = smallBtn(I18n.t("reject"), Utils.DANGER);
        deny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                respond(p.username, false);
                removePendingRow(list, row, p);
            }
        });
        row.addView(deny);
        return row;
    }

    private void removePendingRow(LinearLayout list, View row, Models.Contact p) {
        list.removeView(row);
        for (int i = 0; i < pending.size(); i++) {
            if (pending.get(i).username.equals(p.username)) {
                pending.remove(i);
                break;
            }
        }
        updateChatTabBadge();
        if (pending.isEmpty()) {
            list.removeAllViews();
            list.addView(Utils.tv(this, I18n.t("no_pending"), 14, Utils.TEXT_DIM, Gravity.CENTER));
        }
        if (chatsScroll.getVisibility() == View.VISIBLE) renderChats();
    }

    private View contactRow(final Models.Contact c) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Utils.dp(this, 8), Utils.dp(this, 8), Utils.dp(this, 8), Utils.dp(this, 8));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openChat(c.username, c.nickname, c.hasAvatar);
            }
        });
        row.addView(AvatarManager.avatarView(this, c.username, c.nickname, c.hasAvatar, 42, 16));
        row.addView(Utils.vSpace(this, 10));
        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.addView(Utils.tv(this, c.nickname, 15, Utils.TEXT, Gravity.START));
        String sub = c.online ? I18n.t("online") : I18n.t("offline");
        if (c.status != null && !c.status.isEmpty()) sub = c.status;
        mid.addView(Utils.tv(this, sub, 12, c.online ? Utils.ACCENT : Utils.TEXT_DIM, Gravity.START));
        row.addView(mid, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(Utils.tv(this, "@" + c.username, 12, Utils.TEXT_DIM, Gravity.END));
        return row;
    }

    private Button smallBtn(String text, int color) {
        Button b = Utils.button(this);
        b.setText(text);
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setGravity(Gravity.CENTER);
        b.setPadding(Utils.dp(this, 14), 0, Utils.dp(this, 14), 0);
        b.setBackground(Utils.bg(this, color, 6));
        b.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, Utils.dp(this, 34)));
        return b;
    }

    private void openChat(final Models.Conversation c) {
        if ("group".equals(c.type)) {
            String owner = Session.username;
            for (Models.Group g : groups) {
                if (g.id.equals(c.with)) {
                    owner = g.owner;
                    break;
                }
            }
            Intent i = new Intent(this, GroupChatActivity.class);
            i.putExtra("groupId", c.with);
            i.putExtra("groupName", c.nickname);
            i.putExtra("isOwner", owner.equals(Session.username));
            startActivity(i);
        } else {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("peer", c.with);
            i.putExtra("nickname", c.nickname);
            i.putExtra("hasAvatar", c.hasAvatar);
            startActivity(i);
        }
    }

    private void openGroupDetail(final Models.Conversation c) {
        Intent i = new Intent(this, GroupDetailActivity.class);
        i.putExtra("groupId", c.with);
        i.putExtra("groupName", c.nickname);
        startActivity(i);
    }

    private void hideConversation(final Models.Conversation c) {
        try {
            JSONObject o = new JSONObject();
            o.put("type", "hide_conversation");
            o.put("with", c.with);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
        for (int i = conversations.size() - 1; i >= 0; i--) {
            if (conversations.get(i).with.equals(c.with)) {
                conversations.remove(i);
                break;
            }
        }
        renderChats();
        updateChatTabBadge();
    }

    private void confirmClearGroupHistory(final Models.Conversation c) {
        Ui.show(this, I18n.t("clear_group_history"), Ui.message(this, I18n.t("clear_group_history_confirm")), I18n.t("confirm"), new Ui.Click() {
            @Override
            public void onClick() {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "clear_group_history");
                    o.put("id", c.with);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void confirmLeaveGroup(final Models.Conversation c) {
        Ui.show(this, I18n.t("leave_group"), Ui.message(this, I18n.t("leave_confirm")), I18n.t("quit"), new Ui.Click() {
            @Override
            public void onClick() {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "leave_group");
                    o.put("id", c.with);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void confirmDissolveGroup(final Models.Conversation c) {
        Ui.show(this, I18n.t("dissolve_group"), Ui.message(this, I18n.t("dissolve_confirm")), I18n.t("dissolve"), new Ui.Click() {
            @Override
            public void onClick() {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "dissolve_group");
                    o.put("id", c.with);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void openChat(String username, String nickname, boolean hasAvatar) {
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra("peer", username);
        i.putExtra("nickname", nickname);
        i.putExtra("hasAvatar", hasAvatar);
        startActivity(i);
    }

    private void refreshAll() {
        if (!Net.get().isConnected()) return;
        try {
            JSONObject c = new JSONObject();
            c.put("type", "get_contacts");
            Net.get().send(c);
            JSONObject v = new JSONObject();
            v.put("type", "get_conversations");
            Net.get().send(v);
            JSONObject g = new JSONObject();
            g.put("type", "get_groups");
            Net.get().send(g);
            JSONObject p = new JSONObject();
            p.put("type", "get_posts");
            Net.get().send(p);
            JSONObject a = new JSONObject();
            a.put("type", "get_activities");
            Net.get().send(a);
        } catch (Exception ignored) {
        }
    }

    private void respond(String username, boolean accept) {
        try {
            JSONObject o = new JSONObject();
            o.put("type", "respond_request");
            o.put("from", username);
            o.put("accept", accept);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    // ================= 搜索结果显示 =================

    private void setSearchBox(String text, int color) {
        if (searchResultBox == null) searchResultBox = new LinearLayout(this);
        searchResultBox.removeAllViews();
        searchResultBox.setOrientation(LinearLayout.VERTICAL);
        searchResultBox.setPadding(Utils.dp(this, 14), Utils.dp(this, 10), Utils.dp(this, 14), Utils.dp(this, 10));
        TextView t = Utils.tv(this, text, 14, color, Gravity.CENTER);
        searchResultBox.addView(t);
        renderChats();
    }

    private void showSearching() {
        searchState = "waiting";
        setSearchBox(I18n.t("searching"), Utils.TEXT_DIM);
        refreshHandler.removeCallbacks(searchTimeout);
        refreshHandler.postDelayed(searchTimeout, 10000);
    }

    private void renderSearchResult(JSONObject o) {
        searchState = "idle";
        refreshHandler.removeCallbacks(searchTimeout);
        String status = o.optString("status");
        final String username = o.optString("username");
        final String nickname = o.optString("nickname");
        if ("not_found".equals(status)) {
            setSearchBox(String.format(I18n.t("user_not_found"), username), Utils.DANGER);
            return;
        }
        if ("self".equals(status)) {
            setSearchBox(String.format(I18n.t("this_is_you"), username), Utils.TEXT_DIM);
            return;
        }
        if (searchResultBox == null) searchResultBox = new LinearLayout(this);
        searchResultBox.removeAllViews();
        searchResultBox.setOrientation(LinearLayout.HORIZONTAL);
        searchResultBox.setGravity(Gravity.CENTER_VERTICAL);
        searchResultBox.setPadding(Utils.dp(this, 14), Utils.dp(this, 8), Utils.dp(this, 14), Utils.dp(this, 8));
        searchResultBox.setBackground(Utils.bg(this, Utils.CARD, 12));
        searchResultBox.addView(Utils.letterAvatar(this, nickname, 40, 15));
        searchResultBox.addView(Utils.vSpace(this, 10));

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.addView(Utils.tv(this, nickname, 15, Utils.TEXT, Gravity.START));
        mid.addView(Utils.tv(this, "@" + username, 12, Utils.TEXT_DIM, Gravity.START));
        searchResultBox.addView(mid, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        searchResultBox.addView(Utils.vSpace(this, 8));

        if ("friend".equals(status)) {
            Button btn = smallBtn(I18n.t("send_message"), Utils.ACCENT);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openChat(username, nickname, false);
                }
            });
            searchResultBox.addView(btn);
        } else if ("pending_out".equals(status)) {
            searchResultBox.addView(Utils.tv(this, I18n.t("request_sent"), 13, Utils.TEXT_DIM, Gravity.CENTER));
        } else if ("pending_in".equals(status)) {
            Button btn = smallBtn(I18n.t("accept"), Utils.ACCENT);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    respond(username, true);
                }
            });
            searchResultBox.addView(btn);
        } else {
            Button btn = smallBtn(I18n.t("add_friend"), Utils.ACCENT);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        JSONObject o = new JSONObject();
                        o.put("type", "add_friend");
                        o.put("username", username);
                        Net.get().send(o);
                        setSearchBox(I18n.t("request_sent_waiting"), Utils.TEXT_DIM);
                    } catch (Exception ignored) {
                    }
                }
            });
            searchResultBox.addView(btn);
        }
        renderChats();
    }

    // ================= Net.Listener =================

    private void sendAutoAuth() {
        if (!Session.loggedIn() || Session.token.isEmpty()) return;
        try {
            JSONObject o = new JSONObject();
            o.put("type", "auth");
            o.put("token", Session.token);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    private void updateStatusUi() {
        if (statusDot == null) return;
        if (Net.get().isConnected()) {
            statusDot.setBackground(circle(Utils.ACCENT));
            statusText.setText(I18n.t("connected"));
        } else if (Net.get().isRunning()) {
            statusDot.setBackground(circle(0xFFFFB300));
            statusText.setText(I18n.t("connecting"));
        } else {
            statusDot.setBackground(circle(Utils.DANGER));
            statusText.setText(I18n.t("disconnected"));
        }
    }

    @Override
    public void onConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String[] sp = Session.serverParts();
                AvatarManager.setHost(sp[0]);
                updateStatusUi();
                sendAutoAuth();
                refreshAll();
            }
        });
    }

    @Override
    public void onMessage(final JSONObject obj) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    handle(obj);
                } catch (Throwable t) {
                    reportCrash(t);
                }
            }
        });
    }

    private void handle(JSONObject o) {
        String type = o.optString("type");
        if ("login_ok".equals(type)) {
            Session.save(
                    o.optString("username", Session.username),
                    o.optString("nickname", Session.nickname),
                    Session.server,
                    o.optString("token", Session.token),
                    o.optString("status", Session.status),
                    o.optBoolean("hasAvatar", Session.hasAvatar));
            if (meScroll.getVisibility() == View.VISIBLE) renderMe();
        } else if ("conversations".equals(type)) {
            conversations = Models.parseConversations(o.optJSONArray("conversations"));
            for (Models.Conversation c : conversations) {
                c.pinned = PinnedPrefs.isPinned(this, c.with);
            }
            if (chatsScroll.getVisibility() == View.VISIBLE) renderChats();
            updateChatTabBadge();
        } else if ("contacts".equals(type)) {
            contacts = Models.parseContacts(o.optJSONArray("contacts"));
            pending = Models.parseContacts(o.optJSONArray("pending"));
            if (chatsScroll.getVisibility() == View.VISIBLE) renderChats();
            updateChatTabBadge();
        } else if ("groups".equals(type)) {
            groups = Models.parseGroups(o.optJSONArray("groups"));
            if (communitiesScroll.getVisibility() == View.VISIBLE) renderCommunities();
        } else if ("posts".equals(type)) {
            postsList = Models.parsePosts(o.optJSONArray("posts"));
            renderPosts();
        } else if ("post_added".equals(type) || "posts_changed".equals(type)) {
            refreshAll();
        } else if ("activities".equals(type)) {
            activitiesLoaded = true;
            activities = new ArrayList<>();
            JSONArray arr = o.optJSONArray("activities");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject a = arr.optJSONObject(i);
                    if (a != null) activities.add(a);
                }
            }
            if (callsScroll.getVisibility() == View.VISIBLE) renderActivities();
        } else if ("activities_changed".equals(type)) {
            refreshAll();
        } else if ("password_changed".equals(type)) {
            Utils.toast(this, I18n.t("password_changed"));
        } else if ("account_deleted".equals(type)) {
            logoutToMain();
        } else if ("group_created".equals(type)) {
            Utils.toast(this, I18n.t("group_created_ok"));
            refreshAll();
        } else if ("group_invited".equals(type)) {
            Models.Group g = Models.Group.fromJson(o.optJSONObject("group"));
            Utils.toast(this, String.format(I18n.t("invited_you"), g.name));
            refreshAll();
        } else if ("group_changed".equals(type)) {
            refreshAll();
        } else if ("group_dissolved".equals(type) || "group_kicked".equals(type)) {
            Models.Group g = Models.Group.fromJson(o.optJSONObject("group"));
            if ("group_dissolved".equals(type)) {
                Utils.toast(this, String.format(I18n.t("group_dissolved_msg"), g.name));
            } else {
                Utils.toast(this, String.format(I18n.t("group_kicked_msg"), g.name));
            }
            refreshAll();
        } else if ("search_result".equals(type)) {
            renderSearchResult(o);
        } else if ("new_message".equals(type)) {
            try {
                JSONObject v = new JSONObject();
                v.put("type", "get_conversations");
                Net.get().send(v);
            } catch (Exception ignored) {
            }
        } else if ("new_group_message".equals(type)) {
            try {
                JSONObject v = new JSONObject();
                v.put("type", "get_conversations");
                Net.get().send(v);
                v = new JSONObject();
                v.put("type", "get_groups");
                Net.get().send(v);
            } catch (Exception ignored) {
            }
        } else if ("group_history_cleared".equals(type)) {
            refreshAll();
        } else if ("user_profile".equals(type)) {
            fillFriendCard(o);
        } else if ("friend_request".equals(type)) {
            Utils.toast(this, o.optString("nickname") + " " + I18n.t("request_add_friend"));
            refreshAll();
        } else if ("friend_response".equals(type)) {
            boolean accepted = o.optBoolean("accepted");
            String name = o.optString("nickname");
            Utils.toast(this, accepted ? String.format(I18n.t("accepted_request"), name) : String.format(I18n.t("rejected_request"), name));
            refreshAll();
        } else if ("contacts_changed".equals(type)) {
            refreshAll();
        } else if ("nickname_updated".equals(type)) {
            Session.updateNickname(o.optString("nickname"));
            if (meScroll.getVisibility() == View.VISIBLE) renderMe();
            if (updatesScroll.getVisibility() == View.VISIBLE) renderUpdates();
            Utils.toast(this, I18n.t("nickname_updated_ok"));
        } else if ("status_updated".equals(type)) {
            Session.updateStatus(o.optString("status"));
            if (meScroll.getVisibility() == View.VISIBLE) renderMe();
            Utils.toast(this, I18n.t("status_updated_ok"));
        } else if ("avatar_updated".equals(type)) {
            Session.updateHasAvatar(true);
            AvatarManager.clear(Session.username);
            if (meScroll.getVisibility() == View.VISIBLE) renderMe();
            if (updatesScroll.getVisibility() == View.VISIBLE) renderUpdates();
            Utils.toast(this, I18n.t("avatar_updated_ok"));
        } else if ("history_cleared".equals(type) || "unfriended".equals(type) || "blocked".equals(type) || "unblocked".equals(type)) {
            refreshAll();
        } else if ("toast".equals(type)) {
            Utils.toast(this, I18n.serverMsg(o.optString("message")));
        } else if ("error".equals(type)) {
            String msg = o.optString("message");
            if (msg.contains("请先登录") || msg.contains("登录状态已失效")) {
                sendAutoAuth();
            } else {
                Utils.toast(this, I18n.serverMsg(msg));
            }
        } else if ("kicked".equals(type)) {
            Utils.toast(this, I18n.serverMsg(o.optString("message")));
            logoutToMain();
        }
    }

    @Override
    public void onDisconnected(final String reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateStatusUi();
            }
        });
    }
}
