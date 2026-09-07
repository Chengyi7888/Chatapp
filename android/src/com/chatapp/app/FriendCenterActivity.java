package com.chatapp.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 添加与请�? 添加好友 / 同意好友申请 / 扫一�?*/
public class FriendCenterActivity extends Activity implements Net.Listener {

    private LinearLayout content;
    private final List<Models.Contact> pending = new ArrayList<>();
    private EditText searchInput;
    private LinearLayout resultBox;

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
        TextView title = Utils.tv(this, I18n.t("friend_center"), 17, Utils.TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button scan = Utils.button(this);
        scan.setText("\u25A1");
        scan.setTextSize(20);
        scan.setTextColor(Color.WHITE);
        int scanSize = Utils.dp(this, 40);
        GradientDrawable scanBg = new GradientDrawable();
        scanBg.setShape(GradientDrawable.OVAL);
        scanBg.setColor(Utils.ACCENT);
        scan.setBackground(scanBg);
        scan.setPadding(0, 0, 0, 0);
        scan.setLayoutParams(new LinearLayout.LayoutParams(scanSize, scanSize));
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaPick.scan(FriendCenterActivity.this, 9201, new MediaPick.Callback() {
                    @Override
                    public void onBitmap(Bitmap bmp) {
                        showScanResult(bmp);
                    }
                });
            }
        });
        top.addView(scan);
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
        render();
    }

    private void render() {
        content.removeAllViews();
        TextView addTitle = Utils.tv(this, I18n.t("add_friend"), 16, Utils.TEXT, Gravity.START);
        addTitle.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(addTitle);
        content.addView(Utils.hSpace(this, 8));
        searchInput = Ui.input(this, null, I18n.t("search_username_hint"));
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchInput.setSingleLine(true);
        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    doSearch();
                    return true;
                }
                return false;
            }
        });
        content.addView(searchInput);

        resultBox = new LinearLayout(this);
        resultBox.setOrientation(LinearLayout.VERTICAL);
        content.addView(resultBox);
        content.addView(Utils.hSpace(this, 18));

        TextView reqTitle = Utils.tv(this, I18n.t("friend_requests") + " (" + pending.size() + ")", 16, Utils.TEXT, Gravity.START);
        reqTitle.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(reqTitle);
        content.addView(Utils.hSpace(this, 8));
        if (pending.isEmpty()) {
            content.addView(Utils.tv(this, I18n.t("no_pending"), 13, Utils.TEXT_DIM, Gravity.CENTER));
        } else {
            for (final Models.Contact p : pending) {
                content.addView(pendingRow(p));
            }
        }
    }

    private View pendingRow(final Models.Contact p) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(Utils.bg(this, Utils.CARD, 14));
        row.setPadding(Utils.dp(this, 12), Utils.dp(this, 10), Utils.dp(this, 12), Utils.dp(this, 10));
        row.addView(AvatarManager.avatarView(this, p.username, p.nickname, p.hasAvatar, 40, 15));
        row.addView(Utils.vSpace(this, 10));
        row.addView(Utils.tv(this, p.nickname + " @" + p.username, 14, Utils.TEXT, Gravity.START), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button accept = Utils.button(this);
        accept.setText(I18n.t("accept"));
        accept.setTextSize(12);
        accept.setTextColor(Color.WHITE);
        accept.setBackground(Utils.bg(this, Utils.ACCENT, 14));
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                respond(p.username, true);
            }
        });
        row.addView(accept);
        row.addView(Utils.vSpace(this, 6));
        Button deny = Utils.button(this);
        deny.setText(I18n.t("reject"));
        deny.setTextSize(12);
        deny.setTextColor(Color.WHITE);
        deny.setBackground(Utils.bg(this, Utils.DANGER, 14));
        deny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                respond(p.username, false);
            }
        });
        row.addView(deny);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = Utils.dp(this, 4);
        rlp.bottomMargin = Utils.dp(this, 4);
        row.setLayoutParams(rlp);
        return row;
    }

    private void doSearch() {
        String name = searchInput.getText().toString().trim();
        if (name.isEmpty()) return;
        if (!Net.get().isConnected()) {
            Ui.popup(this, I18n.t("not_connected"));
            return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("type", "search_user");
            o.put("username", name);
            Net.get().send(o);
            resultBox.removeAllViews();
            resultBox.addView(Utils.tv(this, I18n.t("searching"), 13, Utils.TEXT_DIM, Gravity.CENTER));
        } catch (Exception ignored) {
        }
    }

    private void showSearchResult(JSONObject o) {
        String status = o.optString("status");
        final String username = o.optString("username");
        final String nickname = o.optString("nickname");
        resultBox.removeAllViews();
        if ("not_found".equals(status)) {
            resultBox.addView(Utils.tv(this, I18n.f("user_not_found", username), 13, Utils.DANGER, Gravity.CENTER));
            return;
        }
        if ("self".equals(status)) {
            resultBox.addView(Utils.tv(this, I18n.f("this_is_you", username), 13, Utils.TEXT_DIM, Gravity.CENTER));
            return;
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(Utils.bg(this, Utils.CARD, 14));
        row.setPadding(Utils.dp(this, 12), Utils.dp(this, 10), Utils.dp(this, 12), Utils.dp(this, 10));
        row.addView(AvatarManager.avatarView(this, username, nickname, false, 40, 15));
        row.addView(Utils.vSpace(this, 10));

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.addView(Utils.tv(this, nickname, 15, Utils.TEXT, Gravity.START));
        mid.addView(Utils.tv(this, "@" + username, 12, Utils.TEXT_DIM, Gravity.START));
        row.addView(mid, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(Utils.vSpace(this, 8));

        if ("friend".equals(status)) {
            Button added = Utils.button(this);
            added.setText(I18n.t("added"));
            added.setTextSize(13);
            added.setTextColor(Utils.TEXT_DIM);
            added.setBackground(Utils.bg(this, Utils.CARD2, 14));
            added.setEnabled(false);
            row.addView(added);
        } else if ("pending_out".equals(status)) {
            Button req = Utils.button(this);
            req.setText(I18n.t("request_sent"));
            req.setTextSize(13);
            req.setTextColor(Utils.TEXT_DIM);
            req.setBackground(Utils.bg(this, Utils.CARD2, 14));
            req.setEnabled(false);
            row.addView(req);
        } else {
            Button add = Utils.button(this);
            add.setText(I18n.t("add_friend"));
            add.setTextSize(13);
            add.setTextColor(Color.WHITE);
            add.setBackground(Utils.bg(this, Utils.ACCENT, 14));
            add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        JSONObject j = new JSONObject();
                        j.put("type", "add_friend");
                        j.put("username", username);
                        Net.get().send(j);
                        add.setEnabled(false);
                        add.setText(I18n.t("request_sent"));
                        add.setTextColor(Utils.TEXT_DIM);
                        add.setBackground(Utils.bg(FriendCenterActivity.this, Utils.CARD2, 14));
                    } catch (Exception ignored) {
                    }
                }
            });
            row.addView(add);
        }
        resultBox.addView(row);
    }

    private void showScanResult(final Bitmap bmp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout box = new LinearLayout(FriendCenterActivity.this);
                box.setOrientation(LinearLayout.VERTICAL);
                box.setGravity(Gravity.CENTER_HORIZONTAL);
                box.setPadding(Utils.dp(FriendCenterActivity.this, 8), Utils.dp(FriendCenterActivity.this, 6), Utils.dp(FriendCenterActivity.this, 8), Utils.dp(FriendCenterActivity.this, 6));
                ImageView qr = new ImageView(FriendCenterActivity.this);
                int s = Utils.dp(FriendCenterActivity.this, 180);
                qr.setLayoutParams(new LinearLayout.LayoutParams(s, s));
                qr.setImageBitmap(bmp);
                box.addView(qr);
                box.addView(Utils.tv(FriendCenterActivity.this, I18n.t("scan_add_hint"), 12, Utils.TEXT_DIM, Gravity.CENTER));
                box.addView(Utils.hSpace(FriendCenterActivity.this, 8));
                final EditText input = Ui.input(FriendCenterActivity.this, null, "username");
                box.addView(input);
                Button add = Utils.button(FriendCenterActivity.this);
                add.setText(I18n.t("add_friend"));
                add.setTextSize(14);
                add.setTextColor(Color.WHITE);
                add.setBackground(Utils.bg(FriendCenterActivity.this, Utils.ACCENT, 22));
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
                Ui.showSingle(FriendCenterActivity.this, I18n.t("qr_scan"), box, I18n.t("close"), null);
            }
        });
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "get_contacts");
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
                    if ("contacts".equals(type)) {
                        pending.clear();
                        pending.addAll(Models.parseContacts(obj.optJSONArray("pending")));
                        render();
                    } else if ("search_result".equals(type)) {
                        showSearchResult(obj);
                    } else if ("friend_request".equals(type) || "friend_response".equals(type)) {
                        try {
                            JSONObject o = new JSONObject();
                            o.put("type", "get_contacts");
                            Net.get().send(o);
                        } catch (Exception ignored) {
                        }
                    } else if ("toast".equals(type) || "error".equals(type)) {
                        Ui.popup(FriendCenterActivity.this, I18n.serverMsg(obj.optString("message")));
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
