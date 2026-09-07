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
patch(PA, [
  // 头像/照片: 拍照/相册选择
  ['    private void pickAvatar() {\n        Intent i = new Intent(Intent.ACTION_GET_CONTENT);\n        i.setType("image/*");\n        try {\n            startActivityForResult(Intent.createChooser(i, I18n.t("avatar")), PICK_AVATAR);\n        } catch (Exception e) {\n            Utils.toast(this, I18n.t("cannot_open_picker"));\n        }\n    }',
   '    private void pickAvatar() {\n        MediaPick.choose(this, PICK_AVATAR, new MediaPick.Callback() {\n            @Override\n            public void onBitmap(Bitmap bmp) {\n                uploadBitmap(bmp, "set_avatar", null);\n            }\n        });\n    }'],
  ['    private void pickPhoto() {\n        Intent i = new Intent(Intent.ACTION_GET_CONTENT);\n        i.setType("image/*");\n        try {\n            startActivityForResult(Intent.createChooser(i, I18n.t("add_photo")), PICK_PHOTO);\n        } catch (Exception e) {\n            Utils.toast(this, I18n.t("cannot_open_picker"));\n        }\n    }',
   '    private void pickPhoto() {\n        MediaPick.choose(this, PICK_PHOTO, new MediaPick.Callback() {\n            @Override\n            public void onBitmap(Bitmap bmp) {\n                uploadBitmap(bmp, "add_profile_photo", "photo");\n            }\n        });\n    }'],
  ['        if (resultCode == RESULT_OK && data != null && data.getData() != null) {\n            if (requestCode == PICK_AVATAR) {\n                uploadSquare(data.getData(), "set_avatar", null);\n            } else if (requestCode == PICK_PHOTO) {\n                uploadSquare(data.getData(), "add_profile_photo", "photo");\n            }\n        }\n    }',
   '        if (MediaPick.handleResult(this, requestCode, resultCode, data)) return;\n    }\n\n    @Override\n    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {\n        super.onRequestPermissionsResult(requestCode, permissions, grantResults);\n        MediaPick.onPermissionResult(this, requestCode, grantResults);\n    }\n\n    private void uploadBitmap(final Bitmap src, final String type, final String photoFlag) {\n        Thread t = new Thread(new Runnable() {\n            @Override\n            public void run() {\n                try {\n                    Bitmap bmp = src;\n                    int w = bmp.getWidth(), h = bmp.getHeight();\n                    int side = Math.min(w, h);\n                    if (side > 0 && (w != side || h != side)) {\n                        Bitmap cropped = Bitmap.createBitmap(bmp, (w - side) / 2, (h - side) / 2, side, side);\n                        if (cropped != bmp && !bmp.isRecycled()) bmp.recycle();\n                        bmp = cropped;\n                    }\n                    int target = photoFlag != null ? 512 : 256;\n                    if (bmp.getWidth() > target) {\n                        Bitmap scaled = Bitmap.createScaledBitmap(bmp, target, target, true);\n                        if (scaled != bmp && !bmp.isRecycled()) bmp.recycle();\n                        bmp = scaled;\n                    }\n                    ByteArrayOutputStream bos = new ByteArrayOutputStream();\n                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);\n                    final String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);\n                    if (!bmp.isRecycled()) bmp.recycle();\n                    runOnUiThread(new Runnable() {\n                        @Override\n                        public void run() {\n                            try {\n                                JSONObject o = new JSONObject();\n                                o.put("type", type);\n                                o.put("data", b64);\n                                Net.get().send(o);\n                            } catch (Exception ignored) {\n                            }\n                        }\n                    });\n                } catch (Exception e) {\n                    runOnUiThread(new Runnable() {\n                        @Override\n                        public void run() {\n                            Utils.toast(ProfileActivity.this, I18n.t("avatar_upload_failed"));\n                        }\n                    });\n                }\n            }\n        });\n        t.setDaemon(true);\n        t.start();\n    }'],
  // 选择器文字颜色 + 所在地/出生地蓝色
  ['    private void showRegionDialog(final String field) {',
   '    private void stylePicker(NumberPicker p) {\n        try {\n            java.lang.reflect.Field f = NumberPicker.class.getDeclaredField("mInputText");\n            f.setAccessible(true);\n            android.widget.EditText et = (android.widget.EditText) f.get(p);\n            et.setTextColor(Utils.TEXT);\n        } catch (Exception ignored) {\n        }\n    }\n\n    private void showRegionDialog(final String field) {'],
  ['        pPicker.setMinValue(0);\n        pPicker.setMaxValue(provinces.size() - 1);\n        pPicker.setDisplayedValues(provinces.toArray(new String[0]));\n        pPicker.setWrapSelectorWheel(false);',
   '        pPicker.setMinValue(0);\n        pPicker.setMaxValue(provinces.size() - 1);\n        pPicker.setDisplayedValues(provinces.toArray(new String[0]));\n        pPicker.setWrapSelectorWheel(false);\n        stylePicker(pPicker);'],
  ['        cPicker.setWrapSelectorWheel(false);\n        dPicker.setWrapSelectorWheel(false);',
   '        cPicker.setWrapSelectorWheel(false);\n        dPicker.setWrapSelectorWheel(false);\n        stylePicker(cPicker);\n        stylePicker(dPicker);'],
  ['        year.setWrapSelectorWheel(false);\n        month.setWrapSelectorWheel(false);\n        day.setWrapSelectorWheel(false);',
   '        year.setWrapSelectorWheel(false);\n        month.setWrapSelectorWheel(false);\n        day.setWrapSelectorWheel(false);\n        stylePicker(year);\n        stylePicker(month);\n        stylePicker(day);'],
  ['        if (!value.isEmpty() && !value.equals("—")\n                && (label.equals(I18n.t("gender")) || label.equals(I18n.t("birthday")) || label.equals(I18n.t("job")))) {\n            vColor = Utils.ACCENT;\n        }',
   '        if (!value.isEmpty() && !value.equals("—")\n                && (label.equals(I18n.t("gender")) || label.equals(I18n.t("birthday")) || label.equals(I18n.t("job"))\n                || label.equals(I18n.t("location")) || label.equals(I18n.t("birthplace")))) {\n            vColor = Utils.ACCENT;\n        }'],
]);

const HA = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';
patch(HA, [
  // 动态图片: 拍照/相册
  ['    private void pickPostImage() {\n        Intent i = new Intent(Intent.ACTION_GET_CONTENT);\n        i.setType("image/*");\n        try {\n            startActivityForResult(Intent.createChooser(i, I18n.t("moment_add_photo")), PICK_POST_IMAGE);\n        } catch (Exception e) {\n            Utils.toast(this, I18n.t("cannot_open_picker"));\n        }\n    }',
   '    private void pickPostImage() {\n        MediaPick.choose(this, PICK_POST_IMAGE, new MediaPick.Callback() {\n            @Override\n            public void onBitmap(Bitmap bmp) {\n                addPostBitmap(bmp);\n            }\n        });\n    }\n\n    private void addPostBitmap(final Bitmap src) {\n        Thread t = new Thread(new Runnable() {\n            @Override\n            public void run() {\n                try {\n                    Bitmap bmp = src;\n                    int w = bmp.getWidth(), h = bmp.getHeight();\n                    int side = Math.min(w, h);\n                    if (side > 0 && (w != side || h != side)) {\n                        Bitmap cropped = Bitmap.createBitmap(bmp, (w - side) / 2, (h - side) / 2, side, side);\n                        if (cropped != bmp && !bmp.isRecycled()) bmp.recycle();\n                        bmp = cropped;\n                    }\n                    int target = 512;\n                    if (bmp.getWidth() > target) {\n                        Bitmap scaled = Bitmap.createScaledBitmap(bmp, target, target, true);\n                        if (scaled != bmp && !bmp.isRecycled()) bmp.recycle();\n                        bmp = scaled;\n                    }\n                    ByteArrayOutputStream bos = new ByteArrayOutputStream();\n                    bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);\n                    final String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);\n                    if (!bmp.isRecycled()) bmp.recycle();\n                    runOnUiThread(new Runnable() {\n                        @Override\n                        public void run() {\n                            pendingPostImages.add(b64);\n                            Ui.banner(HomeActivity.this, I18n.t("moment_add_photo") + " (" + pendingPostImages.size() + "/3)");\n                        }\n                    });\n                } catch (Exception ignored) {\n                }\n            }\n        });\n        t.setDaemon(true);\n        t.start();\n    }'],
  // onActivityResult: MediaPick 优先
  ['        super.onActivityResult(requestCode, resultCode, data);\n        if (requestCode == PICK_AVATAR && resultCode == RESULT_OK && data != null && data.getData() != null) {\n            uploadAvatar(data.getData());\n        } else if (requestCode == PICK_POST_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {\n            encodePostImage(data.getData());\n        }\n    }',
   '        super.onActivityResult(requestCode, resultCode, data);\n        if (MediaPick.handleResult(this, requestCode, resultCode, data)) return;\n        if (requestCode == PICK_AVATAR && resultCode == RESULT_OK && data != null && data.getData() != null) {\n            uploadAvatar(data.getData());\n        }\n    }'],
  // 权限结果: 通知 + 相机
  ['        if (requestCode == 2001) {\n            if (notifPrompt != null) notifPrompt.setVisibility(View.GONE);\n        }\n    }',
   '        if (requestCode == 2001) {\n            if (notifPrompt != null) notifPrompt.setVisibility(View.GONE);\n        }\n        MediaPick.onPermissionResult(this, requestCode, grantResults);\n    }'],
  // 联系人弹窗: 扫一扫
  ['    private void showFriendListDialog() {\n        LinearLayout list = new LinearLayout(this);\n        list.setOrientation(LinearLayout.VERTICAL);\n        list.setPadding(Utils.dp(this, 8), 0, Utils.dp(this, 8), 0);\n        if (contacts.isEmpty()) {',
   '    private void showFriendListDialog() {\n        LinearLayout list = new LinearLayout(this);\n        list.setOrientation(LinearLayout.VERTICAL);\n        list.setPadding(Utils.dp(this, 8), 0, Utils.dp(this, 8), 0);\n        Button scanBtn = new Button(this);\n        scanBtn.setText(I18n.t("qr_scan"));\n        scanBtn.setTextSize(14);\n        scanBtn.setTextColor(Color.WHITE);\n        scanBtn.setBackground(Utils.bg(this, Utils.ACCENT, 16));\n        scanBtn.setElevation(0);\n        scanBtn.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                MediaPick.scan(HomeActivity.this, 9101, new MediaPick.Callback() {\n                    @Override\n                    public void onBitmap(Bitmap bmp) {\n                        showScanResult(bmp);\n                    }\n                });\n            }\n        });\n        list.addView(scanBtn);\n        list.addView(Utils.divider(this));\n        if (contacts.isEmpty()) {'],
  // 阴影
  ['        pubBtn.setBackground(Utils.bg(this, Utils.ACCENT, 16));', '        pubBtn.setBackground(Utils.bg(this, Utils.ACCENT, 16));\n        pubBtn.setElevation(0);'],
  ['        okBtn.setBackground(Utils.bg(this, Utils.ACCENT, 22));', '        okBtn.setBackground(Utils.bg(this, Utils.ACCENT, 22));\n        okBtn.setElevation(0);'],
  ['        cancelBtn.setBackground(Utils.bg(this, Utils.CARD2, 22));', '        cancelBtn.setBackground(Utils.bg(this, Utils.CARD2, 22));\n        cancelBtn.setElevation(0);'],
  ['        likeBtn.setBackground(Utils.bg(this, Utils.CARD2, 14));', '        likeBtn.setBackground(Utils.bg(this, Utils.CARD2, 14));\n        likeBtn.setElevation(0);'],
  ['        addImg.setBackground(Utils.bg(this, Utils.CARD2, 14));', '        addImg.setBackground(Utils.bg(this, Utils.CARD2, 14));\n        addImg.setElevation(0);'],
]);

// showScanResult 方法
{
  let t = fs.readFileSync(HA, 'utf8');
  const anchor = '    private void showPostDialog() {';
  const methods = `    private void showScanResult(final Bitmap bmp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout box = new LinearLayout(HomeActivity.this);
                box.setOrientation(LinearLayout.VERTICAL);
                box.setGravity(Gravity.CENTER_HORIZONTAL);
                box.setPadding(Utils.dp(HomeActivity.this, 8), Utils.dp(HomeActivity.this, 6), Utils.dp(HomeActivity.this, 8), Utils.dp(HomeActivity.this, 6));
                ImageView qr = new ImageView(HomeActivity.this);
                int s = Utils.dp(HomeActivity.this, 180);
                qr.setLayoutParams(new LinearLayout.LayoutParams(s, s));
                qr.setImageBitmap(bmp);
                box.addView(qr);
                box.addView(Utils.tv(HomeActivity.this, I18n.t("scan_manual_hint"), 12, Utils.TEXT_DIM, Gravity.CENTER));
                box.addView(Utils.hSpace(HomeActivity.this, 8));
                final EditText input = Ui.input(HomeActivity.this, null, "username");
                box.addView(input);
                Button add = new Button(HomeActivity.this);
                add.setText(I18n.t("add_friend"));
                add.setTextSize(14);
                add.setTextColor(Color.WHITE);
                add.setBackground(Utils.bg(HomeActivity.this, Utils.ACCENT, 22));
                add.setElevation(0);
                add.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) return;
                        try {
                            JSONObject o = new JSONObject();
                            o.put("type", "add_friend");
                            o.put("username", name);
                            Net.get().send(o);
                        } catch (Exception ignored) {
                        }
                    }
                });
                box.addView(add);
                Ui.showSingle(HomeActivity.this, I18n.t("qr_scan"), box, I18n.t("close"), null);
            }
        });
    }

    private void showPostDialog() {`;
  if (t.includes(anchor)) { t = t.split(anchor).join(methods); fs.writeFileSync(HA, t); console.log('[HomeActivity.java] scan method added'); }
  else console.log('showPostDialog anchor not found');
}

// Ui 弹窗按钮去阴影
patch(ROOT + '/android/src/com/chatapp/app/Ui.java', [
  ['        b.setBackground(Utils.bg(a, bgColor, 22));', '        b.setBackground(Utils.bg(a, bgColor, 22));\n        b.setElevation(0);'],
]);
console.log('DONE');
