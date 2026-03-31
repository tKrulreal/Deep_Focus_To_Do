package com.example.deepfocustodo.activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.services.BlockerService;

// Màn hình chắn trên app khác
public class BlockPopupActivity extends AppCompatActivity {

    private Button buttonReturn;

    private MediaPlayer mediaPlayer;

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

        mediaPlayer = MediaPlayer.create(this, R.raw.stop_right_there);
        mediaPlayer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BlockerService.isShowing = false;

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        BlockerService.isShowing = false;

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        BlockerService.isShowing = false;

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        finish();
    }


}

