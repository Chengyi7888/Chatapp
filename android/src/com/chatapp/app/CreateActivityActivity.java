package com.chatapp.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

/** 发起活动: 全屏新界面 (替代弹窗) */
public class CreateActivityActivity extends Activity {

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
        TextView back = Utils.tv(this, "<", 26, Utils.TEXT, Gravity.CENTER);
        back.setPadding(Utils.dp(this, 10), 0, Utils.dp(this, 10), Utils.dp(this, 4));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        top.addView(back);
        TextView title = Utils.tv(this, I18n.t("create_activity"), 17, Utils.TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
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

        final EditText titleInput = Ui.input(this, null, I18n.t("activity_title_hint"));
        titleInput.setSingleLine(true);
        content.addView(titleInput);
        content.addView(Utils.hSpace(this, 12));
        final EditText textInput = Ui.input(this, null, I18n.t("activity_text_hint"));
        textInput.setSingleLine(false);
        textInput.setMinLines(3);
        content.addView(textInput);
        content.addView(Utils.hSpace(this, 24));

        TextView publish = Utils.tv(this, I18n.t("publish"), 15, Color.WHITE, Gravity.CENTER);
        publish.setPadding(0, Utils.dp(this, 12), 0, Utils.dp(this, 12));
        publish.setBackground(Utils.bg(this, Utils.ACCENT, 22));
        publish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleInput.getText().toString().trim();
                if (title.isEmpty()) {
                    Utils.toast(CreateActivityActivity.this, I18n.t("activity_title_empty"));
                    return;
                }
                try {
                    JSONObject o = new JSONObject();
                    o.put("type", "add_activity");
                    o.put("title", title);
                    o.put("text", textInput.getText().toString().trim());
                    Net.get().send(o);
                } catch (Exception ignored) {
                }
                finish();
            }
        });
        content.addView(publish, Utils.lp(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }
}
