package edu.northeastern.guildly.model;

import java.util.List;

public class User {
    public String username;       // e.g. "John"
    public String email;          // e.g. "john@example.com"
    public String password;       // NOT SECURE! Plaintext example only
    public String profilePicUrl;  // store URL or some placeholder
    public String aboutMe;        // short bio
    public List<String> habits;   // user’s chosen habits

    // Default constructor (needed for Firebase DataSnapshot)
    public User() { }

    public User(String username,
                String email,
                String password,
                String profilePicUrl,
                String aboutMe,
                List<String> habits) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.profilePicUrl = profilePicUrl;
        this.aboutMe = aboutMe;
        this.habits = habits;
    }
}
