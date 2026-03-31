package com.example.deepfocustodo.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceHelper {

    private static final String PREF_NAME = "focusflow_prefs";
    private static final String KEY_FOCUS_TIME = "focus_time";
    private static final String KEY_BREAK_TIME = "break_time";
    private static final String KEY_LONG_BREAK_TIME = "long_break_time";
    private static final String KEY_MUSIC_ENABLED = "music_enabled";
    private static final String KEY_FOCUS_ACTIVE = "focus_active";

    private final SharedPreferences sharedPreferences;

    public PreferenceHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setFocusTime(int minutes) {
        sharedPreferences.edit().putInt(KEY_FOCUS_TIME, minutes).apply();
    }

    public int getFocusTime() {
        return sharedPreferences.getInt(KEY_FOCUS_TIME, 25);
    }

    public void setBreakTime(int minutes) {
        sharedPreferences.edit().putInt(KEY_BREAK_TIME, minutes).apply();
    }

    public int getBreakTime() {
        return sharedPreferences.getInt(KEY_BREAK_TIME, 5);
    }

    public void setLongBreakTime(int minutes) {
        sharedPreferences.edit().putInt(KEY_LONG_BREAK_TIME, minutes).apply();
    }

    public int getLongBreakTime() {
        return sharedPreferences.getInt(KEY_LONG_BREAK_TIME, 15);
    }

    public void setMusicEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_MUSIC_ENABLED, enabled).apply();
    }

    public boolean isMusicEnabled() {
        return sharedPreferences.getBoolean(KEY_MUSIC_ENABLED, false);
    }

    public void setFocusActive(boolean active) {
        sharedPreferences.edit().putBoolean(KEY_FOCUS_ACTIVE, active).apply();
    }

    public boolean isFocusActive() {
        return sharedPreferences.getBoolean(KEY_FOCUS_ACTIVE, false);
    }
}