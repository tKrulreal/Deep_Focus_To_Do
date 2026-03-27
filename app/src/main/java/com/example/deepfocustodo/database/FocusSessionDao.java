package com.example.deepfocustodo.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

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
    int getTotalPoints();

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE status = 'COMPLETED' AND startTime BETWEEN :startOfDay AND :endOfDay")
    int getTotalFocusMinutesInDay(long startOfDay, long endOfDay);
}