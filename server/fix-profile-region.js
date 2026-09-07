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

const PA = ROOT + '/android/src/com/chatapp/app/ProfileActivity.java';

// 1) 所在地/出生地 改用滑轮
patch(PA, [
  ['        card3.addView(valueRow(I18n.t("location"), location.isEmpty() ? "—" : location, new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                showTextDialog(I18n.t("location"), I18n.t("location_hint"), location, "location");\n            }\n        }));',
   '        card3.addView(valueRow(I18n.t("location"), location.isEmpty() ? "—" : location, new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                showRegionDialog("location");\n            }\n        }));'],
  ['        card3.addView(valueRow(I18n.t("birthplace"), birthplace.isEmpty() ? "—" : birthplace, new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                showTextDialog(I18n.t("birthplace"), I18n.t("birthplace_hint"), birthplace, "birthplace");\n            }\n        }));',
   '        card3.addView(valueRow(I18n.t("birthplace"), birthplace.isEmpty() ? "—" : birthplace, new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                showRegionDialog("birthplace");\n            }\n        }));'],
  // 2) 选中项(性别/生日/职业)绿色高亮
  ['        TextView v = Utils.tv(this, value, 14, Utils.TEXT_DIM, Gravity.END);\n        v.setSingleLine(true);',
   '        int vColor = Utils.TEXT_DIM;\n        if (!value.isEmpty() && !value.equals("—")\n                && (label.equals(I18n.t("gender")) || label.equals(I18n.t("birthday")) || label.equals(I18n.t("job")))) {\n            vColor = Utils.ACCENT;\n        }\n        TextView v = Utils.tv(this, value, 14, vColor, Gravity.END);\n        v.setSingleLine(true);'],
  // 3) 邮箱校验
  ['            @Override\n            public void onClick() {\n                String v = input.getText().toString().trim();\n                sendProfileField(field, v);\n            }\n        });\n    }\n\n    private void sendProfileField(String field, String value) {',
   '            @Override\n            public void onClick() {\n                String v = input.getText().toString().trim();\n                if ("email".equals(field) && !v.isEmpty() && !isValidEmail(v)) {\n                    Ui.banner(ProfileActivity.this, I18n.t("email_invalid"));\n                    return;\n                }\n                sendProfileField(field, v);\n            }\n        });\n    }\n\n    private boolean isValidEmail(String e) {\n        return e != null && e.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,}$");\n    }\n\n    private void sendProfileField(String field, String value) {'],
]);

// 4) 省市区滑轮方法 (插到 showTextDialog 前)
{
  let t = fs.readFileSync(PA, 'utf8');
  const anchor = '    private void showTextDialog(final String title, String hint, String initial, final String field) {';
  const methods = `    private void showRegionDialog(final String field) {
        final List<String> provinces = RegionData.provinces();
        final NumberPicker pPicker = new NumberPicker(this);
        final NumberPicker cPicker = new NumberPicker(this);
        final NumberPicker dPicker = new NumberPicker(this);
        pPicker.setMinValue(0);
        pPicker.setMaxValue(provinces.size() - 1);
        pPicker.setDisplayedValues(provinces.toArray(new String[0]));
        pPicker.setWrapSelectorWheel(false);
        cPicker.setWrapSelectorWheel(false);
        dPicker.setWrapSelectorWheel(false);

        // 解析当前值
        String cur = "location".equals(field) ? location : birthplace;
        int pi = 0, ci = 0, di = 0;
        if (cur != null && !cur.isEmpty()) {
            String[] parts = cur.split("-");
            if (parts.length >= 1) {
                int idx = provinces.indexOf(parts[0]);
                if (idx >= 0) pi = idx;
            }
            List<String> cities = RegionData.cities(provinces.get(pi));
            if (parts.length >= 2) {
                int idx = cities.indexOf(parts[1]);
                if (idx >= 0) ci = idx;
            }
            List<String> ds = RegionData.districts(provinces.get(pi), cities.get(ci));
            if (parts.length >= 3) {
                int idx = ds.indexOf(parts[2]);
                if (idx >= 0) di = idx;
            }
        }

        final Runnable refresh = new Runnable() {
            @Override
            public void run() {
                String province = provinces.get(pPicker.getValue());
                List<String> cities = RegionData.cities(province);
                cPicker.setDisplayedValues(cities.toArray(new String[0]));
                cPicker.setMaxValue(cities.size() - 1);
                if (cPicker.getValue() > cities.size() - 1) cPicker.setValue(0);
                String city = cities.get(cPicker.getValue());
                List<String> ds = RegionData.districts(province, city);
                dPicker.setDisplayedValues(ds.toArray(new String[0]));
                dPicker.setMaxValue(ds.size() - 1);
                if (dPicker.getValue() > ds.size() - 1) dPicker.setValue(0);
            }
        };

        pPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                cPicker.setValue(0);
                dPicker.setValue(0);
                refresh.run();
            }
        });
        cPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                dPicker.setValue(0);
                refresh.run();
            }
        });

        pPicker.setValue(pi);
        refresh.run();
        cPicker.setValue(ci);
        refresh.run();
        dPicker.setValue(di);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.addView(pPicker, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(cPicker, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(dPicker, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Ui.show(this, "location".equals(field) ? I18n.t("location") : I18n.t("birthplace"), box, I18n.t("confirm"), new Ui.Click() {
            @Override
            public void onClick() {
                String province = provinces.get(pPicker.getValue());
                if ("-".equals(province)) {
                    sendProfileField(field, "");
                    return;
                }
                String city = RegionData.cities(province).get(cPicker.getValue());
                String district = RegionData.districts(province, city).get(dPicker.getValue());
                String val = province + "-" + city;
                if (!"-".equals(district)) val += "-" + district;
                sendProfileField(field, val);
            }
        });
    }

    private void showTextDialog(final String title, String hint, String initial, final String field) {`;
  if (t.includes(anchor)) {
    t = t.split(anchor).join(methods);
    fs.writeFileSync(PA, t);
    console.log('[ProfileActivity.java] region wheel added');
  } else {
    console.log('showTextDialog anchor not found');
  }
}

// 5) I18n 邮箱不合法
patch(ROOT + '/android/src/com/chatapp/app/I18n.java', [
  ['        put("email_hint", "输入邮箱地址", "輸入郵箱地址", "Enter email");',
   '        put("email_hint", "输入邮箱地址", "輸入郵箱地址", "Enter email");\n        put("email_invalid", "当前邮箱不合法", "目前郵箱不合法", "Invalid email address");'],
]);
console.log('DONE');
