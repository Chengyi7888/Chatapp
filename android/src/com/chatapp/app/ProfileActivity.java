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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** 个人名片: 头像/昵称/性别/生日/标签/照片/职业/公司/所在地/出生地/邮箱 */
public class ProfileActivity extends Activity implements Net.Listener {

    private static final int PICK_AVATAR = 5001;
    private static final int PICK_PHOTO = 5002;

    private LinearLayout content;
    private View loading;
    private String nickname = "";
    private boolean hasAvatar = false;
    private String gender = "";
    private String birthday = "";
    private List<String> tags = new ArrayList<>();
    private List<String> photos = new ArrayList<>();
    private String job = "";
    private String company = "";
    private String location = "";
    private String birthplace = "";
    private String email = "";

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
        final FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Utils.BG);
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
        TextView back = Utils.tv(this, "‹", 26, Utils.TEXT, Gravity.CENTER);
        back.setPadding(Utils.dp(this, 10), 0, Utils.dp(this, 10), Utils.dp(this, 4));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        top.addView(back);
        TextView title = Utils.tv(this, I18n.t("profile"), 17, Utils.TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title);
        root.addView(top, topLp);

        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        sc.setBackgroundColor(Utils.BG);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Utils.dp(this, 18), Utils.dp(this, 20), Utils.dp(this, 18), Utils.dp(this, 20));
        content.setBackgroundColor(Utils.BG);
        sc.addView(content);
        root.addView(sc, Utils.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        frame.addView(root, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        loading = new TriangleLoadingView(this);
        FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        frame.addView(loading, llp);
        setContentView(frame);
    }

    private void render() {
        if (loading != null) loading.setVisibility(View.GONE);
        content.removeAllViews();

        // 头像
        LinearLayout avatarBox = new LinearLayout(this);
        avatarBox.setOrientation(LinearLayout.VERTICAL);
        avatarBox.setGravity(Gravity.CENTER_HORIZONTAL);
        avatarBox.setPadding(0, 0, 0, Utils.dp(this, 10));
        final View av = AvatarManager.avatarView(this, Session.username, nickname, hasAvatar, 84, 40);
        av.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickAvatar();
            }
        });
        avatarBox.addView(av);
        avatarBox.addView(Utils.hSpace(this, 6));
        TextView avLabel = Utils.tv(this, I18n.t("avatar") + " · " + I18n.t("edit_nickname"), 12, Utils.TEXT_DIM, Gravity.CENTER);
        avatarBox.addView(avLabel);
        content.addView(avatarBox);
        content.addView(Utils.hSpace(this, 18));

        LinearLayout card1 = new LinearLayout(this);
        card1.setOrientation(LinearLayout.VERTICAL);
        card1.setBackground(Utils.bg(this, Utils.CARD, 16));
        card1.addView(valueRow(I18n.t("nickname"), nickname, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNicknameDialog();
            }
        }));
        card1.addView(Utils.divider(this));
        card1.addView(valueRow(I18n.t("gender"), gender.isEmpty() ? "—" : gender, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGenderDialog();
            }
        }));
        card1.addView(Utils.divider(this));
        card1.addView(valueRow(I18n.t("birthday"), birthday.isEmpty() ? "—" : birthday, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBirthdayDialog();
            }
        }));
        card1.addView(Utils.divider(this));
        card1.addView(valueRow(I18n.t("qr_card"), "@" + Session.username, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQrDialog();
            }
        }));
        content.addView(card1);

        content.addView(Utils.hSpace(this, 16));

        LinearLayout card2 = new LinearLayout(this);
        card2.setOrientation(LinearLayout.VERTICAL);
        card2.setBackground(Utils.bg(this, Utils.CARD, 16));
        card2.addView(valueRow(I18n.t("tags"), tags.isEmpty() ? "—" : String.valueOf(tags.size()) + " 个", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTagDialog();
            }
        }));
        card2.addView(Utils.divider(this));
        // 标签行: 横向 chips + ＋
        LinearLayout tagRow = new LinearLayout(this);
        tagRow.setOrientation(LinearLayout.HORIZONTAL);
        tagRow.setGravity(Gravity.CENTER_VERTICAL);
        tagRow.setPadding(Utils.dp(this, 16), Utils.dp(this, 12), Utils.dp(this, 12), Utils.dp(this, 12));
        android.widget.HorizontalScrollView tagSc = new android.widget.HorizontalScrollView(this);
        tagSc.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < tags.size(); i++) {
            final int idx = i;
            final String tag = tags.get(i);
            TextView chip = Utils.tv(this, tag, 13, Utils.ACCENT, Gravity.CENTER);
            chip.setBackground(Utils.bg(this, Utils.CARD2, 14));
            chip.setPadding(Utils.dp(this, 10), Utils.dp(this, 5), Utils.dp(this, 10), Utils.dp(this, 5));
            chip.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    List<String> nt = new ArrayList<>(tags);
                    nt.remove(idx);
                    sendTags(nt);
                    return true;
                }
            });
            chips.addView(chip);
            chips.addView(Utils.vSpace(this, 6));
        }
        TextView plus = Utils.tv(this, "＋", 18, Utils.ACCENT, Gravity.CENTER);
        plus.setBackground(Utils.bg(this, Utils.CARD2, 14));
        plus.setPadding(Utils.dp(this, 10), Utils.dp(this, 5), Utils.dp(this, 10), Utils.dp(this, 5));
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTagDialog();
            }
        });
        chips.addView(plus);
        tagSc.addView(chips);
        tagRow.addView(tagSc, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card2.addView(tagRow);
        card2.addView(Utils.divider(this));
        // 我的照片 3x3
        LinearLayout photoBox = new LinearLayout(this);
        photoBox.setOrientation(LinearLayout.VERTICAL);
        photoBox.setPadding(Utils.dp(this, 16), Utils.dp(this, 12), Utils.dp(this, 16), Utils.dp(this, 12));
        photoBox.addView(Utils.tv(this, I18n.t("my_photos"), 15, Utils.TEXT, Gravity.START));
        photoBox.addView(Utils.hSpace(this, 10));
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        int cell = (getResources().getDisplayMetrics().widthPixels - Utils.dp(this, 36 + 32)) / 3;
        for (int i = 0; i < photos.size(); i++) {
            final int idx = i;
            final String url = photos.get(i);
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackground(Utils.bg(this, Utils.CARD2, 10));
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = cell;
            glp.height = cell;
            glp.setMargins(Utils.dp(this, 3), Utils.dp(this, 3), Utils.dp(this, 3), Utils.dp(this, 3));
            iv.setLayoutParams(glp);
            AvatarManager.loadFileImage(this, url, iv);
            iv.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    try {
                        JSONObject o = new JSONObject();
                        o.put("type", "remove_profile_photo");
                        o.put("index", idx);
                        Net.get().send(o);
                    } catch (Exception ignored) {
                    }
                    return true;
                }
            });
            grid.addView(iv);
        }
        if (photos.size() < 9) {
            TextView addTile = Utils.tv(this, "＋", 26, Utils.ACCENT, Gravity.CENTER);
            addTile.setBackground(Utils.bg(this, Utils.CARD2, 10));
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = cell;
            glp.height = cell;
            glp.setMargins(Utils.dp(this, 3), Utils.dp(this, 3), Utils.dp(this, 3), Utils.dp(this, 3));
            addTile.setLayoutParams(glp);
            addTile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pickPhoto();
                }
            });
            grid.addView(addTile);
        }
        photoBox.addView(grid);
        card2.addView(photoBox);
        content.addView(card2);

        content.addView(Utils.hSpace(this, 16));

        LinearLayout card3 = new LinearLayout(this);
        card3.setOrientation(LinearLayout.VERTICAL);
        card3.setBackground(Utils.bg(this, Utils.CARD, 16));
        card3.addView(valueRow(I18n.t("job"), job.isEmpty() ? "—" : job, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showJobDialog();
            }
        }));
        card3.addView(Utils.divider(this));
        card3.addView(valueRow(I18n.t("company"), company.isEmpty() ? "—" : company, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTextDialog(I18n.t("company"), I18n.t("company_hint"), company, "company");
            }
        }));
        card3.addView(Utils.divider(this));
        card3.addView(valueRow(I18n.t("location"), location.isEmpty() ? "—" : location, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegionDialog("location");
            }
        }));
        card3.addView(Utils.divider(this));
        card3.addView(valueRow(I18n.t("birthplace"), birthplace.isEmpty() ? "—" : birthplace, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegionDialog("birthplace");
            }
        }));
        card3.addView(Utils.divider(this));
        card3.addView(valueRow(I18n.t("email"), email.isEmpty() ? "—" : email, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTextDialog(I18n.t("email"), I18n.t("email_hint"), email, "email");
            }
        }));
        content.addView(card3);
    }

    private View valueRow(String label, String value, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Utils.dp(this, 16), Utils.dp(this, 14), Utils.dp(this, 16), Utils.dp(this, 14));
        row.setOnClickListener(onClick);
        row.addView(Utils.tv(this, label, 15, Utils.TEXT, Gravity.START), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        int vColor = Utils.TEXT_DIM;
        if (!value.isEmpty() && !value.equals("—")
                && (label.equals(I18n.t("gender")) || label.equals(I18n.t("birthday")) || label.equals(I18n.t("job"))
                || label.equals(I18n.t("location")) || label.equals(I18n.t("birthplace")))) {
            vColor = Utils.ACCENT;
        }
        TextView v = Utils.tv(this, value, 14, vColor, Gravity.END);
        v.setSingleLine(true);
        v.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(v, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(Utils.vSpace(this, 6));
        row.addView(Utils.tv(this, "›", 18, Utils.TEXT_DIM, Gravity.END));
        return row;
    }

    // ---------- 编辑 ----------

    private void showNicknameDialog() {
        final EditText input = Ui.input(this, nickname, null);
        Ui.show(this, I18n.t("edit_nickname"), input, I18n.t("save"), new Ui.Click() {
            @Override
            public void onClick() {
                String n = input.getText().toString().trim();
                if (n.isEmpty()) {
                    Utils.toast(ProfileActivity.this, I18n.t("nickname_empty"));
                    return;
                }
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "set_nickname");
                    o.put("nickname", n);
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void showGenderDialog() {
        Ui.list(this, I18n.t("gender"), new String[]{
                        I18n.t("gender_male"), I18n.t("gender_female"), I18n.t("gender_hidden"), I18n.t("gender_other")
                },
                new Ui.Click[]{
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                sendProfileField("gender", I18n.t("gender_male"));
                            }
                        },
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                sendProfileField("gender", I18n.t("gender_female"));
                            }
                        },
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                sendProfileField("gender", I18n.t("gender_hidden"));
                            }
                        },
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                showTextDialog(I18n.t("gender"), I18n.t("gender_other"), gender, "gender");
                            }
                        }
                });
    }

    private void showQrDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8), Utils.dp(this, 6));
        final ImageView qr = new ImageView(this);
        int s = Utils.dp(this, 220);
        qr.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        box.addView(qr);
        box.addView(Utils.hSpace(this, 8));
        box.addView(Utils.tv(this, "@" + Session.username, 14, Utils.TEXT, Gravity.CENTER));
        Ui.showSingle(this, I18n.t("qr_card"), box, I18n.t("close"), null);
        String data = "chatapp:" + Session.username;
        String url = "";
        try {
            url = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data="
                    + java.net.URLEncoder.encode(data, "UTF-8");
        } catch (Exception ignored) {
        }
        AvatarManager.loadBitmapUrl(this, url, new AvatarManager.BitmapCallback() {
            @Override
            public void onBitmap(Bitmap bmp) {
                if (bmp != null) qr.setImageBitmap(bmp);
            }
        });
    }

    private void showBirthdayDialog() {
        final NumberPicker year = new NumberPicker(this);
        final NumberPicker month = new NumberPicker(this);
        final NumberPicker day = new NumberPicker(this);
        int nowYear = Calendar.getInstance().get(Calendar.YEAR);
        year.setMinValue(1826);
        year.setMaxValue(nowYear);
        month.setMinValue(1);
        month.setMaxValue(12);
        day.setMinValue(1);
        day.setMaxValue(31);
        year.setWrapSelectorWheel(false);
        month.setWrapSelectorWheel(false);
        day.setWrapSelectorWheel(false);
        stylePicker(year);
        stylePicker(month);
        stylePicker(day);
        // 解析已有生日; 默认 2026-01-01
        int y = 2026, mo = 1, d = 1;
        if (birthday != null && birthday.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] parts = birthday.split("-");
            y = Integer.parseInt(parts[0]);
            mo = Integer.parseInt(parts[1]);
            d = Integer.parseInt(parts[2]);
        }
        year.setValue(Math.max(1826, Math.min(nowYear, y)));
        month.setValue(mo);
        day.setValue(d);

        android.widget.NumberPicker.OnValueChangeListener listener = new android.widget.NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                updateDayRange(year, month, day);
            }
        };
        year.setOnValueChangedListener(listener);
        month.setOnValueChangedListener(listener);
        day.setOnValueChangedListener(listener);
        updateDayRange(year, month, day);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.addView(year, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(month, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(day, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Ui.show(this, I18n.t("birthday"), box, I18n.t("confirm"), new Ui.Click() {
            @Override
            public void onClick() {
                String bd = String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d",
                        year.getValue(), month.getValue(), day.getValue());
                sendProfileField("birthday", bd);
            }
        });
    }

    private void updateDayRange(NumberPicker year, NumberPicker month, NumberPicker day) {
        int y = year.getValue();
        int mo = month.getValue();
        Calendar now = Calendar.getInstance();
        int maxMonth = 12;
        if (y == now.get(Calendar.YEAR)) maxMonth = now.get(Calendar.MONTH) + 1;
        if (mo > maxMonth) {
            month.setValue(maxMonth);
            mo = maxMonth;
        }
        int max = daysInMonth(y, mo);
        if (y == now.get(Calendar.YEAR) && mo == now.get(Calendar.MONTH) + 1) {
            max = Math.min(max, now.get(Calendar.DAY_OF_MONTH));
        }
        day.setMaxValue(max);
        if (day.getValue() > max) day.setValue(max);
        if (day.getValue() < 1) day.setValue(1);
    }

    private int daysInMonth(int y, int mo) {
        switch (mo) {
            case 1: case 3: case 5: case 7: case 8: case 10: case 12:
                return 31;
            case 4: case 6: case 9: case 11:
                return 30;
            case 2:
                boolean leap = (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0);
                return leap ? 29 : 28;
        }
        return 31;
    }

    private void showTagDialog() {
        final EditText input = Ui.input(this, null, I18n.t("tag_hint"));
        Ui.show(this, I18n.t("add_tag"), input, I18n.t("confirm"), new Ui.Click() {
            @Override
            public void onClick() {
                String raw = input.getText().toString().trim();
                String clean = raw.replaceFirst("^#+", "").replaceAll("[^\\p{L}\\p{N}]", "");
                if (clean.isEmpty()) {
                    Utils.toast(ProfileActivity.this, I18n.t("tag_hint"));
                    return;
                }
                String tag = "#" + clean;
                List<String> nt = new ArrayList<>(tags);
                if (!nt.contains(tag)) nt.add(tag);
                sendTags(nt);
            }
        });
    }

    private void showJobDialog() {
        final String[] jobs = {
                I18n.t("job_computer"), I18n.t("job_manufacturing"), I18n.t("job_medical"),
                I18n.t("job_finance"), I18n.t("job_business"), I18n.t("job_culture"),
                I18n.t("job_entertainment"), I18n.t("job_legal"), I18n.t("job_education"),
                I18n.t("job_admin"), I18n.t("job_student"), I18n.t("job_other")
        };
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        final Dialog[] holder = new Dialog[1];
        for (int i = 0; i < jobs.length; i++) {
            final String j = jobs[i];
            TextView row = Utils.tv(this, j, 15, j.equals(job) ? Utils.ACCENT : Utils.TEXT, Gravity.CENTER_VERTICAL);
            row.setPadding(Utils.dp(this, 8), Utils.dp(this, 13), Utils.dp(this, 8), Utils.dp(this, 13));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Dialog dd = holder[0];
                    if (dd != null && dd.isShowing()) dd.dismiss();
                    if (j.equals(I18n.t("job_other"))) {
                        showTextDialog(I18n.t("job"), I18n.t("job_other"), job, "job");
                    } else {
                        sendProfileField("job", j);
                    }
                }
            });
            list.addView(row);
            if (i < jobs.length - 1) list.addView(Utils.divider(this));
        }
        Dialog d = Ui.showGray(this, I18n.t("job"), list, new Ui.Click() {
            @Override
            public void onClick() {
            }
        });
        holder[0] = d;
    }

    private void stylePicker(NumberPicker p) {
        final boolean dark = Theme.isDark(this);
        final int wheelColor = dark ? Color.WHITE : Color.BLACK;
        final int dividerColor = dark ? Utils.DIVIDER : Color.BLACK;
        try {
            p.setTextColor(wheelColor);
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Field fd = NumberPicker.class.getDeclaredField("mSelectionDivider");
            fd.setAccessible(true);
            android.graphics.drawable.Drawable d = (android.graphics.drawable.Drawable) fd.get(p);
            if (d != null) d.setTint(dividerColor);
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Field f = NumberPicker.class.getDeclaredField("mIncrementButton");
            f.setAccessible(true);
            android.widget.ImageButton ib = (android.widget.ImageButton) f.get(p);
            if (ib != null && ib.getDrawable() != null) ib.getDrawable().setTint(wheelColor);
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Field f2 = NumberPicker.class.getDeclaredField("mDecrementButton");
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

    private void showRegionDialog(final String field) {
        final List<String> provinces = RegionData.provinces();
        final NumberPicker pPicker = new NumberPicker(this);
        final NumberPicker cPicker = new NumberPicker(this);
        final NumberPicker dPicker = new NumberPicker(this);
        pPicker.setMinValue(0);
        pPicker.setMaxValue(provinces.size() - 1);
        pPicker.setDisplayedValues(provinces.toArray(new String[0]));
        pPicker.setWrapSelectorWheel(false);
        stylePicker(pPicker);
        cPicker.setWrapSelectorWheel(false);
        dPicker.setWrapSelectorWheel(false);
        stylePicker(cPicker);
        stylePicker(dPicker);

        // 解析当前值
        String cur = "location".equals(field) ? location : birthplace;
        int pi = 0, ci = 0, di = 0;
        if (cur != null && !cur.isEmpty()) {
            String[] parts = cur.split("-");
            if (parts.length >= 1) {
                int idx = provinces.indexOf(parts[0]);
                if (idx >= 0) pi = idx;
            }
            List<String> cities = RegionData.cities(provinces.get(pi));
            if (parts.length >= 2) {
                int idx = cities.indexOf(parts[1]);
                if (idx >= 0) ci = idx;
            }
            List<String> ds = RegionData.districts(provinces.get(pi), cities.get(ci));
            if (parts.length >= 3) {
                int idx = ds.indexOf(parts[2]);
                if (idx >= 0) di = idx;
            }
        }

        final boolean[] updating = new boolean[]{false};

        final Runnable refresh = new Runnable() {
            @Override
            public void run() {
                if (updating[0]) return;
                updating[0] = true;
                try {
                    String province = provinces.get(pPicker.getValue());
                    List<String> cities = RegionData.cities(province);
                    cPicker.setMaxValue(cities.size() - 1);
                    cPicker.setDisplayedValues(cities.toArray(new String[0]));
                    if (cPicker.getValue() > cities.size() - 1) cPicker.setValue(0);
                    String city = cities.get(cPicker.getValue());
                    List<String> ds = RegionData.districts(province, city);
                    dPicker.setMaxValue(ds.size() - 1);
                    dPicker.setDisplayedValues(ds.toArray(new String[0]));
                    if (dPicker.getValue() > ds.size() - 1) dPicker.setValue(0);
                } finally {
                    updating[0] = false;
                }
            }
        };

        pPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                cPicker.setValue(0);
                dPicker.setValue(0);
                refresh.run();
            }
        });
        cPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                dPicker.setValue(0);
                refresh.run();
            }
        });

        pPicker.setValue(pi);
        refresh.run();
        cPicker.setValue(ci);
        refresh.run();
        dPicker.setValue(di);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.addView(pPicker, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(cPicker, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(dPicker, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Ui.show(this, "location".equals(field) ? I18n.t("location") : I18n.t("birthplace"), box, I18n.t("confirm"), new Ui.Click() {
            @Override
            public void onClick() {
                String province = provinces.get(pPicker.getValue());
                if ("-".equals(province)) {
                    sendProfileField(field, "");
                    return;
                }
                String city = RegionData.cities(province).get(cPicker.getValue());
                String district = RegionData.districts(province, city).get(dPicker.getValue());
                String val = province + "-" + city;
                if (!"-".equals(district)) val += "-" + district;
                sendProfileField(field, val);
            }
        });
    }

    private void showTextDialog(final String title, String hint, String initial, final String field) {
        final EditText input = Ui.input(this, initial, hint);
        Ui.show(this, title, input, I18n.t("save"), new Ui.Click() {
            @Override
            public void onClick() {
                String v = input.getText().toString().trim();
                if ("email".equals(field) && !v.isEmpty() && !isValidEmail(v)) {
                    Ui.popup(ProfileActivity.this, I18n.t("email_invalid"));
                    return;
                }
                sendProfileField(field, v);
            }
        });
    }

    private boolean isValidEmail(String e) {
        return e != null && e.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void sendProfileField(String field, String value) {
        try {
            JSONObject o = new JSONObject();
            o.put("type", "set_profile");
            o.put(field, value);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    private void sendTags(List<String> list) {
        try {
            JSONObject o = new JSONObject();
            o.put("type", "set_profile");
            JSONArray arr = new JSONArray();
            for (String s : list) arr.put(s);
            o.put("tags", arr);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    // ---------- 头像 / 照片 ----------

    private void pickAvatar() {
        MediaPick.choose(this, PICK_AVATAR, new MediaPick.Callback() {
            @Override
            public void onBitmap(Bitmap bmp) {
                uploadBitmap(bmp, "set_avatar", null);
            }
        });
    }

    private void pickPhoto() {
        MediaPick.choose(this, PICK_PHOTO, new MediaPick.Callback() {
            @Override
            public void onBitmap(Bitmap bmp) {
                uploadBitmap(bmp, "add_profile_photo", "photo");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (MediaPick.handleResult(this, requestCode, resultCode, data)) return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MediaPick.onPermissionResult(this, requestCode, grantResults);
    }

    private void uploadBitmap(final Bitmap src, final String type, final String photoFlag) {
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
                    int target = photoFlag != null ? 512 : 256;
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
                            try {
                                JSONObject o = new JSONObject();
                                o.put("type", type);
                                o.put("data", b64);
                                Net.get().send(o);
                            } catch (Exception ignored) {
                            }
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.toast(ProfileActivity.this, I18n.t("avatar_upload_failed"));
                        }
                    });
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void uploadSquare(final Uri uri, final String type, final String photoFlag) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    if (is != null) is.close();
                    if (bmp == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Utils.toast(ProfileActivity.this, I18n.t("cannot_read_image"));
                            }
                        });
                        return;
                    }
                    int w = bmp.getWidth(), h = bmp.getHeight();
                    int side = Math.min(w, h);
                    if (side > 0 && (w != side || h != side)) {
                        Bitmap cropped = Bitmap.createBitmap(bmp, (w - side) / 2, (h - side) / 2, side, side);
                        if (cropped != bmp) bmp.recycle();
                        bmp = cropped;
                    }
                    int target = photoFlag != null ? 512 : 256;
                    if (bmp.getWidth() > target) {
                        Bitmap scaled = Bitmap.createScaledBitmap(bmp, target, target, true);
                        if (scaled != bmp) bmp.recycle();
                        bmp = scaled;
                    }
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                    final String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
                    bmp.recycle();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject o = new JSONObject();
                                o.put("type", type);
                                o.put("data", b64);
                                Net.get().send(o);
                            } catch (Exception ignored) {
                            }
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.toast(ProfileActivity.this, I18n.t("avatar_upload_failed"));
                        }
                    });
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ---------- 网络 ----------

    @Override
    protected void onResume() {
        super.onResume();
        Net.get().setListener(this);
        try {
            JSONObject o = new JSONObject();
            o.put("type", "get_profile");
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
                    o.put("type", "get_profile");
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
        if ("profile".equals(type)) {
            JSONObject p = o.optJSONObject("profile");
            if (p != null) applyProfile(p);
            nickname = o.optString("nickname", nickname);
            hasAvatar = o.optBoolean("hasAvatar", hasAvatar);
            Session.updateHasAvatar(hasAvatar);
            render();
        } else if ("profile_updated".equals(type)) {
            JSONObject p = o.optJSONObject("profile");
            if (p != null) applyProfile(p);
            render();
        } else if ("nickname_updated".equals(type)) {
            nickname = o.optString("nickname", nickname);
            Session.updateNickname(nickname);
            render();
        } else if ("avatar_updated".equals(type)) {
            hasAvatar = true;
            Session.updateHasAvatar(true);
            AvatarManager.clear(Session.username);
            render();
        } else if ("error".equals(type)) {
            if (loading != null) loading.setVisibility(View.GONE);
            Utils.toast(this, I18n.serverMsg(o.optString("message")));
        } else if ("toast".equals(type)) {
            if (loading != null) loading.setVisibility(View.GONE);
            Utils.toast(this, I18n.serverMsg(o.optString("message")));
        }
    }

    private void applyProfile(JSONObject p) {
        gender = p.optString("gender", "");
        birthday = p.optString("birthday", "");
        job = p.optString("job", "");
        company = p.optString("company", "");
        location = p.optString("location", "");
        birthplace = p.optString("birthplace", "");
        email = p.optString("email", "");
        tags.clear();
        JSONArray ta = p.optJSONArray("tags");
        if (ta != null) {
            for (int i = 0; i < ta.length(); i++) tags.add(ta.optString(i));
        }
        photos.clear();
        JSONArray pa = p.optJSONArray("photos");
        if (pa != null) {
            for (int i = 0; i < pa.length(); i++) photos.add(pa.optString(i));
        }
    }

    @Override
    public void onDisconnected(String reason) {
    }
}
