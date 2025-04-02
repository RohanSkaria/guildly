package edu.northeastern.guildly.data;

import java.util.List;
import java.util.Map;
public class User {
    public String username;
    public String email;
    public String password;
    public String profilePicUrl;
    public String aboutMe;

    // Remove the 'List<Habit> habits' field
    // Keep your other fields
    public List<String> friends; // if you do store them as an array
    public Map<String, String> friendRequests;
    public Map<String, Boolean> chats;

    // Needed for Firebase
    public User() {}

    public User(String username,
                String email,
                String password,
                String profilePicUrl,
                String aboutMe,
                // no List<Habit> here
                List<String> friends,
                Map<String, String> friendRequests,
                Map<String, Boolean> chats) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.profilePicUrl = profilePicUrl;
        this.aboutMe = aboutMe;
        this.friends = friends;
        this.friendRequests = friendRequests;
        this.chats = chats;
    }
}
