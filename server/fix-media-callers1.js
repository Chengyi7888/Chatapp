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

// ============ ChatActivity: 拍照/相册 ============
const CA = ROOT + '/android/src/com/chatapp/app/ChatActivity.java';
patch(CA, [
  ['    private void pickImage() {\n        Intent i = new Intent(Intent.ACTION_GET_CONTENT);\n        i.setType("image/*");\n        try {\n            startActivityForResult(Intent.createChooser(i, I18n.t("choose_image")), PICK_IMAGE);\n        } catch (Exception e) {\n            Utils.toast(this, I18n.t("cannot_open_picker"));\n        }\n    }',
   '    private void pickImage() {\n        MediaPick.choose(this, PICK_IMAGE, new MediaPick.Callback() {\n            @Override\n            public void onBitmap(Bitmap bmp) {\n                sendBitmap(bmp);\n            }\n        });\n    }\n\n    private void sendBitmap(Bitmap bmp) {\n        try {\n            ByteArrayOutputStream bos = new ByteArrayOutputStream();\n            bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);\n            String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);\n            bmp.recycle();\n            JSONObject o = new JSONObject();\n            o.put("type", "send_message");\n            o.put("to", peer);\n            o.put("kind", "image");\n            o.put("data", b64);\n            Net.get().send(o);\n            Ui.banner(this, I18n.t("image_sending"));\n            toggleAttach();\n        } catch (Exception e) {\n            Utils.toast(this, I18n.t("image_send_failed"));\n        }\n    }'],
  ['        super.onActivityResult(requestCode, resultCode, data);\n        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {\n            sendImage(data.getData());\n        } else if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {\n            sendFile(data);\n        }\n    }',
   '        super.onActivityResult(requestCode, resultCode, data);\n        if (MediaPick.handleResult(this, requestCode, resultCode, data)) return;\n        if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {\n            sendFile(data);\n        }\n    }\n\n    @Override\n    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {\n        super.onRequestPermissionsResult(requestCode, permissions, grantResults);\n        MediaPick.onPermissionResult(this, requestCode, grantResults);\n    }'],
  // 阴影
  ['        sendBtn.setBackground(Utils.bg(this, Utils.ACCENT, 16));', '        sendBtn.setBackground(Utils.bg(this, Utils.ACCENT, 16));\n        sendBtn.setElevation(0);'],
]);

// ============ GroupChatActivity ============
const GA = ROOT + '/android/src/com/chatapp/app/GroupChatActivity.java';
patch(GA, [
  ['    private void pickImage() {\n        Intent i = new Intent(Intent.ACTION_GET_CONTENT);\n        i.setType("image/*");\n        try {\n            startActivityForResult(Intent.createChooser(i, I18n.t("choose_image")), PICK_IMAGE);\n        } catch (Exception e) {\n            Utils.toast(this, I18n.t("cannot_open_picker"));\n        }\n    }',
   '    private void pickImage() {\n        MediaPick.choose(this, PICK_IMAGE, new MediaPick.Callback() {\n            @Override\n            public void onBitmap(Bitmap bmp) {\n                sendGroupBitmap(bmp);\n            }\n        });\n    }\n\n    private void sendGroupBitmap(Bitmap bmp) {\n        try {\n            ByteArrayOutputStream bos = new ByteArrayOutputStream();\n            bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);\n            String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);\n            bmp.recycle();\n            JSONObject o = new JSONObject();\n            o.put("type", "send_group_message");\n            o.put("id", groupId);\n            o.put("kind", "image");\n            o.put("data", b64);\n            Net.get().send(o);\n            Ui.banner(this, I18n.t("image_sending"));\n            toggleAttach();\n        } catch (Exception e) {\n            Utils.toast(this, I18n.t("image_send_failed"));\n        }\n    }'],
  ['        super.onActivityResult(requestCode, resultCode, data);\n        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {\n            sendImage(data.getData());\n        } else if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {\n            sendFile(data.getData());\n        }\n    }',
   '        super.onActivityResult(requestCode, resultCode, data);\n        if (MediaPick.handleResult(this, requestCode, resultCode, data)) return;\n        if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {\n            sendFile(data.getData());\n        }\n    }\n\n    @Override\n    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {\n        super.onRequestPermissionsResult(requestCode, permissions, grantResults);\n        MediaPick.onPermissionResult(this, requestCode, grantResults);\n    }'],
  ['        sendBtn.setBackground(Utils.bg(this, Utils.ACCENT, 16));', '        sendBtn.setBackground(Utils.bg(this, Utils.ACCENT, 16));\n        sendBtn.setElevation(0);'],
]);
console.log('DONE');
