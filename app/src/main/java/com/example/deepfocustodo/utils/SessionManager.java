package com.example.deepfocustodo.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.deepfocustodo.database.AppDatabase;
import com.example.deepfocustodo.models.FocusSession;
import com.example.deepfocustodo.models.Task;

import java.util.Calendar;

public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final int MINIMUM_SESSION_SECONDS = 10;
    public static final String ACTION_TASK_REACHED_GOAL = "com.example.deepfocustodo.action.TASK_REACHED_GOAL";
    public static final String EXTRA_TASK_ID = "EXTRA_TASK_ID";

    public static void setSelectedTaskId(Context context, Integer taskId) {
        new PreferenceHelper(context).setSelectedTaskId(taskId);
    }

    public static Integer getSelectedTaskId(Context context) {
        return new PreferenceHelper(context).getSelectedTaskId();
    }

    public static void startSession(Context context, String type, int plannedDuration) {
        new PreferenceHelper(context).saveSessionState(System.currentTimeMillis(), type, plannedDuration);
    }

    public static boolean isSessionRunning(Context context) {
        return new PreferenceHelper(context).getSessionStartTime() > 0;
    }

    public static void clearSelectedTask(Context context) {
        new PreferenceHelper(context).setSelectedTaskId(null);
    }

    public static void clearIfSelected(Context context, int taskId) {
        Integer selectedId = getSelectedTaskId(context);
        if (selectedId != null && selectedId == taskId) {
            clearSelectedTask(context);
        }
    }

    public static void recordSession(Context context, int actualDurationMinutes, boolean isCompleted) {
        PreferenceHelper prefs = new PreferenceHelper(context);
        final long start = prefs.getSessionStartTime();

        if (start == 0) return;

        final long end = System.currentTimeMillis();

        if (!isCompleted && (end - start) < (MINIMUM_SESSION_SECONDS * 1000)) {
            prefs.clearSessionState();
            return;
        }

        final Integer taskId = prefs.getSelectedTaskId();
        final String type = prefs.getSessionType();
        final int planned = prefs.getSessionPlannedDuration();

        prefs.clearSessionState();

        AppExecutors.diskIO().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);

                Task task = null;
                if (taskId != null) {
                    task = db.taskDao().getTaskById(taskId);
                }

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long startDay = cal.getTimeInMillis();
                int sessionsToday = db.focusSessionDao().getCompletedSessionsCountInDay(startDay, end);

                int points = PointsCalculator.calculatePoints(actualDurationMinutes, isCompleted, type, task, sessionsToday);
                String status = isCompleted ? "COMPLETED" : "FAILED";

                // Logic increment và check goal
                if (isCompleted && "FOCUS".equals(type) && taskId != null) {
                    db.taskDao().incrementCompletedSessions(taskId);
                    
                    Task updatedTask = db.taskDao().getTaskById(taskId);
                    if (updatedTask != null && updatedTask.getEstimatedSessions() > 0 && updatedTask.getCompletedSessions() >= updatedTask.getEstimatedSessions()) {
                        Log.d(TAG, "Task reached goal: " + taskId + " (" + updatedTask.getCompletedSessions() + "/" + updatedTask.getEstimatedSessions() + ")");
                        Intent intent = new Intent(ACTION_TASK_REACHED_GOAL);
                        intent.setPackage(context.getPackageName());
                        intent.putExtra(EXTRA_TASK_ID, (int) taskId);
                        context.sendBroadcast(intent);
                    }
                }

                FocusSession session = new FocusSession(
                        taskId,
                        type,
                        start,
                        end,
                        planned,
                        actualDurationMinutes,
                        points,
                        status
                );

                db.focusSessionDao().insertSession(session);
            } catch (Exception e) {
                Log.e(TAG, "Error recording session", e);
            }
        });
    }
}
