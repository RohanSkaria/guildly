package edu.northeastern.guildly.data;

public class Friend {
    private String username;
    private int streakCount;
    private int profileImageResource;

    public Friend(String username, int streakCount, int profileImageResource) {
        this.username = username;
        this.streakCount = streakCount;
        this.profileImageResource = profileImageResource;
    }

    public String getUsername() {
        return username;
    }

    public int getStreakCount() {
        return streakCount;
    }

    public int getProfileImageResource() {
        return profileImageResource;
    }
}