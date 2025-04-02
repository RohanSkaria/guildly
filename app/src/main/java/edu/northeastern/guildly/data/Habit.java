package edu.northeastern.guildly.data;

import java.io.Serializable;

/**
 * Represents a single habit with daily streak logic plus isTracked for sign-up.
 */
public class Habit implements Serializable {
    private String habitName;
    private int iconResId;

    private boolean isTracked;       // NEW: indicates if user is actually tracking this habit
    private int streakCount;
    private long lastCompletedTime;
    private boolean completedToday;
    private long nextAvailableTime;

    // Default constructor required for Firebase & deserialization
    public Habit() {
    }

    public Habit(String habitName, int iconResId) {
        this.habitName = habitName;
        this.iconResId = iconResId;
        this.isTracked = false; // by default, not tracked
        this.streakCount = 0;
        this.lastCompletedTime = 0;
        this.completedToday = false;
        this.nextAvailableTime = 0;
    }

    // Getters & Setters

    public String getHabitName() {
        return habitName;
    }
    public int getIconResId() {
        return iconResId;
    }
    public boolean isTracked() {
        return isTracked;
    }
    public int getStreakCount() {
        return streakCount;
    }
    public long getLastCompletedTime() {
        return lastCompletedTime;
    }
    public boolean isCompletedToday() {
        return completedToday;
    }
    public long getNextAvailableTime() {
        return nextAvailableTime;
    }

    public void setHabitName(String habitName) {
        this.habitName = habitName;
    }
    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }
    public void setTracked(boolean tracked) {
        this.isTracked = tracked;
    }
    public void setStreakCount(int streakCount) {
        this.streakCount = streakCount;
    }
    public void setLastCompletedTime(long t) {
        this.lastCompletedTime = t;
    }
    public void setCompletedToday(boolean c) {
        this.completedToday = c;
    }
    public void setNextAvailableTime(long nextTime) {
        this.nextAvailableTime = nextTime;
    }
}
