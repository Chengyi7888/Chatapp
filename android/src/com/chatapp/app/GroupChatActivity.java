package com.chatapp.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 群聊窗口: 与单聊一致的聊天体验 (气泡/图片/文件/撤回/回复/长按操作) */
public class GroupChatActivity extends Activity implements Net.Listener {

    private static final int PICK_IMAGE = 3001;
    private static final int PICK_FILE = 3002;

    private String groupId;
    private String groupName;
    private String groupNotice = "";
    private boolean isOwner = false;

    private LinearLayout msgList;
    private ScrollView scroll;
    private EditText input;
    private List<Models.ChatMsg> messages = new ArrayList<>();
    private SharedPreferences localDel;
    private LinearLayout replyBar;
    private LinearLayout attachPanel;
    private View loading;
    private Dialog atLoadingDialog;
    private TextView plusBtn;
    private TextView replyBarText;
    private String pendingReply = "";
    private TextView noticeBar;
    private TextView typingText;
    private final Handler typingHide = new Handler(Looper.getMainLooper());
    private Dialog forwardSheet;
    private LinearLayout forwardListContainer;
    private Models.ChatMsg forwardTarget;
    private List<Models.Contact> forwardContacts = new ArrayList<>();
    private List<Models.Group> forwardGroups = new ArrayList<>();
    private boolean multiSelect = false;
    private final Set<String> selectedIds = new HashSet<>();
    private LinearLayout selectBar;
    private TextView selectCountText;
    private List<Models.ChatMsg> pendingMultiForward = null;
    private List<Models.GroupMember> groupMembers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setBackgroundColor(Utils.BG);
        Theme.init(this);
        Theme.apply(this);
        localDel = getSharedPreferences("chatapp_group_del", MODE_PRIVATE);
        Intent it = getIntent();
        groupId = it.getStringExtra("groupId");
        groupName = it.getStringExtra("groupName");
        isOwner = it.getBooleanExtra("isOwner", false);
        if (groupId == null) groupId = "";
        if (groupName == null) groupName = groupId;
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
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        TextView title = Utils.tv(this, groupName, 17, Utils.TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        titleBox.addView(title);
        TextView sub = Utils.tv(this, isOwner ? I18n.t("owner") : I18n.t("group_chat"), 11, Utils.TEXT_DIM, Gravity.START);
        titleBox.addView(sub);
        top.addView(titleBox, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView infoBtn = Utils.tv(this, "\u22EE", 22, Utils.TEXT, Gravity.CENTER);
        infoBtn.setPadding(Utils.dp(this, 10), 0, Utils.dp(this, 4), 0);
        infoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDetail();
            }
        });
        top.addView(infoBtn);
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        topLp.setMargins(Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8), 0);
        root.addView(top, topLp);

        // 群公告条
        noticeBar = Utils.tv(this, groupNotice.isEmpty() ? "" : "📢 " + groupNotice, 12, Utils.ACCENT, Gravity.START);
        noticeBar.setPadding(Utils.dp(this, 14), Utils.dp(this, 7), Utils.dp(this, 14), Utils.dp(this, 7));
        noticeBar.setBackground(Utils.bg(this, Utils.CARD, 18));
        noticeBar.setSingleLine(true);
        noticeBar.setEllipsize(TextUtils.TruncateAt.END);
        if (groupNotice.isEmpty()) noticeBar.setVisibility(View.GONE);
        root.addView(noticeBar);

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

        // 附件面板
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
                Ui.popup(GroupChatActivity.this, I18n.t("more_coming"));
            }
        }));

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setBackground(Utils.bg(this, Utils.CARD, 18));
        inputRow.setPadding(Utils.dp(this, 6), Utils.dp(this, 5), Utils.dp(this, 6), Utils.dp(this, 5));

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
        final SharedPreferences draftPrefs = getSharedPreferences("chatapp_drafts", MODE_PRIVATE);
        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                draftPrefs.edit().putString("draft_" + groupId, s.toString()).apply();
                if (s.length() > 0 && Net.get().isConnected()) {
                    try {
                        JSONObject o = new JSONObject();
                        o.put("type", "typing");
                        o.put("with", groupId);
                        Net.get().send(o);
                    } catch (Exception ignored) {
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        inputRow.addView(input, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView atBtn = Utils.tv(this, "@", 16, Utils.ACCENT, Gravity.CENTER);
        atBtn.setBackground(Utils.bg(this, Utils.CARD2, 14));
        atBtn.setPadding(Utils.dp(this, 10), Utils.dp(this, 6), Utils.dp(this, 10), Utils.dp(this, 6));
        atBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAtPicker();
            }
        });
        inputRow.addView(Utils.vSpace(this, 6));
        inputRow.addView(atBtn);
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
            refreshGroupInfo();
            requestMembers();
        }
        try {
            JSONObject o = new JSONObject();
            o.put("type", "mark_group_read");
            o.put("id", groupId);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
        String d = getSharedPreferences("chatapp_drafts", MODE_PRIVATE).getString("draft_" + groupId, "");
        if (d != null && !d.isEmpty()) {
            input.setText(d);
            input.setSelection(d.length());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Net.get().setListener(null);
    }

    private final Runnable typingHideTask = new Runnable() {
        @Override
        public void run() {
            typingText.setVisibility(View.GONE);
        }
    };

    private void refreshGroupInfo() {
        try {
            JSONObject o = new JSONObject();
            o.put("type", "get_groups");
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    private void requestMembers() {
        try {
            JSONObject o = new JSONObject();
            o.put("type", "get_group_members");
            o.put("id", groupId);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    private void requestHistory() {
        if (!Net.get().isConnected()) return;
        try {
            JSONObject o = new JSONObject();
            o.put("type", "get_group_history");
            o.put("id", groupId);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    private void openDetail() {
        Intent i = new Intent(this, GroupDetailActivity.class);
        i.putExtra("groupId", groupId);
        i.putExtra("groupName", groupName);
        startActivity(i);
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
            Set<String> set = new HashSet<>(localDel.getStringSet("deleted_" + groupId, new HashSet<String>()));
            set.add(m.id);
            localDel.edit().putStringSet("deleted_" + groupId, set).apply();
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
                    Ui.popup(GroupChatActivity.this, I18n.f("forwarded_ok", c.nickname));
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
        if (forwardContacts.isEmpty()) {
            forwardListContainer.addView(Utils.tv(this, I18n.t("no_friends_hint"), 14, Utils.TEXT_DIM, Gravity.CENTER));
            return;
        }
        for (final Models.Contact c : forwardContacts) {
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

    private void favoriteMsg(Models.ChatMsg m) {
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
                            o.put("type", "forward_group_message");
                            o.put("group", groupId);
                            o.put("messageId", m.id);
                        } else {
                            o.put("type", "send_group_message");
                            o.put("id", groupId);
                            o.put("text", m.text);
                        }
                        Net.get().send(o);
                        Ui.popup(GroupChatActivity.this, I18n.t("send"));
                    } catch (Exception ignored) {
                    }
                }
            };
        }
        Ui.list(this, I18n.t("favorite"), items.toArray(new String[0]), clicks);
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
        Ui.show(this, I18n.t("forward_to"), Ui.message(this, I18n.f("forward_confirm", c.nickname, c.username)), I18n.t("send"), new Ui.Click() {
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
                    Ui.popup(GroupChatActivity.this, I18n.f("forwarded_ok", c.nickname));
                } catch (Exception ignored) {
                }
                if (forwardSheet != null && forwardSheet.isShowing()) forwardSheet.dismiss();
            }
        });
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
            o.put("type", "send_group_message");
            o.put("id", groupId);
            o.put("text", text);
            if (!reply.isEmpty()) {
                JSONObject r = new JSONObject();
                r.put("text", reply);
                o.put("reply", r);
            }
            Net.get().send(o);
        } catch (Exception ignored) {
        }
        Models.ChatMsg m = new Models.ChatMsg(Session.username, groupId, text, System.currentTimeMillis(), true);
        m.id = "local_" + System.currentTimeMillis() + "_" + Math.abs(text.hashCode());
        m.reply = reply.isEmpty() ? "" : reply;
        m.nickname = Session.nickname;
        appendMsg(m);
    }

    private void showAtPicker() {
        if (groupMembers.isEmpty()) {
            requestMembers();
            LinearLayout lb = new LinearLayout(this);
            lb.setOrientation(LinearLayout.VERTICAL);
            lb.setGravity(Gravity.CENTER);
            lb.setPadding(Utils.dp(this, 30), Utils.dp(this, 30), Utils.dp(this, 30), Utils.dp(this, 30));
            lb.addView(new TriangleLoadingView(this));
            atLoadingDialog = Ui.showSingle(this, I18n.t("loading"), lb, I18n.t("close"), null);
            return;
        }
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(Utils.bg(this, Utils.CARD, 20));
        root.setPadding(Utils.dp(this, 18), Utils.dp(this, 16), Utils.dp(this, 18), Utils.dp(this, 14));
        TextView title = Utils.tv(this, "@", 18, Utils.TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);
        root.addView(Utils.hSpace(this, 10));

        TextView all = Utils.tv(this, I18n.t("at_all"), 15, Utils.ACCENT, Gravity.CENTER_VERTICAL);
        all.setPadding(Utils.dp(this, 8), Utils.dp(this, 12), Utils.dp(this, 8), Utils.dp(this, 12));
        all.setBackground(Utils.bg(this, Utils.CARD2, 14));
        all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertMention(I18n.t("at_all") + " ");
                d.dismiss();
            }
        });
        root.addView(all);
        root.addView(Utils.hSpace(this, 8));

        final List<CheckBox> checks = new ArrayList<>();
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        for (final Models.GroupMember m : groupMembers) {
            if (Session.username.equals(m.username)) continue;
            CheckBox cb = new CheckBox(this);
            cb.setText(m.nickname + " (@" + m.username + ")");
            cb.setTextSize(14);
            cb.setTextColor(Utils.TEXT);
            cb.setButtonTintList(android.content.res.ColorStateList.valueOf(Utils.ACCENT));
            cb.setPadding(Utils.dp(this, 6), Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8));
            cb.setTag(m);
            checks.add(cb);
            list.addView(cb);
        }
        ScrollView sc = new ScrollView(this);
        sc.addView(list);
        sc.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Utils.dp(this, 260)));
        root.addView(sc);
        root.addView(Utils.hSpace(this, 10));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button cancel = Utils.button(this);
        cancel.setText(I18n.t("cancel"));
        cancel.setTextColor(Utils.TEXT);
        cancel.setBackground(Utils.bg(this, Utils.CARD2, 20));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        Button ok = Utils.button(this);
        ok.setText(I18n.t("confirm"));
        ok.setTextColor(Color.WHITE);
        ok.setBackground(Utils.bg(this, Utils.ACCENT, 20));
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder sb = new StringBuilder();
                for (CheckBox cb : checks) {
                    if (cb.isChecked()) {
                        Models.GroupMember m = (Models.GroupMember) cb.getTag();
                        if (m != null) sb.append("@").append(m.nickname).append(" ");
                    }
                }
                if (sb.length() > 0) insertMention(sb.toString());
                d.dismiss();
            }
        });
        row.addView(cancel, Utils.lp(0, Utils.dp(this, 42), 1f));
        row.addView(Utils.vSpace(this, 10));
        row.addView(ok, Utils.lp(0, Utils.dp(this, 42), 1f));
        root.addView(row);

        d.setContentView(root);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.88f), WindowManager.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.CENTER);
        }
        d.show();
    }

    private void insertMention(String text) {
        int pos = Math.max(0, input.getSelectionStart());
        Editable e = input.getText();
        e.insert(pos, text);
        input.setSelection(pos + text.length());
    }

    private void appendMsg(Models.ChatMsg m) {
        messages.add(m);
        addBubble(m);
    }

    private void rerender() {
        msgList.removeAllViews();
        for (Models.ChatMsg m : messages) {
            if (isDeleted(m.id)) continue;
            addBubble(m);
        }
    }

    private void addBubble(final Models.ChatMsg m) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(m.mine ? Gravity.END : Gravity.START);
        row.setPadding(Utils.dp(this, 4), Utils.dp(this, 2), Utils.dp(this, 4), Utils.dp(this, 2));

        // 非本人消息，左侧头像
        if (!m.mine) {
            String sender = (m.nickname != null && !m.nickname.isEmpty()) ? m.nickname : m.from;
            row.addView(AvatarManager.avatarView(this, m.from, sender, m.senderHasAvatar, 36, 14));
            row.addView(Utils.vSpace(this, 8));
        }

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(m.mine ? Gravity.END : Gravity.START);

                if (!m.mine) {
            String sender = (m.nickname != null && !m.nickname.isEmpty()) ? m.nickname : m.from;
            TextView sn = Utils.tv(this, sender, 11, Utils.TEXT_DIM, Gravity.START);
            sn.setPadding(Utils.dp(this, 6), 0, Utils.dp(this, 6), Utils.dp(this, 2));
            col.addView(sn);
        }

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
                    else MediaHelper.showImagePreview(GroupChatActivity.this, m, null);
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
                    MediaHelper.downloadFile(GroupChatActivity.this, m);
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

    private void showActionMenu(final Models.ChatMsg m) {
        final List<String> items = new ArrayList<>();
        items.add(I18n.t("copy"));
        items.add(I18n.t("delete"));
        if (m.mine && System.currentTimeMillis() - m.time <= 120000) {
            items.add(I18n.t("recall"));
        }
        items.add(I18n.t("forward"));
        items.add(I18n.t("multi_select"));
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
        Set<String> set = new HashSet<>(localDel.getStringSet("deleted_" + groupId, new HashSet<String>()));
        set.add(m.id);
        localDel.edit().putStringSet("deleted_" + groupId, set).apply();
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
            o.put("type", "recall_group_message");
            o.put("id", groupId);
            o.put("messageId", m.id);
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

    private boolean isDeleted(String id) {
        if (id == null || id.isEmpty()) return false;
        return localDel.getStringSet("deleted_" + groupId, new HashSet<String>()).contains(id);
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

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(java.util.Locale.getDefault(), "%.1f KB", bytes / 1024f);
        return String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / 1048576f);
    }

    // ---------- 附件 ----------

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
                sendGroupBitmap(bmp);
            }
        });
    }

    private void sendGroupBitmap(Bitmap bmp) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
            bmp.recycle();
            JSONObject o = new JSONObject();
            o.put("type", "send_group_message");
            o.put("id", groupId);
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
            sendFile(data.getData());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MediaPick.onPermissionResult(this, requestCode, grantResults);
    }

    private void sendImage(Uri uri) {
        MediaHelper.encodeAndSend(this, uri, "image", new MediaHelper.SendCallback() {
            @Override
            public void onEncoded(byte[] data, String name) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "send_group_message");
                    o.put("id", groupId);
                    o.put("kind", "image");
                    o.put("data", MediaHelper.base64(data));
                    o.put("name", name);
                    Net.get().send(o);
                    Ui.popup(GroupChatActivity.this, I18n.t("image_sending"));
                    toggleAttach();
                } catch (Exception e) {
                    Utils.toast(GroupChatActivity.this, I18n.t("image_send_failed"));
                }
            }
        });
    }

    private void sendFile(Uri uri) {
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
        final String fname = name;
        MediaHelper.encodeAndSend(this, uri, "file", new MediaHelper.SendCallback() {
            @Override
            public void onEncoded(byte[] data, String name) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "send_group_message");
                    o.put("id", groupId);
                    o.put("kind", "file");
                    o.put("data", MediaHelper.base64(data));
                    o.put("name", fname);
                    Net.get().send(o);
                    Ui.popup(GroupChatActivity.this, I18n.t("file_sending"));
                    toggleAttach();
                } catch (Exception e) {
                    Utils.toast(GroupChatActivity.this, I18n.t("file_send_failed"));
                }
            }
        });
    }

    // ---------- 网络 ----------

    @Override
    public void onConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                requestHistory();
                refreshGroupInfo();
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
        if ("group_history".equals(type) && groupId.equals(o.optString("id"))) {
            messages.clear();
            List<Models.ChatMsg> list = Models.parseMessages(o.optJSONArray("messages"), Session.username);
            for (Models.ChatMsg m : list) {
                if (isDeleted(m.id)) continue;
                messages.add(m);
            }
            rerender();
            if (loading != null) loading.setVisibility(View.GONE);
        } else if ("group_members".equals(type) && groupId.equals(o.optString("id"))) {
            groupMembers = Models.parseMembers(o.optJSONArray("members"));
            if (atLoadingDialog != null && atLoadingDialog.isShowing()) {
                try {
                    atLoadingDialog.dismiss();
                } catch (Exception ignored) {
                }
                atLoadingDialog = null;
                showAtPicker();
            }
        } else if ("new_group_message".equals(type)) {
            if (groupId.equals(o.optString("group"))) {
                Models.ChatMsg m = Models.ChatMsg.fromJson(o, Session.username);
                m.to = groupId;
                // 自己的文本消息已乐观显示, 若与刚发的本地消息相同则跳过回显, 避免重复
                if (m.mine && !"image".equals(m.kind) && !"file".equals(m.kind)) {
                    boolean dup = false;
                    for (Models.ChatMsg x : messages) {
                        if (x.mine && x.text != null && x.text.equals(m.text)
                                && Math.abs(x.time - m.time) < 3000) { dup = true; break; }
                    }
                    if (dup) return;
                }
                if (!isDeleted(m.id)) appendMsg(m);
            }
        } else if ("group_message_recalled".equals(type)) {
            if (groupId.equals(o.optString("group"))) {
                String id = o.optString("id");
                for (int i = messages.size() - 1; i >= 0; i--) {
                    if (messages.get(i).id != null && messages.get(i).id.equals(id)) {
                        messages.remove(i);
                        break;
                    }
                }
                rerender();
            }
        } else if ("group_at_all".equals(type)) {
            String from = o.optString("nickname", o.optString("from"));
            Ui.popup(this, I18n.f("at_all_msg", from));
        } else if ("peer_typing".equals(type)) {
            String from = o.optString("from");
            if (!from.equals(Session.username)) {
                typingText.setVisibility(View.VISIBLE);
                typingHide.removeCallbacks(typingHideTask);
                typingHide.postDelayed(typingHideTask, 3000);
            }
        } else if ("group_history_cleared".equals(type)) {
            if (groupId.equals(o.optString("id"))) {
                messages.clear();
                rerender();
            }
        } else if ("group_changed".equals(type)) {
            Models.Group g = Models.Group.fromJson(o.optJSONObject("group"));
            if (g.id != null && g.id.equals(groupId)) {
                groupName = g.name;
                groupNotice = g.notice;
                noticeBar.setText(groupNotice.isEmpty() ? "" : "📢 " + groupNotice);
                noticeBar.setVisibility(groupNotice.isEmpty() ? View.GONE : View.VISIBLE);
            }
        } else if ("group_dissolved".equals(type)) {
            Models.Group g = Models.Group.fromJson(o.optJSONObject("group"));
            if (g.id != null && g.id.equals(groupId)) {
                Utils.toast(this, I18n.t("group_dissolved"));
                finish();
            }
        } else if ("group_kicked".equals(type)) {
            Models.Group g = Models.Group.fromJson(o.optJSONObject("group"));
            if (g.id != null && g.id.equals(groupId)) {
                Utils.toast(this, I18n.t("you_kicked"));
                finish();
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
