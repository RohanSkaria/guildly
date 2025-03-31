package edu.northeastern.guildly.data;

public class Message {
    public String senderId;   // "alice@example,com"
    public String content;    // actual text
    public long timestamp;    // System.currentTimeMillis()
    public String status;     // e.g., "SENT", "READ", "UNREAD", etc.

    // Default constructor for Firebase
    public Message() {
    }

    public Message(String senderId, String content, long timestamp, String status) {
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
        this.status = status;
    }
}
