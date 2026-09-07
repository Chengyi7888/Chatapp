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

// 服务器: 查询他人名片
patch(ROOT + '/server/server.js', [
  ['    case \'get_profile\': {\n      if (!me) return needAuth(sock);\n      send(sock, { type: \'profile\', profile: users[me].profile || {}, nickname: users[me].nickname, username: me, hasAvatar: !!users[me].hasAvatar });\n      break;\n    }',
   '    case \'get_profile\': {\n      if (!me) return needAuth(sock);\n      send(sock, { type: \'profile\', profile: users[me].profile || {}, nickname: users[me].nickname, username: me, hasAvatar: !!users[me].hasAvatar });\n      break;\n    }\n\n    case \'get_user_profile\': {\n      if (!me) return needAuth(sock);\n      const target = String(msg.username || \'\').trim();\n      const tu = users[target];\n      if (!tu) return send(sock, { type: \'error\', message: \'用户不存在\' });\n      send(sock, { type: \'user_profile\', username: target, nickname: tu.nickname, hasAvatar: !!tu.hasAvatar, status: tu.status || \'\', profile: tu.profile || {} });\n      break;\n    }'],
]);

// AvatarManager: loadFileImage / loadBitmap 加缓存
const AM = ROOT + '/android/src/com/chatapp/app/AvatarManager.java';
patch(AM, [
  ['    public static void loadFileImage(final Context ctx, final String urlPath, final ImageView iv) {\n        Thread t = new Thread(new Runnable() {',
   '    public static void loadFileImage(final Context ctx, final String urlPath, final ImageView iv) {\n        Bitmap cached = cache.get(urlPath);\n        if (cached != null) {\n            iv.setImageBitmap(cached);\n            return;\n        }\n        Thread t = new Thread(new Runnable() {'],
  ['                    if (bmp != null) {\n                        H.post(new Runnable() {\n                            @Override\n                            public void run() {\n                                iv.setImageBitmap(bmp);\n                            }\n                        });\n                    }',
   '                    if (bmp != null) {\n                        cache.put(urlPath, bmp);\n                        H.post(new Runnable() {\n                            @Override\n                            public void run() {\n                                iv.setImageBitmap(bmp);\n                            }\n                        });\n                    }'],
  ['    public static void loadBitmap(final Context ctx, final String urlPath, final BitmapCallback cb) {\n        Thread t = new Thread(new Runnable() {',
   '    public static void loadBitmap(final Context ctx, final String urlPath, final BitmapCallback cb) {\n        Bitmap cached = cache.get(urlPath);\n        if (cached != null) {\n            if (cb != null) cb.onBitmap(cached);\n            return;\n        }\n        Thread t = new Thread(new Runnable() {'],
  ['                    bmp = BitmapFactory.decodeStream(in);\n                    in.close();\n                    c.disconnect();\n                } catch (Exception ignored) {\n                }\n                final Bitmap fb = bmp;\n                H.post(new Runnable() {\n                    @Override\n                    public void run() {\n                        if (cb != null) cb.onBitmap(fb);\n                    }\n                });',
   '                    bmp = BitmapFactory.decodeStream(in);\n                    in.close();\n                    c.disconnect();\n                    if (bmp != null) cache.put(urlPath, bmp);\n                } catch (Exception ignored) {\n                }\n                final Bitmap fb = bmp;\n                H.post(new Runnable() {\n                    @Override\n                    public void run() {\n                        if (cb != null) cb.onBitmap(fb);\n                    }\n                });'],
]);

// I18n: 照片数量
patch(ROOT + '/android/src/com/chatapp/app/I18n.java', [
  ['        put("email_hint", "输入邮箱地址", "輸入郵箱地址", "Enter email");',
   '        put("email_hint", "输入邮箱地址", "輸入郵箱地址", "Enter email");\n        put("photo_count", "%d 张", "%d 張", "%d photos");'],
]);
console.log('DONE');
