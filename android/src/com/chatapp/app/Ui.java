package com.chatapp.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 自定义弹窗: 圆角深色卡片 + 灰色取消 / 绿色确认按钮 */
public class Ui {


    public interface Click {
        void onClick();
    }

    /** 标准双按钮弹窗: 灰色取消 + 绿色确认 */
    public static Dialog show(Activity a, String title, View content, String okText, Click ok) {
        return build(a, title, content, null, okText, ok);
    }

    /** 双按钮弹窗: 灰色取消(带回调) + 绿色确认 */
    public static Dialog show(Activity a, String title, View content, String okText, Click ok, Click cancel) {
        return build(a, title, content, cancel, okText, ok);
    }

    /** 单按钮弹窗(绿色) */
    public static Dialog showSingle(Activity a, String title, View content, String btnText, Click click) {
        return build(a, title, content, null, btnText, click);
    }

    /** 仅灰色取消按钮弹窗 */
    public static Dialog showGray(Activity a, String title, View content, Click cancel) {
        return build(a, title, content, cancel, null, null);
    }

    private static Dialog build(Activity a, String title, View content, Click cancel, String okText, Click ok) {
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(Utils.bg(a, Utils.CARD, 20));
        root.setPadding(dp(a, 20), dp(a, 18), dp(a, 20), dp(a, 16));

        if (title != null && !title.isEmpty()) {
            TextView t = Utils.tv(a, title, 17, Utils.TEXT, Gravity.START);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setPadding(0, 0, 0, dp(a, 12));
            root.addView(t);
        }
        if (content != null) {
            root.addView(content);
        }

        final Dialog[] holder = new Dialog[1];
        if (ok != null) {
            // 灰色取消 + 绿色确认
            root.addView(hSpace(a, 16));
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            Click cancelClick = cancel != null ? cancel : new Click() {
                @Override
                public void onClick() {
                }
            };
            row.addView(button(a, I18n.t("cancel"), Utils.CARD2, cancelClick, holder), lp(0, dp(a, 44), 1f));
            row.addView(vSpace(a, 10));
            row.addView(button(a, okText, Utils.ACCENT, ok, holder), lp(0, dp(a, 44), 1f));
            root.addView(row);
        } else if (cancel != null) {
            root.addView(hSpace(a, 16));
            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(button(a, I18n.t("cancel"), Utils.CARD2, cancel, holder), lp(0, dp(a, 44), 1f));
            root.addView(row);
        }

        Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(root);
        holder[0] = d;
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (a.getResources().getDisplayMetrics().widthPixels * 0.86f);
            w.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            w.setGravity(Gravity.CENTER);
        }
        d.show();
        return d;
    }

    /** 列表弹窗: 每项一行, 底部灰色取消 */
    public static Dialog list(Activity a, String title, String[] items, final Click[] clicks) {
        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        final Dialog[] holder = new Dialog[1];
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            TextView row = Utils.tv(a, items[i], 15, Utils.TEXT, Gravity.CENTER_VERTICAL);
            row.setPadding(dp(a, 8), dp(a, 13), dp(a, 8), dp(a, 13));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Dialog dd = holder[0];
                    if (dd != null && dd.isShowing()) dd.dismiss();
                    if (clicks != null && clicks[idx] != null) clicks[idx].onClick();
                }
            });
            list.addView(row);
            if (i < items.length - 1) list.addView(Utils.divider(a));
        }
        Dialog d = showGray(a, title, list, new Click() {
            @Override
            public void onClick() {
            }
        });
        holder[0] = d;
        return d;
    }

    /** 文本消息(内容区) */
    public static TextView message(Activity a, String text) {
        TextView t = Utils.tv(a, text, 14, Utils.TEXT_DIM, Gravity.START);
        t.setLineSpacing(0, 1.2f);
        return t;
    }

    /** 输入框(弹窗内容区) */

    /** 顶部横幅提示: 从屏幕顶部滑入, 2秒后自动消失; 5秒冷却防止连点 */
    /** 居中弹窗提示: 替代顶部横幅, 2秒后自动消失, 点击内容可立即关闭 */
    public static void popup(Activity a, String text) {
        try {
            LinearLayout root = new LinearLayout(a);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackground(Utils.bg(a, Utils.CARD, 20));
            root.setPadding(dp(a, 26), dp(a, 22), dp(a, 26), dp(a, 22));
            root.addView(Utils.tv(a, text == null ? "" : text, 15, Utils.TEXT, Gravity.CENTER));
            final Dialog d = new Dialog(a);
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            d.setContentView(root);
            Window w = d.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                w.setLayout((int) (a.getResources().getDisplayMetrics().widthPixels * 0.78f), WindowManager.LayoutParams.WRAP_CONTENT);
                w.setGravity(Gravity.CENTER);
            }
            d.setCancelable(true);
            d.show();
            root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        d.dismiss();
                    } catch (Exception ignored) {
                    }
                }
            });
            d.getWindow().getDecorView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (d.isShowing()) d.dismiss();
                    } catch (Exception ignored) {
                    }
                }
            }, 2000);
        } catch (Exception ignored) {
        }
    }
    public static EditText input(Activity a, String initial, String hint) {
        EditText e = new EditText(a);
        e.setSingleLine(true);
        e.setTextSize(15 * Utils.fontScale);
        e.setTextColor(Utils.TEXT);
        e.setHintTextColor(Utils.TEXT_DIM);
        e.setBackground(Utils.bg(a, Utils.CARD2, 12));
        e.setPadding(dp(a, 12), dp(a, 10), dp(a, 12), dp(a, 10));
        if (initial != null) e.setText(initial);
        if (hint != null) e.setHint(hint);
        return e;
    }

    private static Button button(final Activity a, String text, int bgColor, final Click c, final Dialog[] holder) {
        Button b = Utils.button(a);
        b.setText(text);
        b.setTextSize(15);
        b.setTextColor(Utils.TEXT);
        b.setBackground(Utils.bg(a, bgColor, 22));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dd = holder[0];
                if (dd != null && dd.isShowing()) dd.dismiss();
                if (c != null) c.onClick();
            }
        });
        return b;
    }

    private static int dp(Activity a, float v) {
        return (int) (a.getResources().getDisplayMetrics().density * v + 0.5f);
    }

    private static View hSpace(Activity a, float h) {
        View v = new View(a);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(a, h)));
        return v;
    }

    private static View vSpace(Activity a, float w) {
        View v = new View(a);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(a, w), 1));
        return v;
    }

    private static LinearLayout.LayoutParams lp(int w, int h, float weight) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.weight = weight;
        return p;
    }
}
