package edu.northeastern.guildly.data;

public class LeaderboardItem {
    private String username;
    private int streakCount;
    private int profileImageRes;

    public LeaderboardItem(String username, int streakCount, int profileImageRes) {
        this.username = username;
        this.streakCount = streakCount;
        this.profileImageRes = profileImageRes;
    }

    public String getUsername() { return username; }
    public int getStreakCount() { return streakCount; }
    public int getProfileImageRes() { return profileImageRes; }
}
