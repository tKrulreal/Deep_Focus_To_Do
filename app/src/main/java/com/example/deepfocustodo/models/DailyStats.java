package com.example.deepfocustodo.models;

import androidx.room.Ignore;

public class DailyStats {
    private String dateLabel; 
    private int totalMinutes;
    private int sessionCount;
    private int pointsEarned;

    public DailyStats() {
    }

    @Ignore
    public DailyStats(String dateLabel, int totalMinutes, int sessionCount, int pointsEarned) {
        this.dateLabel = dateLabel;
        this.totalMinutes = totalMinutes;
        this.sessionCount = sessionCount;
        this.pointsEarned = pointsEarned;
    }

    public String getDateLabel() { return dateLabel; }
    public int getTotalMinutes() { return totalMinutes; }
    public int getSessionCount() { return sessionCount; }
    public int getPointsEarned() { return pointsEarned; }

    public void setDateLabel(String dateLabel) { this.dateLabel = dateLabel; }
    public void setTotalMinutes(int totalMinutes) { this.totalMinutes = totalMinutes; }
    public void setSessionCount(int sessionCount) { this.sessionCount = sessionCount; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }
}