package com.example.deepfocustodo.utils;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

public class PermissionHelper {

    public static boolean isAllPermissionsGranted(Context context) {
        return hasUsageStatsPermission(context) && hasOverlayPermission(context);
    }

    public static boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }
}
