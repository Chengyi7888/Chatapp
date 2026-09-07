package com.chatapp.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** 圆形头像: 有头像时从服务器加载图片, 否则显示字母占位 (带内存缓存) */
public class AvatarManager {

    private static final Map<String, Bitmap> cache = new HashMap<>();
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static String host = "";

    public static void setHost(String h) {
        host = h;
    }

    public static void clear(String username) {
        if (username != null) cache.remove(username);
    }

    /** 返回圆形头像容器 (可点击, 内部自动加载头像或字母占位) */
    public static View avatarView(Context ctx, String username, String nickname, boolean hasAvatar,
                                  int sizeDp, float textSp) {
        int s = dp(ctx, sizeDp);
        FrameLayout box = new FrameLayout(ctx);
        box.setLayoutParams(new FrameLayout.LayoutParams(s, s));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Utils.CARD2);
        box.setBackground(bg);

        TextView fallback = new TextView(ctx);
        fallback.setLayoutParams(new FrameLayout.LayoutParams(s, s));
        fallback.setGravity(Gravity.CENTER);
        fallback.setTextColor(Utils.TEXT);
        fallback.setTextSize(textSp);
        fallback.setTypeface(Typeface.DEFAULT_BOLD);
        String first = (nickname == null || nickname.length() == 0) ? "?" : nickname.substring(0, 1).toUpperCase(Locale.getDefault());
        fallback.setText(first);
        box.addView(fallback);

        if (hasAvatar) {
            ImageView iv = new ImageView(ctx);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GradientDrawable clip = new GradientDrawable();
            clip.setShape(GradientDrawable.OVAL);
            iv.setBackground(clip);
            iv.setClipToOutline(true);
            box.addView(iv, new FrameLayout.LayoutParams(s, s));
            load(ctx, username, iv);
        }
        return box;
    }


    /** 聊天图片消息加载: urlPath 形如 /files/xxx.jpg */

    public interface BitmapCallback {
        void onBitmap(Bitmap b);
    }

    /** 异步下载图片并返回 Bitmap(主线程回调) */
    public static void loadBitmap(final Context ctx, final String urlPath, final BitmapCallback cb) {
        Bitmap cached = cache.get(urlPath);
        if (cached != null) {
            if (cb != null) cb.onBitmap(cached);
            return;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = null;
                try {
                    String h = host;
                    if (h == null || h.isEmpty()) {
                        String[] sp = Session.serverParts();
                        h = sp[0];
                    }
                    String url = "http://" + h + ":8900" + urlPath;
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setConnectTimeout(6000);
                    c.setReadTimeout(6000);
                    InputStream in = c.getInputStream();
                    bmp = BitmapFactory.decodeStream(in);
                    in.close();
                    c.disconnect();
                    if (bmp != null) cache.put(urlPath, bmp);
                } catch (Exception ignored) {
                }
                final Bitmap fb = bmp;
                H.post(new Runnable() {
                    @Override
                    public void run() {
                        if (cb != null) cb.onBitmap(fb);
                    }
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }
    /** 加载任意 http 图片地址 */
    public static void loadBitmapUrl(final Context ctx, final String url, final BitmapCallback cb) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = null;
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setConnectTimeout(6000);
                    c.setReadTimeout(6000);
                    InputStream in = c.getInputStream();
                    bmp = BitmapFactory.decodeStream(in);
                    in.close();
                    c.disconnect();
                } catch (Exception ignored) {
                }
                final Bitmap fb = bmp;
                H.post(new Runnable() {
                    @Override
                    public void run() {
                        if (cb != null) cb.onBitmap(fb);
                    }
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public static void loadFileImage(final Context ctx, final String urlPath, final ImageView iv) {
        Bitmap cached = cache.get(urlPath);
        if (cached != null) {
            iv.setImageBitmap(cached);
            return;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String h = host;
                    if (h == null || h.isEmpty()) {
                        String[] sp = Session.serverParts();
                        h = sp[0];
                    }
                    String url = "http://" + h + ":8900" + urlPath;
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setConnectTimeout(6000);
                    c.setReadTimeout(6000);
                    InputStream in = c.getInputStream();
                    final Bitmap bmp = BitmapFactory.decodeStream(in);
                    in.close();
                    c.disconnect();
                    if (bmp != null) {
                        cache.put(urlPath, bmp);
                        H.post(new Runnable() {
                            @Override
                            public void run() {
                                iv.setImageBitmap(bmp);
                            }
                        });
                    }
                } catch (Exception ignored) {
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private static void load(final Context ctx, final String username, final ImageView iv) {
        Bitmap cached = cache.get(username);
        if (cached != null) {
            iv.setImageBitmap(cached);
            return;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String h = host;
                    if (h == null || h.isEmpty()) {
                        String[] sp = Session.serverParts();
                        h = sp[0];
                    }
                    String url = "http://" + h + ":8900/avatar/" + URLEncoder.encode(username, "UTF-8") + ".jpg";
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setConnectTimeout(4000);
                    c.setReadTimeout(4000);
                    InputStream in = c.getInputStream();
                    final Bitmap bmp = BitmapFactory.decodeStream(in);
                    in.close();
                    c.disconnect();
                    if (bmp != null) {
                        cache.put(username, bmp);
                        H.post(new Runnable() {
                            @Override
                            public void run() {
                                iv.setImageBitmap(bmp);
                            }
                        });
                    }
                } catch (Exception ignored) {
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private static int dp(Context c, float v) {
        return (int) (c.getResources().getDisplayMetrics().density * v + 0.5f);
    }
}
