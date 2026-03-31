package com.example.deepfocustodo.fragments;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.database.AppDatabase;
import com.example.deepfocustodo.models.DailyStats;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatisticsFragment extends Fragment implements TabRefreshable {

    private static final String BLOCK_PREFS = "BlockedAppsPrefs";
    private static final String KEY_BLOCKED_ATTEMPTS = "blocked_attempt_count";

    private MaterialButtonToggleGroup toggleGroup;
    private LinearLayout layoutNavigation;
    private TextView tvTotalFocusMinutes;
    private TextView tvCompletedSessions;
    private TextView tvTotalPoints;
    private TextView tvBadge;
    private TextView tvStreak;
    private TextView tvTodayUsage;
    private TextView tvBlockedAttempts;
    private TextView tvDailyChart;


    private AppDatabase db;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public StatisticsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toggleGroup = view.findViewById(R.id.toggleGroup);

        layoutNavigation = view.findViewById(R.id.layoutNavigation);

        toggleGroup.addOnButtonCheckedListener(
                (group, checkedId, isChecked) -> {

                    if (!isChecked) return;

                    if (checkedId == R.id.btnDay) {
                        layoutNavigation.setVisibility(View.VISIBLE);
                    }
                    else if (checkedId == R.id.btnTotal) {
                        layoutNavigation.setVisibility(View.GONE);
                    }
                });

        tvTotalFocusMinutes = view.findViewById(R.id.tvTotalFocusMinutes);
        tvCompletedSessions = view.findViewById(R.id.tvCompletedSessions);
        tvTotalPoints = view.findViewById(R.id.tvTotalPoints);
        tvBadge = view.findViewById(R.id.tvBadge);
        tvStreak = view.findViewById(R.id.tvStreak);
        tvTodayUsage = view.findViewById(R.id.tvTodayUsage);
        tvBlockedAttempts = view.findViewById(R.id.tvBlockedAttempts);
        tvDailyChart = view.findViewById(R.id.tvDailyChart);

        db = AppDatabase.getInstance(requireContext());
        loadStatistics();
    }

    @Override
    public void onResume() {
        super.onResume();
        onTabSelected();
    }

    @Override
    public void onTabSelected() {
        if (!isAdded() || getView() == null) {
            return;
        }
        loadStatistics();
    }

    private void loadStatistics() {
        executorService.execute(() -> {
            Integer totalFocus = db.focusSessionDao().getTotalFocusMinutes();
            Integer totalPoints = db.focusSessionDao().getTotalPoints();
            int completedSessions = db.focusSessionDao().getCompletedSessionCount();
            List<Long> completedStartTimes = db.focusSessionDao().getCompletedSessionStartTimes();
            List<DailyStats> recentDailyStats = db.focusSessionDao().getRecentDailyStats(7);

            int streakDays = computeStreakDays(completedStartTimes);
            int usageMinutes = getTodayExternalUsageMinutes();
            int blockedAttempts = getBlockedAttempts();
            int safeTotalPoints = totalPoints == null ? 0 : totalPoints;
            String badge = buildBadgeText(streakDays, safeTotalPoints);
            String chart = buildDailyChart(recentDailyStats);

            if (!isAdded()) {
                return;
            }

            requireActivity().runOnUiThread(() -> {
                tvTotalFocusMinutes.setText((totalFocus == null ? 0 : totalFocus) + " phút");
                tvCompletedSessions.setText(completedSessions + " phiên");
                tvTotalPoints.setText(safeTotalPoints + " điểm");
                tvBadge.setText(badge);
                tvStreak.setText(streakDays + " ngày liên tiếp");
                tvTodayUsage.setText(usageMinutes + " phút");
                tvBlockedAttempts.setText(blockedAttempts + " lần");
                tvDailyChart.setText(chart);
            });
        });
    }

    private int getBlockedAttempts() {
        SharedPreferences prefs = requireContext().getSharedPreferences(BLOCK_PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_BLOCKED_ATTEMPTS, 0);
    }

    private int computeStreakDays(List<Long> startTimes) {
        if (startTimes == null || startTimes.isEmpty()) {
            return 0;
        }

        Set<Long> activeDays = new HashSet<>();
        Calendar calendar = Calendar.getInstance();
        for (Long startTime : startTimes) {
            if (startTime == null) {
                continue;
            }
            calendar.setTimeInMillis(startTime);
            normalizeToStartOfDay(calendar);
            activeDays.add(calendar.getTimeInMillis());
        }

        int streak = 0;
        Calendar cursor = Calendar.getInstance();
        normalizeToStartOfDay(cursor);
        while (activeDays.contains(cursor.getTimeInMillis())) {
            streak++;
            cursor.add(Calendar.DAY_OF_YEAR, -1);
        }
        return streak;
    }

    private int getTodayExternalUsageMinutes() {
        UsageStatsManager usageStatsManager =
                (UsageStatsManager) requireContext().getSystemService(Context.USAGE_STATS_SERVICE);
        if (usageStatsManager == null) {
            return 0;
        }

        Calendar start = Calendar.getInstance();
        normalizeToStartOfDay(start);
        long now = System.currentTimeMillis();

        List<UsageStats> usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                start.getTimeInMillis(),
                now
        );

        long totalMs = 0L;
        if (usageStats != null) {
            for (UsageStats stats : usageStats) {
                if (stats == null) {
                    continue;
                }
                if (requireContext().getPackageName().equals(stats.getPackageName())) {
                    continue;
                }
                totalMs += stats.getTotalTimeInForeground();
            }
        }
        return (int) (totalMs / (1000L * 60L));
    }

    private String buildBadgeText(int streakDays, int totalPoints) {
        if (streakDays >= 7 && totalPoints >= 2000) {
            return "Deep Focus Master";
        }
        if (streakDays >= 3 && totalPoints >= 800) {
            return "On Fire";
        }
        if (totalPoints >= 300) {
            return "Getting Started";
        }
        return "Newcomer";
    }

    private String buildDailyChart(List<DailyStats> stats) {
        if (stats == null || stats.isEmpty()) {
            return "Bieu do 7 ngay\nChua co du lieu";
        }

        Collections.reverse(stats);
        int maxMinutes = 1;
        for (DailyStats item : stats) {
            maxMinutes = Math.max(maxMinutes, item.getTotalMinutes());
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Bieu do 7 ngay (phut)\n");
        for (DailyStats item : stats) {
            int barLength = Math.max(1, Math.round((item.getTotalMinutes() * 16f) / maxMinutes));
            builder.append(String.format(
                    Locale.getDefault(),
                    "%s | %-16s %3d m (%d phien)\n",
                    item.getDateLabel(),
                    repeat("#", barLength),
                    item.getTotalMinutes(),
                    item.getSessionCount()
            ));
        }

        return builder.toString().trim();
    }

    private String repeat(String token, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(token);
        }
        return builder.toString();
    }

    private void normalizeToStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}