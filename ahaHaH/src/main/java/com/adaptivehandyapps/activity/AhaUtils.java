package com.adaptivehandyapps.activity;

import android.app.ActivityManager;
import android.util.Log;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;

public class AhaUtils {
    private static final String TAG = AhaUtils.class.getSimpleName();

    // test if app is in background
    public static boolean isAppInBackground() {
        ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);
        Boolean isBackground = true;
        if (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE) isBackground = false;
        Log.d(TAG, "isAppInBackground " + isBackground);
        return isBackground;
    }

}
