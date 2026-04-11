package com.example.deepfocustodo.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.deepfocustodo.models.DailyStats;
import com.example.deepfocustodo.models.FocusSession;

import java.util.List;

@Dao
public interface FocusSessionDao {

    @Insert
    void insertSession(FocusSession session);

    @Delete
    void deleteSession(FocusSession session);

    @Query("DELETE FROM focus_sessions")
    void clearAllHistory();

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    List<FocusSession> getAllHistory();

    @Query("SELECT * FROM focus_sessions WHERE startTime BETWEEN :startOfDay AND :endOfDay")
    List<FocusSession> getSessionsInDay(long startOfDay, long endOfDay);

    @Query("SELECT SUM(pointsEarned) FROM focus_sessions WHERE status = 'COMPLETED'")
    Integer getTotalPoints();

    @Query("SELECT SUM(pointsEarned) FROM focus_sessions WHERE status = 'COMPLETED' AND startTime BETWEEN :startOfDay AND :endOfDay")
    Integer getPointsEarnedInDay(long startOfDay, long endOfDay);

    @Query("SELECT SUM(pointsEarned) FROM focus_sessions WHERE status = 'COMPLETED' AND startTime <= :endTime")
    Integer getTotalPointsUpTo(long endTime);

    @Query("SELECT SUM(actualDuration) FROM focus_sessions WHERE status = 'COMPLETED' AND startTime BETWEEN :startOfDay AND :endOfDay")
    Integer getTotalFocusMinutesInDay(long startOfDay, long endOfDay);

    @Query("SELECT SUM(actualDuration) FROM focus_sessions WHERE status = 'COMPLETED'")
    Integer getTotalFocusMinutes();

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED'")
    int getCompletedSessionCount();

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED' AND startTime BETWEEN :startOfDay AND :endOfDay")
    int getCompletedSessionsCountInDay(long startOfDay, long endOfDay);

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status != 'COMPLETED'")
    int getTotalFailedSessionCount();

    @Query("SELECT startTime FROM focus_sessions WHERE status = 'COMPLETED' ORDER BY startTime DESC")
    List<Long> getCompletedSessionStartTimes();

    @Query("SELECT startTime FROM focus_sessions WHERE status = 'COMPLETED' AND startTime <= :endTime ORDER BY startTime DESC")
    List<Long> getCompletedSessionStartTimesUpTo(long endTime);

    @Query("SELECT strftime('%d/%m', startTime/1000, 'unixepoch', 'localtime') AS dateLabel, " +
            "SUM(actualDuration) AS totalMinutes, " +
            "COUNT(*) AS sessionCount, " +
            "SUM(pointsEarned) AS pointsEarned " +
            "FROM focus_sessions " +
            "WHERE status = 'COMPLETED' " +
            "GROUP BY strftime('%Y-%m-%d', startTime/1000, 'unixepoch', 'localtime') " +
            "ORDER BY MAX(startTime) DESC " +
            "LIMIT :limit")
    List<DailyStats> getRecentDailyStats(int limit);

    @Query("SELECT taskId AS taskId, SUM(actualDuration) AS totalDuration FROM focus_sessions " +
            "WHERE status = 'COMPLETED' AND taskId IS NOT NULL " +
            "GROUP BY taskId ORDER BY totalDuration DESC LIMIT :limit")
    List<TaskDuration> getTopFocusedTasks(int limit);

    static class TaskDuration {
        public Integer taskId;
        public int totalDuration;
    }
}