package com.chatapp.app;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

/** 全局崩溃捕获: 崩溃时将堆栈上报到服务器日志 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        I18n.init(this);
        Net.get().setExtraListener(new Net.Listener() {
            @Override
            public void onConnected() {
            }

            @Override
            public void onMessage(JSONObject obj) {
                try {
                    String type = obj.optString("type");
                    String from = obj.optString("from");
                    if (from == null || from.isEmpty() || from.equals(Session.username)) return;
                    String text = obj.optString("text");
                    String kind = obj.optString("kind", "text");
                    if ("image".equals(kind)) text = "[" + I18n.t("image") + "]";
                    else if ("file".equals(kind)) text = "[" + I18n.t("file") + "]";
                    if ("new_message".equals(type) || "new_group_message".equals(type)) {
                        Notifier.notifyMessage(getApplicationContext(), obj.optString("nickname", from), text);
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onDisconnected(String reason) {
            }
        });
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append(throwable.toString()).append('\n');
                    for (StackTraceElement e : throwable.getStackTrace()) {
                        sb.append("  at ").append(e.toString()).append('\n');
                    }
                    String stack = sb.toString();
                    Log.e("ChatappCrash", stack);
                    try {
                        JSONObject o = new JSONObject();
                        o.put("type", "crash");
                        o.put("stack", stack);
                        o.put("model", Build.MODEL);
                        o.put("android", Build.VERSION.RELEASE);
                        Net.get().send(o);
                    } catch (Exception ignored) {
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }
}
