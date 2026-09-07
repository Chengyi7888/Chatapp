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

// Models.Post: favorites/comments/forwardFrom
patch(ROOT + '/android/src/com/chatapp/app/Models.java', [
  ['        public List<String> likes = new ArrayList<>();\n        public long time;',
   '        public List<String> likes = new ArrayList<>();\n        public List<String> favorites = new ArrayList<>();\n        public List<Comment> comments = new ArrayList<>();\n        public String forwardFrom = "";\n        public long time;'],
  ['            JSONArray la = o.optJSONArray("likes");\n            if (la != null) for (int i = 0; i < la.length(); i++) p.likes.add(la.optString(i));\n            return p;\n        }\n    }',
   '            JSONArray la = o.optJSONArray("likes");\n            if (la != null) for (int i = 0; i < la.length(); i++) p.likes.add(la.optString(i));\n            JSONArray fa = o.optJSONArray("favorites");\n            if (fa != null) for (int i = 0; i < fa.length(); i++) p.favorites.add(fa.optString(i));\n            JSONArray ca = o.optJSONArray("comments");\n            if (ca != null) {\n                for (int i = 0; i < ca.length(); i++) {\n                    JSONObject c = ca.optJSONObject(i);\n                    if (c != null) p.comments.add(Comment.fromJson(c));\n                }\n            }\n            p.forwardFrom = o.optString("forwardFrom");\n            return p;\n        }\n    }\n\n    public static class Comment {\n        public String from;\n        public String nickname;\n        public String text;\n        public long time;\n\n        public static Comment fromJson(JSONObject o) {\n            Comment c = new Comment();\n            c.from = o.optString("from");\n            c.nickname = o.optString("nickname", c.from);\n            c.text = o.optString("text");\n            c.time = o.optLong("time");\n            return c;\n        }\n    }'],
]);

// I18n 键
patch(ROOT + '/android/src/com/chatapp/app/I18n.java', [
  ['        put("qr_scan", "扫一扫", "掃一掃", "Scan");',
   '        put("qr_scan", "扫一扫", "掃一掃", "Scan");\n        put("comment", "评论", "評論", "Comment");\n        put("my_favorites", "我的收藏", "我的收藏", "My favorites");\n        put("favorited", "已收藏", "已收藏", "Favorited");\n        put("reposted", "已转发", "已轉發", "Forwarded");\n        put("comment_hint", "写评论...", "寫評論...", "Write a comment...");\n        put("fav_messages", "收藏的对话", "收藏的對話", "Favorited messages");\n        put("fav_posts", "收藏的动态", "收藏的動態", "Favorited posts");\n        put("fav_empty", "暂无收藏", "暫無收藏", "No favorites");\n        put("friend_center", "好友中心", "好友中心", "Friends");\n        put("scan_add_hint", "扫码识别待完善，可手动输入用户名添加", "掃碼識別待完善，可手動輸入用戶名新增", "Scan pending, enter username to add");'],
]);
console.log('DONE');
