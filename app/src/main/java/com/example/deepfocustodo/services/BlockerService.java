package com.example.deepfocustodo.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.activities.BlockPopupActivity;
import com.example.deepfocustodo.utils.PreferenceHelper;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class BlockerService extends Service {

    private static final String PREFS_NAME = "BlockedAppsPrefs";
    private static final String CHANNEL_ID = "blocker_service_channel";
    private static final int FOREGROUND_ID = 3001;
    private static final String KEY_BLOCKED_ATTEMPTS = "blocked_attempt_count";
    private static final long CHECK_INTERVAL_MS = 1000L;

    private Handler handler;
    private Runnable checkerRunnable;

    private SharedPreferences sharedPreferences;
    private PreferenceHelper preferenceHelper;

    public static boolean isShowing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferenceHelper = new PreferenceHelper(this);
        handler = new Handler(Looper.getMainLooper());

        createNotificationChannel();
        startForeground(FOREGROUND_ID, buildServiceNotification());

        checkerRunnable = new Runnable() {
            @Override
            public void run() {
                checkBlockedApp();
                handler.postDelayed(this, CHECK_INTERVAL_MS);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.removeCallbacks(checkerRunnable);
        handler.post(checkerRunnable);

        return START_STICKY;
    }

    private void checkBlockedApp() {
        if (!preferenceHelper.isFocusActive()) {
            return;
        }

        UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        if (usm == null) {
            return;
        }

        long time = System.currentTimeMillis();

        List<UsageStats> stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 5,
                time
        );

        if (stats == null || stats.isEmpty()) {
            return;
        }

        SortedMap<Long, UsageStats> sortedMap = new TreeMap<>();

        for (UsageStats usageStats : stats) {
            if (usageStats != null) {
                sortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
        }

        if (!sortedMap.isEmpty()) {
            UsageStats latest = sortedMap.get(sortedMap.lastKey());
            if (latest == null || latest.getPackageName() == null) {
                return;
            }
            String currentApp = latest.getPackageName();

            if (currentApp.equals(getPackageName())) {
                return;
            }

            boolean isBlocked = sharedPreferences.getBoolean(currentApp, false);

            if (isBlocked && !isShowing) {
                isShowing = true;
                incrementBlockedAttemptCount();
                showBlockScreen();
            }
        }
    }

    private void incrementBlockedAttemptCount() {
        int current = sharedPreferences.getInt(KEY_BLOCKED_ATTEMPTS, 0);
        sharedPreferences.edit().putInt(KEY_BLOCKED_ATTEMPTS, current + 1).apply();
    }

    private void showBlockScreen() {
        Intent intent = new Intent(this, BlockPopupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private NotificationCompat.Builder baseNotificationBuilder() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("DeepFocus dang hoat dong")
                .setContentText("Dang theo doi app xao nhang trong focus mode")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
    }

    private android.app.Notification buildServiceNotification() {
        return baseNotificationBuilder().build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Blocker Service",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        if (handler != null && checkerRunnable != null) {
            handler.removeCallbacks(checkerRunnable);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
