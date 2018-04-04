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

	//////////////settings//////////////////////////
	public final static String DEFAULT_STRING_NADA = "nada";
	public final static Float DEFAULT_FLOAT = 0.0f;
	public final static Boolean DEFAULT_BOOLEAN = false;
	///////////////////////////////////////////////////////////////////////////
	public static void setDefaults(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		prefs.edit().putString(ALBUMNAME_KEY, DEFAULT_STRING_NADA);
		return;
	}
	///////////////////////////////////////////////////////////////////////////
	public static String toString(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return TAG + "-->" + "\n" +
				ALBUMNAME_KEY + ": " +
				prefs.getString(ALBUMNAME_KEY, DEFAULT_STRING_NADA)  + "\n";
	}
	///////////////////////////////////////////////////////////////////////////
	// getters & setters
	public static String getPrefs(Context context, String key) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(key, DEFAULT_STRING_NADA);
	}
	public static Float getFloatPrefs(Context context, String key) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getFloat(key, DEFAULT_FLOAT);
	}
	public static Boolean getBooleanPrefs(Context context, String key) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(key, DEFAULT_BOOLEAN);
	}
	public static void setPrefs(Context context, String key, String value) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		prefs.edit().putString(key, value).apply();
		Log.d(TAG,"setPrefs key->" + key + ", value->" + value);
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
