package com.example.deepfocustodo.models;

public class FocusPlaylist {

    private final String id;
    private final String title;
    private final String subtitle;
    private final int iconBackgroundRes;
    private final int rawResId;

    public FocusPlaylist(String id, String title, String subtitle, int iconBackgroundRes, int rawResId) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.iconBackgroundRes = iconBackgroundRes;
        this.rawResId = rawResId;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public int getIconBackgroundRes() {
        return iconBackgroundRes;
    }

    public int getRawResId() {
        return rawResId;
    }
}

