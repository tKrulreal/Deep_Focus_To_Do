package com.example.deepfocustodo.activities;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
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

public class BlockPermissionActivity extends AppCompatActivity {


    private Button btnUsagePermission;

    private Button btnOverlayPermission;

    private Button btnBlockApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.block_permission);

        btnUsagePermission = findViewById(R.id.btnUsagePermission);
        btnOverlayPermission = findViewById(R.id.btnOverlayPermission);

        btnBlockApp = findViewById(R.id.btnBlockApp);
        btnBlockApp.setOnClickListener(v -> finish());

        btnUsagePermission.setOnClickListener(v -> {
            if (!hasUsageStatsPermission()) {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            } else {
                Toast.makeText(this, "Usage Access đã được cấp", Toast.LENGTH_SHORT).show();
            }
        });

        btnOverlayPermission.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Overlay Permission đã được cấp", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
}
