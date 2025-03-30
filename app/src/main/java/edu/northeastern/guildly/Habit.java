package edu.northeastern.guildly;

public class Habit {
    private String name;
    private int iconResId;

    public Habit(String name, int iconResId) {
        this.name = name;
        this.iconResId = iconResId;
    }

    public String getName() {
        return name;
    }

    public int getIconResId() {
        return iconResId;
    }
}

