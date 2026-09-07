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

// 1) 加载动画: 先画灰色框架, 再画绿色(绿色永远在上层)
patch(ROOT + '/android/src/com/chatapp/app/TriangleLoadingView.java', [
  ['        // 累积点亮: 先亮左边, 再下边, 再右边; 全亮后一起熄灭\n        int lit = Math.min(3, (int) phase);\n        for (int i = 0; i < 3; i++) {\n            float[] e = edges[i];\n            if (i < lit) {\n                canvas.drawLine(e[0], e[1], e[2], e[3], accentPaint);\n            } else {\n                canvas.drawLine(e[0], e[1], e[2], e[3], darkPaint);\n            }\n        }',
   '        // 先画全部灰色框架, 再画点亮边(绿色永远在上层, 避免灰色覆盖)\n        for (float[] e : edges) {\n            canvas.drawLine(e[0], e[1], e[2], e[3], darkPaint);\n        }\n        int lit = Math.min(3, (int) phase);\n        for (int i = 0; i < lit && i < 3; i++) {\n            float[] e = edges[i];\n            canvas.drawLine(e[0], e[1], e[2], e[3], accentPaint);\n        }'],
]);

// 2) 字体缩放真正生效: Utils.tv / Ui.input / 聊天输入框
patch(ROOT + '/android/src/com/chatapp/app/Utils.java', [
  ['        TextView t = new TextView(c);\n        t.setText(text);\n        t.setTextSize(sizeSp);',
   '        TextView t = new TextView(c);\n        t.setText(text);\n        t.setTextSize(sizeSp * fontScale);'],
]);
patch(ROOT + '/android/src/com/chatapp/app/Ui.java', [
  ['        e.setTextSize(15);', '        e.setTextSize(15 * Utils.fontScale);'],
]);
patch(ROOT + '/android/src/com/chatapp/app/ChatActivity.java', [
  ['        input.setTextSize(15);', '        input.setTextSize(15 * Utils.fontScale);'],
  ['        searchInput.setTextSize(14);', '        searchInput.setTextSize(14 * Utils.fontScale);'],
]);
patch(ROOT + '/android/src/com/chatapp/app/GroupChatActivity.java', [
  ['        input.setTextSize(15);', '        input.setTextSize(15 * Utils.fontScale);'],
]);

// 3) 搜索框: 常显 + 文字
patch(ROOT + '/android/src/com/chatapp/app/ChatActivity.java', [
  // 去掉顶栏 🔍 按钮
  ['        TextView searchBtn = Utils.tv(this, "🔍", 18, Utils.TEXT, Gravity.CENTER);\n        searchBtn.setPadding(Utils.dp(this, 8), 0, Utils.dp(this, 4), 0);\n        searchBtn.setOnClickListener(new View.OnClickListener() {\n            @Override\n            public void onClick(View v) {\n                toggleSearch();\n            }\n        });\n        top.addView(searchBtn);\n',
   ''],
  // 搜索行常显
  ['        searchRow.setVisibility(View.GONE);\n        root.addView(searchRow, srp);', '        root.addView(searchRow, srp);'],
]);
patch(ROOT + '/android/src/com/chatapp/app/I18n.java', [
  ['put("search_msg", "搜索消息", "搜尋訊息", "Search messages");', 'put("search_msg", "搜索聊天记录", "搜尋聊天紀錄", "Search chat history");'],
]);
console.log('DONE');
