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

// Manifest 注册 AccountDeleteActivity
for (const m of [ROOT + '/android/AndroidManifest.xml', ROOT + '/build/AndroidManifest.xml']) {
  patch(m, [
    ['        <activity\n            android:name=".FavoritesActivity"',
     '        <activity\n            android:name=".AccountDeleteActivity"\n            android:exported="false"\n            android:screenOrientation="portrait" />\n\n        <activity\n            android:name=".FavoritesActivity"'],
  ]);
}

const HA = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';
patch(HA, [
  // 注销 -> 验证页面
  ['    private void confirmDeleteAccount() {\n        Ui.show(this, I18n.t("delete_account"), Ui.message(this, I18n.t("delete_account_confirm")), I18n.t("confirm"), new Ui.Click() {\n            @Override\n            public void onClick() {\n                try {\n                    JSONObject j = new JSONObject();\n                    j.put("type", "delete_account");\n                    Net.get().send(j);\n                } catch (Exception ignored) {\n                }\n            }\n        });\n    }',
   '    private void confirmDeleteAccount() {\n        Ui.show(this, I18n.t("delete_account"), Ui.message(this, I18n.t("delete_account_confirm")), I18n.t("confirm"), new Ui.Click() {\n            @Override\n            public void onClick() {\n                startActivity(new Intent(HomeActivity.this, AccountDeleteActivity.class));\n            }\n        });\n    }'],
  // 关于: 版本检测 + 升级
  ['    private void showAboutDialog() {\n        Ui.showSingle(this, I18n.t("about"), Ui.message(this, "Chatapp v2.2.7.0\\n" + I18n.t("current_server") + " " + Session.server + "\\n" + I18n.t("status_label") + " " + (Net.get().isConnected() ? I18n.t("connected") : I18n.t("disconnected"))), I18n.t("ok_good"), null);\n    }',
   '    private void showAboutDialog() {\n        final LinearLayout box = new LinearLayout(this);\n        box.setOrientation(LinearLayout.VERTICAL);\n        box.addView(Ui.message(this, "Chatapp v" + UpdateChecker.currentVersion(this) + "\\n"\n                + I18n.t("current_server") + " " + Session.server + "\\n"\n                + I18n.t("status_label") + " " + (Net.get().isConnected() ? I18n.t("connected") : I18n.t("disconnected"))));\n        final Button up = new Button(this);\n        up.setText(I18n.t("checking_update"));\n        up.setTextSize(14);\n        up.setTextColor(Color.WHITE);\n        up.setBackground(Utils.bg(this, Utils.ACCENT, 22));\n        up.setElevation(0);\n        up.setEnabled(false);\n        box.addView(Utils.hSpace(this, 10));\n        box.addView(up);\n        Ui.showSingle(this, I18n.t("about"), box, I18n.t("close"), null);\n        UpdateChecker.checkAsync(this, new UpdateChecker.Result() {\n            @Override\n            public void onResult(boolean hasUpdate, String version, String file) {\n                if (hasUpdate) {\n                    up.setText(I18n.t("upgrade") + " (v" + version + ")");\n                    up.setEnabled(true);\n                    up.setOnClickListener(new View.OnClickListener() {\n                        @Override\n                        public void onClick(View v) {\n                            UpdateChecker.downloadLatest(HomeActivity.this);\n                        }\n                    });\n                } else {\n                    up.setText(I18n.t("current_latest"));\n                    up.setTextColor(Utils.TEXT_DIM);\n                    up.setBackground(Utils.bg(HomeActivity.this, Utils.CARD2, 22));\n                }\n            }\n        });\n    }'],
]);
console.log('DONE');
