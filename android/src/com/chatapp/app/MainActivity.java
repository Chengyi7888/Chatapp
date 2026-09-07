package com.chatapp.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

/** 登录 / 注册�?(暗黑模式) */
public class MainActivity extends Activity implements Net.Listener {

    private EditText serverField, userField, passField, confirmField;
    private TextView title, subtitle, toggle, status;
    private Button loginBtn, registerBtn;
    private LinearLayout confirmRow;
    private boolean registerMode = false;
    private boolean awaitingAuth = false;
    private boolean everConnected = false;
    private final Handler authHandler = new Handler(Looper.getMainLooper());
    private final Runnable authRetry = new Runnable() {
        @Override
        public void run() {
            if (awaitingAuth) {
                sendAuth();
                authHandler.postDelayed(authRetry, 5000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setBackgroundColor(Utils.BG);
        Session.init(this);
        I18n.init(this);
        Theme.init(this);
        Theme.apply(this);

        if (Session.loggedIn()) {
            goHome();
            return;
        }

        buildUi();
        serverField.setText(Session.server);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Utils.BG);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(Utils.dp(this, 28), Utils.dp(this, 20), Utils.dp(this, 28), Utils.dp(this, 20));

        // 顶部弹性占�? 表单在屏幕中垂直居中
        root.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));

        title = Utils.tv(this, "Chatapp", 32, Utils.TEXT, Gravity.CENTER);
        title.setPadding(0, 0, 0, Utils.dp(this, 10));
        root.addView(title);

        subtitle = Utils.tv(this, I18n.t("app_subtitle"), 14, Utils.TEXT_DIM, Gravity.CENTER);
        root.addView(subtitle);
        root.addView(Utils.tv(this, "App v1.0", 11, Utils.TEXT_DIM, Gravity.CENTER));
        root.addView(Utils.hSpace(this, 28));

        serverField = edit(I18n.t("server_hint"), InputType.TYPE_CLASS_TEXT);
        userField = edit(I18n.t("username_hint"), InputType.TYPE_CLASS_TEXT);
        passField = edit(I18n.t("password_hint"), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        confirmField = edit(I18n.t("confirm_password"), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        root.addView(serverField);
        root.addView(userField);
        root.addView(passField);

        confirmRow = new LinearLayout(this);
        confirmRow.setOrientation(LinearLayout.VERTICAL);
        confirmRow.addView(confirmField);
        confirmRow.setVisibility(View.GONE);
        root.addView(confirmRow);

        root.addView(Utils.hSpace(this, 12));

        toggle = Utils.tv(this, I18n.t("no_account_register"), 14, Utils.ACCENT, Gravity.CENTER);
        toggle.setPadding(0, Utils.dp(this, 12), 0, Utils.dp(this, 12));
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMode();
            }
        });
        root.addView(toggle);

        loginBtn = Utils.button(this);
        loginBtn.setText(I18n.t("login"));
        loginBtn.setTextSize(16);
        loginBtn.setTextColor(Color.WHITE);
        loginBtn.setBackground(Utils.bg(this, Utils.ACCENT, 10));
        loginBtn.setPadding(0, Utils.dp(this, 12), 0, Utils.dp(this, 12));
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doAuth(false);
            }
        });
        root.addView(loginBtn);

        registerBtn = Utils.button(this);
        registerBtn.setText(I18n.t("register"));
        registerBtn.setTextSize(16);
        registerBtn.setTextColor(Color.WHITE);
        registerBtn.setBackground(Utils.bg(this, Utils.ACCENT_DARK, 10));
        registerBtn.setPadding(0, Utils.dp(this, 12), 0, Utils.dp(this, 12));
        registerBtn.setVisibility(View.GONE);
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doAuth(true);
            }
        });
        root.addView(registerBtn);

        root.addView(Utils.hSpace(this, 18));
        status = Utils.tv(this, "", 13, Utils.ACCENT, Gravity.CENTER);
        root.addView(status);

        // 底部弹性占�? 表单在屏幕中垂直居中
        root.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));

        ScrollView sc = new ScrollView(this);
        sc.setFillViewport(true);
        sc.addView(root);
        setContentView(sc);
    }

    private EditText edit(String hint, int inputType) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setInputType(inputType);
        e.setTextSize(15);
        e.setTextColor(Utils.TEXT);
        e.setHintTextColor(Utils.TEXT_DIM);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Utils.dp(this, 14);
        e.setLayoutParams(lp);
        e.setPadding(Utils.dp(this, 12), Utils.dp(this, 10), Utils.dp(this, 12), Utils.dp(this, 10));
        e.setBackground(Utils.bg(this, Utils.CARD, 8));
        return e;
    }

    private void toggleMode() {
        registerMode = !registerMode;
        confirmRow.setVisibility(registerMode ? View.VISIBLE : View.GONE);
        loginBtn.setVisibility(registerMode ? View.GONE : View.VISIBLE);
        registerBtn.setVisibility(registerMode ? View.VISIBLE : View.GONE);
        toggle.setText(registerMode ? I18n.t("has_account_login") : I18n.t("no_account_register"));
    }

    private JSONObject buildAuth() {
        try {
            JSONObject o = new JSONObject();
            if (registerMode) {
                o.put("type", "register");
                o.put("username", userField.getText().toString().trim());
                o.put("password", passField.getText().toString());
            } else {
                o.put("type", "login");
                o.put("username", userField.getText().toString().trim());
                o.put("password", passField.getText().toString());
            }
            return o;
        } catch (Exception e) {
            return null;
        }
    }

    private void doAuth(final boolean register) {
        final String hostPort = serverField.getText().toString().trim();
        final String username = userField.getText().toString().trim();
        final String password = passField.getText().toString();
        final String confirm = confirmField.getText().toString();

        if (hostPort.isEmpty()) {
            Utils.toast(this, I18n.t("fill_server"));
            return;
        }
        if (username.isEmpty()) {
            Utils.toast(this, I18n.t("fill_username"));
            return;
        }
        if (!isValidAccount(username)) {
            Utils.toast(this, I18n.t("account_format"));
            return;
        }
        if (!isValidPassword(password)) {
            Utils.toast(this, I18n.t("password_rule"));
            return;
        }
        if (register && !confirm.equals(password)) {
            Utils.toast(this, I18n.t("confirm_password_mismatch"));
            return;
        }

        JSONObject auth = buildAuth();
        if (auth == null) {
            Utils.toast(this, I18n.t("request_failed"));
            return;
        }

        setBusy(true);
        awaitingAuth = true;
        everConnected = false;
        authHandler.removeCallbacks(authRetry);
        authHandler.postDelayed(authRetry, 5000);
        Session.save("", "", hostPort); // 先记住服务器地址

        String[] parts = Session.serverParts();
        Net.get().setListener(this);
        Net.get().start(parts[0], Integer.parseInt(parts[1]), auth.toString());
    }

    private boolean isValidAccount(String s) {
        return s.matches("[A-Za-z0-9\\-_/~@]{2,20}") && s.matches(".*[A-Za-z].*") && s.matches(".*[0-9].*");
    }

    private boolean isValidPassword(String s) {
        return s.length() >= 8 && s.length() <= 16 && s.matches(".*[A-Za-z].*") && s.matches(".*[0-9].*");
    }

    private void setBusy(boolean busy) {
        status.setText(busy ? I18n.t("connecting_server") : "");
        loginBtn.setEnabled(!busy);
        registerBtn.setEnabled(!busy);
    }

    private void sendAuth() {
        if (!awaitingAuth) return;
        JSONObject auth = buildAuth();
        if (auth == null) return;
        boolean ok = Net.get().send(auth);
        if (!ok && everConnected) {
            status.setText(I18n.t("send_failed_retry"));
        }
    }

    private void goHome() {
        Intent i = new Intent(this, HomeActivity.class);
        startActivity(i);
        finish();
    }

    // ---------- Net.Listener ----------

    @Override
    public void onConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                everConnected = true;
                if (awaitingAuth) {
                    status.setText(I18n.t("connected_verifying"));
                }
            }
        });
    }

    @Override
    public void onMessage(final JSONObject obj) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                handle(obj);
            }
        });
    }

    private void handle(JSONObject o) {
        String type = o.optString("type");
        if ("login_ok".equals(type)) {
            awaitingAuth = false;
            Session.save(
                    o.optString("username"),
                    o.optString("nickname"),
                    serverField.getText().toString().trim(),
                    o.optString("token"),
                    o.optString("status"),
                    o.optBoolean("hasAvatar", false));
            Utils.toast(this, I18n.serverMsg(o.optString("welcome", "登录成功")));
            goHome();
        } else if ("error".equals(type)) {
            setBusy(false);
            awaitingAuth = false;
            status.setText(I18n.serverMsg(o.optString("message")));
        } else if ("toast".equals(type)) {
            setBusy(false);
            awaitingAuth = false;
            Utils.toast(this, I18n.serverMsg(o.optString("message")));
        }
    }

    @Override
    public void onDisconnected(final String reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (awaitingAuth) {
                    setBusy(false);
                    status.setText(everConnected
                            ? I18n.t("conn_lost_reconnect")
                            : I18n.t("cannot_connect"));
                }
            }
        });
    }
}
