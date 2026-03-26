package com.example.deepfocustodo.models;

public class DailyStats {
    private String dateLabel; 
    private int totalMinutes;
    private int sessionCount;
    private int pointsEarned;

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
}