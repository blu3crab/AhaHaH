package com.adaptivehandyapps.util;
//
// Created by mat on 3/31/2018.
//
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.adaptivehandyapps.ahahah.R;

import java.io.FileNotFoundException;
import java.io.InputStream;

//////////////////////////////////////////////////////////////////////////////////////////
public class DisplayUtils {
    private static final String TAG = "DisplayUtils";

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
    public static String metricsToString(Context context)
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
    public static Bitmap decodeToBitmap(Context context, Uri imageUri ) {
        String imagePath = PrefsUtils.DEFAULT_STRING_NADA;
        Bitmap bitmap = null;
        imagePath = imageUri.toString();
        Log.v(TAG, "decodeToBitmap image URI " + imageUri + "\n" + "imagePath " + imagePath);
        if (imageUri.toString().startsWith("content://com.google.android.apps.photos")) {
            try {
                InputStream is = context.getContentResolver().openInputStream(imageUri);
                if (is != null) {
                    // decode bitmap
                    bitmap = BitmapFactory.decodeStream(is);
                    Log.v(TAG, "decodeToBitmap Photos image path " + imagePath.toString());
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Ooops! decodeToBitmap cannot find " + imageUri.toString());
//                Toast.makeText(context, R.string.sketch_empty_image_path_toast, Toast.LENGTH_SHORT).show();
            }
        }
        else {
            // get image path
            imagePath = getRealPathFromURI(context, imageUri);
            Log.v(TAG, "decodeToBitmap getRealPathFromURI image path " + imagePath);
            // decode bitmap
            bitmap = BitmapFactory.decodeFile(imagePath, null);
        }
        return bitmap;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    public static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////
    public static String getOrientationText(Context context, int orientation) {
        String orientationText = context.getString(R.string.orientation_landscape);
        if (orientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            orientationText = context.getString(R.string.orientation_portrait);
        return orientationText;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
}
