package com.chatapp.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.app.DownloadManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.util.Base64;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 聊天窗口: 长按消息支持 复制/删除/撤回/转发/回复 (暗黑模式) */
public class ChatActivity extends Activity implements Net.Listener {

    private static final int PICK_IMAGE = 2001;
    private static final int PICK_FILE = 2002;

    private String peer;
    private String peerNickname;
    private boolean peerHasAvatar = false;
    private LinearLayout msgList;
    private ScrollView scroll;
    private EditText input;
    private List<Models.ChatMsg> messages = new ArrayList<>();
    private SharedPreferences localDel;

    private LinearLayout replyBar;
    private LinearLayout attachPanel;
    private View loading;
    private long peerReadTime = 0;
    private TextView typingText;
    private final Handler typingHide = new Handler(Looper.getMainLooper());
    private SharedPreferences pinPrefs;
    private SharedPreferences draftPrefs;
    private Dialog previewDialog;
    private Bitmap previewBitmap;
    private Models.ChatMsg pendingForward;
    private TextView plusBtn;
    private TextView replyBarText;
    private String pendingReply = "";

    private Dialog forwardSheet;
    private LinearLayout forwardListContainer;
    private Models.ChatMsg forwardTarget;
    private boolean multiSelect = false;
    private final Set<String> selectedIds = new HashSet<>();
    private LinearLayout selectBar;
    private TextView selectCountText;
    private List<Models.ChatMsg> pendingMultiForward = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setBackgroundColor(Utils.BG);
        Theme.init(this);
        Theme.apply(this);
        localDel = getSharedPreferences("chatapp_chat", MODE_PRIVATE);
        pinPrefs = getSharedPreferences("chatapp_msg_pin", MODE_PRIVATE);
        draftPrefs = getSharedPreferences("chatapp_drafts", MODE_PRIVATE);
        Intent it = getIntent();
        peer = it.getStringExtra("peer");
        peerNickname = it.getStringExtra("nickname");
        if (peer == null) peer = "";
        if (peerNickname == null) peerNickname = peer;
        peerHasAvatar = it.getBooleanExtra("hasAvatar", false);

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
        TextView title = Utils.tv(this, peerNickname, 17, Utils.TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        top.addView(title, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView toolsBtn = Utils.tv(this, "\u22EE", 22, Utils.TEXT, Gravity.CENTER);
        toolsBtn.setPadding(Utils.dp(this, 8), 0, Utils.dp(this, 2), Utils.dp(this, 2));
        toolsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ChatActivity.this, ChatToolsActivity.class);
                i.putExtra("peer", peer);
                i.putExtra("nickname", peerNickname);
                startActivity(i);
            }
        });
        top.addView(toolsBtn);
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        topLp.setMargins(Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8), 0);
        root.addView(top, topLp);


        msgList = new LinearLayout(this);
        msgList.setOrientation(LinearLayout.VERTICAL);
        msgList.setBackgroundColor(Utils.BG);
        msgList.setPadding(Utils.dp(this, 8), Utils.dp(this, 8), Utils.dp(this, 8), Utils.dp(this, 8));
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Utils.BG);
        scroll.addView(msgList);
        root.addView(scroll, Utils.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        typingText = Utils.tv(this, I18n.t("typing_hint"), 11, Utils.ACCENT, Gravity.START);
        typingText.setPadding(Utils.dp(this, 12), Utils.dp(this, 4), Utils.dp(this, 12), Utils.dp(this, 4));
        typingText.setVisibility(View.GONE);
        root.addView(typingText);

        // 回复指示
        replyBar = new LinearLayout(this);
        replyBar.setOrientation(LinearLayout.HORIZONTAL);
        replyBar.setGravity(Gravity.CENTER_VERTICAL);
        replyBar.setBackground(Utils.bg(this, Utils.CARD, 18));
        replyBar.setPadding(Utils.dp(this, 12), Utils.dp(this, 6), Utils.dp(this, 6), Utils.dp(this, 6));
        replyBarText = Utils.tv(this, "", 12, Utils.ACCENT, Gravity.START);
        replyBar.addView(replyBarText, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView cancelReply = Utils.tv(this, "\u00D7", 16, Utils.TEXT_DIM, Gravity.CENTER);
        cancelReply.setPadding(Utils.dp(this, 10), 0, Utils.dp(this, 10), 0);
        cancelReply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pendingReply = "";
                replyBar.setVisibility(View.GONE);
            }
        });
        replyBar.addView(cancelReply);
        replyBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(Utils.dp(this, 8), 0, Utils.dp(this, 8), 0);
        root.addView(replyBar, rlp);
        buildSelectBar(root);

        // 附件功能面板(点击＋展开)
        attachPanel = new LinearLayout(this);
        attachPanel.setOrientation(LinearLayout.HORIZONTAL);
        attachPanel.setGravity(Gravity.CENTER);
        attachPanel.setBackground(Utils.bg(this, Utils.CARD, 18));
        attachPanel.setPadding(Utils.dp(this, 8), Utils.dp(this, 12), Utils.dp(this, 8), Utils.dp(this, 12));
        attachPanel.setVisibility(View.GONE);
        attachPanel.addView(attachBtn("Ima", I18n.t("image"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImage();
            }
        }));
        attachPanel.addView(Utils.vSpace(this, 18));
        attachPanel.addView(attachBtn("Fil", I18n.t("file"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFile();
            }
        }));
        attachPanel.addView(Utils.vSpace(this, 18));
        attachPanel.addView(attachBtn("Fav", I18n.t("favorite"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFavPicker();
            }
        }));
        attachPanel.addView(Utils.vSpace(this, 18));
        attachPanel.addView(attachBtn("More", I18n.t("more"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ui.popup(ChatActivity.this, I18n.t("more_coming"));
            }
        }));

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setBackground(Utils.bg(this, Utils.CARD, 18));
        inputRow.setPadding(Utils.dp(this, 6), Utils.dp(this, 5), Utils.dp(this, 6), Utils.dp(this, 5));

        // 白色圆圈＋按钮，位于输入框左侧
        plusBtn = Utils.tv(this, "+", 22, Utils.TEXT, Gravity.CENTER);
        int ps = Utils.dp(this, 40);
        plusBtn.setLayoutParams(new LinearLayout.LayoutParams(ps, ps));
        GradientDrawable pg = new GradientDrawable();
        pg.setShape(GradientDrawable.OVAL);
        pg.setStroke(Utils.dp(this, 2), Utils.TEXT);
        pg.setColor(Utils.CARD2);
        plusBtn.setBackground(pg);
        plusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAttach();
            }
        });
        inputRow.addView(plusBtn);
        inputRow.addView(Utils.vSpace(this, 6));

        input = new EditText(this);
        input.setHint(I18n.t("input_message"));
        input.setSingleLine(true);
        input.setTextSize(15 * Utils.fontScale);
        input.setTextColor(Utils.TEXT);
        input.setHintTextColor(Utils.TEXT_DIM);
        input.setBackground(Utils.bg(this, Utils.CARD2, 16));
        input.setPadding(Utils.dp(this, 14), Utils.dp(this, 8), Utils.dp(this, 14), Utils.dp(this, 8));
        input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendText();
                    return true;
                }
                return false;
            }
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                draftPrefs.edit().putString("draft_" + peer, s.toString()).apply();
                if (s.length() > 0) sendTyping();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        inputRow.addView(input, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        inputRow.addView(Utils.vSpace(this, 6));

        Button sendBtn = Utils.button(this);
        sendBtn.setText(I18n.t("send"));
        sendBtn.setTextSize(14);
        sendBtn.setTextColor(Color.WHITE);
        sendBtn.setBackground(Utils.bg(this, Utils.ACCENT, 16));
        sendBtn.setMinHeight(0);
        sendBtn.setHeight(Utils.dp(this, 40));
        sendBtn.setPadding(Utils.dp(this, 14), 0, Utils.dp(this, 14), 0);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendText();
            }
        });
        inputRow.addView(sendBtn);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ilp.setMargins(Utils.dp(this, 8), 0, Utils.dp(this, 8), Utils.dp(this, 8));
        root.addView(inputRow, ilp);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        alp.setMargins(Utils.dp(this, 8), 0, Utils.dp(this, 8), 0);
        root.addView(attachPanel, alp);

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Utils.BG);
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
        if (Net.get().isConnected()) {
            requestHistory();
        }
        String d = draftPrefs.getString("draft_" + peer, "");
        if (d != null && !d.isEmpty()) {
            input.setText(d);
            input.setSelection(d.length());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Net.get().setListener(null);
        if (forwardSheet != null && forwardSheet.isShowing()) forwardSheet.dismiss();
    }

    private final Runnable typingHideTask = new Runnable() {
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

    private void favoriteMsg(Models.ChatMsg m) {
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
            Ui.popup(this, I18n.t("favorited"));
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
            Ui.popup(this, I18n.t("fav_empty"));
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
                        Ui.popup(ChatActivity.this, I18n.t("send"));
                    } catch (Exception ignored) {
                    }
                }
            };
        }
        Ui.list(this, I18n.t("favorite"), items.toArray(new String[0]), clicks);
    }

    private void togglePin(Models.ChatMsg m) {
        Set<String> set = new HashSet<>(pinPrefs.getStringSet("pin_" + peer, new HashSet<String>()));
        if (set.contains(m.id)) set.remove(m.id); else set.add(m.id);
        pinPrefs.edit().putStringSet("pin_" + peer, set).apply();
        rerender();
    }

    private void requestHistory() {
        if (!Net.get().isConnected()) return;
        try {
            JSONObject o = new JSONObject();
            o.put("type", "get_history");
            o.put("with", peer);
            Net.get().send(o);
            markRead();
        } catch (Exception ignored) {
        }
    }

    private void markRead() {
        try {
            JSONObject o = new JSONObject();
            o.put("type", "mark_read");
            o.put("with", peer);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    // ---------- 发送 ----------

    private void sendText() {
        final String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        input.setText("");
        String reply = pendingReply;
        pendingReply = "";
        replyBar.setVisibility(View.GONE);
        try {
            JSONObject o = new JSONObject();
            o.put("type", "send_message");
            o.put("to", peer);
            o.put("text", text);
            if (!reply.isEmpty()) {
                JSONObject r = new JSONObject();
                r.put("text", reply);
                o.put("reply", r);
            }
            Net.get().send(o);
        } catch (Exception ignored) {
        }
        Models.ChatMsg m = new Models.ChatMsg(Session.username, peer, text, System.currentTimeMillis(), true);
        m.id = "local_" + System.currentTimeMillis() + "_" + Math.abs(text.hashCode());
        m.reply = reply.isEmpty() ? "" : reply;
        appendMsg(m);
    }

    private void appendMsg(Models.ChatMsg m) {
        messages.add(m);
        addBubble(m, false, false);
    }

    private void rerender() {
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
            addBubble(m, pins.contains(m.id), m == lastMine && peerReadTime >= m.time);
        }
    }

    private void addBubble(final Models.ChatMsg m, final boolean pinned, final boolean showRead) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(m.mine ? Gravity.END : Gravity.START);
        row.setPadding(Utils.dp(this, 4), Utils.dp(this, 2), Utils.dp(this, 4), Utils.dp(this, 2));

        // 非本人消息，左侧头像
        if (!m.mine) {
            row.addView(AvatarManager.avatarView(this, peer, peerNickname, peerHasAvatar, 36, 14));
            row.addView(Utils.vSpace(this, 8));
        }

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(m.mine ? Gravity.END : Gravity.START);

        if (pinned) {
            TextView pb = Utils.tv(this, "📌 " + I18n.t("pin_msg"), 10, Utils.ACCENT, Gravity.START);
            pb.setPadding(Utils.dp(this, 6), 0, Utils.dp(this, 6), Utils.dp(this, 2));
            col.addView(pb);
        }

        // 气泡内容 (可含引用)
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(Utils.dp(this, 12), Utils.dp(this, 8), Utils.dp(this, 12), Utils.dp(this, 8));
        boolean sel = multiSelect && selectedIds.contains(m.id);
        int bColor = m.mine ? Utils.ACCENT : Color.WHITE;
        if (sel) bubble.setBackground(Utils.ring(this, bColor, Utils.ACCENT, 2, 8));
        else bubble.setBackground(Utils.bg(this, bColor, 8));

        if (m.reply != null && !m.reply.isEmpty()) {
            TextView quote = Utils.tv(this, "↩ " + truncate8(m.reply), 11, 0xFF444444, Gravity.START);
            quote.setBackground(Utils.bg(this, Utils.QUOTE_BG, 6));
            quote.setPadding(Utils.dp(this, 8), Utils.dp(this, 4), Utils.dp(this, 8), Utils.dp(this, 4));
            LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            qlp.bottomMargin = Utils.dp(this, 4);
            bubble.addView(quote, qlp);
        }
        if ("image".equals(m.kind) && !m.url.isEmpty()) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setAdjustViewBounds(true);
            iv.setMaxWidth(Utils.dp(this, 240));
            iv.setMaxHeight(Utils.dp(this, 300));
            iv.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            GradientDrawable ibg = new GradientDrawable();
            ibg.setCornerRadius(Utils.dp(this, 6));
            ibg.setColor(m.mine ? Utils.BUBBLE_OUT : Utils.BUBBLE_IN);
            iv.setBackground(ibg);
            iv.setClipToOutline(true);
            iv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (multiSelect) toggleSelect(m);
                    else showImagePreview(m);
                }
            });
            bubble.addView(iv);
            AvatarManager.loadFileImage(this, m.url, iv);
        } else if ("file".equals(m.kind) && !m.url.isEmpty()) {
            LinearLayout fileRow = new LinearLayout(this);
            fileRow.setOrientation(LinearLayout.HORIZONTAL);
            fileRow.setGravity(Gravity.CENTER_VERTICAL);
            fileRow.setPadding(0, Utils.dp(this, 4), 0, Utils.dp(this, 4));
            fileRow.addView(Utils.tv(this, "📄", 22, Utils.TEXT, Gravity.CENTER));
            fileRow.addView(Utils.vSpace(this, 8));
            LinearLayout fm = new LinearLayout(this);
            fm.setOrientation(LinearLayout.VERTICAL);
            String fname = m.name.isEmpty() ? I18n.t("file") : m.name;
            fm.addView(Utils.tv(this, fname, 14, Utils.TEXT, Gravity.START));
            fm.addView(Utils.tv(this, formatSize(m.size), 11, Utils.TEXT_DIM, Gravity.START));
            fileRow.addView(fm);
            fileRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    downloadFile(m);
                }
            });
            bubble.addView(fileRow);
        } else {
            TextView text = Utils.tv(this, m.text, 15, Color.BLACK, Gravity.START);
            android.graphics.Paint p = new android.graphics.Paint();
            p.setTextSize(Utils.sp(this, 15));
            int maxW = (int) p.measureText("中中中中中中中中中中中中中中中中中中") + Utils.dp(this, 24);
            text.setMaxWidth(maxW);
            bubble.addView(text, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        bubble.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (multiSelect) { toggleSelect(m); return true; }
                showActionMenu(m);
                return true;
            }
        });
        bubble.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (multiSelect) toggleSelect(m);
            }
        });
        col.addView(bubble, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView time = Utils.tv(this, Utils.timeText(m.time), 10, Utils.TEXT_DIM, Gravity.END);
        time.setPadding(0, Utils.dp(this, 2), m.mine ? Utils.dp(this, 4) : 0, 0);
        col.addView(time);
        if (showRead && m.mine && peerReadTime >= m.time) {
            TextView rd = Utils.tv(this, I18n.t("read"), 10, Utils.ACCENT, Gravity.END);
            rd.setPadding(0, Utils.dp(this, 2), 0, 0);
            col.addView(rd);
        }

        row.addView(col);

        // 本人消息: 右侧头像
        if (m.mine) {
            row.addView(Utils.vSpace(this, 8));
            row.addView(AvatarManager.avatarView(this, Session.username, Session.nickname, Session.hasAvatar, 36, 14));
        }

        msgList.addView(row);
        scroll.post(new Runnable() {
            @Override
            public void run() {
                scroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    // ---------- 长按操作 ----------

    private void showActionMenu(final Models.ChatMsg m) {
        final List<String> items = new ArrayList<>();
        items.add(I18n.t("copy"));
        items.add(I18n.t("delete"));
        if (m.mine && System.currentTimeMillis() - m.time <= 120000) {
            items.add(I18n.t("recall"));
        }
        items.add(I18n.t("forward"));
        items.add(I18n.t("multi_select"));
        boolean pinned = pinPrefs.getStringSet("pin_" + peer, new HashSet<String>()).contains(m.id);
        items.add(pinned ? I18n.t("unpin_msg") : I18n.t("pin_msg"));
        items.add(I18n.t("favorite"));
        items.add(I18n.t("reply"));
        Ui.Click[] clicks = new Ui.Click[items.size()];
        for (int i = 0; i < items.size(); i++) {
            final String act = items.get(i);
            clicks[i] = new Ui.Click() {
                @Override
                public void onClick() {
                    if (I18n.t("copy").equals(act)) copyMsg(m);
                    else if (I18n.t("delete").equals(act)) deleteMsg(m);
                    else if (I18n.t("recall").equals(act)) recallMsg(m);
                    else if (I18n.t("forward").equals(act)) openForwardSheet(m);
                    else if (I18n.t("multi_select").equals(act)) enterMultiSelect();
                    else if (I18n.t("pin_msg").equals(act) || I18n.t("unpin_msg").equals(act)) togglePin(m);
                    else if (I18n.t("favorite").equals(act)) favoriteMsg(m);
                    else if (I18n.t("reply").equals(act)) setReply(m);
                }
            };
        }
        Ui.list(this, I18n.t("message_actions"), items.toArray(new String[0]), clicks);
    }

    private void copyMsg(Models.ChatMsg m) {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("msg", mediaLabel(m)));
            Utils.toast(this, I18n.t("copied"));
        } catch (Exception e) {
            Utils.toast(this, I18n.t("copy_failed"));
        }
    }

    private void deleteMsg(Models.ChatMsg m) {
        if (m.id == null || m.id.isEmpty()) return;
        Set<String> set = new HashSet<>(localDel.getStringSet("deleted_" + peer, new HashSet<String>()));
        set.add(m.id);
        localDel.edit().putStringSet("deleted_" + peer, set).apply();
        hideOnServer(m.id);
        rerender();
        Ui.popup(this, I18n.t("deleted_self"));
    }

    private void hideOnServer(String id) {
        try {
            JSONObject o = new JSONObject();
            o.put("type", "hide_message");
            o.put("id", id);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    private void recallMsg(final Models.ChatMsg m) {
        try {
            JSONObject o = new JSONObject();
            o.put("type", "recall_message");
            o.put("id", m.id);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    private String mediaLabel(Models.ChatMsg m) {
        if (m.text != null && !m.text.isEmpty()) return m.text;
        if ("image".equals(m.kind)) return I18n.t("media_image");
        if ("file".equals(m.kind)) return m.name == null || m.name.isEmpty() ? I18n.t("media_file") : m.name;
        return "";
    }

    private void setReply(Models.ChatMsg m) {
        pendingReply = mediaLabel(m);
        replyBarText.setText(I18n.t("reply_prefix") + truncate8(pendingReply));
        replyBar.setVisibility(View.VISIBLE);
        input.requestFocus();
    }

    // ---------- 多选 ----------

    private void enterMultiSelect() {
        multiSelect = true;
        selectedIds.clear();
        selectBar.setVisibility(View.VISIBLE);
        updateSelectBar();
        rerender();
    }

    private void exitMultiSelect() {
        multiSelect = false;
        selectedIds.clear();
        selectBar.setVisibility(View.GONE);
        rerender();
    }

    private void toggleSelect(Models.ChatMsg m) {
        if (m.id == null || m.id.isEmpty()) return;
        if (selectedIds.contains(m.id)) selectedIds.remove(m.id);
        else selectedIds.add(m.id);
        updateSelectBar();
        rerender();
    }

    private void updateSelectBar() {
        selectCountText.setText(String.valueOf(selectedIds.size()));
    }

    private List<Models.ChatMsg> selectedMessages() {
        List<Models.ChatMsg> list = new ArrayList<>();
        for (Models.ChatMsg m : messages) {
            if (selectedIds.contains(m.id)) list.add(m);
        }
        return list;
    }

    private TextView selectActionBtn(String text, View.OnClickListener onClick) {
        TextView t = Utils.tv(this, text, 14, Utils.TEXT, Gravity.CENTER);
        t.setBackground(Utils.bg(this, Utils.CARD2, 14));
        t.setPadding(Utils.dp(this, 12), Utils.dp(this, 7), Utils.dp(this, 12), Utils.dp(this, 7));
        t.setOnClickListener(onClick);
        return t;
    }

    private void buildSelectBar(final LinearLayout root) {
        selectBar = new LinearLayout(this);
        selectBar.setOrientation(LinearLayout.HORIZONTAL);
        selectBar.setGravity(Gravity.CENTER_VERTICAL);
        selectBar.setBackground(Utils.bg(this, Utils.CARD, 18));
        LinearLayout.LayoutParams sbp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sbp.setMargins(Utils.dp(this, 8), 0, Utils.dp(this, 8), 0);
        TextView close = Utils.tv(this, "\u00D7", 18, Utils.TEXT_DIM, Gravity.CENTER);
        close.setPadding(Utils.dp(this, 10), 0, Utils.dp(this, 6), 0);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitMultiSelect();
            }
        });
        selectBar.addView(close);
        selectCountText = Utils.tv(this, "0", 13, Utils.ACCENT, Gravity.CENTER);
        selectCountText.setPadding(Utils.dp(this, 4), 0, Utils.dp(this, 10), 0);
        selectBar.addView(selectCountText);
        selectBar.addView(selectActionBtn(I18n.t("forward"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forwardSelected();
            }
        }));
        selectBar.addView(Utils.vSpace(this, 6));
        selectBar.addView(selectActionBtn(I18n.t("copy"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copySelected();
            }
        }));
        selectBar.addView(Utils.vSpace(this, 6));
        selectBar.addView(selectActionBtn(I18n.t("reply"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                replySelected();
            }
        }));
        selectBar.addView(Utils.vSpace(this, 6));
        selectBar.addView(selectActionBtn(I18n.t("delete"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSelected();
            }
        }));
        selectBar.setVisibility(View.GONE);
        root.addView(selectBar, sbp);
    }

    private void copySelected() {
        StringBuilder sb = new StringBuilder();
        for (Models.ChatMsg m : selectedMessages()) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(mediaLabel(m));
        }
        if (sb.length() == 0) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("msgs", sb.toString()));
        exitMultiSelect();
        Ui.popup(this, I18n.t("copied"));
    }

    private void replySelected() {
        StringBuilder sb = new StringBuilder();
        for (Models.ChatMsg m : selectedMessages()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(truncate8(mediaLabel(m)));
        }
        pendingReply = sb.toString();
        replyBarText.setText(I18n.t("reply_prefix") + truncate8(pendingReply));
        replyBar.setVisibility(View.VISIBLE);
        exitMultiSelect();
    }

    private void deleteSelected() {
        for (Models.ChatMsg m : selectedMessages()) {
            if (m.id == null || m.id.isEmpty()) continue;
            Set<String> set = new HashSet<>(localDel.getStringSet("deleted_" + peer, new HashSet<String>()));
            set.add(m.id);
            localDel.edit().putStringSet("deleted_" + peer, set).apply();
            hideOnServer(m.id);
        }
        exitMultiSelect();
        Ui.popup(this, I18n.t("deleted_self"));
    }

    private void forwardSelected() {
        List<Models.ChatMsg> sel = selectedMessages();
        if (sel.isEmpty()) return;
        if (forwardContacts.isEmpty()) {
            pendingMultiForward = sel;
            try {
                JSONObject o = new JSONObject();
                o.put("type", "get_contacts");
                Net.get().send(o);
            } catch (Exception ignored) {
            }
            return;
        }
        showMultiForwardList(sel);
    }

    private void showMultiForwardList(final List<Models.ChatMsg> sel) {
        String[] names = new String[forwardContacts.size()];
        Ui.Click[] clicks = new Ui.Click[forwardContacts.size()];
        for (int i = 0; i < forwardContacts.size(); i++) {
            final Models.Contact c = forwardContacts.get(i);
            names[i] = c.nickname + " (@" + c.username + ")";
            clicks[i] = new Ui.Click() {
                @Override
                public void onClick() {
                    for (Models.ChatMsg m : sel) {
                        try {
                            JSONObject o = new JSONObject();
                            if ("image".equals(m.kind) || "file".equals(m.kind)) {
                                o.put("type", "forward_message");
                                o.put("to", c.username);
                                o.put("id", m.id);
                            } else {
                                o.put("type", "send_message");
                                o.put("to", c.username);
                                o.put("text", m.text);
                            }
                            Net.get().send(o);
                        } catch (Exception ignored) {
                        }
                    }
                    exitMultiSelect();
                    Ui.popup(ChatActivity.this, I18n.f("forwarded_ok", c.nickname));
                }
            };
        }
        Ui.list(this, I18n.t("forward_to"), names, clicks);
    }

    // ---------- 转发 ----------

    private void openForwardSheet(final Models.ChatMsg m) {
        forwardTarget = m;
        forwardSheet = new Dialog(this);
        forwardSheet.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setBackgroundColor(Utils.CARD);
        v.setPadding(Utils.dp(this, 16), Utils.dp(this, 16), Utils.dp(this, 16), Utils.dp(this, 16));
        TextView title = Utils.tv(this, I18n.t("forward_to"), 16, Utils.TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, Utils.dp(this, 10));
        v.addView(title);
        forwardListContainer = new LinearLayout(this);
        forwardListContainer.setOrientation(LinearLayout.VERTICAL);
        forwardListContainer.addView(Utils.tv(this, I18n.t("loading"), 14, Utils.TEXT_DIM, Gravity.CENTER));
        v.addView(forwardListContainer);
        forwardSheet.setContentView(v);
        Window w = forwardSheet.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Utils.CARD));
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.55f));
            w.setGravity(Gravity.BOTTOM);
        }
        forwardSheet.show();
        try {
            JSONObject o = new JSONObject();
            o.put("type", "get_contacts");
            Net.get().send(o);
            JSONObject g = new JSONObject();
            g.put("type", "get_groups");
            Net.get().send(g);
        } catch (Exception ignored) {
        }
    }

    private void renderForwardList() {
        if (forwardListContainer == null) return;
        forwardListContainer.removeAllViews();
        List<Models.Contact> list = forwardContacts;
        if (list.isEmpty()) {
            forwardListContainer.addView(Utils.tv(this, I18n.t("no_friends_hint"), 14, Utils.TEXT_DIM, Gravity.CENTER));
            return;
        }
        for (final Models.Contact c : list) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Utils.dp(this, 6), Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8));
            row.addView(AvatarManager.avatarView(this, c.username, c.nickname, c.hasAvatar, 40, 15));
            row.addView(Utils.vSpace(this, 10));
            row.addView(Utils.tv(this, c.nickname, 15, Utils.TEXT, Gravity.START), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(Utils.tv(this, "@" + c.username, 12, Utils.TEXT_DIM, Gravity.END));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmForward(c);
                }
            });
            forwardListContainer.addView(row);
            forwardListContainer.addView(Utils.divider(this));
        }
        if (!forwardGroups.isEmpty()) {
            TextView gh = Utils.tv(this, I18n.t("forward_to_group"), 13, Utils.ACCENT, Gravity.START);
            gh.setPadding(0, Utils.dp(this, 10), 0, Utils.dp(this, 4));
            forwardListContainer.addView(gh);
            for (final Models.Group g : forwardGroups) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(Utils.dp(this, 6), Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8));
                row.addView(Utils.letterAvatar(this, g.name, 40, 15));
                row.addView(Utils.vSpace(this, 10));
                row.addView(Utils.tv(this, g.name + " (" + g.memberCount + ")", 15, Utils.TEXT, Gravity.START), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmForwardGroup(g);
                    }
                });
                forwardListContainer.addView(row);
                forwardListContainer.addView(Utils.divider(this));
            }
        }
    }

    private void confirmForwardGroup(final Models.Group g) {
        try {
            JSONObject o = new JSONObject();
            o.put("type", "forward_group_message");
            o.put("group", g.id);
            o.put("messageId", forwardTarget.id);
            Net.get().send(o);
            Ui.popup(this, I18n.f("forwarded_ok", g.name));
        } catch (Exception ignored) {
        }
        if (forwardSheet != null && forwardSheet.isShowing()) forwardSheet.dismiss();
    }

    private void confirmForward(final Models.Contact c) {
        Ui.show(this, I18n.t("forward"), Ui.message(this, String.format(I18n.t("forward_confirm"), c.nickname, c.username)), I18n.t("send"), new Ui.Click() {
            @Override
            public void onClick() {
                try {
                    JSONObject o = new JSONObject();
                    if ("image".equals(forwardTarget.kind) || "file".equals(forwardTarget.kind)) {
                        o.put("type", "forward_message");
                        o.put("to", c.username);
                        o.put("id", forwardTarget.id);
                    } else {
                        o.put("type", "send_message");
                        o.put("to", c.username);
                        o.put("text", forwardTarget.text);
                    }
                    Net.get().send(o);
                    Ui.popup(ChatActivity.this, I18n.f("forwarded_ok", c.nickname));
                } catch (Exception ignored) {
                }
                if (forwardSheet != null && forwardSheet.isShowing()) forwardSheet.dismiss();
            }
        });
    }

    // ---------- 工具 ----------

    private boolean isDeleted(String id) {
        if (id == null || id.isEmpty()) return false;
        return localDel.getStringSet("deleted_" + peer, new HashSet<String>()).contains(id);
    }

    private String truncate8(String s) {
        if (s == null) return "";
        int count = 0, idx = 0;
        while (idx < s.length() && count < 8) {
            int cp = s.codePointAt(idx);
            idx += Character.charCount(cp);
            count++;
        }
        if (idx >= s.length()) return s;
        return s.substring(0, idx) + "...";
    }

    // ---------- Net.Listener ----------

    // ---------- 附件功能 ----------

    private View attachBtn(String icon, String label, View.OnClickListener onClick) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        TextView circle = Utils.tv(this, icon, 15, Utils.TEXT, Gravity.CENTER);
        circle.setTypeface(Typeface.DEFAULT_BOLD);
        int s = Utils.dp(this, 54);
        circle.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(Utils.CARD2);
        circle.setBackground(g);
        box.addView(circle);
        TextView lb = Utils.tv(this, label, 11, Utils.TEXT_DIM, Gravity.CENTER);
        lb.setPadding(0, Utils.dp(this, 4), 0, 0);
        box.addView(lb);
        box.setOnClickListener(onClick);
        return box;
    }

    private void toggleAttach() {
        boolean show = attachPanel.getVisibility() != View.VISIBLE;
        if (show) {
            attachPanel.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(getResources().getDisplayMetrics().widthPixels, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED));
            final int target = attachPanel.getMeasuredHeight();
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) attachPanel.getLayoutParams();
            lp.height = 0;
            attachPanel.setLayoutParams(lp);
            attachPanel.setVisibility(View.VISIBLE);
            android.animation.ValueAnimator va = android.animation.ValueAnimator.ofInt(0, target);
            final LinearLayout.LayoutParams alp = (LinearLayout.LayoutParams) attachPanel.getLayoutParams();
            va.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(android.animation.ValueAnimator animation) {
                    alp.height = (Integer) animation.getAnimatedValue();
                    attachPanel.setLayoutParams(alp);
                }
            });
            va.setDuration(200).start();
            plusBtn.animate().rotation(135).setDuration(200).start();
        } else {
            final LinearLayout.LayoutParams clp = (LinearLayout.LayoutParams) attachPanel.getLayoutParams();
            android.animation.ValueAnimator cva = android.animation.ValueAnimator.ofInt(clp.height, 0);
            cva.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(android.animation.ValueAnimator animation) {
                    clp.height = (Integer) animation.getAnimatedValue();
                    attachPanel.setLayoutParams(clp);
                }
            });
            cva.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    attachPanel.setVisibility(View.GONE);
                    clp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    attachPanel.setLayoutParams(clp);
                }
            });
            cva.setDuration(160).start();
            plusBtn.animate().rotation(0).setDuration(160).start();
        }
    }

    private void pickImage() {
        MediaPick.choose(this, PICK_IMAGE, new MediaPick.Callback() {
            @Override
            public void onBitmap(Bitmap bmp) {
                sendBitmap(bmp);
            }
        });
    }

    private void sendBitmap(Bitmap bmp) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
            bmp.recycle();
            JSONObject o = new JSONObject();
            o.put("type", "send_message");
            o.put("to", peer);
            o.put("kind", "image");
            o.put("data", b64);
            Net.get().send(o);
            Ui.popup(this, I18n.t("image_sending"));
            toggleAttach();
        } catch (Exception e) {
            Utils.toast(this, I18n.t("image_send_failed"));
        }
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        try {
            startActivityForResult(Intent.createChooser(i, I18n.t("choose_file")), PICK_FILE);
        } catch (Exception e) {
            Utils.toast(this, I18n.t("cannot_open_file_picker"));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (MediaPick.handleResult(this, requestCode, resultCode, data)) return;
        if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            sendFile(data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MediaPick.onPermissionResult(this, requestCode, grantResults);
    }

    private void sendImage(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (is != null) is.close();
            if (bmp == null) {
                Utils.toast(this, I18n.t("cannot_read_image"));
                return;
            }
            int target = 1024;
            int w = bmp.getWidth();
            int h = bmp.getHeight();
            if (w > target || h > target) {
                float scale = Math.min((float) target / w, (float) target / h);
                Bitmap scaled = Bitmap.createScaledBitmap(bmp, Math.max(1, Math.round(w * scale)), Math.max(1, Math.round(h * scale)), true);
                if (scaled != bmp) bmp.recycle();
                bmp = scaled;
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
            bmp.recycle();
            JSONObject o = new JSONObject();
            o.put("type", "send_message");
            o.put("to", peer);
            o.put("kind", "image");
            o.put("data", b64);
            Net.get().send(o);
            Ui.popup(this, I18n.t("image_sending"));
            toggleAttach();
        } catch (Exception e) {
            Utils.toast(this, I18n.t("image_send_failed"));
        }
    }

    private void sendFile(Intent data) {
        try {
            Uri uri = data.getData();
            String name = I18n.t("file");
            try {
                android.database.Cursor cur = getContentResolver().query(uri, null, null, null, null);
                if (cur != null && cur.moveToFirst()) {
                    int idx = cur.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) name = cur.getString(idx);
                    cur.close();
                }
            } catch (Exception ignored) {
            }
            InputStream is = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            long total = 0;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
                total += n;
                if (total > 5 * 1024 * 1024) break;
            }
            is.close();
            if (total > 5 * 1024 * 1024) {
                Utils.toast(this, I18n.t("file_too_large"));
                return;
            }
            String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
            JSONObject o = new JSONObject();
            o.put("type", "send_message");
            o.put("to", peer);
            o.put("kind", "file");
            o.put("data", b64);
            o.put("name", name);
            Net.get().send(o);
            Ui.popup(this, I18n.t("file_sending"));
            toggleAttach();
        } catch (Exception e) {
            Utils.toast(this, I18n.t("file_send_failed"));
        }
    }

    private void downloadFile(Models.ChatMsg m) {
        try {
            String[] sp = Session.serverParts();
            String url = "http://" + sp[0] + ":8900" + m.url;
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
            req.setTitle(m.name.isEmpty() ? I18n.t("file_download_title") : m.name);
            req.setDescription(I18n.t("downloading"));
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, m.name.isEmpty() ? "chatapp_file.bin" : m.name);
            dm.enqueue(req);
            Utils.toast(this, I18n.t("download_started"));
        } catch (Exception e) {
            Utils.toast(this, I18n.t("download_failed"));
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(java.util.Locale.getDefault(), "%.1f KB", bytes / 1024f);
        return String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / 1048576f);
    }

    // ---------- 图片预览 ----------

    private void showImagePreview(final Models.ChatMsg m) {
        previewDialog = new Dialog(this);
        previewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        final ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.MATRIX);
        root.addView(iv, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        final float[] scaleRef = {1f};
        final Matrix matrix = new Matrix();
        final ScaleGestureDetector sgd = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float ns = Math.max(1f, Math.min(6f, scaleRef[0] * detector.getScaleFactor()));
                float real = ns / scaleRef[0];
                scaleRef[0] = ns;
                matrix.postScale(real, real, detector.getFocusX(), detector.getFocusY());
                iv.setImageMatrix(matrix);
                return true;
            }
        });
        iv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                sgd.onTouchEvent(event);
                return true;
            }
        });

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setBackgroundColor(Utils.CARD);
        bar.setPadding(Utils.dp(this, 8), Utils.dp(this, 10), Utils.dp(this, 8), Utils.dp(this, 10));
        Button fwd = previewBtn(I18n.t("forward"));
        fwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forwardImageFromPreview(m);
            }
        });
        bar.addView(fwd);
        bar.addView(Utils.vSpace(this, 14));
        Button save = previewBtn(I18n.t("save"));
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImage(m);
            }
        });
        bar.addView(save);
        bar.addView(Utils.vSpace(this, 14));
        Button close = previewBtn(I18n.t("close"));
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (previewDialog != null) previewDialog.dismiss();
            }
        });
        bar.addView(close);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        blp.gravity = Gravity.BOTTOM;
        root.addView(bar, blp);

        previewDialog.setContentView(root);
        Window w = previewDialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        previewDialog.show();

        AvatarManager.loadBitmap(this, m.url, new AvatarManager.BitmapCallback() {
            @Override
            public void onBitmap(Bitmap bmp) {
                if (bmp != null && previewDialog != null && previewDialog.isShowing()) {
                    previewBitmap = bmp;
                    iv.setImageBitmap(bmp);
                    float vw = getResources().getDisplayMetrics().widthPixels;
                    float vh = getResources().getDisplayMetrics().heightPixels;
                    float s = Math.min(vw / bmp.getWidth(), vh / bmp.getHeight());
                    if (s > 3f) s = 3f;
                    scaleRef[0] = s;
                    matrix.reset();
                    matrix.postScale(s, s);
                    matrix.postTranslate((vw - bmp.getWidth() * s) / 2f, (vh - bmp.getHeight() * s) / 2f);
                    iv.setImageMatrix(matrix);
                }
            }
        });
    }

    private Button previewBtn(String text) {
        Button b = Utils.button(this);
        b.setText(text);
        b.setTextSize(14);
        b.setTextColor(Utils.TEXT);
        b.setBackground(Utils.bg(this, Utils.CARD2, 18));
        b.setPadding(Utils.dp(this, 18), Utils.dp(this, 6), Utils.dp(this, 18), Utils.dp(this, 6));
        return b;
    }

    private void forwardImageFromPreview(Models.ChatMsg m) {
        pendingForward = m;
        try {
            JSONObject o = new JSONObject();
            o.put("type", "get_contacts");
            Net.get().send(o);
            Ui.popup(this, I18n.t("choose_forward_friend"));
        } catch (Exception ignored) {
        }
    }

    private void sendImageTo(String username) {
        try {
            if (previewBitmap == null) {
                Utils.toast(this, I18n.t("image_not_ready"));
                return;
            }
            int target = 1024;
            Bitmap bmp = previewBitmap;
            int w = bmp.getWidth();
            int h = bmp.getHeight();
            if (w > target || h > target) {
                float s = Math.min((float) target / w, (float) target / h);
                bmp = Bitmap.createScaledBitmap(bmp, Math.round(w * s), Math.round(h * s), true);
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
            JSONObject o = new JSONObject();
            o.put("type", "send_message");
            o.put("to", username);
            o.put("kind", "image");
            o.put("data", b64);
            Net.get().send(o);
            Utils.toast(this, I18n.t("forwarded"));
        } catch (Exception e) {
            Utils.toast(this, I18n.t("forward_failed"));
        }
    }

    private void saveImage(Models.ChatMsg m) {
        try {
            String[] sp = Session.serverParts();
            String url = "http://" + sp[0] + ":8900" + m.url;
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
            req.setTitle(I18n.t("image"));
            req.setDescription(I18n.t("saving"));
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "chatapp_img_" + m.id + ".jpg");
            dm.enqueue(req);
            Utils.toast(this, I18n.t("saving_image"));
        } catch (Exception e) {
            Utils.toast(this, I18n.t("save_failed"));
        }
    }

    private List<Models.Contact> forwardContacts = new ArrayList<>();
    private List<Models.Group> forwardGroups = new ArrayList<>();

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

    private void handle(JSONObject o) {
        String type = o.optString("type");
        if ("history".equals(type) && peer.equals(o.optString("with"))) {
            messages.clear();
            List<Models.ChatMsg> list = Models.parseMessages(o.optJSONArray("messages"), Session.username);
            for (Models.ChatMsg m : list) {
                if (isDeleted(m.id)) continue;
                messages.add(m);
            }
            peerReadTime = o.optLong("peerReadTime", 0);
            rerender();
            if (loading != null) loading.setVisibility(View.GONE);
        } else if ("new_message".equals(type)) {
            String from = o.optString("from");
            if (from.equals(peer)) {
                Models.ChatMsg m = Models.ChatMsg.fromJson(o, Session.username);
                if (!isDeleted(m.id)) appendMsg(m);
                markRead();
            }
            else if (from.equals(Session.username) && peer.equals(o.optString("to"))) {
                String kind = o.optString("kind", "text");
                if ("image".equals(kind) || "file".equals(kind)) {
                    Models.ChatMsg m = Models.ChatMsg.fromJson(o, Session.username);
                    if (!isDeleted(m.id)) appendMsg(m);
                }
            }
        } else if ("message_recalled".equals(type)) {
            String id = o.optString("id");
            for (int i = messages.size() - 1; i >= 0; i--) {
                if (messages.get(i).id != null && messages.get(i).id.equals(id)) {
                    messages.remove(i);
                    break;
                }
            }
            rerender();
        } else if ("peer_read".equals(type)) {
            if (peer.equals(o.optString("with"))) {
                peerReadTime = o.optLong("time");
                rerender();
            }
        } else if ("peer_typing".equals(type)) {
            if (peer.equals(o.optString("from"))) {
                typingText.setVisibility(View.VISIBLE);
                typingHide.removeCallbacks(typingHideTask);
                typingHide.postDelayed(typingHideTask, 3000);
            }
        } else if ("groups".equals(type)) {
            forwardGroups = Models.parseGroups(o.optJSONArray("groups"));
            if (forwardSheet != null && forwardSheet.isShowing()) renderForwardList();
        } else if ("contacts".equals(type)) {
            forwardContacts = Models.parseContacts(o.optJSONArray("contacts"));
            if (forwardSheet != null && forwardSheet.isShowing()) renderForwardList();
            if (pendingMultiForward != null) {
                List<Models.ChatMsg> pf = pendingMultiForward;
                pendingMultiForward = null;
                showMultiForwardList(pf);
            }
            if (pendingForward != null) {
                final Models.ChatMsg pm = pendingForward;
                pendingForward = null;
                if (forwardContacts.isEmpty()) {
                    Utils.toast(this, I18n.t("no_friends_yet"));
                } else {
                    String[] names = new String[forwardContacts.size()];
                    final Ui.Click[] clicks = new Ui.Click[forwardContacts.size()];
                    for (int i = 0; i < forwardContacts.size(); i++) {
                        final Models.Contact c = forwardContacts.get(i);
                        names[i] = c.nickname + " (@" + c.username + ")";
                        clicks[i] = new Ui.Click() {
                            @Override
                            public void onClick() {
                                sendImageTo(c.username);
                            }
                        };
                    }
                    Ui.list(this, I18n.t("forward_to"), names, clicks);
                }
            }
        } else if ("conversations_changed".equals(type)) {
            // 无需处理, 聊天列表由主界面刷新
        } else if ("error".equals(type)) {
            if (loading != null) loading.setVisibility(View.GONE);
            Utils.toast(this, I18n.serverMsg(o.optString("message")));
        } else if ("toast".equals(type)) {
            if (loading != null) loading.setVisibility(View.GONE);
            Utils.toast(this, I18n.serverMsg(o.optString("message")));
        }
    }

    @Override
    public void onDisconnected(String reason) {
    }
}
