package com.example.deepfocustodo.models;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;


@Entity(
        tableName = "focus_sessions",
        foreignKeys = @ForeignKey(
                entity = Task.class,
                parentColumns = "id",
                childColumns = "taskId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("taskId")}
)
public class FocusSession {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private Integer taskId; 
    private String type; // FOCUS, SHORT_BREAK, LONG_BREAK
    private long startTime;
    private long endTime;
    private int plannedDuration; // in minutes
    private int actualDuration; // in minutes
    private int pointsEarned; 
    private String status; // COMPLETED, FAILED, ABANDONED

    public FocusSession() {
    }

    @Ignore
    public FocusSession(Integer taskId, String type, long startTime, long endTime, int plannedDuration, int actualDuration, int pointsEarned, String status) {
        this.taskId = taskId;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.plannedDuration = plannedDuration;
        this.actualDuration = actualDuration;
        this.pointsEarned = pointsEarned;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public int getPlannedDuration() { return plannedDuration; }
    public void setPlannedDuration(int plannedDuration) { this.plannedDuration = plannedDuration; }

    public int getActualDuration() { return actualDuration; }
    public void setActualDuration(int actualDuration) { this.actualDuration = actualDuration; }

    // Backward-compatible aliases used by legacy UI/statistics code.
    public int getDurationMinutes() { return actualDuration; }
    public void setDurationMinutes(int durationMinutes) { this.actualDuration = durationMinutes; }

    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}