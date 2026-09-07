package com.chatapp.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 聊天工具: 从聊天窗口右上角 ⋮ 进入, 提供 搜索聊天记录 / 导出聊天记录 */
public class ChatToolsActivity extends Activity implements Net.Listener {

    private String peer;
    private String peerNickname;
    private final List<Models.ChatMsg> messages = new ArrayList<>();
    private LinearLayout content;
    private EditText searchInput;
    private LinearLayout resultList;
    private View loading;
    private boolean loaded = false;
    private boolean inSearch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setBackgroundColor(Utils.BG);
        Theme.init(this);
        Theme.apply(this);
        Intent it = getIntent();
        peer = it.getStringExtra("peer");
        peerNickname = it.getStringExtra("nickname");
        if (peer == null) peer = "";
        if (peerNickname == null) peerNickname = peer;
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
        TextView title = Utils.tv(this, I18n.t("chat_tools"), 17, Utils.TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        top.addView(title, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        topLp.setMargins(Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8), 0);
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

        showMenu();

        FrameLayout frame = new FrameLayout(this);
        frame.addView(root, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        loading = new TriangleLoadingView(this);
        frame.addView(loading, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        loading.setVisibility(View.GONE);
        setContentView(frame);
    }

    private void showMenu() {
        inSearch = false;
        content.removeAllViews();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Utils.bg(this, Utils.CARD, 16));
        card.addView(menuRow(I18n.t("search_msg"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterSearch();
            }
        }));
        card.addView(Utils.divider(this));
        card.addView(menuRow(I18n.t("export_chat"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportChat();
            }
        }));
        card.addView(Utils.divider(this));
        card.addView(menuRow(I18n.t("delete_history"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmClearHistory();
            }
        }));
        content.addView(card);
        if (loaded) {
            TextView count = Utils.tv(this, I18n.f("chat_history_count", messages.size()), 12, Utils.TEXT_DIM, Gravity.CENTER);
            count.setPadding(0, Utils.dp(this, 12), 0, 0);
            content.addView(count);
        }
    }

    private View menuRow(String label, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Utils.dp(this, 16), Utils.dp(this, 15), Utils.dp(this, 16), Utils.dp(this, 15));
        row.setOnClickListener(onClick);
        row.addView(Utils.tv(this, label, 15, Utils.TEXT, Gravity.START), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private void confirmClearHistory() {
        Ui.show(this, I18n.t("delete_history"), Ui.message(this, String.format(I18n.t("confirm_clear_history_msg"), peerNickname)), I18n.t("confirm"), new Ui.Click() {
            @Override
            public void onClick() {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "clear_history");
                    o.put("with", peer);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void enterSearch() {
        inSearch = true;
        content.removeAllViews();
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(Utils.bg(this, Utils.CARD, 16));
        panel.setPadding(Utils.dp(this, 12), Utils.dp(this, 10), Utils.dp(this, 12), Utils.dp(this, 10));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        searchInput = new EditText(this);
        searchInput.setHint(I18n.t("search_msg"));
        searchInput.setSingleLine(true);
        searchInput.setTextSize(14 * Utils.fontScale);
        searchInput.setTextColor(Utils.TEXT);
        searchInput.setHintTextColor(Utils.TEXT_DIM);
        searchInput.setBackground(Utils.bg(this, Utils.CARD2, 14));
        searchInput.setPadding(Utils.dp(this, 12), Utils.dp(this, 7), Utils.dp(this, 12), Utils.dp(this, 7));
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                renderResults();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        row.addView(searchInput, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView backMenu = Utils.tv(this, I18n.t("done"), 13, Utils.ACCENT, Gravity.CENTER);
        backMenu.setPadding(Utils.dp(this, 10), 0, Utils.dp(this, 2), 0);
        backMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu();
            }
        });
        row.addView(backMenu);
        panel.addView(row);

        resultList = new LinearLayout(this);
        resultList.setOrientation(LinearLayout.VERTICAL);
        panel.addView(resultList);
        content.addView(panel);

        renderResults();
        searchInput.requestFocus();
    }

    private void renderResults() {
        if (resultList == null) return;
        resultList.removeAllViews();
        String q = searchInput == null ? "" : searchInput.getText().toString().trim().toLowerCase();
        int count = 0;
        for (Models.ChatMsg m : messages) {
            String hay = m.text == null ? "" : m.text;
            if (!q.isEmpty() && !hay.toLowerCase().contains(q)) continue;
            resultList.addView(resultRow(m));
            resultList.addView(Utils.divider(this));
            count++;
        }
        if (count == 0) {
            TextView empty = Utils.tv(this, messages.isEmpty() ? I18n.t("no_chat_history") : I18n.t("no_search_match"),
                    13, Utils.TEXT_DIM, Gravity.CENTER);
            empty.setPadding(0, Utils.dp(this, 16), 0, Utils.dp(this, 16));
            resultList.addView(empty);
        }
    }

    private View resultRow(Models.ChatMsg m) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, Utils.dp(this, 9), 0, Utils.dp(this, 7));
        String sender = m.mine ? Session.nickname : peerNickname;
        row.addView(Utils.tv(this, sender + " · " + Utils.timeText(m.time), 11, Utils.TEXT_DIM, Gravity.START));
        TextView txt = Utils.tv(this, m.text == null ? "" : m.text, 14, Utils.TEXT, Gravity.START);
        row.addView(txt);
        return row;
    }

    private void exportChat() {
        StringBuilder sb = new StringBuilder();
        sb.append("Chatapp 聊天记录 - ").append(peerNickname).append("\n\n");
        for (Models.ChatMsg m : messages) {
            String sender = m.mine ? Session.nickname : peerNickname;
            sb.append("[").append(Utils.timeText(m.time)).append("] ").append(sender).append(": ");
            if ("image".equals(m.kind)) sb.append("[图片]");
            else if ("file".equals(m.kind)) sb.append("[文件] ").append(m.name);
            else sb.append(m.text == null ? "" : m.text);
            sb.append("\n");
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

    @Override
    protected void onResume() {
        super.onResume();
        Net.get().setListener(this);
        if (Net.get().isConnected()) requestHistory();
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
                requestHistory();
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (loading != null) loading.setVisibility(View.GONE);
            }
        });
    }

    private void requestHistory() {
        if (!Net.get().isConnected()) return;
        try {
            JSONObject o = new JSONObject();
            o.put("type", "get_history");
            o.put("with", peer);
            Net.get().send(o);
            if (loading != null) loading.setVisibility(View.VISIBLE);
        } catch (Exception ignored) {
        }
    }

    private void handle(JSONObject o) {
        String type = o.optString("type");
        if ("history".equals(type) && peer.equals(o.optString("with"))) {
            messages.clear();
            messages.addAll(Models.parseMessages(o.optJSONArray("messages"), Session.username));
            loaded = true;
            if (loading != null) loading.setVisibility(View.GONE);
            if (inSearch) renderResults();
            else showMenu();
        } else if ("history_cleared".equals(type) && peer.equals(o.optString("with"))) {
            messages.clear();
            loaded = true;
            if (loading != null) loading.setVisibility(View.GONE);
            Utils.toast(this, I18n.t("history_cleared"));
            if (inSearch) renderResults();
            else showMenu();
        }
    }
}