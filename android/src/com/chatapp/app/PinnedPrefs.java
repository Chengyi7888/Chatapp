package com.chatapp.app;

import android.content.Context;
import android.content.SharedPreferences;

/** 会话置顶: 本地偏好 (每个用户自己的设备上保存) */
public class PinnedPrefs {
    private static SharedPreferences prefs;

    public static void init(Context c) {
        if (prefs == null) {
            prefs = c.getApplicationContext().getSharedPreferences("chatapp_pins", Context.MODE_PRIVATE);
        }
    }

    public static boolean isPinned(Context c, String key) {
        init(c);
        return prefs.getBoolean("pin_" + key, false);
    }

    public static void setPinned(Context c, String key, boolean pinned) {
        init(c);
        prefs.edit().putBoolean("pin_" + key, pinned).apply();
    }
}
