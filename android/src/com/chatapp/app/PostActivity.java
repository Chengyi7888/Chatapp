package com.chatapp.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/** 发布动态: 全屏编辑页, 左上角返回, 右上角发布 */
public class PostActivity extends Activity {

    private static final int PICK_POST_IMAGE = 1002;
    private final List<String> images = new ArrayList<>();
    private EditText input;
    private GridLayout grid;

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
        top.setPadding(Utils.dp(this, 4), Utils.dp(this, 8), Utils.dp(this, 8), Utils.dp(this, 8));
        TextView back = Utils.tv(this, I18n.t("返回", "返回", "Back"), 14, Utils.TEXT, Gravity.CENTER);
        back.setPadding(Utils.dp(this, 14), Utils.dp(this, 6), Utils.dp(this, 14), Utils.dp(this, 6));
        back.setBackground(Utils.bg(this, Utils.CARD2, 14));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        top.addView(back);
        TextView title = Utils.tv(this, I18n.t("dynamics"), 17, Utils.TEXT, Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView publish = Utils.tv(this, I18n.t("publish"), 14, Color.WHITE, Gravity.CENTER);
        publish.setPadding(Utils.dp(this, 18), Utils.dp(this, 6), Utils.dp(this, 18), Utils.dp(this, 6));
        publish.setBackground(Utils.bg(this, Utils.ACCENT, 14));
        publish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishPost();
            }
        });
        top.addView(publish);
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        topLp.setMargins(Utils.dp(this, 8), Utils.dp(this, 6), Utils.dp(this, 8), 0);
        root.addView(top, topLp);

        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        sc.setBackgroundColor(Utils.BG);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Utils.dp(this, 16), Utils.dp(this, 18), Utils.dp(this, 16), Utils.dp(this, 20));
        content.setBackgroundColor(Utils.BG);
        sc.addView(content);
        root.addView(sc, Utils.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        input = Ui.input(this, null, I18n.t("moment_hint"));
        input.setSingleLine(false);
        input.setMinLines(3);
        content.addView(input);
        content.addView(Utils.hSpace(this, 12));

        grid = new GridLayout(this);
        grid.setColumnCount(3);
        content.addView(grid);

        setContentView(root);
        refreshGrid();
    }

    private void refreshGrid() {
        grid.removeAllViews();
        int cell = (getResources().getDisplayMetrics().widthPixels - Utils.dp(this, 36 + 48)) / 3;
        for (int i = 0; i < images.size(); i++) {
            final int idx = i;
            FrameLayout box = new FrameLayout(this);
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = cell;
            glp.height = cell;
            glp.setMargins(Utils.dp(this, 2), Utils.dp(this, 2), Utils.dp(this, 2), Utils.dp(this, 2));
            box.setLayoutParams(glp);
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackground(Utils.bg(this, Utils.CARD2, 8));
            try {
                byte[] data = Base64.decode(images.get(idx), Base64.NO_WRAP);
                iv.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
            } catch (Exception ignored) {
            }
            box.addView(iv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            TextView x = Utils.tv(this, "X", 11, Color.WHITE, Gravity.CENTER);
            int xs = Utils.dp(this, 22);
            FrameLayout.LayoutParams xlp = new FrameLayout.LayoutParams(xs, xs, Gravity.TOP | Gravity.END);
            xlp.setMargins(0, Utils.dp(this, 3), Utils.dp(this, 3), 0);
            x.setLayoutParams(xlp);
            GradientDrawable xbg = new GradientDrawable();
            xbg.setShape(GradientDrawable.OVAL);
            xbg.setColor(Utils.DANGER);
            x.setBackground(xbg);
            final int fIdx = idx;
            x.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    images.remove(fIdx);
                    refreshGrid();
                }
            });
            box.addView(x);
            grid.addView(box);
        }
        if (images.size() < 9) {
            TextView add = Utils.tv(this, "+", 30, Utils.ACCENT, Gravity.CENTER);
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = cell;
            glp.height = cell;
            glp.setMargins(Utils.dp(this, 2), Utils.dp(this, 2), Utils.dp(this, 2), Utils.dp(this, 2));
            add.setLayoutParams(glp);
            add.setBackground(Utils.ring(this, Utils.CARD2, Utils.ACCENT, 1, 14));
            add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (images.size() >= 9) return;
                    MediaPick.choose(PostActivity.this, PICK_POST_IMAGE, new MediaPick.Callback() {
                        @Override
                        public void onBitmap(Bitmap bmp) {
                            addImage(bmp);
                        }
                    });
                }
            });
            grid.addView(add);
        }
    }

    private void addImage(final Bitmap src) {
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
                            if (images.size() >= 9) return;
                            images.add(b64);
                            refreshGrid();
                        }
                    });
                } catch (Exception ignored) {
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void publishPost() {
        String text = input.getText().toString().trim();
        if (text.isEmpty() && images.isEmpty()) {
            Utils.toast(this, I18n.t("srv_moment_empty"));
            return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("type", "add_post");
            o.put("text", text);
            JSONArray arr = new JSONArray();
            for (String b : images) arr.put(b);
            o.put("images", arr);
            Net.get().send(o);
        } catch (Exception ignored) {
        }
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        MediaPick.handleResult(this, requestCode, resultCode, data);
    }
}
