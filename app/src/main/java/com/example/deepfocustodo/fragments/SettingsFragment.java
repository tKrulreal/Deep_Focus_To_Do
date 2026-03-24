package com.example.deepfocustodo.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.activities.BlockedAppsActivity;
import com.example.deepfocustodo.utils.PreferenceHelper;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsFragment extends Fragment {

    private TextInputEditText edtFocusTime;
    private TextInputEditText edtBreakTime;
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
        switchMusic = view.findViewById(R.id.switchMusic);
        btnSaveSettings = view.findViewById(R.id.btnSaveSettings);
        btnOpenBlockedApps = view.findViewById(R.id.btnOpenBlockedApps);

        preferenceHelper = new PreferenceHelper(requireContext());

        loadSavedSettings();

        btnSaveSettings.setOnClickListener(v -> saveSettings());

        btnOpenBlockedApps.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), BlockedAppsActivity.class);
            startActivity(intent);
        });
    }

    private void loadSavedSettings() {
        edtFocusTime.setText(String.valueOf(preferenceHelper.getFocusTime()));
        edtBreakTime.setText(String.valueOf(preferenceHelper.getBreakTime()));
        switchMusic.setChecked(preferenceHelper.isMusicEnabled());
    }

    private void saveSettings() {
        String focusText = edtFocusTime.getText() != null ? edtFocusTime.getText().toString().trim() : "";
        String breakText = edtBreakTime.getText() != null ? edtBreakTime.getText().toString().trim() : "";

        if (!TextUtils.isEmpty(focusText)) {
            preferenceHelper.setFocusTime(Integer.parseInt(focusText));
        }

        if (!TextUtils.isEmpty(breakText)) {
            preferenceHelper.setBreakTime(Integer.parseInt(breakText));
        }

        preferenceHelper.setMusicEnabled(switchMusic.isChecked());
    }
}