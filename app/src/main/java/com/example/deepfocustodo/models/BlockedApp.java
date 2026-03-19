package com.example.deepfocustodo.models;

public class BlockedApp {

    private int id;
    private String appName;
    private String packageName;

    public BlockedApp() {
    }

    public BlockedApp(int id, String appName, String packageName) {
        this.id = id;
        this.appName = appName;
        this.packageName = packageName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}