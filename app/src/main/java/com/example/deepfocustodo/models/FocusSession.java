package com.example.deepfocustodo.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "focus_sessions")
public class FocusSession {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private Integer taskId; 
    private long startTime;
    private long endTime;
    private int durationMinutes;
    private int pointsEarned; 
    private String status; 

    public FocusSession() {
    }

    public FocusSession(Integer taskId, long startTime, long endTime, int durationMinutes, int pointsEarned, String status) {
        this.taskId = taskId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMinutes = durationMinutes;
        this.pointsEarned = pointsEarned;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}