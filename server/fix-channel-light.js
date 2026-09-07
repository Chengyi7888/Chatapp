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

const HA = ROOT + '/android/src/com/chatapp/app/HomeActivity.java';
patch(HA, [
  // 字段
  ['    private TextView updateTipTitle, updateTipSub;',
   '    private TextView updateTipTitle, updateTipSub;\n    private TextView channelDropdownText;\n    private String selectedChannelUrl = "";\n    private final Handler updateCheckHandler = new Handler(Looper.getMainLooper());\n    private final Runnable updateCheckTask = new Runnable() {\n        @Override\n        public void run() {\n            if (updatesScroll != null && updatesScroll.getVisibility() == View.VISIBLE) {\n                UpdateChecker.checkAsync(HomeActivity.this, new UpdateChecker.Result() {\n                    @Override\n                    public void onResult(boolean hasUpdate, String version, String file) {\n                        if (updateTipCard != null) {\n                            if (hasUpdate) {\n                                updateTipTitle.setText(I18n.t("new_version_available") + " (v" + version + ")");\n                                updateTipSub.setText(I18n.t("click_confirm_update"));\n                                updateTipCard.setVisibility(View.VISIBLE);\n                            }\n                        }\n                    }\n                });\n            }\n            updateCheckHandler.postDelayed(this, 300000);\n        }\n    };'],
  // 确认更新后关闭提示卡
  ['        okBtn.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                UpdateChecker.downloadLatest(HomeActivity.this);\n            }\n        });',
   '        okBtn.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                UpdateChecker.downloadLatest(HomeActivity.this);\n                if (updateTipCard != null) updateTipCard.setVisibility(View.GONE);\n            }\n        });'],
  // 频道区改造
  ['        LinearLayout dropdown = new LinearLayout(this);\n        dropdown.setOrientation(LinearLayout.HORIZONTAL);\n        dropdown.setGravity(Gravity.CENTER_VERTICAL);\n        dropdown.setBackground(Utils.bg(this, Utils.CARD2, 12));\n        dropdown.setPadding(Utils.dp(this, 12), Utils.dp(this, 10), Utils.dp(this, 12), Utils.dp(this, 10));\n        dropdown.addView(Utils.tv(this, I18n.t("all_channels"), 14, Utils.TEXT, Gravity.START));\n        dropdown.addView(Utils.tv(this, " ▾", 14, Utils.ACCENT, Gravity.END));\n        channel.addView(dropdown);\n        channel.addView(Utils.hSpace(this, 12));\n        LinearLayout btnRow = new LinearLayout(this);\n        btnRow.setOrientation(LinearLayout.HORIZONTAL);\n        Button c1 = darkBtn(I18n.t("follow"));\n        Button c2 = darkBtn(I18n.t("recommend"));\n        btnRow.addView(c1, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));\n        btnRow.addView(Utils.vSpace(this, 10));\n        btnRow.addView(c2, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));\n        channel.addView(btnRow);',
   '        LinearLayout dropdown = new LinearLayout(this);\n        dropdown.setOrientation(LinearLayout.HORIZONTAL);\n        dropdown.setGravity(Gravity.CENTER_VERTICAL);\n        dropdown.setBackground(Utils.bg(this, Utils.CARD2, 12));\n        dropdown.setPadding(Utils.dp(this, 12), Utils.dp(this, 10), Utils.dp(this, 12), Utils.dp(this, 10));\n        channelDropdownText = Utils.tv(this, I18n.t("all_channels"), 14, Utils.TEXT, Gravity.START);\n        dropdown.addView(channelDropdownText, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));\n        dropdown.addView(Utils.tv(this, " ▾", 14, Utils.ACCENT, Gravity.END));\n        dropdown.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                showChannelDialog();\n            }\n        });\n        channel.addView(dropdown);\n        channel.addView(Utils.hSpace(this, 12));\n        LinearLayout btnRow = new LinearLayout(this);\n        btnRow.setOrientation(LinearLayout.HORIZONTAL);\n        Button c1 = darkBtn(I18n.t("follow"));\n        c1.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                openChannel();\n            }\n        });\n        Button c2 = darkBtn(I18n.t("recommend"));\n        btnRow.addView(c1, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));\n        btnRow.addView(Utils.vSpace(this, 10));\n        btnRow.addView(c2, Utils.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));\n        channel.addView(btnRow);'],
  // onResume 启动定时检测
  ['            updateStatusUi();\n            refreshHandler.removeCallbacks(refreshTask);\n            refreshHandler.postDelayed(refreshTask, 15000);',
   '            updateStatusUi();\n            refreshHandler.removeCallbacks(refreshTask);\n            refreshHandler.postDelayed(refreshTask, 15000);\n            updateCheckHandler.removeCallbacks(updateCheckTask);\n            updateCheckHandler.postDelayed(updateCheckTask, 300000);'],
  // onPause 停止
  ['        Net.get().setListener(null);\n        refreshHandler.removeCallbacks(refreshTask);\n        refreshHandler.removeCallbacks(searchTimeout);\n    }',
   '        Net.get().setListener(null);\n        refreshHandler.removeCallbacks(refreshTask);\n        refreshHandler.removeCallbacks(searchTimeout);\n        updateCheckHandler.removeCallbacks(updateCheckTask);\n    }'],
]);

// 频道方法
{
  let t = fs.readFileSync(HA, 'utf8');
  const anchor = '    private void showCreateGroupDialog() {';
  const methods = `    private static final String[][] CHANNELS = {
            {"bilibili", "https://www.bilibili.com"},
            {"youtube", "https://www.youtube.com"},
            {"douyin", "https://www.douyin.com"},
            {"tiktok", "https://www.tiktok.com"},
            {"xiaohongshu", "https://www.xiaohongshu.com"},
            {"facebook", "https://www.facebook.com"},
            {"weibo", "https://weibo.com"},
            {"instagram", "https://www.instagram.com"}
    };

    private String channelName(String key) {
        if ("bilibili".equals(key)) return I18n.t("哔哩哔哩", "嗶哩嗶哩", "Bilibili");
        if ("youtube".equals(key)) return I18n.t("YouTube", "YouTube", "YouTube");
        if ("douyin".equals(key)) return I18n.t("抖音", "抖音", "Douyin");
        if ("tiktok".equals(key)) return I18n.t("TikTok", "TikTok", "TikTok");
        if ("xiaohongshu".equals(key)) return I18n.t("小红书", "小紅書", "Xiaohongshu");
        if ("facebook".equals(key)) return I18n.t("Facebook", "Facebook", "Facebook");
        if ("weibo".equals(key)) return I18n.t("微博", "微博", "Weibo");
        return I18n.t("Instagram", "Instagram", "Instagram");
    }

    private List<String[]> availableChannels() {
        List<String[]> list = new ArrayList<>();
        String lang = I18n.lang();
        String region = Session.region == null ? "" : Session.region;
        if (I18n.ZH_HANS.equals(lang) && "中国".equals(region)) {
            addChannel(list, "bilibili");
            addChannel(list, "douyin");
            addChannel(list, "xiaohongshu");
            addChannel(list, "weibo");
        } else if (I18n.ZH_HANT.equals(lang)
                && (region.contains("香港") || region.contains("澳门") || region.contains("台湾"))) {
            for (String[] c : CHANNELS) list.add(c);
        } else {
            addChannel(list, "youtube");
            addChannel(list, "tiktok");
            addChannel(list, "facebook");
            addChannel(list, "instagram");
        }
        return list;
    }

    private void addChannel(List<String[]> list, String key) {
        for (String[] c : CHANNELS) {
            if (c[0].equals(key)) list.add(c);
        }
    }

    private void showChannelDialog() {
        final List<String[]> list = availableChannels();
        String[] names = new String[list.size()];
        final Ui.Click[] clicks = new Ui.Click[list.size()];
        for (int i = 0; i < list.size(); i++) {
            final String[] c = list.get(i);
            names[i] = channelName(c[0]);
            clicks[i] = new Ui.Click() {
                @Override
                public void onClick() {
                    selectedChannelUrl = c[1];
                    if (channelDropdownText != null) channelDropdownText.setText(channelName(c[0]));
                }
            };
        }
        Ui.list(this, I18n.t("all_channels"), names, clicks);
    }

    private void openChannel() {
        if (selectedChannelUrl == null || selectedChannelUrl.isEmpty()) {
            Ui.banner(this, I18n.t("channel_pick_first"));
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(selectedChannelUrl));
            startActivity(i);
        } catch (Exception e) {
            Ui.banner(this, I18n.t("cannot_open_channel"));
        }
    }

    private void showCreateGroupDialog() {`;
  if (t.includes(anchor)) { t = t.split(anchor).join(methods); fs.writeFileSync(HA, t); console.log('[HomeActivity.java] channel methods added'); }
  else console.log('showCreateGroupDialog anchor not found');
}

// I18n 频道提示键
patch(ROOT + '/android/src/com/chatapp/app/I18n.java', [
  ['        put("moments_empty", "暂无动态，发一条吧", "暫無動態，發一條吧", "No moments yet, post one");',
   '        put("moments_empty", "暂无动态，发一条吧", "暫無動態，發一條吧", "No moments yet, post one");\n        put("channel_pick_first", "请先选择频道", "請先選擇頻道", "Please pick a channel first");\n        put("cannot_open_channel", "无法打开该应用", "無法開啟該應用", "Can\'t open this app");'],
]);

// 白昼对比度: Ui 按钮文字 / 附件图标 / 预览按钮
patch(ROOT + '/android/src/com/chatapp/app/Ui.java', [
  ['        b.setTextColor(Color.WHITE);', '        b.setTextColor(Utils.TEXT);'],
]);
for (const f of ['ChatActivity.java', 'GroupChatActivity.java']) {
  patch(ROOT + '/android/src/com/chatapp/app/' + f, [
    ['        TextView circle = Utils.tv(this, icon, 15, Color.WHITE, Gravity.CENTER);',
     '        TextView circle = Utils.tv(this, icon, 15, Utils.TEXT, Gravity.CENTER);'],
  ]);
}
patch(ROOT + '/android/src/com/chatapp/app/MediaHelper.java', [
  ['        TextView t = Utils.tv(a, text, 14, a.getResources().getColor(android.R.color.white), Gravity.CENTER);',
   '        TextView t = Utils.tv(a, text, 14, Utils.TEXT, Gravity.CENTER);'],
]);
console.log('DONE');
