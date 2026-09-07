'use strict';
const fs = require('fs');
const ROOT = 'C:/Users/cheng/Documents/Codex/Chatapp';

function patch(file, pairs) {
  let t = fs.readFileSync(file, 'utf8');
  let ok = 0, miss = [];
  for (const [old, neu] of pairs) {
    if (t.includes(old)) { t = t.split(old).join(neu); ok++; }
    else miss.push(old.slice(0, 70));
  }
  fs.writeFileSync(file, t);
  console.log('[' + file.split('/').pop() + '] ' + ok + ' patched' + (miss.length ? '  MISS: ' + miss.join(' | ') : ''));
}

const UC = ROOT + '/android/src/com/chatapp/app/UpdateChecker.java';

// imports
patch(UC, [
  ['import android.net.Uri;\nimport android.os.Handler;',
   'import android.net.Uri;\nimport android.os.Build;\nimport android.os.Handler;\nimport android.provider.Settings;'],
  // 安装权限判断 + 设置引导 + 自动重试
  ['    private static void installApk(Activity activity, File apk) {\n        try {\n            Uri uri = Uri.parse("content://com.chatapp.app.fileprovider/" + Uri.encode(apk.getName()));\n            Intent i = new Intent(Intent.ACTION_VIEW);\n            i.setDataAndType(uri, "application/vnd.android.package-archive");\n            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);\n            activity.startActivity(i);\n        } catch (Exception e) {\n            Ui.banner(activity, I18n.t("install_failed"));\n        }\n    }\n}',
   '    private static boolean pendingInstall = false;\n\n    private static void installApk(final Activity activity, File apk) {\n        // Android 8+ 需要"允许安装未知应用"权限\n        if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) {\n            pendingInstall = true;\n            Ui.show(activity, I18n.t("install_perm_title"), Ui.message(activity, I18n.t("install_perm_msg")), I18n.t("install_go_settings"), new Ui.Click() {\n                @Override\n                public void onClick() {\n                    try {\n                        Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,\n                                Uri.parse("package:" + activity.getPackageName()));\n                        activity.startActivity(i);\n                    } catch (Exception e) {\n                        try {\n                            activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES));\n                        } catch (Exception ignored) {\n                        }\n                    }\n                }\n            });\n            return;\n        }\n        try {\n            Uri uri = Uri.parse("content://com.chatapp.app.fileprovider/" + Uri.encode(apk.getName()));\n            Intent i = new Intent(Intent.ACTION_VIEW);\n            i.setDataAndType(uri, "application/vnd.android.package-archive");\n            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);\n            activity.startActivity(i);\n            pendingInstall = false;\n        } catch (Exception e) {\n            Ui.banner(activity, I18n.t("install_failed"));\n        }\n    }\n\n    /** 从系统设置返回后自动重试安装 */\n    public static void retryPendingInstall(Activity activity) {\n        if (!pendingInstall) return;\n        if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) return;\n        File f = new File(activity.getCacheDir(), "chatapp-update.apk");\n        if (f.exists() && f.length() > 100000) {\n            pendingInstall = false;\n            installApk(activity, f);\n        }\n    }\n}'],
]);

// I18n 键
const I18N = ROOT + '/android/src/com/chatapp/app/I18n.java';
patch(I18N, [
  ['        put("install_failed", "安装失败，请检查系统设置", "安裝失敗，請檢查系統設定", "Install failed, check settings");',
   '        put("install_failed", "安装失败，请检查系统设置", "安裝失敗，請檢查系統設定", "Install failed, check settings");\n        put("install_perm_title", "需要安装权限", "需要安裝權限", "Install permission required");\n        put("install_perm_msg", "请允许本应用安装未知应用，才能完成更新安装", "請允許本應用安裝未知應用，才能完成更新安裝", "Allow this app to install unknown apps to finish the update");\n        put("install_go_settings", "去设置", "前往設定", "Go to settings");'],
]);

// HomeActivity onResume 自动重试
const HA = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';
patch(HA, [
  ['        if (Theme.apply(this)) reapplyTheme();',
   '        if (Theme.apply(this)) reapplyTheme();\n        UpdateChecker.retryPendingInstall(this);'],
]);
console.log('DONE');
