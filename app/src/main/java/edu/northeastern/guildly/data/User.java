package edu.northeastern.guildly.data;

import java.util.List;
import java.util.Map;

public class User {
    public String username;
    public String email;
    public String password;
    public String profilePicUrl;
    public String aboutMe;

    public List<String> friends;
    public Map<String, String> friendRequests;
    public Map<String, Boolean> chats;

    // New field to track the number of weekly challenges completed
    public int weeklyChallengePts;

    // Needed for Firebase (default constructor)
    public User() {
        // Initialize the new field if needed
        this.weeklyChallengePts = 0;
    }

    public User(String username,
                String email,
                String password,
                String profilePicUrl,
                String aboutMe,
                List<String> friends,
                Map<String, String> friendRequests,
                Map<String, Boolean> chats,
                int weeklyChallengePts) {

        this.username = username;
        this.email = email;
        this.password = password;
        this.profilePicUrl = profilePicUrl;
        this.aboutMe = aboutMe;
        this.friends = friends;
        this.friendRequests = friendRequests;
        this.chats = chats;
        this.weeklyChallengePts = weeklyChallengePts;
    }
}
