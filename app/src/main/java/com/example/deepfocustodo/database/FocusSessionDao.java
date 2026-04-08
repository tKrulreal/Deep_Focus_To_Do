package com.example.deepfocustodo.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.deepfocustodo.models.DailyStats;
import com.example.deepfocustodo.models.FocusSession;

import java.util.List;

@Dao
public interface FocusSessionDao {

    @Insert
    void insertSession(FocusSession session);

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    List<FocusSession> getAllHistory();

    @Query("SELECT * FROM focus_sessions WHERE startTime BETWEEN :startOfDay AND :endOfDay")
    List<FocusSession> getSessionsInDay(long startOfDay, long endOfDay);

    @Query("SELECT SUM(pointsEarned) FROM focus_sessions WHERE status = 'COMPLETED'")
    Integer getTotalPoints();

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE status = 'COMPLETED' AND startTime BETWEEN :startOfDay AND :endOfDay")
    Integer getTotalFocusMinutesInDay(long startOfDay, long endOfDay);

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE status = 'COMPLETED'")
    Integer getTotalFocusMinutes();

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED'")
    int getCompletedSessionCount();

    @Query("SELECT startTime FROM focus_sessions WHERE status = 'COMPLETED' ORDER BY startTime DESC")
    List<Long> getCompletedSessionStartTimes();

    @Query("SELECT strftime('%d/%m', startTime/1000, 'unixepoch', 'localtime') AS dateLabel, " +
            "SUM(durationMinutes) AS totalMinutes, " +
            "COUNT(*) AS sessionCount, " +
            "SUM(pointsEarned) AS pointsEarned " +
            "FROM focus_sessions " +
            "WHERE status = 'COMPLETED' " +
            "GROUP BY strftime('%Y-%m-%d', startTime/1000, 'unixepoch', 'localtime') " +
            "ORDER BY MAX(startTime) DESC " +
            "LIMIT :limit")
    List<DailyStats> getRecentDailyStats(int limit);

    // Tính tổng điểm kiếm được tính đến cuối ngày đang chọn
    @Query("SELECT SUM(pointsEarned) FROM focus_sessions WHERE status = 'COMPLETED' AND startTime <= :endOfDayMs")
    Integer getTotalPointsUpTo(long endOfDayMs);

    // Lấy lịch sử các phiên tính đến cuối ngày đang chọn
    @Query("SELECT startTime FROM focus_sessions WHERE status = 'COMPLETED' AND startTime <= :endOfDayMs ORDER BY startTime DESC")
    List<Long> getCompletedSessionStartTimesUpTo(long endOfDayMs);
}