package edu.northeastern.guildly.data;

import java.util.List;
import java.util.Map;

/**
 * Represents a single chat (conversation) in the database.
 * - chatId: Unique identifier (often the key in "chats" node).
 * - participants: List of user keys (e.g., ["alice@example,com", "bob@example,com"]).
 * - messages: A map (or child node) of messageId -> Message object.
 */
public class Chats {
    public String chatId;
    public List<String> participants;  // E.g. [ "alice@example,com", "bob@example,com" ]
    public Map<String, Message> messages;

    // Default constructor required by Firebase
    public Chats() {
    }

    // Full constructor if needed
    public Chats(String chatId,
                 List<String> participants,
                 Map<String, Message> messages) {
        this.chatId = chatId;
        this.participants = participants;
        this.messages = messages;
    }
}
