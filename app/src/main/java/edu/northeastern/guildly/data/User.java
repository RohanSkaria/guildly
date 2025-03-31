package edu.northeastern.guildly.data;

import java.util.List;
import java.util.Map;

/**
 * Represents a user in the system.
 * Includes:
 * - Basic profile info (username, email, password, etc.)
 * - Habits array
 * - Friends (list of user keys who are confirmed friends)
 * - Friend requests (map of user keys to a status: "pending", "accepted", or "rejected")
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

    // Default constructor required by Firebase
    public User() {
    }

    // Full constructor
    public User(
            String username,
            String email,
            String password,
            String profilePicUrl,
            String aboutMe,
            List<String> habits,
            List<String> friends,
            Map<String, String> friendRequests
    ) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.profilePicUrl = profilePicUrl;
        this.aboutMe = aboutMe;
        this.habits = habits;
        this.friends = friends;
        this.friendRequests = friendRequests;
    }
}
