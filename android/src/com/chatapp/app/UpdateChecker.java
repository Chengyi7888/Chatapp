package com.chatapp.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/** 检查更新: 通过系统下载管理器下载, 下载完成点通知安装 (稳定可靠) */
public class UpdateChecker {

    public static final int UPDATE_PORT = 8900;
    private static boolean checkedOnce = false;
    private static String lastFile = "Chatapp.apk";

    public interface Result {
        void onResult(boolean hasUpdate, String version, String file);
    }

    public static boolean isCheckedOnce() {
        return checkedOnce;
    }

    public static void setCheckedOnce() {
        checkedOnce = true;
    }

    /** 异步检查更新, 结果通过回调返回 */
    public static void checkAsync(final Activity activity, final Result result) {
        final Handler h = new Handler(Looper.getMainLooper());
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean hasUpdate = false;
                String version = "";
                String file = "Chatapp.apk";
                try {
                    String[] sp = Session.serverParts();
                    URL u = new URL("http://" + sp[0] + ":" + UPDATE_PORT + "/version.json");
                    HttpURLConnection c = (HttpURLConnection) u.openConnection();
                    c.setConnectTimeout(4000);
                    c.setReadTimeout(4000);
                    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    r.close();
                    JSONObject o = new JSONObject(sb.toString());
                    version = o.optString("version", "");
                    file = o.optString("file", "Chatapp.apk");
                    String current = currentVersion(activity);
                    hasUpdate = !version.isEmpty() && !version.equals(current);
                    lastFile = file;
                } catch (Exception ignored) {
                }
                final boolean fu = hasUpdate;
                final String fv = version;
                final String ff = file;
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        if (result != null) result.onResult(fu, fv, ff);
                    }
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /** 兼容旧调用: silent=true 时不弹任何提示 */
    public static void check(final Activity activity, final boolean silent) {
        checkAsync(activity, new Result() {
            @Override
            public void onResult(boolean hasUpdate, String version, String file) {
                if (hasUpdate) {
                    if (!silent) showUpdateDialog(activity, version, file);
                } else if (!silent) {
                    Utils.toast(activity, I18n.f("latest_version", currentVersion(activity)));
                }
            }
        });
    }

    public static String currentVersion(Context ctx) {
        try {
            return ctx.getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    /** 开始下载: 系统下载管理器, 完成后通知栏点击安装 */
    public static void downloadLatest(Context ctx) {
        startDownload(ctx, lastFile);
    }

    private static void showUpdateDialog(final Activity activity, final String version, final String file) {
        Ui.show(activity, I18n.t("new_version_title"), Ui.message(activity, I18n.f("update_prompt", version)), I18n.t("update_now"), new Ui.Click() {
            @Override
            public void onClick() {
                startDownload(activity, file);
            }
        });
    }

    private static void startDownload(Context ctx, String file) {
        try {
            String[] sp = Session.serverParts();
            String url = "http://" + sp[0] + ":" + UPDATE_PORT + "/" + file;
            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
            req.setTitle(I18n.t("update_title"));
            req.setDescription(I18n.t("update_desc"));
            req.setMimeType("application/vnd.android.package-archive");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "chatapp.apk");
            dm.enqueue(req);
        } catch (Exception e) {
            Utils.toast(ctx, I18n.t("download_failed"));
        }
    }
}
