'use strict';
const fs = require('fs');
const p = 'C:/Users/cheng/Documents/Codex/Chatapp/android/src/com/chatapp/app/HomeActivity.java';
let h = fs.readFileSync(p, 'utf8');
const startMark = '        themeGroup.addView(followRow);';
const endMark = '\n    private void showRegionDialog() {';
const si = h.indexOf(startMark);
const ei = h.indexOf(endMark);
if (si >= 0 && ei > si) {
  const correct = `        themeGroup.addView(followRow);
        themeGroup.addView(Utils.divider(this));
        LinearLayout vibrateRow = new LinearLayout(this);
        vibrateRow.setOrientation(LinearLayout.HORIZONTAL);
        vibrateRow.setGravity(Gravity.CENTER_VERTICAL);
        vibrateRow.setPadding(Utils.dp(this, 16), Utils.dp(this, 14), Utils.dp(this, 12), Utils.dp(this, 14));
        vibrateRow.addView(Utils.tv(this, I18n.t("vibrate_feedback"), 15, Utils.TEXT, Gravity.START), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        android.widget.Switch vibrateSwitch = new android.widget.Switch(this);
        vibrateSwitch.setChecked(Theme.vibrate);
        vibrateSwitch.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                Theme.setVibrate(isChecked);
                Utils.vibrate(HomeActivity.this);
            }
        });
        vibrateRow.addView(vibrateSwitch);
        themeGroup.addView(vibrateRow);
        meLayout.addView(themeGroup);
    }

    private String langLabel() {
        String l = I18n.lang();
        if (I18n.ZH_HANT.equals(l)) return I18n.t("zh_hant");
        if (I18n.EN.equals(l)) return I18n.t("english");
        return I18n.t("zh_hans");
    }

    private void showLanguageRegionDialog() {
        Ui.list(this, I18n.t("language"), new String[]{
                        I18n.t("zh_hans"), I18n.t("zh_hant"), I18n.t("english")
                },
                new Ui.Click[]{
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                Session.updateLang(I18n.ZH_HANS);
                                reapplyTheme();
                            }
                        },
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                Session.updateLang(I18n.ZH_HANT);
                                reapplyTheme();
                            }
                        },
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                Session.updateLang(I18n.EN);
                                reapplyTheme();
                            }
                        }
                });
    }
`;
  h = h.slice(0, si) + correct + h.slice(ei);
  fs.writeFileSync(p, h);
  console.log('HomeActivity repaired');
} else {
  console.log('anchors not found si=' + si + ' ei=' + ei);
}
