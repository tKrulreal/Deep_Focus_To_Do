package com.example.deepfocustodo.models;

import android.graphics.drawable.Drawable;

public class AppItem {
    private String appName;
    private String packageName;
    private Drawable icon;
    private boolean isBlocked;

    public AppItem(String appName, String packageName, Drawable icon, boolean isBlocked) {
        this.appName = appName;
        this.packageName = packageName;
        this.icon = icon;
        this.isBlocked = isBlocked;
    }

    public String getAppName() { return appName; }
    public String getPackageName() { return packageName; }
    public Drawable getIcon() { return icon; }
    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }
}
