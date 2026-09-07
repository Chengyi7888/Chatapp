package com.chatapp.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Environment;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** 媒体辅助: 图片预览(缩放)/保存/转发, 文件下载, 图片文件编码发送 (单聊与群聊共用) */
public class MediaHelper {

    public interface OnForward {
        void onForward(Models.ChatMsg m);
    }

    /** 根据消息中的 url 拼出完整 http 地址 */
    public static String fullUrl(Context c, String url) {
        if (url == null || url.isEmpty()) return "";
        if (url.startsWith("http")) return url;
        String[] sp = Session.serverParts();
        return "http://" + sp[0] + ":" + UpdateChecker.UPDATE_PORT + url;
    }

    /** 图片预览: 可缩放、保存、转发 */
    public static void showImagePreview(final Activity a, final Models.ChatMsg m, final OnForward onForward) {
        try {
            String u = fullUrl(a, m.url);
            if (u.isEmpty()) return;
            final Dialog d = new Dialog(a);
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            FrameLayout root = new FrameLayout(a);
            root.setBackgroundColor(0xCC000000);
            final ImageView img = new ImageView(a);
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            root.addView(img, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            final Matrix matrix = new Matrix();
            final ScaleGestureDetector sgd = new ScaleGestureDetector(a, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    float f = detector.getScaleFactor();
                    matrix.postScale(f, f);
                    img.setImageMatrix(matrix);
                    return true;
                }
            });
            img.setScaleType(ImageView.ScaleType.MATRIX);
            img.setOnTouchListener(new View.OnTouchListener() {
                float lastX, lastY;

                @Override
                public boolean onTouch(View v, MotionEvent ev) {
                    sgd.onTouchEvent(ev);
                    switch (ev.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            lastX = ev.getRawX();
                            lastY = ev.getRawY();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            if (!sgd.isInProgress()) {
                                float dx = ev.getRawX() - lastX;
                                float dy = ev.getRawY() - lastY;
                                matrix.postTranslate(dx, dy);
                                img.setImageMatrix(matrix);
                                lastX = ev.getRawX();
                                lastY = ev.getRawY();
                            }
                            return true;
                    }
                    return false;
                }
            });

            LinearLayout bar = new LinearLayout(a);
            bar.setOrientation(LinearLayout.HORIZONTAL);
            bar.setGravity(Gravity.CENTER);
            bar.setPadding(Utils.dp(a, 12), Utils.dp(a, 10), Utils.dp(a, 12), Utils.dp(a, 10));
            bar.addView(btn(a, I18n.t("save"), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveImage(a, m);
                }
            }));
            bar.addView(Utils.vSpace(a, 10));
            if (onForward != null) {
                bar.addView(btn(a, I18n.t("forward"), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        d.dismiss();
                        onForward.onForward(m);
                    }
                }));
                bar.addView(Utils.vSpace(a, 10));
            }
            bar.addView(btn(a, I18n.t("close"), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    d.dismiss();
                }
            }));
            FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            blp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            blp.bottomMargin = Utils.dp(a, 30);
            root.addView(bar, blp);

            d.setContentView(root);
            Window w = d.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new ColorDrawable(0xCC000000));
                w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            }
            d.show();

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        java.net.URL url = new java.net.URL(u);
                        java.net.HttpURLConnection c = (java.net.HttpURLConnection) url.openConnection();
                        c.setConnectTimeout(6000);
                        c.setReadTimeout(6000);
                        InputStream in = c.getInputStream();
                        final Bitmap bmp = BitmapFactory.decodeStream(in);
                        in.close();
                        if (bmp != null) {
                            a.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    img.setImageBitmap(bmp);
                                    img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                }
                            });
                        }
                    } catch (Exception ignored) {
                    }
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (Exception ignored) {
        }
    }

    private static TextView btn(Activity a, String text, View.OnClickListener onClick) {
        TextView t = Utils.tv(a, text, 14, Utils.TEXT, Gravity.CENTER);
        t.setBackground(Utils.bg(a, 0x66FFFFFF, 18));
        t.setPadding(Utils.dp(a, 18), Utils.dp(a, 8), Utils.dp(a, 18), Utils.dp(a, 8));
        t.setOnClickListener(onClick);
        return t;
    }

    public static void saveImage(Context ctx, Models.ChatMsg m) {
        try {
            String u = fullUrl(ctx, m.url);
            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(u));
            req.setTitle(m.name == null || m.name.isEmpty() ? "chatapp_image.jpg" : m.name);
            req.setMimeType("image/jpeg");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "Chatapp");
            dm.enqueue(req);
            Utils.toast(ctx, I18n.t("saving_image_started"));
        } catch (Exception e) {
            Utils.toast(ctx, I18n.t("save_failed"));
        }
    }

    public static void downloadFile(Context ctx, Models.ChatMsg m) {
        try {
            String u = fullUrl(ctx, m.url);
            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(u));
            req.setTitle(m.name == null || m.name.isEmpty() ? "chatapp_file" : m.name);
            req.setDescription(I18n.t("file_download_desc"));
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    m.name == null || m.name.isEmpty() ? "chatapp_file.bin" : m.name);
            dm.enqueue(req);
            Utils.toast(ctx, I18n.t("download_file_started"));
        } catch (Exception e) {
            Utils.toast(ctx, I18n.t("download_failed"));
        }
    }

    /** 读取 Uri 内容并按 kind 编码为 base64, 回调发送 */
    public static void encodeAndSend(final Activity a, final Uri uri, final String kind, final SendCallback cb) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream in = a.getContentResolver().openInputStream(uri);
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    byte[] buf = new byte[16384];
                    int n;
                    long total = 0;
                    while ((n = in.read(buf)) > 0) {
                        bout.write(buf, 0, n);
                        total += n;
                        if (total > (kind.equals("image") ? 2500000 : 5000000)) {
                            in.close();
                            a.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Utils.toast(a, I18n.t("file_too_large_send"));
                                }
                            });
                            return;
                        }
                    }
                    in.close();
                    final byte[] data = bout.toByteArray();
                    final String name = uri.getLastPathSegment() == null ? "file" : uri.getLastPathSegment();
                    if (kind.equals("image")) {
                        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                        if (bmp != null) {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            int q = 88;
                            bmp.compress(Bitmap.CompressFormat.JPEG, q, out);
                            while (out.size() > 2400000 && q > 40) {
                                out.reset();
                                q -= 12;
                                bmp.compress(Bitmap.CompressFormat.JPEG, q, out);
                            }
                            final byte[] compressed = out.toByteArray();
                            final String fname = name.endsWith(".jpg") || name.endsWith(".jpeg") ? name : name + ".jpg";
                            a.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    cb.onEncoded(compressed, fname);
                                }
                            });
                            return;
                        }
                    }
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cb.onEncoded(data, name);
                        }
                    });
                } catch (Exception e) {
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.toast(a, I18n.t("read_failed"));
                        }
                    });
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public interface SendCallback {
        void onEncoded(byte[] data, String name);
    }

    public static String base64(byte[] data) {
        return android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP);
    }
}
