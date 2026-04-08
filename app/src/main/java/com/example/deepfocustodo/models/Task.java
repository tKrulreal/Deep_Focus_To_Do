package com.example.deepfocustodo.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String description;
    private boolean completed;
    private long createdAt;
    private long updatedAt;
    private int priority; // 1: Low, 2: Medium, 3: High
    private int estimatedSessions; // Pomodoros estimated
    private int completedSessions; // Pomodoros completed

    public Task() {
    }

    public Task(String title, String description, boolean completed, long createdAt, int priority, int estimatedSessions) {
        this.title = title;
        this.description = description;
        this.completed = completed;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.priority = priority;
        this.estimatedSessions = estimatedSessions;
        this.completedSessions = 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getEstimatedSessions() { return estimatedSessions; }
    public void setEstimatedSessions(int estimatedSessions) { this.estimatedSessions = estimatedSessions; }

    public int getCompletedSessions() { return completedSessions; }
    public void setCompletedSessions(int completedSessions) { this.completedSessions = completedSessions; }
}