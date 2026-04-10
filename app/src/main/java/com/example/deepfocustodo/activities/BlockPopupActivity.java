package com.example.deepfocustodo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.services.BlockerService;

// Màn hình chắn trên app khác
public class BlockPopupActivity extends AppCompatActivity {

    private Button buttonReturn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blocked_screen_popup);

        buttonReturn = findViewById(R.id.buttonReturn);

        // Trở về Homescreen
        buttonReturn.setOnClickListener(v -> {
            BlockerService.isShowing = false;

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            finish();
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BlockerService.isShowing = false;

    }

    @Override
    protected void onPause() {
        super.onPause();

        BlockerService.isShowing = false;

    }

    @Override
    protected void onStop() {
        super.onStop();

        BlockerService.isShowing = false;

        finish();
    }


}

