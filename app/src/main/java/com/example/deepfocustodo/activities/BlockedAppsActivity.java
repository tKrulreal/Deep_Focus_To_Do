package com.example.deepfocustodo.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deepfocustodo.R;

public class BlockedAppsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_apps);

        btnBack = findViewById(R.id.btnBackBlockedApps);
        tvInfo = findViewById(R.id.tvBlockedAppsInfo);

        tvInfo.setText("Người thứ 4 sẽ làm");
        btnBack.setOnClickListener(v -> finish());
    }
}