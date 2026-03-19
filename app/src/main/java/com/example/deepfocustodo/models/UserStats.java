package com.example.deepfocustodo.models;

public class UserStats {

    private int totalFocusMinutes;
    private int totalPoints;
    private int streakDays;

    public UserStats() {
    }

    public UserStats(int totalFocusMinutes, int totalPoints, int streakDays) {
        this.totalFocusMinutes = totalFocusMinutes;
        this.totalPoints = totalPoints;
        this.streakDays = streakDays;
    }

    public int getTotalFocusMinutes() {
        return totalFocusMinutes;
    }

    public void setTotalFocusMinutes(int totalFocusMinutes) {
        this.totalFocusMinutes = totalFocusMinutes;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public int getStreakDays() {
        return streakDays;
    }

    public void setStreakDays(int streakDays) {
        this.streakDays = streakDays;
    }
}