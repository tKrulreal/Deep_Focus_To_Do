package com.example.deepfocustodo.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.deepfocustodo.models.FocusSession;
import java.util.List;

@Dao
public interface FocusSessionDao {
    @Insert
    long insertSession(FocusSession session);

    @Delete
    void deleteSession(FocusSession session);

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    List<FocusSession> getAllHistory();

    @Query("SELECT * FROM focus_sessions WHERE startTime BETWEEN :start AND :end ORDER BY startTime DESC")
    List<FocusSession> getHistoryInRange(long start, long end);

    @Query("SELECT SUM(actualDuration) FROM focus_sessions WHERE status = 'COMPLETED' AND type = 'FOCUS'")
    Integer getTotalFocusMinutes();

    @Query("SELECT SUM(pointsEarned) FROM focus_sessions WHERE status = 'COMPLETED'")
    Integer getTotalPoints();

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED' AND type = 'FOCUS'")
    int getCompletedSessionCount();

    @Query("SELECT startTime FROM focus_sessions WHERE status = 'COMPLETED' AND type = 'FOCUS' ORDER BY startTime DESC")
    List<Long> getCompletedSessionStartTimes();

    @Query("SELECT SUM(actualDuration) FROM focus_sessions WHERE status = 'COMPLETED' AND type = 'FOCUS' AND startTime BETWEEN :startOfDay AND :endOfDay")
    Integer getTotalFocusMinutesInDay(long startOfDay, long endOfDay);

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED' AND type = 'FOCUS' AND startTime BETWEEN :startOfDay AND :endOfDay")
    int getCompletedSessionsCountInDay(long startOfDay, long endOfDay);

    @Query("SELECT SUM(pointsEarned) FROM focus_sessions WHERE status = 'COMPLETED' AND startTime BETWEEN :startOfDay AND :endOfDay")
    Integer getPointsEarnedInDay(long startOfDay, long endOfDay);

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'FAILED' AND type = 'FOCUS'")
    int getTotalFailedSessionCount();

    @Query("SELECT taskId, SUM(actualDuration) as totalDuration FROM focus_sessions " +
            "WHERE status = 'COMPLETED' AND type = 'FOCUS' AND taskId IS NOT NULL " +
            "GROUP BY taskId ORDER BY totalDuration DESC LIMIT :limit")
    List<TaskDuration> getTopFocusedTasks(int limit);

    @Query("DELETE FROM focus_sessions")
    void clearAllHistory();

    class TaskDuration {
        public Integer taskId;
        public int totalDuration;
    }
}