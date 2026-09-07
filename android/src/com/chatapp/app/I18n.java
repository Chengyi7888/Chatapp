package com.chatapp.app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 全局多语言支持: 简体中文 / 繁體中文 / English
 * 所有界面文案通过 I18n.t(key) 获取; 服务器提示通过 serverMsg() 翻译。
 */
public class I18n {

    public static final String ZH_HANS = "zh-Hans";
    public static final String ZH_HANT = "zh-Hant";
    public static final String EN = "en";

    private static String lang = EN;
    private static SharedPreferences prefs;

    public static void init(Context c) {
        if (prefs == null) {
            prefs = c.getApplicationContext().getSharedPreferences("chatapp_i18n", Context.MODE_PRIVATE);
        }
        lang = prefs.getString("lang", EN);
    }

    public static String lang() {
        return lang;
    }

    public static void setLang(String l) {
        lang = l;
        if (prefs != null) prefs.edit().putString("lang", l).apply();
    }

    /** 三语直取: 按当前语言返回对应文案 */
    public static String t(String zhHans, String zhHant, String en) {
        if (ZH_HANT.equals(lang)) return zhHant;
        if (EN.equals(lang)) return en;
        return zhHans;
    }

    /** 通过键取词 (键 -> 三语数组) */
    public static String t(String key) {
        String[] v = DICT.get(key);
        if (v == null) return key;
        if (ZH_HANT.equals(lang)) return v[1];
        if (EN.equals(lang)) return v[2];
        return v[0];
    }

    /** 带参数格式化文案 */
    public static String f(String key, Object... args) {
        return String.format(java.util.Locale.getDefault(), t(key), args);
    }

    /** 翻译服务器返回的提示消息 (简体中文 -> 当前语言) */
    public static String serverMsg(String zh) {
        if (zh == null) return zh;
        String key = SERVER_MSG.get(zh);
        return key == null ? zh : t(key);
    }

    /** 键 -> [简体, 繁體, English] */
    private static final java.util.Map<String, String[]> DICT = new java.util.HashMap<>();

    /** 服务器简体提示 -> 键 */
    private static final java.util.Map<String, String> SERVER_MSG = new java.util.HashMap<>();

    static {
        // 通用
        put("cancel", "取消", "取消", "Cancel");
        put("confirm", "确认", "確認", "OK");
        put("save", "保存", "儲存", "Save");
        put("close", "关闭", "關閉", "Close");
        put("ok_good", "好的", "好的", "OK");
        put("got_it", "知道了", "知道了", "Got it");
        put("search", "搜索", "搜尋", "Search");
        put("send", "发送", "傳送", "Send");
        put("copy", "复制", "複製", "Copy");
        put("delete", "删除", "刪除", "Delete");
        put("recall", "撤回", "撤回", "Recall");
        put("forward", "转发", "轉發", "Forward");
        put("reply", "回复", "回覆", "Reply");
        put("image", "图片", "圖片", "Image");
        put("file", "文件", "檔案", "File");
        put("favorite", "收藏", "收藏", "Favorite");
        put("more", "更多", "更多", "More");
        put("yesterday", "昨天", "昨天", "Yesterday");
        put("file_download_desc", "Chatapp 文件下载", "Chatapp 檔案下載", "Chatapp file download");
        // 标签页
        put("updates", "更新", "更新", "Updates");
        put("calls", "通话", "通話", "Calls");
        put("communities", "社群", "社群", "Communities");
        put("chats", "聊天", "聊天", "Chats");
        put("me", "自己", "自己", "Me");
        // 好友
        put("add_friend", "添加好友", "新增好友", "Add friend");
        put("friend_requests", "新的好友请求", "新的好友請求", "New requests");
        put("contacts", "联系人列表", "聯絡人清單", "Contacts");
        put("no_conversations", "暂无会话", "暫無會話", "No conversations");
        put("start_chat", "开始聊天吧", "開始聊天吧", "Say hi");
        put("pin", "置顶", "置頂", "Pin");
        put("unpin", "取消置顶", "取消置頂", "Unpin");
        put("delete_history", "删除聊天记录", "刪除聊天記錄", "Clear chat");
        put("history_cleared", "聊天记录已删除", "聊天記錄已刪除", "Chat history cleared");
        put("delete_friend", "删除好友", "刪除好友", "Remove friend");
        put("block", "加入黑名单", "加入黑名單", "Block");
        put("unblock", "移出黑名单", "移出黑名單", "Unblock");
        put("send_message", "发消息", "發訊息", "Message");
        put("friend_card", "好友名片", "好友名片", "Contact card");
        put("accept", "接受", "接受", "Accept");
        put("reject", "拒绝", "拒絕", "Decline");
        // 群聊
        put("create_group", "新建群聊", "建立群聊", "New group");
        put("group_name", "群名称", "群組名稱", "Group name");
        put("group_notice", "群公告", "群公告", "Notice");
        put("group_members", "群成员", "群成員", "Members");
        put("edit_notice", "编辑群公告", "編輯群公告", "Edit notice");
        put("edit_group_info", "修改群资料", "修改群資料", "Edit group info");
        put("invite", "邀请好友", "拉人進群", "Invite");
        put("kick", "踢出群聊", "踢出群聊", "Remove");
        put("transfer_owner", "转让群主", "轉讓群主", "Transfer owner");
        put("leave_group", "退出群聊", "退出群聊", "Leave group");
        put("dissolve_group", "解散群聊", "解散群聊", "Dissolve group");
        put("owner", "群主", "群主", "Owner");
        put("group_chat", "群聊", "群聊", "Group chat");
        put("group_info", "群聊信息", "群聊資訊", "Group info");
        put("members_count", "%d 位成员", "%d 位成員", "%d members");
        put("no_notice", "(暂无公告)", "(暫無公告)", "(No notice)");
        put("online", "在线", "線上", "Online");
        put("offline", "离线", "離線", "Offline");
        put("remove", "移除", "移除", "Remove");
        put("transfer", "转让", "轉讓", "Transfer");
        put("quit", "退出", "退出", "Leave");
        put("dissolve", "解散", "解散", "Dissolve");
        put("invite_friends", "邀请好友进群", "邀請好友進群", "Invite friends");
        put("no_friends_invite", "暂无好友可邀请", "暫無好友可邀請", "No friends to invite");
        put("all_in_group", "所有好友都已在群中", "所有好友都已在群中", "All friends already in group");
        put("notice_input_hint", "输入群公告", "輸入群公告", "Enter notice");
        put("group_name_input_hint", "输入新群名称", "輸入新群組名稱", "Enter new group name");
        put("choose_group_avatar", "选择群头像", "選擇群頭像", "Choose group avatar");
        put("avatar_uploading_group", "群头像上传中...", "群頭像上傳中...", "Uploading group avatar...");
        put("leave_confirm", "确定退出该群聊？", "確定退出該群聊？", "Leave this group?");
        put("dissolve_confirm", "解散后所有成员将被移出，且不可恢复。确定解散？", "解散後所有成員將被移出，且不可恢復。確定解散？", "All members will be removed. Dissolve?");
        put("group_dissolved", "群聊已被解散", "群聊已被解散", "Group dissolved");
        put("you_kicked", "你已被移出群聊", "你已被移出群聊", "You were removed");
        put("left_ok", "已退出群聊", "已退出群聊", "Left group");
        put("dissolved_ok", "群聊已解散", "群聊已解散", "Group dissolved");
        put("enter_group_chat", "进入群聊", "進入群聊", "Open chat");
        put("edit_group_name", "修改群名称", "修改群名稱", "Edit group name");
        put("change_group_avatar", "更换群头像", "更換群頭像", "Change avatar");
        put("group_created_ok", "群聊创建成功", "群聊建立成功", "Group created");
        put("invited_you", "%s 邀请你加入了群聊", "%s 邀請你加入了群聊", "%s invited you to a group");
        put("group_dissolved_msg", "群聊 %s 已被解散", "群聊 %s 已被解散", "Group %s dissolved");
        put("group_kicked_msg", "你已被移出群聊 %s", "你已被移出群聊 %s", "You were removed from %s");
        put("kick_confirm", "确定将 %s 移出群聊？", "確定將 %s 移出群聊？", "Remove %s from group?");
        put("transfer_confirm", "确定将群主转让给 %s ？\n转让后你将不再拥有管理权限。", "確定將群主轉讓給 %s ？\n轉讓後你將不再擁有管理權限。", "Transfer ownership to %s?\nYou'll lose admin rights.");
        put("input_group_name", "输入群名称", "輸入群組名稱", "Enter group name");
        put("enter_group_name", "请输入群名称", "請輸入群組名稱", "Enter a group name");
        put("create", "创建", "建立", "Create");
        put("not_connected", "未连接服务器", "未連接伺服器", "Not connected");
        put("group_intro", "创建群聊，和好友一起聊天、分享、协作", "建立群聊，與好友一起聊天、分享、協作", "Create a group to chat with friends");
        put("no_groups", "还没有加入任何群聊\n点右上角＋创建或等群主拉你", "還沒有加入任何群聊\n點右上角＋建立或等群主邀請你", "No groups yet\nTap ＋ to create or wait for an invite");
        put("group_owner_count", "群主 · %d 人", "群主 · %d 人", "Owner · %d members");
        put("people_count", "%d 人", "%d 人", "%d members");
        // 更新页
        put("new_version_available", "有新的版本可更新", "有新的版本可更新", "New version available");
        put("click_confirm_update", "点击确认开始更新", "點擊確認開始更新", "Tap to update");
        put("dynamics", "动态", "動態", "Moments");
        put("my_dynamics", "我的动态", "我的動態", "My moments");
        put("share_moment", "分享你的生活瞬间", "分享你的生活瞬間", "Share your moments");
        put("channels", "频道", "頻道", "Channels");
        put("browse_channels", "浏览你关注的频道内容", "瀏覽你關注的頻道內容", "Browse followed channels");
        put("all_channels", "全部频道", "全部頻道", "All channels");
        put("follow", "关注", "關注", "Follow");
        put("goto", "前往", "前往", "Go");
        put("recommend", "推荐", "推薦", "Recommend");
        put("channel_coming_soon", "频道模块即将上线", "頻道模組即將上線", "Channels coming soon");
        put("latest_version", "已是最新版本 v%s", "已是最新版本 v%s", "Up to date (v%s)");
        put("new_version_title", "发现新版本", "發現新版本", "New version");
        put("update_prompt", "Chatapp v%s 已发布，是否立即更新？\n(下载完成后点击系统通知即可安装)", "Chatapp v%s 已發布，是否立即更新？\n(下載完成後點擊系統通知即可安裝)", "Chatapp v%s is ready. Update now?\n(Tap the notification to install)");
        put("update_now", "立即更新", "立即更新", "Update now");
        put("update_title", "Chatapp 更新", "Chatapp 更新", "Chatapp update");
        put("update_desc", "下载完成后点击此通知安装", "下載完成後點擊此通知安裝", "Tap to install after download");
        // 活动页
        put("activities", "活动", "活動", "Activities");
        put("create_activity", "发起活动", "發起活動", "Create activity");
        put("activity_title_hint", "活动标题", "活動標題", "Activity title");
        put("activity_text_hint", "活动内容（选填）", "活動內容（選填）", "Details (optional)");
        put("activity_title_empty", "活动标题不能为空", "活動標題不能為空", "Title can't be empty");
        put("activities_empty", "暂无活动，敬请期待", "暫無活動，敬請期待", "No activities yet");
        put("activity_join", "参加", "參加", "Join");
        put("activity_joined", "已参加", "已參加", "Joined");
        put("activity_delete", "删除活动", "刪除活動", "Delete activity");
        put("jump_title", "跳一跳", "跳一跳", "Jump Jump");
        put("jump_howto", "长按蓄力，松开跳跃", "長按蓄力，鬆開跳躍", "Press and hold, release to jump");
        put("jump_score", "得分 %s", "得分 %s", "Score %s");
        put("jump_best", "最高 %s", "最高 %s", "Best %s");
        put("jump_start", "开始游戏", "開始遊戲", "Start");
        put("jump_game_over", "游戏结束", "遊戲結束", "Game Over");
        put("jump_restart", "再来一局", "再來一局", "Play Again");
        put("srv_activity_system_only", "仅系统可发布活动", "僅系統可發佈活動", "Only the system can publish activities");
        put("srv_activity_title_empty", "活动标题不能为空", "活動標題不能為空", "Title can't be empty");
        server("活动标题不能为空", "srv_activity_title_empty");
        server("仅系统可发布活动", "srv_activity_system_only");
        put("enable_permission", "开启权限", "開啟權限", "Enable");
        // 聊天页
        put("enable_notifications", "开启通知以接收新消息提醒", "開啟通知以接收新訊息提醒", "Enable notifications");
        put("enable", "开启", "開啟", "Enable");
        put("all", "全部", "全部", "All");
        put("unread", "未读", "未讀", "Unread");
        put("new_friend_requests_count", "新的好友请求 (%d)", "新的好友請求 (%d)", "New requests (%d)");
        put("no_conversations_hint", "暂无会话\n点右下角绿色＋添加好友", "暫無會話\n點右下角綠色＋新增好友", "No conversations\nTap ＋ to add friends");
        put("contacts_sync_hint", "开启通讯录权限后可同步联系人", "開啟通訊錄權限後可同步聯絡人", "Enable contacts to sync");
        put("blocked_status", "已拉黑", "已拉黑", "Blocked");
        put("friend_status", "好友", "好友", "Friend");
        put("connected", "已连接", "已連線", "Connected");
        put("connecting", "连接中", "連接中", "Connecting");
        put("disconnected", "已断开", "已斷線", "Disconnected");
        // 消息操作
        put("input_message", "输入消息...", "輸入訊息...", "Type a message...");
        put("message_actions", "消息操作", "訊息操作", "Message actions");
        put("copied", "已复制", "已複製", "Copied");
        put("copy_failed", "复制失败", "複製失敗", "Copy failed");
        put("deleted_self", "已删除(仅自己不可见)", "已刪除(僅自己不可見)", "Deleted (hidden for you)");
        put("reply_prefix", "回复: ", "回覆: ", "Reply: ");
        put("forward_to", "转发给", "轉發給", "Forward to");
        put("loading", "加载中...", "載入中...", "Loading...");
        put("no_friends_hint", "暂无好友，先添加好友吧", "暫無好友，先新增好友吧", "No friends yet");
        put("no_friends_yet", "还没有好友", "還沒有好友", "No friends yet");
        put("forward_confirm", "发送给 %s (@%s) ?", "傳送給 %s (@%s) ？", "Send to %s (@%s)?");
        put("forwarded_ok", "已转发给 %s", "已轉發給 %s", "Forwarded to %s");
        put("choose_image", "选择图片", "選擇圖片", "Choose image");
        put("choose_file", "选择文件", "選擇檔案", "Choose file");
        put("cannot_open_picker", "无法打开图片选择器", "無法開啟圖片選擇器", "Can't open picker");
        put("cannot_open_file_picker", "无法打开文件管理器", "無法開啟檔案管理員", "Can't open file picker");
        put("cannot_read_image", "无法读取图片", "無法讀取圖片", "Can't read image");
        put("image_send_failed", "图片发送失败", "圖片傳送失敗", "Image send failed");
        put("file_too_large", "文件超过 5MB", "檔案超過 5MB", "File exceeds 5MB");
        put("file_send_failed", "文件发送失败", "檔案傳送失敗", "File send failed");
        put("download_started", "开始下载", "開始下載", "Downloading");
        put("download_failed", "下载失败", "下載失敗", "Download failed");
        put("saving_image", "正在保存到下载目录", "正在儲存到下載目錄", "Saving to downloads");
        put("save_failed", "保存失败", "儲存失敗", "Save failed");
        put("image_not_ready", "图片未加载完成", "圖片未載入完成", "Image not loaded");
        put("forward_failed", "转发失败", "轉發失敗", "Forward failed");
        put("forwarded", "已转发", "已轉發", "Forwarded");
        put("image_sending", "图片发送中...", "圖片傳送中...", "Sending image...");
        put("file_sending", "文件发送中...", "檔案傳送中...", "Sending file...");
        put("favorite_coming", "收藏功能即将上线", "收藏功能即將上線", "Favorites coming soon");
        put("more_coming", "更多功能即将上线", "更多功能即將上線", "More coming soon");
        put("choose_forward_friend", "选择要转发的好友", "選擇要轉發的好友", "Choose a friend to forward");
        put("file_download_title", "Chatapp 文件", "Chatapp 檔案", "Chatapp file");
        put("downloading", "下载中...", "下載中...", "Downloading...");
        put("saving", "保存中...", "儲存中...", "Saving...");
        put("saving_image_started", "已开始保存图片", "已開始儲存圖片", "Saving image...");
        put("download_file_started", "已开始下载文件", "已開始下載檔案", "Downloading file...");
        put("file_too_large_send", "文件过大，无法发送", "檔案過大，無法傳送", "File too large");
        put("read_failed", "读取文件失败", "讀取檔案失敗", "Read failed");
        // 自己页
        put("language_region", "语言与地区", "語言與地區", "Language & region");
        put("language", "语言", "語言", "Language");
        put("region", "地区", "地區", "Region");
        put("zh_hans", "中文简体", "中文簡體", "Simplified Chinese");
        put("zh_hant", "中文繁體", "中文繁體", "Traditional Chinese");
        put("english", "English", "English", "English");
        put("theme_switch_day", "切换为白昼", "切換為白晝", "Switch to light");
        put("theme_switch_night", "切换为黑夜", "切換為黑夜", "Switch to dark");
        put("follow_system", "跟随系统切换", "跟隨系統切換", "Follow system");
        put("liquid_glass", "液态玻璃（测试中）", "液態玻璃（測試中）", "Liquid glass (beta)");
        put("liquid_glass_confirm", "液态玻璃功能尚未完善，是否开启？", "液態玻璃功能尚未完善，是否開啟？", "Liquid glass isn't fully ready. Enable it?");
        put("vibrate_feedback", "振动反馈", "振動回饋", "Haptic feedback");
        put("channel_messages", "消息提醒", "訊息提醒", "Message alerts");
        put("channel_messages_desc", "新消息到达时提醒", "新訊息到達時提醒", "Notify when new messages arrive");
        put("hide", "隐藏", "隱藏", "Hide");
        put("multi_select", "多选", "多選", "Multi-select");
        put("clear_group_history", "删除群聊天记录", "刪除群聊天記錄", "Clear group chat");
        put("clear_group_history_confirm", "确定删除该群的所有聊天记录？", "確定刪除該群的所有聊天記錄？", "Clear all messages in this group?");
        put("media_image", "[图片]", "[圖片]", "[Image]");
        put("media_file", "[文件]", "[檔案]", "[File]");
        put("update_downloading", "正在下载更新...", "正在下載更新...", "Downloading update...");
        put("install_failed", "安装失败，请检查系统设置", "安裝失敗，請檢查系統設定", "Install failed, check settings");
        put("install_perm_title", "需要安装权限", "需要安裝權限", "Install permission required");
        put("install_perm_msg", "请允许本应用安装未知应用，才能完成更新安装", "請允許本應用安裝未知應用，才能完成更新安裝", "Allow this app to install unknown apps to finish the update");
        put("install_go_settings", "去设置", "前往設定", "Go to settings");
        put("profile", "个人名片", "個人名片", "Profile card");
        put("avatar", "头像", "頭像", "Avatar");
        put("nickname", "昵称", "暱稱", "Nickname");
        put("gender", "性别", "性別", "Gender");
        put("gender_male", "男", "男", "Male");
        put("gender_female", "女", "女", "Female");
        put("gender_hidden", "隐藏", "隱藏", "Hidden");
        put("gender_other", "其他", "其他", "Other");
        put("birthday", "生日", "生日", "Birthday");
        put("tags", "个性标签", "個性標籤", "Tags");
        put("my_photos", "我的照片", "我的照片", "My photos");
        put("job", "职业", "職業", "Job");
        put("company", "公司", "公司", "Company");
        put("location", "所在地", "所在地", "Location");
        put("birthplace", "出生地", "出生地", "Birthplace");
        put("email", "邮箱", "郵箱", "Email");
        put("add_tag", "添加标签", "新增標籤", "Add tag");
        put("tag_hint", "输入标签内容（自动加#）", "輸入標籤內容（自動加#）", "Enter tag (auto #)");
        put("add_photo", "添加照片", "新增照片", "Add photo");
        put("max_photos", "最多9张照片", "最多9張照片", "Max 9 photos");
        put("job_computer", "计算机", "計算機", "Computer");
        put("job_manufacturing", "生产", "生產", "Manufacturing");
        put("job_medical", "医疗", "醫療", "Medical");
        put("job_finance", "金融", "金融", "Finance");
        put("job_business", "商业", "商業", "Business");
        put("job_culture", "文化", "文化", "Culture");
        put("job_entertainment", "娱乐", "娛樂", "Entertainment");
        put("job_legal", "法务", "法務", "Legal");
        put("job_education", "教育", "教育", "Education");
        put("job_admin", "行政", "行政", "Admin");
        put("job_student", "学生", "學生", "Student");
        put("job_other", "其他职业", "其他職業", "Other");
        put("location_hint", "格式：广东-深圳-南山", "格式：廣東-深圳-南山", "Format: Guangdong-Shenzhen-Nanshan");
        put("birthplace_hint", "格式：广东-深圳-南山", "格式：廣東-深圳-南山", "Format: Guangdong-Shenzhen-Nanshan");
        put("company_hint", "输入公司名称", "輸入公司名稱", "Enter company");
        put("email_hint", "输入邮箱地址", "輸入郵箱地址", "Enter email");
        put("email_invalid", "当前邮箱不合法", "目前郵箱不合法", "Invalid email address");
        put("read", "已读", "已讀", "Read");
        put("typing_hint", "对方正在输入...", "對方正在輸入...", "Typing...");
        put("search_msg", "搜索聊天记录", "搜尋聊天紀錄", "Search chat history");
        put("export_chat", "导出聊天记录", "匯出聊天紀錄", "Export chat");
        put("chat_tools", "聊天工具", "聊天工具", "Chat tools");
        put("no_chat_history", "暂无聊天记录", "暫無聊天紀錄", "No chat history");
        put("no_search_match", "未找到匹配的记录", "未找到匹配的紀錄", "No matching messages");
        put("chat_history_count", "共 %s 条记录", "共 %s 條紀錄", "%s messages");
        put("pin_msg", "置顶消息", "置頂訊息", "Pin message");
        put("unpin_msg", "取消置顶", "取消置頂", "Unpin message");
        put("font_size", "字体大小", "字體大小", "Font size");
        put("font_small", "小", "小", "Small");
        put("font_normal", "标准", "標準", "Normal");
        put("font_large", "大", "大", "Large");
        put("font_xlarge", "特大", "特大", "Extra large");
        put("dnd", "免打扰", "勿擾", "Do not disturb");
        put("dnd_start", "开始", "開始", "Start");
        put("dnd_end", "结束", "結束", "End");
        put("at_all", "@所有人", "@所有人", "@everyone");
        put("at_all_msg", "%s @了全体成员", "%s @了全體成員", "%s @ everyone");
        put("change_password", "修改密码", "修改密碼", "Change password");
        put("old_password", "原密码", "原密碼", "Old password");
        put("new_password", "新密码", "新密碼", "New password");
        put("confirm_password", "确认密码", "確認密碼", "Confirm password");
        put("password_changed", "密码已修改", "密碼已修改", "Password changed");
        put("delete_account", "注销账号", "註銷帳號", "Delete account");
        put("delete_account_confirm", "确定注销账号？此操作不可恢复", "確定註銷帳號？此操作不可恢復", "Delete this account? This cannot be undone");
        put("forward_to_group", "转发到群聊", "轉發到群聊", "Forward to group");
        put("qr_card", "二维码名片", "二維碼名片", "QR card");
        put("publish", "发布", "發佈", "Publish");
        put("like", "点赞", "點讚", "Like");
        put("liked", "已赞", "已讚", "Liked");
        put("moment_hint", "分享你的新鲜事...", "分享你的新鮮事...", "Share something...");
        put("moment_add_photo", "添加图片", "新增圖片", "Add image");
        put("moments_empty", "暂无动态，发一条吧", "暫無動態，發一條吧", "No moments yet, post one");
        put("channel_pick_first", "请先选择频道", "請先選擇頻道", "Please pick a channel first");
        put("cannot_open_channel", "无法打开该应用", "無法開啟該應用", "Can't open this app");
        put("choose_source", "选择方式", "選擇方式", "Choose source");
        put("take_photo", "拍照", "拍照", "Take photo");
        put("choose_image", "从相册选择", "從相冊選擇", "From gallery");
        put("cannot_open_camera", "无法打开相机", "無法開啟相機", "Can't open camera");
        put("rotate", "旋转", "旋轉", "Rotate");
        put("retake", "重拍", "重拍", "Retake");
        put("done", "完成", "完成", "Done");
        put("qr_scan", "扫一扫", "掃一掃", "Scan");
        put("comment", "评论", "評論", "Comment");
        put("my_favorites", "我的收藏", "我的收藏", "My favorites");
        put("favorited", "已收藏", "已收藏", "Favorited");
        put("reposted", "已转发", "已轉發", "Forwarded");
        put("comment_hint", "写评论...", "寫評論...", "Write a comment...");
        put("fav_messages", "收藏的对话", "收藏的對話", "Favorited messages");
        put("fav_posts", "收藏的动态", "收藏的動態", "Favorited posts");
        put("fav_empty", "暂无收藏", "暫無收藏", "No favorites");
        put("friend_center", "添加与请求", "添加與請求", "Add & Requests");
        put("scan_add_hint", "扫码识别待完善，可手动输入用户名添加", "掃碼識別待完善，可手動輸入用戶名新增", "Scan pending, enter username to add");
        put("added", "已添加", "已添加", "Added");
        put("upgrade", "升级到最新版本", "升級到最新版本", "Upgrade");
        put("current_latest", "已是最新版本", "已是最新版本", "Up to date");
        put("checking_update", "检测中...", "檢測中...", "Checking...");
        put("verify_identity", "验证身份", "驗證身份", "Verify identity");
        put("verify_nickname", "输入用户昵称", "輸入用戶暱稱", "Enter nickname");
        put("verify_username", "输入用户名", "輸入用戶名", "Enter username");
        put("verify_password", "输入用户密码", "輸入用戶密碼", "Enter password");
        put("confirm_delete2", "再次确认注销？此操作不可恢复", "再次確認註銷？此操作不可恢復", "Confirm delete again? Cannot be undone");
        put("fav_delete", "删除收藏", "刪除收藏", "Remove favorite");
        put("fav_forward", "转发", "轉發", "Forward");
        put("scan_manual_hint", "扫码识别待完善，请手动输入对方用户名添加", "掃碼識別待完善，請手動輸入對方用戶名新增", "Scan recognition pending, enter username to add");
        put("photo_count", "%d 张", "%d 張", "%d photos");
        put("changelog", "更新日志", "更新日誌", "Changelog");
        put("server_settings", "服务器设置", "伺服器設定", "Server settings");
        put("edit_nickname", "修改昵称", "修改暱稱", "Edit nickname");
        put("about", "关于", "關於", "About");
        put("logout", "退出登录", "登出", "Log out");
        put("status_hint", "愿你每天都有好心情", "願你每天都有好心情", "Have a nice day!");
        put("edit_status_title", "编辑文案", "編輯文案", "Edit status");
        put("status_empty", "文案不能为空", "文案不能為空", "Status can't be empty");
        put("nickname_empty", "昵称不能为空", "暱稱不能為空", "Nickname can't be empty");
        put("current_server", "当前服务器:", "目前伺服器:", "Server: ");
        put("saved_reconnecting", "已保存，重新连接中...", "已儲存，重新連線中...", "Saved, reconnecting...");
        put("confirm_logout_msg", "确定退出当前账号？", "確定登出目前帳號？", "Log out of this account?");
        put("no_pending", "暂无新的好友请求", "暫無新的好友請求", "No new requests");
        put("no_friends", "还没有好友\n点右下角绿色＋添加", "還沒有好友\n點右下角綠色＋新增", "No friends yet\nTap ＋ to add");
        put("new_friends", "新的好友", "新的好友", "New requests");
        put("search_username_hint", "输入对方用户名", "輸入對方用戶名", "Enter username");
        put("searching", "搜索中...", "搜尋中...", "Searching...");
        put("user_not_found", "未找到用户 @%s", "未找到用戶 @%s", "User @%s not found");
        put("this_is_you", "这是你自己 @%s", "這是你自己 @%s", "This is you @%s");
        put("request_sent", "已发送请求", "已傳送請求", "Request sent");
        put("request_sent_waiting", "请求已发送，等待对方同意", "請求已傳送，等待對方同意", "Request sent, waiting");
        put("not_connected_check", "未连接服务器，请检查地址", "未連接伺服器，請檢查地址", "Not connected, check address");
        put("nickname_updated_ok", "昵称已更新", "暱稱已更新", "Nickname updated");
        put("status_updated_ok", "文案已更新", "文案已更新", "Status updated");
        put("avatar_updated_ok", "头像已更新", "頭像已更新", "Avatar updated");
        put("avatar_uploading", "头像上传中...", "頭像上傳中...", "Uploading avatar...");
        put("avatar_upload_failed", "头像上传失败", "頭像上傳失敗", "Upload failed");
        put("choose_avatar", "选择头像", "選擇頭像", "Choose avatar");
        put("search_no_response", "搜索无响应，请检查服务器连接", "搜尋無回應，請檢查伺服器連接", "Search timed out, check connection");
        put("status_label", "状态", "狀態", "Status");
        put("request_add_friend", "请求添加你为好友", "請求添加你為好友", "wants to add you");
        put("accepted_request", "%s 接受了你的好友请求", "%s 接受了你的好友請求", "%s accepted your request");
        put("rejected_request", "%s 拒绝了你的好友请求", "%s 拒絕了你的好友請求", "%s declined your request");
        put("fatal_init", "界面初始化失败\n请点下方按钮退出登录后重试", "介面初始化失敗\n請點下方按鈕登出後重試", "Failed to load UI\nTap below to log out");
        put("confirm_clear_history_msg", "确定删除与 %s 的聊天记录？\n(仅自己可见的记录会被删除)", "確定刪除與 %s 的聊天記錄？\n(僅自己可見的記錄會被刪除)", "Clear chat with %s?\n(Only visible to you)");
        put("confirm_unfriend_msg", "确定删除好友 %s ？\n删除后需要重新添加才能聊天。", "確定刪除好友 %s ？\n刪除後需要重新添加才能聊天。", "Remove friend %s?\nYou must re-add to chat.");
        // 登录页
        put("app_subtitle", "Safe Reliable Simple", "Safe Reliable Simple", "Safe Reliable Simple");
        put("server_hint", "服务器地址（电脑IP:8899）", "伺服器地址（電腦IP:8899）", "Server (PC IP:8899)");
        put("username_hint", "用户账号（英文+数字，符号限- _ / ~ @）", "用戶帳號（英文+數字，符號限- _ / ~ @）", "Account (letters & digits; - _ / ~ @ only)");
        put("password_hint", "登录密码（8-16位，需同时含字母和数字）", "登入密碼（8-16位，需同時含字母和數字）", "Password (8-16, letters & digits)");
        put("nickname_hint", "昵称（显示给好友的名字）", "暱稱（顯示給好友的名字）", "Nickname shown to friends");
        put("no_account_register", "没有账号？点击注册", "沒有帳號？點擊註冊", "No account? Register");
        put("has_account_login", "已有账号？直接登录", "已有帳號？直接登入", "Have account? Log in");
        put("login", "登  录", "登  入", "Log in");
        put("register", "注  册", "註  冊", "Register");
        put("fill_server", "请填写服务器地址", "請填寫伺服器地址", "Enter server address");
        put("fill_username", "请填写用户账号", "請填寫用戶帳號", "Enter account");
        put("password_min", "密码需 8-16 位且同时包含字母和数字", "密碼需 8-16 位且同時包含字母和數字", "Password needs 8-16 chars with letters & digits");
        put("fill_nickname", "请填写昵称", "請填寫暱稱", "Enter nickname");
        put("account_format", "用户账号需为英文+数字，且符号只能使用 - _ / ~ @", "用戶帳號需為英文+數字，且符號只能使用 - _ / ~ @", "Account needs letters & digits; symbols only - _ / ~ @");
        put("password_rule", "登录密码需 8-16 位，且同时包含字母和数字", "登入密碼需 8-16 位，且同時包含字母和數字", "Password needs 8-16 chars with letters & digits");
        put("confirm_password_mismatch", "两次输入的密码不一致", "兩次輸入的密碼不一致", "Passwords do not match");
        put("request_failed", "请求构造失败，请重试", "請求建構失敗，請重試", "Request failed, retry");
        put("connecting_server", "连接服务器中...", "連接伺服器中...", "Connecting...");
        put("send_failed_retry", "请求发送失败，正在自动重试...", "請求傳送失敗，正在自動重試...", "Send failed, retrying...");
        put("connected_verifying", "已连接，验证中...", "已連接，驗證中...", "Connected, verifying...");
        put("conn_lost_reconnect", "连接中断，正在自动重连...", "連接中斷，正在自動重連...", "Disconnected, reconnecting...");
        put("cannot_connect", "无法连接服务器，请检查地址和网络", "無法連接伺服器，請檢查地址和網路", "Can't connect, check address");
    }

    static {
        // 服务器提示翻译映射
        server("请先登录", "srv_login_first");
        server("用户账号需为英文+数字（2-20位），符号只能使用 - _ / ~ @", "srv_bad_username");
        server("登录密码需 8-16 位，且同时包含字母和数字", "srv_bad_password");
        server("用户账号已被注册", "srv_exists");
        server("登录状态已失效，请重新登录", "srv_auth_expired");
        server("你的账号在另一台设备上登录", "srv_kicked_other");
        server("昵称不能为空且不超过 20 字", "srv_nickname_invalid");
        server("用户不存在", "srv_user_not_found");
        server("不能添加自己为好友", "srv_add_self");
        server("你们已经是好友了", "srv_already_friends");
        server("对方已向你发送请求，去接受吧", "srv_pending_in");
        server("请求已发送，等待对方同意", "srv_pending_out");
        server("好友请求已发送", "srv_request_sent");
        server("没有来自该用户的请求", "srv_no_request");
        server("已添加好友", "srv_added");
        server("已拒绝请求", "srv_rejected");
        server("还不是好友，先添加好友吧", "srv_not_friend");
        server("消息发送失败：对方已将你加入黑名单", "srv_blocked_reject");
        server("文件数据不合法或过大", "srv_bad_file");
        server("消息不存在或已被撤回", "srv_msg_gone");
        server("只能撤回自己的消息", "srv_recall_own");
        server("超过两分钟，无法撤回", "srv_recall_timeout");
        server("头像数据不合法", "srv_bad_avatar");
        server("头像保存失败", "srv_avatar_failed");
        server("群名称不能为空且不超过 24 字", "srv_group_name_invalid");
        server("已存在同名群聊", "srv_group_name_exists");
        server("群聊不存在或你已不在群中", "srv_group_not_found");
        server("仅群主可以修改群资料", "srv_owner_info");
        server("仅群主可以编辑群公告", "srv_owner_notice");
        server("仅群主可以邀请成员", "srv_owner_invite");
        server("只能邀请你的好友进群", "srv_invite_friend_only");
        server("该用户已在群中", "srv_already_in_group");
        server("仅群主可以踢人", "srv_owner_kick");
        server("群主不能踢自己", "srv_kick_self");
        server("该用户不在群中", "srv_not_in_group");
        server("仅群主可以转让群主", "srv_owner_transfer");
        server("对方不在群中", "srv_not_in_group2");
        server("你已经是群主", "srv_already_owner");
        server("群主不能退出，请先转让群主或解散群聊", "srv_owner_leave");
        server("仅群主可以解散群聊", "srv_owner_dissolve");
        server("不能拉黑自己", "srv_block_self");
        server("原密码错误", "srv_old_password_wrong");
        server("新密码需 8-16 位且同时包含字母和数字", "srv_new_password_short");
        server("动态内容不能为空", "srv_moment_empty");
        server("用户名错误", "srv_username_wrong");
        server("用户昵称错误", "srv_nickname_wrong");
        server("用户密码错误", "srv_password_wrong");
        server("登录成功", "srv_welcome_ok");
        server("欢迎回来", "srv_welcome_back");
        server("注册成功，欢迎使用 Chatapp", "srv_welcome_reg");
    }

    static {
        put("srv_login_first", "请先登录", "請先登入", "Please log in first");
        put("srv_bad_username", "用户账号需为英文+数字（2-20位），符号只能使用 - _ / ~ @", "用戶帳號需為英文+數字（2-20位），符號只能使用 - _ / ~ @", "Account must be 2-20 letters & digits, symbols only - _ / ~ @");
        put("srv_bad_password", "登录密码需 8-16 位，且同时包含字母和数字", "登入密碼需 8-16 位，且同時包含字母和數字", "Password must be 8-16 chars with letters & digits");
        put("srv_exists", "用户账号已被注册", "用戶帳號已被註冊", "Account already registered");
        put("srv_auth_expired", "登录状态已失效，请重新登录", "登入狀態已失效，請重新登入", "Session expired, please log in again");
        put("srv_kicked_other", "你的账号在另一台设备上登录", "你的帳號在另一台設備上登入", "Your account logged in on another device");
        put("srv_nickname_invalid", "昵称不能为空且不超过 20 字", "暱稱不能為空且不超過 20 字", "Nickname can't be empty (max 20 chars)");
        put("srv_user_not_found", "用户不存在", "用戶不存在", "User not found");
        put("srv_add_self", "不能添加自己为好友", "不能新增自己為好友", "Can't add yourself");
        put("srv_already_friends", "你们已经是好友了", "你們已經是好友了", "Already friends");
        put("srv_pending_in", "对方已向你发送请求，去接受吧", "對方已向你傳送請求，去接受吧", "They sent you a request, accept it");
        put("srv_pending_out", "请求已发送，等待对方同意", "請求已傳送，等待對方同意", "Request sent, waiting");
        put("srv_request_sent", "好友请求已发送", "好友請求已傳送", "Friend request sent");
        put("srv_no_request", "没有来自该用户的请求", "沒有來自該用戶的請求", "No request from this user");
        put("srv_added", "已添加好友", "已新增好友", "Friend added");
        put("srv_rejected", "已拒绝请求", "已拒絕請求", "Request declined");
        put("srv_not_friend", "还不是好友，先添加好友吧", "還不是好友，先新增好友吧", "Not friends yet");
        put("srv_blocked_reject", "消息发送失败：对方已将你加入黑名单", "訊息傳送失敗：對方已將你加入黑名單", "Send failed: you're blocked");
        put("srv_bad_file", "文件数据不合法或过大", "檔案資料不合法或過大", "Invalid or too large file");
        put("srv_msg_gone", "消息不存在或已被撤回", "訊息不存在或已被撤回", "Message gone");
        put("srv_recall_own", "只能撤回自己的消息", "只能撤回自己的訊息", "Only recall your own messages");
        put("srv_recall_timeout", "超过两分钟，无法撤回", "超過兩分鐘，無法撤回", "Past 2 minutes, can't recall");
        put("srv_bad_avatar", "头像数据不合法", "頭像資料不合法", "Invalid avatar");
        put("srv_avatar_failed", "头像保存失败", "頭像儲存失敗", "Avatar save failed");
        put("srv_group_name_invalid", "群名称不能为空且不超过 24 字", "群組名稱不能為空且不超過 24 字", "Group name can't be empty (max 24 chars)");
        put("srv_group_name_exists", "已存在同名群聊", "已存在同名群聊", "A group with this name exists");
        put("srv_group_not_found", "群聊不存在或你已不在群中", "群聊不存在或你已不在群中", "Group not found or you left");
        put("srv_owner_info", "仅群主可以修改群资料", "僅群主可以修改群資料", "Only owner can edit group info");
        put("srv_owner_notice", "仅群主可以编辑群公告", "僅群主可以編輯群公告", "Only owner can edit notice");
        put("srv_owner_invite", "仅群主可以邀请成员", "僅群主可以邀請成員", "Only owner can invite");
        put("srv_invite_friend_only", "只能邀请你的好友进群", "只能邀請你的好友進群", "Only friends can be invited");
        put("srv_already_in_group", "该用户已在群中", "該用戶已在群中", "Already in group");
        put("srv_owner_kick", "仅群主可以踢人", "僅群主可以踢人", "Only owner can remove members");
        put("srv_kick_self", "群主不能踢自己", "群主不能踢自己", "Owner can't remove self");
        put("srv_not_in_group", "该用户不在群中", "該用戶不在群中", "Not in group");
        put("srv_owner_transfer", "仅群主可以转让群主", "僅群主可以轉讓群主", "Only owner can transfer");
        put("srv_not_in_group2", "对方不在群中", "對方不在群中", "Not in group");
        put("srv_already_owner", "你已经是群主", "你已經是群主", "You're already owner");
        put("srv_owner_leave", "群主不能退出，请先转让群主或解散群聊", "群主不能退出，請先轉讓群主或解散群聊", "Owner can't leave; transfer or dissolve first");
        put("srv_owner_dissolve", "仅群主可以解散群聊", "僅群主可以解散群聊", "Only owner can dissolve");
        put("srv_block_self", "不能拉黑自己", "不能拉黑自己", "Can't block yourself");
        put("srv_old_password_wrong", "原密码错误", "原密碼錯誤", "Old password is wrong");
        put("srv_new_password_short", "新密码需 8-16 位且同时包含字母和数字", "新密碼需 8-16 位且同時包含字母和數字", "New password needs 8-16 chars with letters & digits");
        put("srv_moment_empty", "动态内容不能为空", "動態內容不能為空", "Content cannot be empty");
        put("srv_username_wrong", "用户名错误", "用戶名錯誤", "Username is wrong");
        put("srv_nickname_wrong", "用户昵称错误", "用戶暱稱錯誤", "Nickname is wrong");
        put("srv_password_wrong", "用户密码错误", "用戶密碼錯誤", "Password is wrong");
        put("srv_welcome_ok", "登录成功", "登入成功", "Logged in");
        put("srv_welcome_back", "欢迎回来", "歡迎回來", "Welcome back");
        put("srv_welcome_reg", "注册成功，欢迎使用 Chatapp", "註冊成功，歡迎使用 Chatapp", "Registered, welcome to Chatapp");
    }

    private static void put(String key, String zhHans, String zhHant, String en) {
        DICT.put(key, new String[]{zhHans, zhHant, en});
    }

    private static void server(String zh, String key) {
        SERVER_MSG.put(zh, key);
    }
}
