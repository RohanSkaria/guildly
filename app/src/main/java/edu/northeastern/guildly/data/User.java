package edu.northeastern.guildly.data;

import java.util.List;
import java.util.Map;

/**
 * Represents a user in the system.
 * Includes:
 *  - Basic profile info (username, email, password, etc.)
 *  - Habits array
 *  - Friends (list of user keys who are confirmed friends)
 *  - Friend requests (map of user keys to a status: "pending", "accepted", or "rejected")
 *  - (Optional) chats: references to chat IDs this user is part of
 */
public class User {

    public String username;
    public String email;
    public String password;
    public String profilePicUrl;
    public String aboutMe;
    public List<String> habits;

    /**
     * A list of user keys (e.g., sanitized emails) representing confirmed friends.
     */
    public List<String> friends;

    /**
     * A map of user keys to a status string, for friend requests.
     * For example: {"alice@example,com": "pending", "bob@example,com": "accepted"}
     */
    public Map<String, String> friendRequests;

    /**
     * (Optional) A map of chatId -> true, for references to the chats a user is in.
     * Example: { "-N32AbCdEfG": true, "-N67XyZ...": true }
     */
    public Map<String, Boolean> chats;

    // Default constructor required by Firebase
    public User() {
    }

    // Full constructor with all fields (including new 'chats')
    public User(String username,
                String email,
                String password,
                String profilePicUrl,
                String aboutMe,
                List<String> habits,
                List<String> friends,
                Map<String, String> friendRequests,
                Map<String, Boolean> chats) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.profilePicUrl = profilePicUrl;
        this.aboutMe = aboutMe;
        this.habits = habits;
        this.friends = friends;
        this.friendRequests = friendRequests;
        this.chats = chats;
    }
}
