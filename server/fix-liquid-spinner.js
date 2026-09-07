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

// ============ 移除液态玻璃 ============
patch(ROOT + '/android/src/com/chatapp/app/Theme.java', [
  ['    public static boolean followSystem = false;\n    public static boolean liquid = false;', '    public static boolean followSystem = false;'],
  ['        followSystem = prefs.getBoolean("follow", false);\n        liquid = prefs.getBoolean("liquid", false);', '        followSystem = prefs.getBoolean("follow", false);'],
  ['    public static void setLiquid(boolean l) {\n        liquid = l;\n        if (prefs != null) prefs.edit().putBoolean("liquid", l).apply();\n    }\n\n', ''],
  ['        if (liquid) {\n            // 液态玻璃: 半透明玻璃面板 + 描边 (较透, 非磨砂)\n            Utils.liquid = true;\n            if (d) {\n                Utils.CARD = Color.argb(0x24, 255, 255, 255);\n                Utils.CARD2 = Color.argb(0x33, 255, 255, 255);\n                Utils.DIVIDER = Color.argb(0x2E, 255, 255, 255);\n            } else {\n                Utils.CARD = Color.argb(0x59, 255, 255, 255);\n                Utils.CARD2 = Color.argb(0x8C, 255, 255, 255);\n                Utils.DIVIDER = Color.argb(0x2A, 0, 0, 0);\n            }\n        } else {\n            Utils.liquid = false;\n        }\n        return changed;', '        return changed;'],
]);
patch(ROOT + '/android/src/com/chatapp/app/Utils.java', [
  ['    /** 液态玻璃开关: 开启后所有按钮/卡片/提示框使用透明玻璃质感 */\n    public static boolean liquid = false;\n\n', ''],
  ['    /** 圆角背景; 液态玻璃开启时自动转为半透明 + 细描边 */\n    public static GradientDrawable bg(Context c, int color, float radiusDp) {\n        GradientDrawable g = new GradientDrawable();\n        g.setCornerRadius(dp(c, radiusDp));\n        int fill = color;\n        if (liquid) {\n            if (Color.alpha(color) == 255) {\n                // 不透明颜色 -> 降透明度, 更透\n                int a = isDarkBg() ? 120 : 145;\n                fill = Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));\n            }\n            g.setStroke(dp(c, 1), glassBorder());\n        }\n        g.setColor(fill);\n        return g;\n    }',
   '    /** 圆角背景 */\n    public static GradientDrawable bg(Context c, int color, float radiusDp) {\n        GradientDrawable g = new GradientDrawable();\n        g.setColor(color);\n        g.setCornerRadius(dp(c, radiusDp));\n        return g;\n    }'],
  ['    private static boolean isDarkBg() {\n        int sum = Color.red(BG) + Color.green(BG) + Color.blue(BG);\n        return sum < 384;\n    }\n\n    private static int glassBorder() {\n        return isDarkBg() ? 0x3DFFFFFF : 0x2A000000;\n    }\n\n', ''],
]);

// HomeActivity: 移除液态玻璃行
{
  let h = fs.readFileSync(ROOT + '/android/src/com/chatapp/app/HomeActivity.java', 'utf8');
  const startMark = '        themeGroup.addView(followRow);\n        themeGroup.addView(Utils.divider(this));\n        LinearLayout liquidRow = new LinearLayout(this);';
  const endMark = '        themeGroup.addView(liquidRow);\n        themeGroup.addView(Utils.divider(this));\n        LinearLayout vibrateRow = new LinearLayout(this);';
  const si = h.indexOf(startMark);
  const ei = h.indexOf(endMark);
  if (si >= 0 && ei > si) {
    h = h.slice(0, si) + '        themeGroup.addView(followRow);\n        themeGroup.addView(Utils.divider(this));\n' + h.slice(ei + endMark.length - 'LinearLayout vibrateRow = new LinearLayout(this);'.length - 1);
    // 简单方式: 用替换
    const replacement = '        themeGroup.addView(followRow);\n        themeGroup.addView(Utils.divider(this));\n        LinearLayout vibrateRow = new LinearLayout(this);';
    h = h.slice(0, si) + replacement + h.slice(ei + endMark.length);
    fs.writeFileSync(ROOT + '/android/src/com/chatapp/app/HomeActivity.java', h);
    console.log('[HomeActivity.java] liquid glass row removed');
  } else {
    console.log('liquid row anchors not found si=' + si + ' ei=' + ei);
  }
}

// ============ 加载动画: ChatActivity / GroupChatActivity / GroupDetailActivity ============
const CA = ROOT + '/android/src/com/chatapp/app/ChatActivity.java';
patch(CA, [
  ['import android.widget.LinearLayout;\nimport android.widget.ScrollView;', 'import android.widget.LinearLayout;\nimport android.widget.ProgressBar;\nimport android.widget.ScrollView;'],
  ['    private LinearLayout replyBar;\n    private LinearLayout attachPanel;', '    private LinearLayout replyBar;\n    private LinearLayout attachPanel;\n    private ProgressBar loading;'],
  ['        root.addView(attachPanel, alp);\n\n        setContentView(root);\n    }',
   '        root.addView(attachPanel, alp);\n\n        FrameLayout frame = new FrameLayout(this);\n        frame.setBackgroundColor(Utils.BG);\n        frame.addView(root, new FrameLayout.LayoutParams(\n                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));\n        loading = new ProgressBar(this);\n        frame.addView(loading, new FrameLayout.LayoutParams(\n                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));\n        setContentView(frame);\n    }'],
  ['            rerender();\n        } else if ("new_message".equals(type)) {', '            rerender();\n            if (loading != null) loading.setVisibility(View.GONE);\n        } else if ("new_message".equals(type)) {'],
  ['        } else if ("error".equals(type)) {\n            Utils.toast(this, I18n.serverMsg(o.optString("message")));\n        } else if ("toast".equals(type)) {\n            Utils.toast(this, I18n.serverMsg(o.optString("message")));\n        }',
   '        } else if ("error".equals(type)) {\n            if (loading != null) loading.setVisibility(View.GONE);\n            Utils.toast(this, I18n.serverMsg(o.optString("message")));\n        } else if ("toast".equals(type)) {\n            if (loading != null) loading.setVisibility(View.GONE);\n            Utils.toast(this, I18n.serverMsg(o.optString("message")));\n        }'],
]);

const GA = ROOT + '/android/src/com/chatapp/app/GroupChatActivity.java';
patch(GA, [
  ['import android.widget.LinearLayout;\nimport android.widget.ScrollView;', 'import android.widget.LinearLayout;\nimport android.widget.ProgressBar;\nimport android.widget.ScrollView;'],
  ['    private LinearLayout replyBar;\n    private LinearLayout attachPanel;', '    private LinearLayout replyBar;\n    private LinearLayout attachPanel;\n    private ProgressBar loading;'],
  ['        root.addView(attachPanel, alp);\n\n        setContentView(root);\n    }',
   '        root.addView(attachPanel, alp);\n\n        FrameLayout frame = new FrameLayout(this);\n        frame.setBackgroundColor(Utils.BG);\n        frame.addView(root, new FrameLayout.LayoutParams(\n                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));\n        loading = new ProgressBar(this);\n        frame.addView(loading, new FrameLayout.LayoutParams(\n                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));\n        setContentView(frame);\n    }'],
  ['            rerender();\n        } else if ("new_group_message".equals(type)) {', '            rerender();\n            if (loading != null) loading.setVisibility(View.GONE);\n        } else if ("new_group_message".equals(type)) {'],
  ['        } else if ("error".equals(type)) {\n            Utils.toast(this, I18n.serverMsg(o.optString("message")));\n        } else if ("toast".equals(type)) {\n            Utils.toast(this, I18n.serverMsg(o.optString("message")));\n        }',
   '        } else if ("error".equals(type)) {\n            if (loading != null) loading.setVisibility(View.GONE);\n            Utils.toast(this, I18n.serverMsg(o.optString("message")));\n        } else if ("toast".equals(type)) {\n            if (loading != null) loading.setVisibility(View.GONE);\n            Utils.toast(this, I18n.serverMsg(o.optString("message")));\n        }'],
]);

const GD = ROOT + '/android/src/com/chatapp/app/GroupDetailActivity.java';
patch(GD, [
  ['import android.widget.LinearLayout;\nimport android.widget.ScrollView;', 'import android.widget.LinearLayout;\nimport android.widget.ProgressBar;\nimport android.widget.ScrollView;'],
  ['    private LinearLayout memberList;\n    private LinearLayout ownerPanel;', '    private LinearLayout memberList;\n    private LinearLayout ownerPanel;\n    private ProgressBar loading;'],
  ['        sc.addView(body);\n        root.addView(sc, Utils.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));\n        setContentView(root);\n    }',
   '        sc.addView(body);\n        root.addView(sc, Utils.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));\n\n        FrameLayout frame = new FrameLayout(this);\n        frame.setBackgroundColor(Utils.BG);\n        frame.addView(root, new FrameLayout.LayoutParams(\n                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));\n        loading = new ProgressBar(this);\n        frame.addView(loading, new FrameLayout.LayoutParams(\n                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));\n        setContentView(frame);\n    }'],
  ['        } else if ("group_members".equals(type) && groupId.equals(o.optString("id"))) {\n            renderMembers(Models.parseMembers(o.optJSONArray("members")));\n        }',
   '        } else if ("group_members".equals(type) && groupId.equals(o.optString("id"))) {\n            if (loading != null) loading.setVisibility(View.GONE);\n            renderMembers(Models.parseMembers(o.optJSONArray("members")));\n        }'],
  ['        } else if ("error".equals(type)) {\n            Utils.toast(this, I18n.serverMsg(o.optString("message")));\n        } else if ("toast".equals(type)) {\n            Utils.toast(this, I18n.serverMsg(o.optString("message")));\n        }',
   '        } else if ("error".equals(type)) {\n            if (loading != null) loading.setVisibility(View.GONE);\n            Utils.toast(this, I18n.serverMsg(o.optString("message")));\n        } else if ("toast".equals(type)) {\n            if (loading != null) loading.setVisibility(View.GONE);\n            Utils.toast(this, I18n.serverMsg(o.optString("message")));\n        }'],
]);
console.log('DONE');
