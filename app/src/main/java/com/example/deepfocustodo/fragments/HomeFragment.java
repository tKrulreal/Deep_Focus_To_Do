package com.example.deepfocustodo.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.activities.HistoryActivity;

public class HomeFragment extends Fragment {

    private Button btnStart, btnPause, btnReset, btnViewHistory;
    private TextView tvTimer, tvMode;
    private RecyclerView recyclerTasks;

    private CountDownTimer timer;
    private final long focusTime = 25 * 60 * 1000;
    private final long breakTime = 5 * 60 * 1000;
    private long timeLeft = focusTime;

    private boolean isRunning = false;
    private boolean isFocus = true;

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ View
        btnStart = view.findViewById(R.id.btnStart);
        btnPause = view.findViewById(R.id.btnPause);
        btnReset = view.findViewById(R.id.btnReset);
        btnViewHistory = view.findViewById(R.id.btnViewHistory);
        tvTimer = view.findViewById(R.id.tvTimer);
        tvMode = view.findViewById(R.id.tvMode);
        recyclerTasks = view.findViewById(R.id.recyclerTasks);

        // Khôi phục trạng thái khi xoay màn hình
        if (savedInstanceState != null) {
            timeLeft = savedInstanceState.getLong("timeLeft");
            isFocus = savedInstanceState.getBoolean("isFocus");
            isRunning = savedInstanceState.getBoolean("isRunning");
            if (isRunning) {
                startTimer();
            }
        }

        if (recyclerTasks != null) {
            recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
            // Ở đây bạn có thể gán adapter cho recyclerTasks sau khi load dữ liệu từ DB
        }

        updateTimerText();
        updateModeText();
        updateButtonStates();

        btnStart.setOnClickListener(v -> {
            startTimer();
            updateButtonStates();
        });

        btnPause.setOnClickListener(v -> {
            if (isRunning) pauseTimer();
            else resumeTimer();
            updateButtonStates();
        });

        btnReset.setOnClickListener(v -> {
            resetTimer();
            updateButtonStates();
        });

        if (btnViewHistory != null) {
            btnViewHistory.setOnClickListener(v -> startActivity(new Intent(requireContext(), HistoryActivity.class)));
        }
    }

    private void startTimer() {
        if (timer != null) timer.cancel();
        timer = new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                isRunning = false;
                if (isFocus) {
                    Toast.makeText(requireContext(), "Hết giờ tập trung!", Toast.LENGTH_SHORT).show();
                    startBreak();
                } else {
                    Toast.makeText(requireContext(), "Hết giờ nghỉ!", Toast.LENGTH_SHORT).show();
                    startFocus();
                }
                updateButtonStates();
            }
        }.start();
        isRunning = true;
    }

    private void pauseTimer() {
        if (timer != null) timer.cancel();
        isRunning = false;
    }

    private void resumeTimer() {
        startTimer();
    }

    private void resetTimer() {
        if (timer != null) timer.cancel();
        isFocus = true;
        timeLeft = focusTime;
        isRunning = false;
        updateTimerText();
        updateModeText();
    }

    private void startFocus() {
        isFocus = true;
        timeLeft = focusTime;
        updateModeText();
        startTimer();
    }

    private void startBreak() {
        isFocus = false;
        timeLeft = breakTime;
        updateModeText();
        startTimer();
    }

    private void updateTimerText() {
        int minutes = (int) (timeLeft / 1000) / 60;
        int seconds = (int) (timeLeft / 1000) % 60;
        tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void updateModeText() {
        if (tvMode != null) {
            tvMode.setText(isFocus ? "TẬP TRUNG" : "NGHỈ NGƠI");
            tvMode.setTextColor(isFocus ? getResources().getColor(android.R.color.holo_red_light) : getResources().getColor(android.R.color.holo_green_light));
        }
    }

    private void updateButtonStates() {
        btnStart.setEnabled(!isRunning && timeLeft == (isFocus ? focusTime : breakTime));
        btnPause.setEnabled(isRunning || (timeLeft < (isFocus ? focusTime : breakTime) && timeLeft > 0));
        btnPause.setText(isRunning ? "Tạm dừng" : "Tiếp tục");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("timeLeft", timeLeft);
        outState.putBoolean("isFocus", isFocus);
        outState.putBoolean("isRunning", isRunning);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timer != null) timer.cancel();
    }
}
