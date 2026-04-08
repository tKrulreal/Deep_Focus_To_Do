package com.example.deepfocustodo.utils;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppExecutors {
    private static final ExecutorService diskIO = Executors.newSingleThreadExecutor();
    private static final Handler mainThread = new Handler(Looper.getMainLooper());

    public static ExecutorService diskIO() {
        return diskIO;
    }

    public static void mainThread(Runnable runnable) {
        mainThread.post(runnable);
    }
}