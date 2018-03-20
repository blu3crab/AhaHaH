// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.util;

import android.content.SharedPreferences;

import com.adaptivehandyapps.activity.AhaHahActivity;

public class AhaSettings {
	private static final String TAG = "AhaSettings"; 
	
	private AhaHahActivity mParentActivity;

	//////////////settings//////////////////////////
	public final static String NADA = "nada";
	private String mAlbumName = NADA;

	/////////////save/restore settings////////////////
	private int MODE_PRIVATE = 0;
	public static final String PREFS_NAME = "AhaSettingsFile";
	private String mKeyAlbum = "Album";

	///////////////////////////////////////////////////////////////////////////////
    // constructor
	public AhaSettings() {
		mParentActivity = AhaHahActivity.mParentActivity;
	}
	/////////////getters/setters////////////////
	public String getAlbumName() { return mAlbumName; }
	
	public boolean setAlbumName(String name) 
	{ 
		mAlbumName = name;
		return true; 
	}

	///////////////////////////////////////////////////////////////////////////////
	// save sketch settings
	public void saveSettings() {
		SharedPreferences settings = mParentActivity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
	    SharedPreferences.Editor editor = settings.edit();
//	    editor.remove(PREFS_NAME);

	    editor.putString(mKeyAlbum, mAlbumName);

	    // commit the edits!
	    editor.commit();
		
	}
	// restore sketch settings
	public void restoreSettings() {
		SharedPreferences settings = mParentActivity.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		mAlbumName = settings.getString(mKeyAlbum, NADA);
	}
}
