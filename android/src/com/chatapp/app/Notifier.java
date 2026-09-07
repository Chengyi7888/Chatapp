package com.chatapp.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.Calendar;

/** 新消息系统通知: 私聊/群聊收到消息时在通知栏提醒 */
public class Notifier {

    private static final String CHANNEL_ID = "chatapp_messages";

    /** 是否允许发通知 (Android 13+ 需要运行时权限) */
    public static boolean canNotify(Context ctx) {
        if (Build.VERSION.SDK_INT >= 33) {
            return ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < 26) return;
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, I18n.t("channel_messages"), NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription(I18n.t("channel_messages_desc"));
            nm.createNotificationChannel(ch);
        } catch (Exception ignored) {
        }
    }

    /** 免打扰时段内不提醒 */
    public static boolean dndAllowed(Context ctx) {
        try {
            SharedPreferences p = ctx.getSharedPreferences("chatapp_dnd", Context.MODE_PRIVATE);
            if (!p.getBoolean("enabled", false)) return true;
            int start = p.getInt("start", 23);
            int end = p.getInt("end", 7);
            int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if (start <= end) return !(h >= start && h < end);
            return !(h >= start || h < end);
        } catch (Exception ignored) {
            return true;
        }
    }

    public static void notifyMessage(Context ctx, String title, String text) {
        if (!canNotify(ctx)) return;
        if (!dndAllowed(ctx)) return;
        try {
            ensureChannel(ctx);
            Intent i = new Intent(ctx, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(ctx, 0, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(ctx, CHANNEL_ID)
                    : new Notification.Builder(ctx);
            b.setSmallIcon(android.R.drawable.stat_notify_chat)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify((int) (System.currentTimeMillis() & 0x7fffffff), b.build());
        } catch (Exception ignored) {
        }
    }
}
