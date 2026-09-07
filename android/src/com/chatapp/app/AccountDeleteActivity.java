package com.chatapp.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

/** 注销账号: 输入昵称/用户�?密码验证, 二次确认后注销 */
public class AccountDeleteActivity extends Activity implements Net.Listener {

    private EditText nickField, userField, passField;

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
        TextView back = Utils.tv(this, "<", 26, Utils.TEXT, Gravity.CENTER);
        back.setPadding(Utils.dp(this, 10), 0, Utils.dp(this, 10), Utils.dp(this, 4));
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        top.addView(back);
        TextView title = Utils.tv(this, I18n.t("delete_account"), 17, Utils.TEXT, Gravity.START);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title);
        root.addView(top, topLp);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Utils.dp(this, 24), Utils.dp(this, 30), Utils.dp(this, 24), Utils.dp(this, 20));
        body.addView(Utils.tv(this, I18n.t("verify_identity"), 16, Utils.TEXT, Gravity.CENTER));
        body.addView(Utils.hSpace(this, 14));

        nickField = Ui.input(this, null, I18n.t("verify_nickname"));
        userField = Ui.input(this, null, I18n.t("verify_username"));
        passField = Ui.input(this, null, I18n.t("verify_password"));
        body.addView(nickField);
        body.addView(Utils.hSpace(this, 10));
        body.addView(userField);
        body.addView(Utils.hSpace(this, 10));
        body.addView(passField);
        body.addView(Utils.hSpace(this, 20));

        Button ok = Utils.button(this);
        ok.setText(I18n.t("confirm"));
        ok.setTextSize(16);
        ok.setTextColor(Color.WHITE);
        ok.setBackground(Utils.bg(this, Utils.DANGER, 24));
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verify();
            }
        });
        body.addView(ok);

        ScrollView sc = new ScrollView(this);
        sc.addView(body);
        root.addView(sc, Utils.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void verify() {
        try {
            JSONObject o = new JSONObject();
            o.put("type", "verify_account");
            o.put("nickname", nickField.getText().toString().trim());
            o.put("username", userField.getText().toString().trim());
            o.put("password", passField.getText().toString());
            Net.get().send(o);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Net.get().setListener(this);
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
                String type = obj.optString("type");
                if ("account_verify_ok".equals(type)) {
                    // 二次确认
                    Ui.show(AccountDeleteActivity.this, I18n.t("delete_account"),
                            Ui.message(AccountDeleteActivity.this, I18n.t("confirm_delete2")),
                            I18n.t("confirm"), new Ui.Click() {
                                @Override
                                public void onClick() {
                                    try {
                                        JSONObject o = new JSONObject();
                                        o.put("type", "delete_account");
                                        o.put("nickname", nickField.getText().toString().trim());
                                        o.put("username", userField.getText().toString().trim());
                                        o.put("password", passField.getText().toString());
                                        Net.get().send(o);
                                    } catch (Exception ignored) {
                                    }
                                }
                            });
                } else if ("account_deleted".equals(type)) {
                    Session.logout();
                    Net.get().stop();
                    Intent i = new Intent(AccountDeleteActivity.this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                } else if ("error".equals(type)) {
                    Ui.popup(AccountDeleteActivity.this, I18n.serverMsg(obj.optString("message")));
                } else if ("toast".equals(type)) {
                    Ui.popup(AccountDeleteActivity.this, I18n.serverMsg(obj.optString("message")));
                }
            }
        });
    }

    @Override
    public void onDisconnected(String reason) {
    }
}
