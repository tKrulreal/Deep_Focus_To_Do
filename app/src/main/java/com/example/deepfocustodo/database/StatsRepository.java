package com.example.deepfocustodo.database;

import android.content.Context;
import com.example.deepfocustodo.models.DailyStats;
import com.example.deepfocustodo.models.FocusSession;
import com.example.deepfocustodo.models.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsRepository {
    private final FocusSessionDao sessionDao;
    private final TaskDao taskDao;

    public StatsRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.sessionDao = db.focusSessionDao();
        this.taskDao = db.taskDao();
    }

    public int getTotalPoints() {
        Integer p = sessionDao.getTotalPoints();
        return p != null ? p : 0;
    }

    public int getTotalFocusMinutes() {
        Integer m = sessionDao.getTotalFocusMinutes();
        return m != null ? m : 0;
    }

    public int getCompletedCount() {
        return sessionDao.getCompletedSessionCount();
    }

    public int getPointsToday() {
        Calendar cal = Calendar.getInstance();
        long start = getStartOfDay(cal.getTimeInMillis());
        long end = getEndOfDay(cal.getTimeInMillis());
        Integer p = sessionDao.getPointsEarnedInDay(start, end);
        return p != null ? p : 0;
    }

    public int getFocusMinutesToday() {
        Calendar cal = Calendar.getInstance();
        long start = getStartOfDay(cal.getTimeInMillis());
        long end = getEndOfDay(cal.getTimeInMillis());
        Integer m = sessionDao.getTotalFocusMinutesInDay(start, end);
        return m != null ? m : 0;
    }

    public int getSessionsCompletedToday() {
        Calendar cal = Calendar.getInstance();
        long start = getStartOfDay(cal.getTimeInMillis());
        long end = getEndOfDay(cal.getTimeInMillis());
        return sessionDao.getCompletedSessionsCountInDay(start, end);
    }

    public Map<String, Integer> getCompletionRatio() {
        Map<String, Integer> ratio = new HashMap<>();
        int completed = sessionDao.getCompletedSessionCount();
        int failed = sessionDao.getTotalFailedSessionCount();
        ratio.put("Completed", completed);
        ratio.put("Failed", failed);
        return ratio;
    }

    public List<DailyStats> getLast7DaysStats() {
        List<DailyStats> statsList = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        // Move to 6 days ago
        cal.add(Calendar.DAY_OF_YEAR, -6);
        
        for (int i = 0; i < 7; i++) {
            long start = getStartOfDay(cal.getTimeInMillis());
            long end = getEndOfDay(cal.getTimeInMillis());
            
            Integer mins = sessionDao.getTotalFocusMinutesInDay(start, end);
            int count = sessionDao.getCompletedSessionsCountInDay(start, end);
            Integer p = sessionDao.getPointsEarnedInDay(start, end);
            
            String label = cal.get(Calendar.DAY_OF_MONTH) + "/" + (cal.get(Calendar.MONTH) + 1);
            statsList.add(new DailyStats(label, mins != null ? mins : 0, count, p != null ? p : 0));
            
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return statsList;
    }

    public String getUserRank(int points) {
        if (points >= 5000) return "Focus Master";
        if (points >= 2000) return "Deep Worker";
        if (points >= 800) return "Productive Student";
        return "Newcomer";
    }

    public Map<String, Integer> getTopTasksWithNames(int limit) {
        List<FocusSessionDao.TaskDuration> data = sessionDao.getTopFocusedTasks(limit);
        Map<String, Integer> result = new HashMap<>();
        if (data != null) {
            for (FocusSessionDao.TaskDuration item : data) {
                String name = "Unknown Task";
                if (item.taskId != null) {
                    Task t = taskDao.getTaskById(item.taskId);
                    if (t != null) name = t.getTitle();
                }
                result.put(name, item.totalDuration);
            }
        }
        return result;
    }

    private long getStartOfDay(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long getEndOfDay(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }
}