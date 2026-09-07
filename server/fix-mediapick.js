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

const MP = ROOT + '/android/src/com/chatapp/app/MediaPick.java';
patch(MP, [
  // 相册直接回调, 拍照进编辑器; 扫码模式直接回调
  ['        if (bmp == null) {\n            Ui.banner(a, I18n.t("cannot_read_image"));\n            return true;\n        }\n        showEditor(a, bmp, cb);\n        return true;\n    }',
   '        if (bmp == null) {\n            Ui.banner(a, I18n.t("cannot_read_image"));\n            return true;\n        }\n        boolean fromCamera = (data == null || data.getData() == null);\n        if (fromCamera && !scanMode) {\n            showEditor(a, bmp, cb);\n        } else {\n            cb.onBitmap(bmp);\n        }\n        return true;\n    }\n\n    /** 扫码模式: 拍照后直接回调原图, 不进编辑器 */\n    public static void scan(Activity a, int requestCode, Callback cb) {\n        scanMode = true;\n        pending = cb;\n        lastRequest = requestCode;\n        takePhoto(a, requestCode);\n    }\n\n    private static boolean scanMode = false;'],
  ['        lastRequest = -1;\n        final Callback cb = pending;\n        pending = null;\n        if (cb == null) return true;\n        if (resultCode != Activity.RESULT_OK) return true;',
   '        lastRequest = -1;\n        final Callback cb = pending;\n        pending = null;\n        scanMode = false;\n        if (cb == null) return true;\n        if (resultCode != Activity.RESULT_OK) return true;'],
  // 重拍时不是扫码
  ['                d.dismiss();\n                pending = cb;\n                lastRequest = 9002;\n                launchCamera(a, 9002);',
   '                d.dismiss();\n                scanMode = false;\n                pending = cb;\n                lastRequest = 9002;\n                launchCamera(a, 9002);'],
]);

// Manifest: CAMERA + MediaProvider
for (const m of [ROOT + '/android/AndroidManifest.xml', ROOT + '/build/AndroidManifest.xml']) {
  patch(m, [
    ['<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />',
     '<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />\n    <uses-permission android:name="android.permission.CAMERA" />'],
    ['        <provider\n            android:name=".ApkProvider"',
     '        <provider\n            android:name=".MediaProvider"\n            android:authorities="com.chatapp.app.media"\n            android:exported="false"\n            android:grantUriPermissions="true" />\n\n        <provider\n            android:name=".ApkProvider"'],
  ]);
}

// I18n 键
patch(ROOT + '/android/src/com/chatapp/app/I18n.java', [
  ['        put("cannot_open_channel", "无法打开该应用", "無法開啟該應用", "Can\'t open this app");',
   '        put("cannot_open_channel", "无法打开该应用", "無法開啟該應用", "Can\'t open this app");\n        put("choose_source", "选择方式", "選擇方式", "Choose source");\n        put("take_photo", "拍照", "拍照", "Take photo");\n        put("choose_image", "从相册选择", "從相冊選擇", "From gallery");\n        put("cannot_open_camera", "无法打开相机", "無法開啟相機", "Can\'t open camera");\n        put("rotate", "旋转", "旋轉", "Rotate");\n        put("retake", "重拍", "重拍", "Retake");\n        put("done", "完成", "完成", "Done");\n        put("qr_scan", "扫一扫", "掃一掃", "Scan");\n        put("scan_manual_hint", "扫码识别待完善，请手动输入对方用户名添加", "掃碼識別待完善，請手動輸入對方用戶名新增", "Scan recognition pending, enter username to add");'],
]);
console.log('DONE');
