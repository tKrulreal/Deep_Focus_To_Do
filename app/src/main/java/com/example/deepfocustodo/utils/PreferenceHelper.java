package com.example.deepfocustodo.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceHelper {

    private static final String PREF_NAME = "focusflow_prefs";
    private static final String KEY_FOCUS_TIME = "focus_time";
    private static final String KEY_BREAK_TIME = "break_time";
    private static final String KEY_LONG_BREAK_TIME = "long_break_time";
    private static final String KEY_MUSIC_ENABLED = "music_enabled";
    private static final String KEY_SELECTED_PLAYLIST_ID = "selected_playlist_id";
    private static final String KEY_FOCUS_ACTIVE = "focus_active";
    private static final String KEY_SELECTED_TASK_ID = "selected_task_id";
    
    // Session Persistence
    private static final String KEY_SESSION_START_TIME = "session_start_time";
    private static final String KEY_SESSION_TYPE = "session_type";
    private static final String KEY_SESSION_PLANNED_DURATION = "session_planned_duration";

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

    public void setSelectedPlaylistId(String playlistId) {
        sharedPreferences.edit().putString(KEY_SELECTED_PLAYLIST_ID, playlistId).apply();
    }

    public String getSelectedPlaylistId() {
        return sharedPreferences.getString(KEY_SELECTED_PLAYLIST_ID, "lofi_beats");
    }

    public void setFocusActive(boolean active) {
        sharedPreferences.edit().putBoolean(KEY_FOCUS_ACTIVE, active).apply();
    }

    public boolean isFocusActive() {
        return sharedPreferences.getBoolean(KEY_FOCUS_ACTIVE, false);
    }

    public void setSelectedTaskId(Integer taskId) {
        if (taskId == null) {
            sharedPreferences.edit().remove(KEY_SELECTED_TASK_ID).apply();
        } else {
            sharedPreferences.edit().putInt(KEY_SELECTED_TASK_ID, taskId).apply();
        }
    }

    public Integer getSelectedTaskId() {
        if (!sharedPreferences.contains(KEY_SELECTED_TASK_ID)) {
            return null;
        }
        return sharedPreferences.getInt(KEY_SELECTED_TASK_ID, -1);
    }
    
    public void saveSessionState(long startTime, String type, int plannedDuration) {
        sharedPreferences.edit()
                .putLong(KEY_SESSION_START_TIME, startTime)
                .putString(KEY_SESSION_TYPE, type)
                .putInt(KEY_SESSION_PLANNED_DURATION, plannedDuration)
                .apply();
    }
    
    public long getSessionStartTime() { return sharedPreferences.getLong(KEY_SESSION_START_TIME, 0); }
    public String getSessionType() { return sharedPreferences.getString(KEY_SESSION_TYPE, "FOCUS"); }
    public int getSessionPlannedDuration() { return sharedPreferences.getInt(KEY_SESSION_PLANNED_DURATION, 25); }
    
    public void clearSessionState() {
        sharedPreferences.edit()
                .remove(KEY_SESSION_START_TIME)
                .remove(KEY_SESSION_TYPE)
                .remove(KEY_SESSION_PLANNED_DURATION)
                .apply();
    }
}