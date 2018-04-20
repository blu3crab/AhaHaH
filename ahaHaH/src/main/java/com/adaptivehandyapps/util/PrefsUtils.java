// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class PrefsUtils {
	private static final String TAG = "PrefsUtils";

	////////////////////////////////////////////////////////////////////////////
	// preferences keys
	public static final String ALBUMNAME_KEY = "albumName";
	public static final String IMAGEPATH_KEY = "imagePath";
	public static final String SKETCH_SHAPE_KEY = "sketchShape";
	public static final String SKETCH_TOOL_KEY = "sketchTool";
	public static final String SKETCH_SIZE_KEY = "sketchSize";
	public static final String SKETCH_STYLE_KEY = "sketchStyle";
	public static final String SKETCH_COLOR_KEY = "sketchColor";
	public static final String SKETCH_CUSTOM_COLOR_KEY = "sketchCustomColor";

	//////////////settings//////////////////////////
	public final static String DEFAULT_STRING_NADA = "nada";
	public final static int DEFAULT_INTEGER = 0;
	public final static float DEFAULT_FLOAT = 0.0f;
	public final static boolean DEFAULT_BOOLEAN = false;
	///////////////////////////////////////////////////////////////////////////
	public static void setDefaults(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit().putString(ALBUMNAME_KEY, DEFAULT_STRING_NADA);
		prefs.edit().putString(IMAGEPATH_KEY, DEFAULT_STRING_NADA);

		return;
	}
	///////////////////////////////////////////////////////////////////////////
	public static String toString(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return TAG + "-->" + "\n" +
				ALBUMNAME_KEY + ": " +
				prefs.getString(ALBUMNAME_KEY, DEFAULT_STRING_NADA)  + "\n" +
				IMAGEPATH_KEY + ": " +
				prefs.getString(IMAGEPATH_KEY, DEFAULT_STRING_NADA)  + "\n";
	}
	///////////////////////////////////////////////////////////////////////////
	// getters
	public static String getPrefs(Context context, String key, String defaultValue) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(key, defaultValue);
	}
	public static int getPrefs(Context context, String key, int defaultValue) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//		String v = prefs.getString(key, "nada");
//		int value = prefs.getInt(key, defaultValue);
//		return value;
		return prefs.getInt(key, defaultValue);
	}
	public static float getPrefs(Context context, String key, float defaultValue) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getFloat(key, defaultValue);
	}
	public static boolean getPrefs(Context context, String key, boolean defaultValue) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(key, defaultValue);
	}
//	public static String getPrefs(Context context, String key) {
//		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//		return prefs.getString(key, DEFAULT_STRING_NADA);
//	}
//	public static Integer getIntPrefs(Context context, String key) {
//		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//		return prefs.getInt(key, DEFAULT_INTEGER);
//	}
//	public static Float getFloatPrefs(Context context, String key) {
//		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//		return prefs.getFloat(key, DEFAULT_FLOAT);
//	}
//	public static Boolean getBooleanPrefs(Context context, String key) {
//		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//		return prefs.getBoolean(key, DEFAULT_BOOLEAN);
//	}
	///////////////////////////////////////////////////////////////////////////
	// setters
	public static void setPrefs(Context context, String key, String value) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit().putString(key, value).apply();
		Log.d(TAG,"setPrefs key->" + key + ", value->" + value);
		return;
	}
    public static void setPrefs(Context context, String key, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putInt(key, value).apply();
        return;
    }
    public static void setPrefs(Context context, String key, float value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putFloat(key, value).apply();
        return;
    }
	public static void setPrefs(Context context, String key, Boolean value) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit().putBoolean(key, value).apply();
		return;
	}
	///////////////////////////////////////////////////////////////////////////
}
