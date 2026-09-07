package com.chatapp.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 简单的数据模型 */
public class Models {

    public static class ChatMsg {
        public String from;
        public String id;
        public String to;
        public String text;
        public String reply;
        public String kind = "text";
        public String url = "";
        public String name = "";
        public String nickname = "";
        public boolean senderHasAvatar;
        public long size;
        public long time;
        public boolean mine;

        public ChatMsg(String from, String to, String text, long time, boolean mine) {
            this.from = from;
            this.to = to;
            this.text = text;
            this.time = time;
            this.mine = mine;
        }

        public static ChatMsg fromJson(JSONObject o, String me) {
            ChatMsg c = new ChatMsg(
                    o.optString("from"),
                    o.optString("to"),
                    o.optString("text"),
                    o.optLong("time"),
                    o.optString("from").equals(me));
            c.id = o.optString("id");
            c.reply = o.optString("reply");
            c.kind = o.optString("kind", "text");
            c.url = o.optString("url");
            c.name = o.optString("name");
            c.size = o.optLong("size");
            c.nickname = o.optString("nickname");
            c.senderHasAvatar = o.optBoolean("senderHasAvatar");
            return c;
        }
    }

    public static class Conversation {
        public String with;
        public String nickname;
        public String lastText;
        public long lastTime;
        public int unread;
        public boolean hasAvatar;
        public boolean pinned;
        public boolean blocked;
        public String type = "peer"; // peer 好友会话 / group 群聊会话
        public int memberCount;
        public String lastKind = "text";

        public static Conversation fromJson(JSONObject o) {
            Conversation c = new Conversation();
            c.with = o.optString("with");
            c.nickname = o.optString("nickname", c.with);
            c.lastText = o.optString("lastText");
            c.lastTime = o.optLong("lastTime");
            c.unread = o.optInt("unread");
            c.hasAvatar = o.optBoolean("hasAvatar");
            c.blocked = o.optBoolean("blocked");
            c.type = o.optString("type", "peer");
            c.memberCount = o.optInt("memberCount");
            c.lastKind = o.optString("lastKind", "text");
            return c;
        }
    }

    public static class Contact {
        public String username;
        public String nickname;
        public String status;
        public boolean online;
        public boolean hasAvatar;
        public boolean blocked;

        public static Contact fromJson(JSONObject o) {
            Contact c = new Contact();
            c.username = o.optString("username");
            c.nickname = o.optString("nickname", c.username);
            c.status = o.optString("status");
            c.online = o.optBoolean("online");
            c.hasAvatar = o.optBoolean("hasAvatar");
            c.blocked = o.optBoolean("blocked");
            return c;
        }
    }

    public static class Group {
        public String id;
        public String name;
        public String owner;
        public String notice = "";
        public int memberCount;
        public boolean hasAvatar;
        public List<String> members = new ArrayList<>();

        public static Group fromJson(JSONObject o) {
            Group g = new Group();
            g.id = o.optString("id");
            g.name = o.optString("name");
            g.owner = o.optString("owner");
            g.notice = o.optString("notice");
            g.memberCount = o.optInt("memberCount");
            g.hasAvatar = o.optBoolean("hasAvatar");
            JSONArray arr = o.optJSONArray("members");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) g.members.add(arr.optString(i));
            }
            return g;
        }
    }

    public static class GroupMember {
        public String username;
        public String nickname;
        public boolean online;
        public boolean hasAvatar;
        public boolean isOwner;

        public static GroupMember fromJson(JSONObject o) {
            GroupMember m = new GroupMember();
            m.username = o.optString("username");
            m.nickname = o.optString("nickname", m.username);
            m.online = o.optBoolean("online");
            m.hasAvatar = o.optBoolean("hasAvatar");
            m.isOwner = o.optBoolean("isOwner");
            return m;
        }
    }

    public static class Post {
        public String id;
        public String from;
        public String nickname;
        public boolean hasAvatar;
        public String text = "";
        public List<String> images = new ArrayList<>();
        public List<String> likes = new ArrayList<>();
        public List<String> favorites = new ArrayList<>();
        public List<Comment> comments = new ArrayList<>();
        public String forwardFrom = "";
        public long time;

        public static Post fromJson(JSONObject o) {
            Post p = new Post();
            p.id = o.optString("id");
            p.from = o.optString("from");
            p.nickname = o.optString("nickname", p.from);
            p.hasAvatar = o.optBoolean("hasAvatar");
            p.text = o.optString("text");
            p.time = o.optLong("time");
            JSONArray ia = o.optJSONArray("images");
            if (ia != null) for (int i = 0; i < ia.length(); i++) p.images.add(ia.optString(i));
            JSONArray la = o.optJSONArray("likes");
            if (la != null) for (int i = 0; i < la.length(); i++) p.likes.add(la.optString(i));
            JSONArray fa = o.optJSONArray("favorites");
            if (fa != null) for (int i = 0; i < fa.length(); i++) p.favorites.add(fa.optString(i));
            JSONArray ca = o.optJSONArray("comments");
            if (ca != null) {
                for (int i = 0; i < ca.length(); i++) {
                    JSONObject c = ca.optJSONObject(i);
                    if (c != null) p.comments.add(Comment.fromJson(c));
                }
            }
            p.forwardFrom = o.optString("forwardFrom");
            return p;
        }
    }

    public static class Comment {
        public String from;
        public String nickname;
        public String text;
        public long time;

        public static Comment fromJson(JSONObject o) {
            Comment c = new Comment();
            c.from = o.optString("from");
            c.nickname = o.optString("nickname", c.from);
            c.text = o.optString("text");
            c.time = o.optLong("time");
            return c;
        }
    }

    public static List<Post> parsePosts(JSONArray arr) {
        List<Post> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) list.add(Post.fromJson(o));
        }
        return list;
    }

    public static List<ChatMsg> parseMessages(JSONArray arr, String me) {
        List<ChatMsg> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) list.add(ChatMsg.fromJson(o, me));
        }
        return list;
    }

    public static List<Conversation> parseConversations(JSONArray arr) {
        List<Conversation> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) list.add(Conversation.fromJson(o));
        }
        return list;
    }

    public static List<Contact> parseContacts(JSONArray arr) {
        List<Contact> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) list.add(Contact.fromJson(o));
        }
        return list;
    }

    public static List<Group> parseGroups(JSONArray arr) {
        List<Group> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) list.add(Group.fromJson(o));
        }
        return list;
    }

    public static List<GroupMember> parseMembers(JSONArray arr) {
        List<GroupMember> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) list.add(GroupMember.fromJson(o));
        }
        return list;
    }
}
