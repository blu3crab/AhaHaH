package com.adaptivehandyapps.util;
//
// Created by mat on 3/31/2018.
//
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

//////////////////////////////////////////////////////////////////////////////////////////
public class AhaDisplayMetrics {
    private static final String TAG = "AhaDisplayMetrics";

    //////////////////////////////////////////////////////////////////////////////////////////
    // getters
    public static int getDisplayWidth(Context context) {
        DisplayMetrics displayMetrics = getDisplayMetrics(context);
        return displayMetrics.widthPixels;
    }
    public static int getDisplayHeight(Context context) {
        DisplayMetrics displayMetrics = getDisplayMetrics(context);
        return displayMetrics.heightPixels;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    public static DisplayMetrics getDisplayMetrics(Context context) {
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(displayMetrics);
//        Log.v(TAG, "display metrics W/H: " + displayMetrics.widthPixels + "/" + displayMetrics.heightPixels);
        return displayMetrics;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    // return string of key display metrics
    public static String toString(Context context)
    {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        float xdpi = metrics.xdpi;
        float ydpi = metrics.ydpi;
        float density = metrics.density;
        int densityDpi = metrics.densityDpi;

        String toastText = "Display: " +
                width + " x " + height + ", " + xdpi + " x " + ydpi + ", density(dpi) " + density + "(" + densityDpi + ")";
		Log.d(TAG, toastText);
        return toastText;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
}
