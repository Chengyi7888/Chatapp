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

const GA = ROOT + '/android/src/com/chatapp/app/GroupChatActivity.java';
patch(GA, [
  ['import android.os.Bundle;', 'import android.os.Bundle;\nimport android.os.Handler;\nimport android.os.Looper;'],
  ['    private TextView noticeBar;', '    private TextView noticeBar;\n    private TextView typingText;\n    private final Handler typingHide = new Handler(Looper.getMainLooper());'],
  ['        localDel = getSharedPreferences("chatapp_group_del", MODE_PRIVATE);',
   '        localDel = getSharedPreferences("chatapp_group_del", MODE_PRIVATE);'],
  // 输入中提示条 (scroll 后)
  ['        root.addView(scroll, Utils.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));\n\n        // 回复指示条',
   '        root.addView(scroll, Utils.lp(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));\n\n        typingText = Utils.tv(this, I18n.t("typing_hint"), 11, Utils.ACCENT, Gravity.START);\n        typingText.setPadding(Utils.dp(this, 12), Utils.dp(this, 4), Utils.dp(this, 12), Utils.dp(this, 4));\n        typingText.setVisibility(View.GONE);\n        root.addView(typingText);\n\n        // 回复指示条'],
  // 输入监听: 输入中 + 草稿
  ['        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {\n            @Override\n            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {\n                if (actionId == EditorInfo.IME_ACTION_SEND) {\n                    sendText();\n                    return true;\n                }\n                return false;\n            }\n        });\n        inputRow.addView(input, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));',
   '        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {\n            @Override\n            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {\n                if (actionId == EditorInfo.IME_ACTION_SEND) {\n                    sendText();\n                    return true;\n                }\n                return false;\n            }\n        });\n        final SharedPreferences draftPrefs = getSharedPreferences("chatapp_drafts", MODE_PRIVATE);\n        input.addTextChangedListener(new android.text.TextWatcher() {\n            @Override\n            public void beforeTextChanged(CharSequence s, int a, int b, int c) {\n            }\n\n            @Override\n            public void onTextChanged(CharSequence s, int a, int b, int c) {\n                draftPrefs.edit().putString("draft_" + groupId, s.toString()).apply();\n                if (s.length() > 0 && Net.get().isConnected()) {\n                    try {\n                        JSONObject o = new JSONObject();\n                        o.put("type", "typing");\n                        o.put("with", groupId);\n                        Net.get().send(o);\n                    } catch (Exception ignored) {\n                    }\n                }\n            }\n\n            @Override\n            public void afterTextChanged(Editable s) {\n            }\n        });\n        inputRow.addView(input, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));'],
  // onResume 恢复草稿
  ['        try {\n            JSONObject o = new JSONObject();\n            o.put("type", "mark_group_read");\n            o.put("id", groupId);\n            Net.get().send(o);\n        } catch (Exception ignored) {\n        }\n    }',
   '        try {\n            JSONObject o = new JSONObject();\n            o.put("type", "mark_group_read");\n            o.put("id", groupId);\n            Net.get().send(o);\n        } catch (Exception ignored) {\n        }\n        String d = getSharedPreferences("chatapp_drafts", MODE_PRIVATE).getString("draft_" + groupId, "");\n        if (d != null && !d.isEmpty()) {\n            input.setText(d);\n            input.setSelection(d.length());\n        }\n    }'],
  // peer_typing 处理
  ['        } else if ("group_history_cleared".equals(type)) {\n            if (groupId.equals(o.optString("id"))) {\n                messages.clear();\n                rerender();\n            }\n        }',
   '        } else if ("peer_typing".equals(type)) {\n            String from = o.optString("from");\n            if (!from.equals(Session.username)) {\n                typingText.setVisibility(View.VISIBLE);\n                typingHide.removeCallbacks(typingHideTask);\n                typingHide.postDelayed(typingHideTask, 3000);\n            }\n        } else if ("group_history_cleared".equals(type)) {\n            if (groupId.equals(o.optString("id"))) {\n                messages.clear();\n                rerender();\n            }\n        }'],
]);

// 新增 typingHideTask
{
  let t = fs.readFileSync(GA, 'utf8');
  const anchor = '    private void refreshGroupInfo() {';
  const methods = `    private final Runnable typingHideTask = new Runnable() {
        @Override
        public void run() {
            typingText.setVisibility(View.GONE);
        }
    };

    private void refreshGroupInfo() {`;
  if (t.includes(anchor)) { t = t.split(anchor).join(methods); fs.writeFileSync(GA, t); console.log('[GroupChatActivity.java] typing task added'); }
  else console.log('refreshGroupInfo anchor not found');
}

// HomeActivity: 字体大小 + 免打扰
const HA = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';
patch(HA, [
  ['        Theme.init(this);\n        Theme.apply(this);\n        I18n.init(this);',
   '        Theme.init(this);\n        Theme.apply(this);\n        I18n.init(this);\n        Utils.loadFontScale(this);'],
  // 在 langGroup 前插入 utilCard
  ['        meLayout.addView(Utils.hSpace(this, 16));\n\n        // 语言与地区 (位于更新日志下方)',
   '        meLayout.addView(Utils.hSpace(this, 16));\n\n        LinearLayout utilCard = new LinearLayout(this);\n        utilCard.setOrientation(LinearLayout.VERTICAL);\n        utilCard.setBackground(Utils.bg(this, Utils.CARD, 16));\n        utilCard.addView(menuRow(I18n.t("font_size"), new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                showFontDialog();\n            }\n        }));\n        utilCard.addView(Utils.divider(this));\n        utilCard.addView(menuRow(I18n.t("dnd"), new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                showDndDialog();\n            }\n        }));\n        meLayout.addView(utilCard);\n\n        meLayout.addView(Utils.hSpace(this, 16));\n\n        // 语言与地区 (位于更新日志下方)'],
]);

// 新增 showFontDialog / showDndDialog
{
  let t = fs.readFileSync(HA, 'utf8');
  const anchor = '    private void showLanguageRegionDialog() {';
  const methods = `    private void showFontDialog() {
        Ui.list(this, I18n.t("font_size"), new String[]{
                        I18n.t("font_small"), I18n.t("font_normal"), I18n.t("font_large"), I18n.t("font_xlarge")
                },
                new Ui.Click[]{
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                setFont(0.85f);
                            }
                        },
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                setFont(1.0f);
                            }
                        },
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                setFont(1.15f);
                            }
                        },
                        new Ui.Click() {
                            @Override
                            public void onClick() {
                                setFont(1.3f);
                            }
                        }
                });
    }

    private void setFont(float s) {
        getSharedPreferences("chatapp_font", MODE_PRIVATE).edit().putFloat("scale", s).apply();
        Utils.fontScale = s;
        reapplyTheme();
    }

    private void showDndDialog() {
        final SharedPreferences p = getSharedPreferences("chatapp_dnd", MODE_PRIVATE);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final android.widget.Switch sw = new android.widget.Switch(this);
        sw.setText(I18n.t("dnd"));
        sw.setChecked(p.getBoolean("enabled", false));
        box.addView(sw);
        box.addView(Utils.hSpace(this, 12));
        final android.widget.NumberPicker start = new android.widget.NumberPicker(this);
        start.setMinValue(0);
        start.setMaxValue(23);
        start.setValue(p.getInt("start", 23));
        start.setWrapSelectorWheel(false);
        final android.widget.NumberPicker end = new android.widget.NumberPicker(this);
        end.setMinValue(0);
        end.setMaxValue(23);
        end.setValue(p.getInt("end", 7));
        end.setWrapSelectorWheel(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(Utils.tv(this, I18n.t("dnd_start"), 14, Utils.TEXT, Gravity.CENTER), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(start, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(Utils.tv(this, I18n.t("dnd_end"), 14, Utils.TEXT, Gravity.CENTER), Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(end, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(row);
        Ui.show(this, I18n.t("dnd"), box, I18n.t("confirm"), new Ui.Click() {
            @Override
            public void onClick() {
                p.edit().putBoolean("enabled", sw.isChecked())
                        .putInt("start", start.getValue())
                        .putInt("end", end.getValue()).apply();
                Utils.toast(HomeActivity.this, I18n.t("dnd") + " ✓");
            }
        });
    }

    private void showLanguageRegionDialog() {`;
  if (t.includes(anchor)) { t = t.split(anchor).join(methods); fs.writeFileSync(HA, t); console.log('[HomeActivity.java] font/dnd methods added'); }
  else console.log('showLanguageRegionDialog anchor not found');
}
console.log('DONE');
