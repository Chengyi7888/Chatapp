package com.chatapp.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;

/** 主题管理: 黑夜(黑绿) / 白昼(白蓝), 可选跟随系统, 可选液态玻璃质感 */
public class Theme {

    private static SharedPreferences prefs;
    public static boolean dark = true;
    public static boolean followSystem = false;
    public static boolean vibrate = false;
    private static boolean lastApplied = true;

    public static void init(Context c) {
        if (prefs == null) {
            prefs = c.getApplicationContext().getSharedPreferences("chatapp_theme", Context.MODE_PRIVATE);
        }
        dark = prefs.getBoolean("dark", true);
        followSystem = prefs.getBoolean("follow", false);
        vibrate = prefs.getBoolean("vibrate", false);
    }

    public static boolean isDark(Context c) {
        if (followSystem) {
            int night = c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return night == Configuration.UI_MODE_NIGHT_YES;
        }
        return dark;
    }

    public static void setDark(boolean d) {
        dark = d;
        if (prefs != null) prefs.edit().putBoolean("dark", d).apply();
    }

    public static void setFollow(boolean f) {
        followSystem = f;
        if (prefs != null) prefs.edit().putBoolean("follow", f).apply();
    }

    public static void setVibrate(boolean v) {
        vibrate = v;
        if (prefs != null) prefs.edit().putBoolean("vibrate", v).apply();
    }

    /** 应用配色; 返回是否发生了切换(用于决定是否重绘界面) */
    public static boolean apply(Context c) {
        init(c);
        boolean d = isDark(c);
        boolean changed = (d != lastApplied);
        lastApplied = d;
        if (d) {
            // 黑夜: 纯黑 + 亮绿
            Utils.BG = Color.rgb(0, 0, 0);
            Utils.CARD = Color.rgb(28, 28, 30);
            Utils.CARD2 = Color.rgb(44, 44, 46);
            Utils.TEXT = Color.WHITE;
            Utils.TEXT_DIM = Color.rgb(142, 142, 147);
            Utils.ACCENT = Color.rgb(37, 211, 102);
            Utils.ACCENT_DARK = Color.rgb(20, 150, 80);
            Utils.BUBBLE_OUT = Color.rgb(31, 44, 51);
            Utils.BUBBLE_IN = Color.rgb(38, 45, 49);
            Utils.DIVIDER = Color.rgb(44, 44, 46);
            Utils.DANGER = Color.rgb(255, 69, 58);
            Utils.QUOTE_BG = 0x1AFFFFFF;
        } else {
            // 白昼: 纯白背景 + 浅灰卡片 + 蓝色强调
            Utils.BG = Color.WHITE;
            Utils.CARD = Color.rgb(244, 246, 249); // 浅灰卡片, 与白色背景区分
            Utils.CARD2 = Color.rgb(233, 236, 240);
            Utils.TEXT = Color.rgb(26, 26, 26);
            Utils.TEXT_DIM = Color.rgb(120, 128, 138);
            Utils.ACCENT = Color.rgb(26, 115, 232);
            Utils.ACCENT_DARK = Color.rgb(21, 87, 176);
            Utils.BUBBLE_OUT = Color.rgb(207, 231, 255);
            Utils.BUBBLE_IN = Color.WHITE;
            Utils.DIVIDER = Color.rgb(233, 236, 239);
            Utils.DANGER = Color.rgb(229, 57, 53);
            Utils.QUOTE_BG = 0x14000000;
        }
        return changed;
    }
}
