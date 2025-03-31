package edu.northeastern.guildly.data;

public class FriendChatItem {
    public String friendKey;
    public String friendUsername;
    public String chatId;           // existing chat ID or null
    public String lastMessage;      // actual last message or "Say hello..."
    public int lastMessageIconRes;  // e.g. R.drawable.ic_msg_solid or -1 if no icon

    public FriendChatItem(String friendKey,
                          String friendUsername,
                          String chatId,
                          String lastMessage,
                          int lastMessageIconRes) {
        this.friendKey = friendKey;
        this.friendUsername = friendUsername;
        this.chatId = chatId;
        this.lastMessage = lastMessage;
        this.lastMessageIconRes = lastMessageIconRes;
    }
}
