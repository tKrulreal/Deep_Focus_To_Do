package com.example.deepfocustodo.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.activities.BlockedAppsActivity;
import com.example.deepfocustodo.services.PomodoroService;
import com.example.deepfocustodo.utils.PreferenceHelper;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsFragment extends Fragment implements TabRefreshable {

    private static final int MIN_MINUTES = 1;
    private static final int MAX_MINUTES = 180;

    private TextInputEditText edtFocusTime;
    private TextInputEditText edtBreakTime;
    private TextInputEditText edtLongBreakTime;
    private SwitchCompat switchMusic;
    private Button btnSaveSettings;
    private Button btnOpenBlockedApps;

    private PreferenceHelper preferenceHelper;

    public SettingsFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtFocusTime = view.findViewById(R.id.edtFocusTime);
        edtBreakTime = view.findViewById(R.id.edtBreakTime);
        edtLongBreakTime = view.findViewById(R.id.edtLongBreakTime);
        switchMusic = view.findViewById(R.id.switchMusic);
        btnSaveSettings = view.findViewById(R.id.btnSaveSettings);
        btnOpenBlockedApps = view.findViewById(R.id.btnOpenBlockedApps);

        preferenceHelper = new PreferenceHelper(requireContext());

        loadSavedSettings();
        updateBlockedAppsButton();

        btnSaveSettings.setOnClickListener(v -> saveSettings());

        btnOpenBlockedApps.setOnClickListener(v -> {

            if (isPomodoroRunning()) {

                Toast.makeText(
                        requireContext(),
                        "Pomodoro đang chạy. Không thể thực hiện chặn ứng dụng!",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            Intent intent =
                    new Intent(requireContext(), BlockedAppsActivity.class);

            startActivity(intent);
        });
    }

    @Override
    public void onTabSelected() {
        if (!isAdded() || getView() == null) {
            return;
        }
        loadSavedSettings();
        updateBlockedAppsButton();
    }

    private void loadSavedSettings() {
        edtFocusTime.setText(String.valueOf(preferenceHelper.getFocusTime()));
        edtBreakTime.setText(String.valueOf(preferenceHelper.getBreakTime()));
        edtLongBreakTime.setText(String.valueOf(preferenceHelper.getLongBreakTime()));
        switchMusic.setChecked(preferenceHelper.isMusicEnabled());
    }

    private void saveSettings() {
        String focusText = edtFocusTime.getText() != null ? edtFocusTime.getText().toString().trim() : "";
        String breakText = edtBreakTime.getText() != null ? edtBreakTime.getText().toString().trim() : "";
        String longBreakText = edtLongBreakTime.getText() != null ? edtLongBreakTime.getText().toString().trim() : "";

        if (TextUtils.isEmpty(focusText) || TextUtils.isEmpty(breakText) || TextUtils.isEmpty(longBreakText)) {
            Toast.makeText(requireContext(), "Vui long nhap day du thoi gian", Toast.LENGTH_SHORT).show();
            return;
        }

        int focusMinutes;
        int breakMinutes;
        int longBreakMinutes;

        try {
            focusMinutes = Integer.parseInt(focusText);
            breakMinutes = Integer.parseInt(breakText);
            longBreakMinutes = Integer.parseInt(longBreakText);
        } catch (NumberFormatException exception) {
            Toast.makeText(requireContext(), "Gia tri thoi gian khong hop le", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidMinutes(focusMinutes) || !isValidMinutes(breakMinutes) || !isValidMinutes(longBreakMinutes)) {
            Toast.makeText(requireContext(), "Thoi gian hop le tu 1 den 180 phut", Toast.LENGTH_SHORT).show();
            return;
        }

        preferenceHelper.setFocusTime(focusMinutes);
        preferenceHelper.setBreakTime(breakMinutes);
        preferenceHelper.setLongBreakTime(longBreakMinutes);
        preferenceHelper.setMusicEnabled(switchMusic.isChecked());

        syncPomodoroServiceWithLatestSettings();
        Toast.makeText(requireContext(), "Da luu cai dat", Toast.LENGTH_SHORT).show();
    }

    private boolean isValidMinutes(int minutes) {
        return minutes >= MIN_MINUTES && minutes <= MAX_MINUTES;
    }

    private void syncPomodoroServiceWithLatestSettings() {
        PomodoroService.TimerState state = PomodoroService.readState(requireContext());
        if (!state.isRunning && !state.sessionInProgress) {
            return;
        }

        Intent intent = new Intent(requireContext(), PomodoroService.class);
        intent.setAction(PomodoroService.ACTION_APPLY_SETTINGS);
        requireContext().startService(intent);
    }

    private boolean isPomodoroRunning() {
        PomodoroService.TimerState state =
                PomodoroService.readState(requireContext());

        return state.isRunning && state.sessionInProgress;
    }

    private void updateBlockedAppsButton() {

        boolean running = isPomodoroRunning();

        if (running) {
            btnOpenBlockedApps.setEnabled(false);
            btnOpenBlockedApps.setAlpha(0.4f);   // mờ đi
        } else {
            btnOpenBlockedApps.setEnabled(true);
            btnOpenBlockedApps.setAlpha(1f);
        }
    }
}