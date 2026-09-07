# Chatapp App 页面结构地图（重组参考）

> 用途：页面/功能重组时的对照清单。原则：只新增、只转移，不误删。

## 一、页面（Activity）

| 页面 | 文件 | 现有功能块 |
| --- | --- | --- |
| 登录/注册页 | MainActivity.java | 服务器地址输入、用户名/密码、昵称(注册)、登录/注册按钮、版本显示 |
| 主界面（三标签） | HomeActivity.java | 顶栏(标题/连接状态/退出)、底部三标签切换 |
| ├ 聊天标签 | HomeActivity.java (renderChats) | 会话列表(头像/昵称/最后消息/时间/未读角标)、空状态提示 |
| ├ 联系人标签 | HomeActivity.java (renderContacts) | 添加好友按钮、搜索结果卡片区、新的好友(接受/拒绝)、好友列表(在线状态) |
| └ 我的标签 | HomeActivity.java (renderMe) | 大头像、昵称、@用户名、服务器地址、版本、改昵称、检查更新、退出登录 |
| 聊天窗口 | ChatActivity.java | 顶栏(返回/对方昵称)、消息气泡(收发)、输入框+发送 |

## 二、公共组件（一般不动）

| 文件 | 职责 |
| --- | --- |
| Net.java | TCP 连接 + 发送队列（v1.11 架构，所有请求走网络线程） |
| Models.java | 数据模型（ChatMsg / Conversation / Contact） |
| Session.java | 本地会话（用户名/昵称/服务器地址） |
| Utils.java | 颜色、控件、头像、时间格式 |
| UpdateChecker.java | 检查更新/下载 APK |
| App.java | 全局崩溃上报 |

## 三、消息类型分发（handle()）

主界面 HomeActivity.handle() 已分发：conversations / contacts / search_result / new_message / friend_request / friend_response / contacts_changed / nickname_updated / toast / error / kicked。

> 新增功能时，如需服务器推送新事件，在 handle() 加分支即可（服务端 server.js 同步加处理）。

## 四、重组注意事项

1. 转移功能 = 移动对应方法 + 其使用的字段引用，例如 renderContacts 里的搜索框/结果卡片逻辑。
2. 新增页面 = 新 Activity 文件 + AndroidManifest.xml 注册。
3. HomeActivity 的 tab 切换（selectTab）与 ScrollView 可见性绑定（chatsScroll/contactsScroll/meScroll），改标签结构时一起改。
4. 聊天窗口入口：openChat(username, nickname) 通过 Intent 传参。
5. 备份位置：C:\Users\cheng\Documents\Codex\Chatapp-build-backup（已删除）。
