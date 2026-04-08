package com.example.deepfocustodo.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.adapters.AppListAdapter;
import com.example.deepfocustodo.models.AppItem;
import com.example.deepfocustodo.utils.PermissionHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockedAppsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private Button btnBlockPermission;
    private RecyclerView rvAppsList;
    private AppListAdapter adapter;

    private TextView tvBlockedAppsInfo;

    // Nơi lưu trữ trạng thái các app bị chặn
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "BlockedAppsPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_apps);

        btnBack = findViewById(R.id.btnBackBlockedApps);
        btnBlockPermission = findViewById(R.id.btnBlockPermission);
        rvAppsList = findViewById(R.id.rvAppsList);
        tvBlockedAppsInfo = findViewById(R.id.tvBlockedAppsInfo);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Nút Quay lại
        btnBack.setOnClickListener(v -> finish());

        // Nút chuyển đến màn hình cấp quyền
        btnBlockPermission.setOnClickListener(v -> {
            Intent intent = new Intent(BlockedAppsActivity.this, BlockPermissionActivity.class);
            startActivity(intent);
        });

        setupRecyclerView();
        loadInstalledApps();

    }

    private void setupRecyclerView() {
        rvAppsList.setLayoutManager(new LinearLayoutManager(this));

        // Khi người dùng gạt Switch, lưu vào SharedPreferences
        adapter = new AppListAdapter((packageName, isBlocked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(packageName, isBlocked);
            editor.apply();
        });

        rvAppsList.setAdapter(adapter);
    }

    private void loadInstalledApps() {
        // Sử dụng Background Thread (Executor) để tránh làm lag màn hình UI khi tải icon app
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            PackageManager pm = getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(intent, 0);
            List<AppItem> installedApps = new ArrayList<>();

            for (ResolveInfo resolveInfo : resolveInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;

                // Bỏ qua chính ứng dụng của bạn
                if (packageName.equals(getPackageName())) {
                    continue;
                }

                String appName = resolveInfo.loadLabel(pm).toString();
                Drawable icon = resolveInfo.loadIcon(pm);

                // Lấy trạng thái đã bị block từ trước đó
                boolean isBlocked = sharedPreferences.getBoolean(packageName, false);

                installedApps.add(new AppItem(appName, packageName, icon, isBlocked));
            }

            // SẮP XẾP: App bị Block (Switch ON) lên trước, sau đó sắp xếp theo A-Z
            Collections.sort(installedApps, (app1, app2) -> {
                if (app1.isBlocked() != app2.isBlocked()) {
                    return app1.isBlocked() ? -1 : 1;
                }
                return app1.getAppName().compareToIgnoreCase(app2.getAppName());
            });

            // Đưa dữ liệu lên UI Thread để cập nhật giao diện
            runOnUiThread(() -> adapter.setAppList(installedApps));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndRefreshStatus();
    }

    private void checkAndRefreshStatus() {
        if (PermissionHelper.isAllPermissionsGranted(this)) {
            // Khi ĐÃ có đủ quyền
            tvBlockedAppsInfo.setText("✅ Ứng dụng đã sẵn sàng hoạt động");
            tvBlockedAppsInfo.setTextColor(Color.parseColor("#4CAF50")); // Màu xanh lá
        } else {
            // Khi CHƯA có đủ quyền
            tvBlockedAppsInfo.setText("⚠️ Cần cấp quyền để bắt đầu chặn ứng dụng");
            tvBlockedAppsInfo.setTextColor(Color.parseColor("#F44336")); // Màu đỏ
        }
    }
}