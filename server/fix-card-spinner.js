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

// 1) 好友名片/成员名片: 未填字段显示 "—" 占位
patch(ROOT + '/android/src/com/chatapp/app/HomeActivity.java', [
  ['        JSONArray photos = p.optJSONArray("photos");\n        int photoCount = photos == null ? 0 : photos.length();\n        if (photoCount > 0) addField(box, I18n.t("my_photos"), I18n.f("photo_count", photoCount));\n    }',
   '        JSONArray photos = p.optJSONArray("photos");\n        int photoCount = photos == null ? 0 : photos.length();\n        addField(box, I18n.t("my_photos"), photoCount > 0 ? I18n.f("photo_count", photoCount) : "");\n    }'],
  ['    private void addField(LinearLayout box, String label, String value) {\n        if (value == null || value.isEmpty()) return;',
   '    private void addField(LinearLayout box, String label, String value) {\n        if (value == null || value.isEmpty()) value = "—";'],
]);
patch(ROOT + '/android/src/com/chatapp/app/GroupDetailActivity.java', [
  ['        JSONArray photos = p.optJSONArray("photos");\n        int photoCount = photos == null ? 0 : photos.length();\n        if (photoCount > 0) addField(box, I18n.t("my_photos"), I18n.f("photo_count", photoCount));\n    }',
   '        JSONArray photos = p.optJSONArray("photos");\n        int photoCount = photos == null ? 0 : photos.length();\n        addField(box, I18n.t("my_photos"), photoCount > 0 ? I18n.f("photo_count", photoCount) : "");\n    }'],
  ['    private void addField(LinearLayout box, String label, String value) {\n        if (value == null || value.isEmpty()) return;',
   '    private void addField(LinearLayout box, String label, String value) {\n        if (value == null || value.isEmpty()) value = "—";'],
]);

// 2) ProfileActivity: 居中加载转圈
const PA = ROOT + '/android/src/com/chatapp/app/ProfileActivity.java';
patch(PA, [
  ['import android.widget.EditText;\nimport android.widget.GridLayout;',
   'import android.widget.EditText;\nimport android.widget.FrameLayout;\nimport android.widget.GridLayout;\nimport android.widget.ProgressBar;'],
  ['    private LinearLayout content;\n    private String nickname = "";',
   '    private LinearLayout content;\n    private ProgressBar loading;\n    private String nickname = "";'],
  ['    private void buildUi() {\n        LinearLayout root = new LinearLayout(this);\n        root.setOrientation(LinearLayout.VERTICAL);\n        root.setBackgroundColor(Utils.BG);\n',
   '    private void buildUi() {\n        final FrameLayout frame = new FrameLayout(this);\n        frame.setBackgroundColor(Utils.BG);\n        LinearLayout root = new LinearLayout(this);\n        root.setOrientation(LinearLayout.VERTICAL);\n        root.setBackgroundColor(Utils.BG);\n'],
  ['        sc.addView(content);\n        root.addView(sc, Utils.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));\n        setContentView(root);\n    }',
   '        sc.addView(content);\n        root.addView(sc, Utils.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));\n        frame.addView(root, new FrameLayout.LayoutParams(\n                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));\n        loading = new ProgressBar(this);\n        FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(\n                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);\n        frame.addView(loading, llp);\n        setContentView(frame);\n    }'],
  ['    private void render() {\n        content.removeAllViews();', '    private void render() {\n        if (loading != null) loading.setVisibility(View.GONE);\n        content.removeAllViews();'],
  ['        } else if ("error".equals(type)) {\n            Utils.toast(this, I18n.serverMsg(o.optString("message")));\n        } else if ("toast".equals(type)) {\n            Utils.toast(this, I18n.serverMsg(o.optString("message")));\n        }',
   '        } else if ("error".equals(type)) {\n            if (loading != null) loading.setVisibility(View.GONE);\n            Utils.toast(this, I18n.serverMsg(o.optString("message")));\n        } else if ("toast".equals(type)) {\n            if (loading != null) loading.setVisibility(View.GONE);\n            Utils.toast(this, I18n.serverMsg(o.optString("message")));\n        }'],
]);
console.log('DONE');
