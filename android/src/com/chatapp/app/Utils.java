package com.chatapp.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** 通用工具: 主题颜色、单位换算、控件创建 */
public class Utils {
    // 暗黑模式配色: 纯黑背景 + 白色文字 + 亮绿强调
    public static int BG = Color.rgb(0, 0, 0);
    public static int CARD = Color.rgb(28, 28, 30);
    public static int CARD2 = Color.rgb(44, 44, 46);
    public static int TEXT = Color.WHITE;
    public static int TEXT_DIM = Color.rgb(142, 142, 147);
    public static int ACCENT = Color.rgb(37, 211, 102);
    public static int ACCENT_DARK = Color.rgb(20, 150, 80);
    public static int BUBBLE_OUT = Color.rgb(31, 44, 51);
    public static int BUBBLE_IN = Color.rgb(38, 45, 49);
    public static int DIVIDER = Color.rgb(44, 44, 46);
    public static int DANGER = Color.rgb(255, 69, 58);
    public static int QUOTE_BG = 0x1AFFFFFF;

    /** 全局字体缩放系数 */
    public static float fontScale = 1.0f;

    public static int dp(Context c, float v) {
        return (int) (c.getResources().getDisplayMetrics().density * v + 0.5f);
    }

    public static int sp(Context c, float v) {
        return (int) (c.getResources().getDisplayMetrics().scaledDensity * v * fontScale + 0.5f);
    }

    /** 从偏好读取字体缩放 */
    public static void loadFontScale(Context c) {
        try {
            fontScale = c.getSharedPreferences("chatapp_font", Context.MODE_PRIVATE).getFloat("scale", 1.0f);
        } catch (Exception ignored) {
        }
    }

    public static TextView tv(Context c, String text, float sizeSp, int color, int gravity) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(sizeSp * fontScale);
        t.setTextColor(color);
        t.setGravity(gravity);
        return t;
    }

    /** 圆角背景 */
    public static GradientDrawable bg(Context c, int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }
    /** Flat button: remove system default pressed shadow/elevation (light-mode shadow). */
    public static Button button(Context c) {
        Button b = new Button(c);
        b.setStateListAnimator(null);
        return b;
    }

    public static GradientDrawable ring(Context c, int fillColor, int strokeColor, int strokeDp, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fillColor);
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(dp(c, strokeDp), strokeColor);
        return g;
    }

    public static View hSpace(Context c, float hDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, hDp)));
        return v;
    }

    public static View vSpace(Context c, float wDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, wDp), 1));
        return v;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 0.7f)));
        v.setBackgroundColor(DIVIDER);
        return v;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams lp(int w, int h, float weight) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.weight = weight;
        return p;
    }

    /** 圆形头像: 深色底 + 亮绿描边 + 表情/首字 */
    public static TextView avatar(Context c, String emoji, int sizeDp, float textSp) {
        TextView t = new TextView(c);
        int s = dp(c, sizeDp);
        t.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        t.setGravity(Gravity.CENTER);
        t.setTextSize(textSp);
        t.setText(emoji);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(CARD2);
        g.setStroke(dp(c, 2), ACCENT);
        t.setBackground(g);
        return t;
    }

    /** 字母头像(联系人/会话), 按名字取稳定颜色 */
    public static TextView letterAvatar(Context c, String name, int sizeDp, float textSp) {
        TextView t = new TextView(c);
        int s = dp(c, sizeDp);
        t.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        t.setGravity(Gravity.CENTER);
        t.setTextColor(Color.WHITE);
        t.setTextSize(textSp);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        String first = (name == null || name.length() == 0) ? "?" : name.substring(0, 1).toUpperCase(Locale.getDefault());
        t.setText(first);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(colorFor(name == null ? "" : name));
        t.setBackground(g);
        return t;
    }

    /** 根据名字取稳定颜色 */
    public static int colorFor(String s) {
        int[] palette = {
                0xE57373, 0xF06292, 0xBA68C8, 0x9575CD, 0x7986CB, 0x64B5F6,
                0x4FC3F7, 0x4DB6AC, 0x81C784, 0xAED581, 0xFFB74D, 0xFF8A65
        };
        int h = s.hashCode();
        return palette[Math.abs(h) % palette.length];
    }

    /** 时间显示: 今天 HH:mm, 昨天"昨天", 更早 MM/dd */
    public static String timeText(long ts) {
        Date d = new Date(ts);
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        Calendar now = Calendar.getInstance();
        SimpleDateFormat hm = new SimpleDateFormat("HH:mm", Locale.getDefault());
        if (c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && c.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
            return hm.format(d);
        }
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && c.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
            return I18n.t("yesterday");
        }
        return new SimpleDateFormat("MM/dd", Locale.getDefault()).format(d);
    }

    public static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }

    /** 轻微振动 (30ms), 受 Theme.vibrate 开关控制 */
    public static void vibrate(Context c) {
        if (!Theme.vibrate) return;
        try {
            Vibrator v;
            if (Build.VERSION.SDK_INT >= 31) {
                VibratorManager vm = (VibratorManager) c.getSystemService(Context.VIBRATOR_SERVICE);
                v = vm == null ? null : vm.getDefaultVibrator();
            } else {
                v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (v == null || !v.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(30);
            }
        } catch (Exception ignored) {
        }
    }
}
