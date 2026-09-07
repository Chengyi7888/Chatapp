package com.chatapp.app;

import android.content.Context;
import android.content.SharedPreferences;

/** 本地会话信息 */
public class Session {
    private static final String PREFS = "chatapp";
    private static SharedPreferences prefs;

    public static String username = "";
    public static String nickname = "";
    public static String server = "";
    public static String token = "";
    public static String status = "Hello,world";
    public static boolean hasAvatar = false;
    public static String lang = I18n.ZH_HANS;
    public static String region = "";

    public static void init(Context c) {
        if (prefs == null) {
            prefs = c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        }
        username = prefs.getString("username", "");
        nickname = prefs.getString("nickname", "");
        server = prefs.getString("server", "100.100.1.100:1100");
        token = prefs.getString("token", "");
        status = prefs.getString("status", "Hello,world");
        hasAvatar = prefs.getBoolean("hasAvatar", false);
        lang = prefs.getString("lang", I18n.EN);
        region = prefs.getString("region", "中国");
    }

    public static boolean loggedIn() {
        return !username.isEmpty();
    }

    public static void save(String u, String n, String srv) {
        save(u, n, srv, token, status, hasAvatar);
    }

    public static void save(String u, String n, String srv, String tok) {
        save(u, n, srv, tok, status, hasAvatar);
    }

    public static void save(String u, String n, String srv, String tok, String st) {
        save(u, n, srv, tok, st, hasAvatar);
    }

    public static void save(String u, String n, String srv, String tok, String st, boolean av) {
        username = u == null ? "" : u;
        nickname = n == null ? "" : n;
        server = srv == null ? "" : srv;
        token = tok == null ? "" : tok;
        status = (st == null || st.isEmpty()) ? "Hello,world" : st;
        hasAvatar = av;
        prefs.edit()
                .putString("username", username)
                .putString("nickname", nickname)
                .putString("server", server)
                .putString("token", token)
                .putString("status", status)
                .putBoolean("hasAvatar", hasAvatar)
                .apply();
    }

    public static void updateNickname(String n) {
        nickname = n;
        prefs.edit().putString("nickname", n).apply();
    }

    public static void updateStatus(String s) {
        status = (s == null || s.isEmpty()) ? "Hello,world" : s;
        prefs.edit().putString("status", status).apply();
    }

    public static void updateHasAvatar(boolean av) {
        hasAvatar = av;
        prefs.edit().putBoolean("hasAvatar", av).apply();
    }

    public static void updateLang(String l) {
        lang = l;
        I18n.setLang(l);
        prefs.edit().putString("lang", l).apply();
    }

    public static void updateRegion(String r) {
        region = r;
        prefs.edit().putString("region", r).apply();
    }

    public static void logout() {
        username = "";
        nickname = "";
        token = "";
        status = "Hello,world";
        hasAvatar = false;
        prefs.edit()
                .remove("username").remove("nickname").remove("token")
                .remove("status").remove("hasAvatar")
                .apply();
    }

    /** 拆分 "host:port", 默认 127.0.0.1:8899 */
    public static String[] serverParts() {
        String s = server.trim();
        if (s.isEmpty()) s = "100.100.1.100:1100";
        if (!s.contains(":")) s = s + ":1100";
        String[] p = s.split(":");
        return new String[]{p[0].trim(), p.length > 1 && !p[1].trim().isEmpty() ? p[1].trim() : "1100"};
    }
}
