package com.example.deepfocustodo.utils;

import android.content.Context;
import com.example.deepfocustodo.database.AppDatabase;
import com.example.deepfocustodo.models.FocusSession;

public class SessionManager {
    private static Integer selectedTaskId = null;
    private static long sessionStartTime = 0;

    private static final int POINTS_PER_MINUTE = 10;
    private static final int COMPLETION_BONUS = 50;

    public static void setSelectedTaskId(Integer taskId) {
        selectedTaskId = taskId;
    }

    public static Integer getSelectedTaskId() {
        return selectedTaskId;
    }

    public static void startSession() {
        sessionStartTime = System.currentTimeMillis();
    }

    public static boolean isSessionRunning() {
        return sessionStartTime > 0;
    }

    public static void clearSelectedTask() {
        selectedTaskId = null;
    }

    public static void recordSession(Context context, boolean isCompleted) {
        if (sessionStartTime == 0) return;

        long endTime = System.currentTimeMillis();
        long durationMs = endTime - sessionStartTime;
        int durationMinutes = (int) Math.max(1, durationMs / (1000 * 60));

        int points = isCompleted ? (durationMinutes * POINTS_PER_MINUTE) + COMPLETION_BONUS : 0;
        String status = isCompleted ? "COMPLETED" : "FAILED";

        FocusSession session = new FocusSession(
                selectedTaskId,
                sessionStartTime,
                endTime,
                durationMinutes,
                points,
                status
        );

        new Thread(() -> {
            AppDatabase.getInstance(context).focusSessionDao().insertSession(session);
        }).start();
        
        // Reset start time for next session
        sessionStartTime = 0;
    }
}