package edu.northeastern.guildly.data;

public class FriendChatItem {
    public String friendKey;
    public String friendUsername;
    public String chatId;
    public String lastMessage;
    public int lastMessageIconRes;
    public String timestamp;

    public FriendChatItem(String friendKey,
                          String friendUsername,
                          String chatId,
                          String lastMessage,
                          int lastMessageIconRes,
                          String timestamp) {
        this.friendKey = friendKey;
        this.friendUsername = friendUsername;
        this.chatId = chatId;
        this.lastMessage = lastMessage;
        this.lastMessageIconRes = lastMessageIconRes;
        this.timestamp = timestamp;
    }
}
