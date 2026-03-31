package com.example.deepfocustodo.activities;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.utils.PermissionHelper;

public class BlockPermissionActivity extends AppCompatActivity {


    private TextView tvUsageStatus, tvOverlayStatus;
    private Button btnUsagePermission, btnOverlayPermission, btnBlockApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.block_permission);

        // Ánh xạ View
        tvUsageStatus = findViewById(R.id.tvUsageStatus);
        tvOverlayStatus = findViewById(R.id.tvOverlayStatus);
        btnUsagePermission = findViewById(R.id.btnUsagePermission);
        btnOverlayPermission = findViewById(R.id.btnOverlayPermission);

        btnBlockApp = findViewById(R.id.btnBlockApp);
        btnBlockApp.setOnClickListener(v -> finish());



        // Sự kiện nút Bước 1
        btnUsagePermission.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        });

        // Sự kiện nút Bước 2
        btnOverlayPermission.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Thiết bị không cần cấp quyền này", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra lại mỗi khi người dùng quay lại từ màn hình Cài đặt
        updateUI();
    }

    private void updateUI() {
        // Kiểm tra quyền 1
        if (PermissionHelper.hasUsageStatsPermission(this)) {
            tvUsageStatus.setText("Trạng thái: Đã cấp");
            tvUsageStatus.setTextColor(Color.parseColor("#4CAF50")); // Màu xanh
        } else {
            tvUsageStatus.setText("Trạng thái: Chưa cấp");
            tvUsageStatus.setTextColor(Color.parseColor("#F44336")); // Màu đỏ
        }

        // Kiểm tra quyền 2
        if (PermissionHelper.hasOverlayPermission(this)) {
            tvOverlayStatus.setText("Trạng thái: Đã cấp");
            tvOverlayStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            tvOverlayStatus.setText("Trạng thái: Chưa cấp");
            tvOverlayStatus.setTextColor(Color.parseColor("#F44336"));
        }
    }

}
