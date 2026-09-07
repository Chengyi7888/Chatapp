package com.chatapp.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** 群详�?管理�? 群资料、成员、群主权�?拉人/踢人/转让/公告/解散) */
public class GroupDetailActivity extends Activity implements Net.Listener {

    private static final int PICK_AVATAR = 4001;

    private String groupId;
    private Models.Group group;
    private List<Models.Contact> contacts = new ArrayList<>();
    private LinearLayout memberList;
    private LinearLayout ownerPanel;
    private View loading;
    private View rootView;
    private boolean groupLoaded = false;
    private boolean membersLoaded = false;
    private View dissolveRow;
    private TextView nameText, countText, noticeText;
    private LinearLayout avatarBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setBackgroundColor(Utils.BG);
        Theme.init(this);
        Theme.apply(this);
        groupId = getIntent().getStringExtra("groupId");
        if (groupId == null) groupId = "";
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
        TextView back = Utils.tv(this, "<", 26, Utils.TEXT, Gravity.CENTER);
        back.setPadding(Utils.dp(this, 10), 0, Utils.dp(this, 10), Utils.dp(this, 4));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        top.addView(back);
        TextView title = Utils.tv(this, I18n.t("group_info"), 17, Utils.TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title);
        root.addView(top, topLp);

        ScrollView sc = new ScrollView(this);
        sc.setBackgroundColor(Utils.BG);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Utils.dp(this, 18), Utils.dp(this, 20), Utils.dp(this, 18), Utils.dp(this, 20));
        body.setBackgroundColor(Utils.BG);

        // 群头�?+ 名称
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setGravity(Gravity.CENTER_HORIZONTAL);
        avatarBox = new LinearLayout(this);
        avatarBox.setOrientation(LinearLayout.VERTICAL);
        avatarBox.setGravity(Gravity.CENTER_HORIZONTAL);
        avatarBox.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        head.addView(avatarBox);
        head.addView(Utils.hSpace(this, 12));
        nameText = Utils.tv(this, "", 22, Utils.TEXT, Gravity.CENTER);
        nameText.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(nameText);
        countText = Utils.tv(this, "", 13, Utils.TEXT_DIM, Gravity.CENTER);
        countText.setPadding(0, Utils.dp(this, 4), 0, 0);
        head.addView(countText);
        body.addView(head);
        body.addView(Utils.hSpace(this, 18));

                LinearLayout noticeCard = new LinearLayout(this);
        noticeCard.setOrientation(LinearLayout.VERTICAL);
        noticeCard.setBackground(Utils.bg(this, Utils.CARD, 16));
        noticeCard.setPadding(Utils.dp(this, 16), Utils.dp(this, 12), Utils.dp(this, 16), Utils.dp(this, 12));
        TextView noticeTitle = Utils.tv(this, I18n.t("group_notice"), 14, Utils.TEXT, Gravity.START);
        noticeTitle.setTypeface(Typeface.DEFAULT_BOLD);
        noticeCard.addView(noticeTitle);
        noticeText = Utils.tv(this, "", 13, Utils.TEXT_DIM, Gravity.START);
        noticeText.setPadding(0, Utils.dp(this, 6), 0, 0);
        noticeCard.addView(noticeText);
        body.addView(noticeCard);
        body.addView(Utils.hSpace(this, 16));

        // 成员列表
        LinearLayout memberCard = new LinearLayout(this);
        memberCard.setOrientation(LinearLayout.VERTICAL);
        memberCard.setBackground(Utils.bg(this, Utils.CARD, 16));
        memberCard.setPadding(Utils.dp(this, 16), Utils.dp(this, 12), Utils.dp(this, 16), Utils.dp(this, 12));
        TextView memberTitle = Utils.tv(this, I18n.t("group_members"), 14, Utils.TEXT, Gravity.START);
        memberTitle.setTypeface(Typeface.DEFAULT_BOLD);
        memberCard.addView(memberTitle);
        memberList = new LinearLayout(this);
        memberList.setOrientation(LinearLayout.VERTICAL);
        memberList.setPadding(0, Utils.dp(this, 6), 0, 0);
        memberCard.addView(memberList);
        body.addView(memberCard);
        body.addView(Utils.hSpace(this, 16));

        // 群主操作面板
        ownerPanel = new LinearLayout(this);
        ownerPanel.setOrientation(LinearLayout.VERTICAL);
        ownerPanel.setBackground(Utils.bg(this, Utils.CARD, 16));
        ownerPanel.addView(actionRow(I18n.t("edit_notice"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editNotice();
            }
        }));
        ownerPanel.addView(Utils.divider(this));
        ownerPanel.addView(actionRow(I18n.t("edit_group_name"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editName();
            }
        }));
        ownerPanel.addView(Utils.divider(this));
        ownerPanel.addView(actionRow(I18n.t("change_group_avatar"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickAvatar();
            }
        }));
        ownerPanel.addView(Utils.divider(this));
        ownerPanel.addView(actionRow(I18n.t("invite"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openInvite();
            }
        }));
        body.addView(ownerPanel);
        body.addView(Utils.hSpace(this, 16));

                LinearLayout historyCard = new LinearLayout(this);
        historyCard.setOrientation(LinearLayout.VERTICAL);
        historyCard.setBackground(Utils.bg(this, Utils.CARD, 16));
        historyCard.addView(actionRow(I18n.t("clear_group_history"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmClearHistory();
            }
        }));
        body.addView(historyCard);
        body.addView(Utils.hSpace(this, 16));

        // 退�?解散
        LinearLayout dangerCard = new LinearLayout(this);
        dangerCard.setOrientation(LinearLayout.VERTICAL);
        dangerCard.setBackground(Utils.bg(this, Utils.CARD, 16));
        dangerCard.addView(actionRow(I18n.t("leave_group"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmLeave();
            }
        }));
        dangerCard.addView(Utils.divider(this));
        dissolveRow = actionRow(I18n.t("dissolve_group"), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDissolve();
            }
        });
        dangerCard.addView(dissolveRow);
        body.addView(dangerCard);
        body.addView(Utils.hSpace(this, 16));

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

    private View actionRow(String label, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Utils.dp(this, 16), Utils.dp(this, 14), Utils.dp(this, 16), Utils.dp(this, 14));
        row.setOnClickListener(onClick);
        row.addView(Utils.tv(this, label, 15, Utils.TEXT, Gravity.START), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Net.get().setListener(this);
        refresh();
        requestMembers();
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

    private void refresh() {
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

    private void checkLoaded() {
        if (groupLoaded && membersLoaded) {
            if (loading != null) loading.setVisibility(View.GONE);
            if (rootView != null) rootView.setVisibility(View.VISIBLE);
        }
    }
    private void renderGroup() {
        if (group == null) return;
        nameText.setText(group.name);
        countText.setText(String.format(I18n.t("members_count"), group.memberCount));
        noticeText.setText(group.notice.isEmpty() ? I18n.t("no_notice") : group.notice);
        boolean owner = group.owner.equals(Session.username);
        ownerPanel.setVisibility(owner ? View.VISIBLE : View.GONE);
        if (dissolveRow != null) dissolveRow.setVisibility(owner ? View.VISIBLE : View.GONE);
        avatarBox.removeAllViews();
        final View av = group.hasAvatar
                ? AvatarManager.avatarView(this, "g_" + group.id, group.name, true, 72, 28)
                : Utils.letterAvatar(this, group.name, 72, 28);
        av.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (group != null && group.owner.equals(Session.username)) {
                    pickAvatar();
                }
            }
        });
        avatarBox.addView(av);
    }

    // ---------- 成员 ----------

    private void renderMembers(List<Models.GroupMember> members) {
        memberList.removeAllViews();
        for (final Models.GroupMember m : members) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, Utils.dp(this, 7), 0, Utils.dp(this, 7));
            final View memAv = AvatarManager.avatarView(this, m.username, m.nickname, m.hasAvatar, 40, 15);
            memAv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showMemberCard(m);
                }
            });
            row.addView(memAv);
            row.addView(Utils.vSpace(this, 10));
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.addView(Utils.tv(this, m.nickname, 15, Utils.TEXT, Gravity.START));
            String sub = m.online ? I18n.t("online") : I18n.t("offline");
            info.addView(Utils.tv(this, m.isOwner ? I18n.t("owner") + " · " + sub : sub, 11, m.isOwner ? Utils.ACCENT : Utils.TEXT_DIM, Gravity.START));
            row.addView(info, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            if (group != null && group.owner.equals(Session.username) && !m.isOwner) {
                TextView kick = Utils.tv(this, I18n.t("remove"), 13, Utils.DANGER, Gravity.CENTER);
                kick.setBackground(Utils.bg(this, Utils.CARD2, 14));
                kick.setPadding(Utils.dp(this, 12), Utils.dp(this, 5), Utils.dp(this, 12), Utils.dp(this, 5));
                kick.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmKick(m);
                    }
                });
                row.addView(kick);
                row.addView(Utils.vSpace(this, 8));
                TextView transfer = Utils.tv(this, I18n.t("transfer"), 13, Utils.ACCENT, Gravity.CENTER);
                transfer.setBackground(Utils.bg(this, Utils.CARD2, 14));
                transfer.setPadding(Utils.dp(this, 12), Utils.dp(this, 5), Utils.dp(this, 12), Utils.dp(this, 5));
                transfer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmTransfer(m);
                    }
                });
                row.addView(transfer);
            }
            memberList.addView(row);
            memberList.addView(Utils.divider(this));
        }
    }

    private void confirmKick(final Models.GroupMember m) {
        Ui.show(this, I18n.t("kick"), Ui.message(this, String.format(I18n.t("kick_confirm"), m.nickname)), I18n.t("kick"), new Ui.Click() {
            @Override
            public void onClick() {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "kick_group");
                    o.put("id", groupId);
                    o.put("username", m.username);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void confirmTransfer(final Models.GroupMember m) {
        Ui.show(this, I18n.t("transfer_owner"), Ui.message(this, String.format(I18n.t("transfer_confirm"), m.nickname)), I18n.t("transfer"), new Ui.Click() {
            @Override
            public void onClick() {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "transfer_owner");
                    o.put("id", groupId);
                    o.put("username", m.username);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void openInvite() {
        Intent i = new Intent(this, GroupInviteActivity.class);
        i.putExtra("groupId", groupId);
        ArrayList<String> ms = new ArrayList<>();
        if (group != null && group.members != null) ms.addAll(group.members);
        i.putExtra("members", ms);
        startActivity(i);
    }

    private void showInviteDialog() {
        if (contacts.isEmpty()) {
            Utils.toast(this, I18n.t("no_friends_invite"));
            return;
        }
        final List<String> names = new ArrayList<>();
        final List<Models.Contact> list = new ArrayList<>();
        for (Models.Contact c : contacts) {
            if (group == null || !group.members.contains(c.username)) {
                names.add(c.nickname + " (@" + c.username + ")");
                list.add(c);
            }
        }
        if (list.isEmpty()) {
            Utils.toast(this, I18n.t("all_in_group"));
            return;
        }
        Ui.Click[] clicks = new Ui.Click[list.size()];
        for (int i = 0; i < list.size(); i++) {
            final Models.Contact c = list.get(i);
            clicks[i] = new Ui.Click() {
                @Override
                public void onClick() {
                    try {
                        JSONObject o = new JSONObject();
                        o.put("type", "invite_group");
                        o.put("id", groupId);
                        o.put("username", c.username);
                        Net.get().send(o);
                    } catch (Exception ignored) {
                    }
                }
            };
        }
        Ui.list(this, I18n.t("invite_friends"), names.toArray(new String[0]), clicks);
    }

    private void editNotice() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final android.widget.EditText input = Ui.input(this, group == null ? "" : group.notice, I18n.t("notice_input_hint"));
        input.setSingleLine(false);
        input.setMinLines(3);
        box.addView(input);
        Ui.show(this, I18n.t("edit_notice"), box, I18n.t("save"), new Ui.Click() {
            @Override
            public void onClick() {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "set_group_notice");
                    o.put("id", groupId);
                    o.put("notice", input.getText().toString().trim());
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void editName() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final android.widget.EditText input = Ui.input(this, group == null ? "" : group.name, I18n.t("group_name_input_hint"));
        box.addView(input);
        Ui.show(this, I18n.t("edit_group_name"), box, I18n.t("save"), new Ui.Click() {
            @Override
            public void onClick() {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "set_group_info");
                    o.put("id", groupId);
                    o.put("name", input.getText().toString().trim());
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
            startActivityForResult(Intent.createChooser(i, I18n.t("choose_group_avatar")), PICK_AVATAR);
        } catch (Exception e) {
            Utils.toast(this, I18n.t("cannot_open_picker"));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AVATAR && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uploadAvatar(data.getData());
        }
    }

    private void uploadAvatar(final Uri uri) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream in = getContentResolver().openInputStream(uri);
                    Bitmap bmp = BitmapFactory.decodeStream(in);
                    if (in != null) in.close();
                    if (bmp == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Utils.toast(GroupDetailActivity.this, I18n.t("cannot_read_image"));
                            }
                        });
                        return;
                    }
                    int target = 512;
                    int w = bmp.getWidth(), h = bmp.getHeight();
                    if (w > target || h > target) {
                        float s = Math.min((float) target / w, (float) target / h);
                        Bitmap scaled = Bitmap.createScaledBitmap(bmp, Math.max(1, Math.round(w * s)), Math.max(1, Math.round(h * s)), true);
                        if (scaled != bmp) bmp.recycle();
                        bmp = scaled;
                    }
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, 82, bos);
                    final String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
                    bmp.recycle();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject o = new JSONObject();
                                o.put("type", "set_group_info");
                                o.put("id", groupId);
                                o.put("avatarData", b64);
                                Net.get().send(o);
                                Ui.popup(GroupDetailActivity.this, I18n.t("avatar_uploading_group"));
                            } catch (Exception ignored) {
                            }
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.toast(GroupDetailActivity.this, I18n.t("avatar_upload_failed"));
                        }
                    });
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private Dialog memberCardDialog;
    private LinearLayout memberCardBox;
    private boolean memberCardLoaded = false;
    private String memberCardUsername = "";
    private String memberCardNickname = "";
    private boolean memberCardHasAvatar = false;

    private void showMemberCard(final Models.GroupMember m) {
        memberCardLoaded = false;
        memberCardUsername = m.username;
        memberCardNickname = m.nickname;
        memberCardHasAvatar = m.hasAvatar;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(Utils.dp(this, 24), Utils.dp(this, 34), Utils.dp(this, 24), Utils.dp(this, 34));
        box.addView(new TriangleLoadingView(this));
        memberCardBox = box;
        memberCardDialog = Ui.showSingle(this, I18n.t("friend_card"), box, I18n.t("close"), null);
        try {
            JSONObject o = new JSONObject();
            o.put("type", "get_user_profile");
            o.put("username", m.username);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    private void fillMemberCard(JSONObject o) {
        if (memberCardDialog == null || !memberCardDialog.isShowing() || memberCardLoaded) return;
        if (!memberCardUsername.equals(o.optString("username"))) return;
        memberCardLoaded = true;
        rebuildMemberCard(o.optJSONObject("profile"));
    }

    private void rebuildMemberCard(JSONObject p) {
        if (memberCardBox == null) return;
        memberCardBox.removeAllViews();
        memberCardBox.setGravity(Gravity.CENTER_HORIZONTAL);
        memberCardBox.addView(AvatarManager.avatarView(this, memberCardUsername, memberCardNickname, memberCardHasAvatar, 64, 26));
        memberCardBox.addView(Utils.hSpace(this, 8));
        TextView nm = Utils.tv(this, memberCardNickname, 17, Utils.TEXT, Gravity.CENTER);
        nm.setTypeface(Typeface.DEFAULT_BOLD);
        memberCardBox.addView(nm);
        memberCardBox.addView(Utils.tv(this, "@" + memberCardUsername, 13, Utils.TEXT_DIM, Gravity.CENTER));
        memberCardBox.addView(Utils.hSpace(this, 10));
        memberCardBox.addView(Utils.divider(this));
        memberCardBox.addView(Utils.hSpace(this, 6));
        if (p != null) appendProfileFields(memberCardBox, p);
        // 非好友且不是自己 -> 添加好友
        boolean isSelf = memberCardUsername.equals(Session.username);
        boolean isFriend = false;
        for (Models.Contact c : contacts) {
            if (c.username.equals(memberCardUsername)) isFriend = true;
        }
        if (!isSelf && !isFriend) {
            memberCardBox.addView(Utils.hSpace(this, 10));
            Button addBtn = Utils.button(this);
            addBtn.setText(I18n.t("add_friend"));
            addBtn.setTextSize(14);
            addBtn.setTextColor(Color.WHITE);
            addBtn.setBackground(Utils.bg(this, Utils.ACCENT, 22));
            addBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        JSONObject o = new JSONObject();
                        o.put("type", "add_friend");
                        o.put("username", memberCardUsername);
                        Net.get().send(o);
                    } catch (Exception ignored) {
                    }
                    ((Button) v).setText(I18n.t("request_sent"));
                    v.setEnabled(false);
                }
            });
            memberCardBox.addView(addBtn);
        }
        if (memberCardDialog != null && memberCardDialog.getWindow() != null) {
            memberCardDialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.86f),
                    WindowManager.LayoutParams.WRAP_CONTENT);
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


    private void confirmClearHistory() {
        Ui.show(this, I18n.t("clear_group_history"), Ui.message(this, I18n.t("clear_group_history_confirm")), I18n.t("confirm"), new Ui.Click() {
            @Override
            public void onClick() {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "clear_group_history");
                    o.put("id", groupId);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void confirmLeave() {
        Ui.show(this, I18n.t("leave_group"), Ui.message(this, I18n.t("leave_confirm")), I18n.t("quit"), new Ui.Click() {
            @Override
            public void onClick() {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "leave_group");
                    o.put("id", groupId);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void confirmDissolve() {
        Ui.show(this, I18n.t("dissolve_group"), Ui.message(this, I18n.t("dissolve_confirm")), I18n.t("dissolve"), new Ui.Click() {
            @Override
            public void onClick() {
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "dissolve_group");
                    o.put("id", groupId);
                    Net.get().send(o);
                } catch (Exception ignored) {
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
                refresh();
                requestMembers();
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
        if ("groups".equals(type)) {
            List<Models.Group> list = Models.parseGroups(o.optJSONArray("groups"));
            for (Models.Group g : list) {
                if (g.id.equals(groupId)) {
                    group = g;
                    renderGroup();
                    break;
                }
            }
            groupLoaded = true;
            checkLoaded();
        } else if ("group_members".equals(type) && groupId.equals(o.optString("id"))) {
            renderMembers(Models.parseMembers(o.optJSONArray("members")));
            membersLoaded = true;
            checkLoaded();
        } else if ("contacts".equals(type)) {
            contacts = Models.parseContacts(o.optJSONArray("contacts"));
        } else if ("group_updated".equals(type)) {
            Models.Group g = Models.Group.fromJson(o.optJSONObject("group"));
            if (g.id.equals(groupId)) {
                group = g;
                renderGroup();
            }
        } else if ("group_changed".equals(type)) {
            Models.Group g = Models.Group.fromJson(o.optJSONObject("group"));
            if (g.id.equals(groupId)) {
                group = g;
                renderGroup();
                requestMembers();
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
        } else if ("group_history_cleared".equals(type)) {
            if (groupId.equals(o.optString("id"))) {
                Ui.popup(this, I18n.t("clear_group_history_confirm"));
            }
        } else if ("user_profile".equals(type)) {
            fillMemberCard(o);
        } else if ("group_left".equals(type)) {
            Models.Group g = Models.Group.fromJson(o.optJSONObject("group"));
            if (g.id != null && g.id.equals(groupId)) {
                Utils.toast(this, I18n.t("left_ok"));
                finish();
            }
        } else if ("group_dissolved_ok".equals(type)) {
            if (groupId.equals(o.optString("id"))) {
                Utils.toast(this, I18n.t("dissolved_ok"));
                finish();
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
