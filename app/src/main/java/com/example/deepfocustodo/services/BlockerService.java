package com.example.deepfocustodo.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.example.deepfocustodo.activities.BlockPopupActivity;
import com.example.deepfocustodo.activities.BlockedAppsActivity;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class BlockerService extends Service {

    // Handler dùng để chạy lặp lại việc kiểm tra app mỗi 1 giây
    private Handler handler = new Handler();

    // Lưu danh sách app bị chặn (SharedPreferences)
    private SharedPreferences sharedPreferences;

    public static boolean isShowing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("BlockedAppsPrefs", MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Bắt đầu vòng lặp kiểm tra app đang mở
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkBlockedApp();
                handler.postDelayed(this, 1000);
            }
        }, 1000);

        return START_STICKY;
    }

    private void checkBlockedApp() {
        // Kiểm tra pomodoro có đang chạy không ( sẽ làm sau)
//        if (false) {
//            return;
//        }

        // Lấy service để kiểm tra app đang sử dụng
        UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();

        // Lấy danh sách app được dùng trong 5 giây gần nhất

        List<UsageStats> stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 1000 * 5,
                time
        );

        if (stats == null || stats.isEmpty()) return;

        // Sắp xếp theo thời gian sử dụng gần nhất
        SortedMap<Long, UsageStats> sortedMap = new TreeMap<>();

        for (UsageStats usageStats : stats) {
            sortedMap.put(usageStats.getLastTimeUsed(), usageStats);
        }

        // Nếu có dữ liệu
        if (!sortedMap.isEmpty()) {

            // Lấy app được dùng gần nhất (app đang mở)
            String currentApp = sortedMap.get(sortedMap.lastKey()).getPackageName();

            // Bỏ qua chính app của bạn
            if (currentApp.equals(getPackageName())) return;

            // Kiểm tra xem app này có nằm trong danh sách bị block không
            boolean isBlocked = sharedPreferences.getBoolean(currentApp, false);

            // Nếu là app bị block và chưa hiển thị popup
            if (isBlocked && !isShowing) {

                // Đánh dấu là đang hiển thị popup
                isShowing = true;

                // Hiển thị màn hình chặn
                showBlockScreen();
            }
        }
    }

    private void showBlockScreen() {
        Intent intent = new Intent(this, BlockPopupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
