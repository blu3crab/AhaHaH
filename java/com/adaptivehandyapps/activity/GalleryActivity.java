// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.adaptivehandyapps.gallery.*;

public class GalleryActivity extends FragmentActivity 
	implements GalleryGridFragment.OnImageSelectedListener {
	
	private static final String TAG = "GalleryActivity";

	public static final int REQUEST_CODE_SELECT_BACKDROP = 2;
	public static final int REQUEST_CODE_SELECT_OVERLAY = 3;
	public static final String EXTRA_REQUEST_CODE = "reqCode";
	public static final String EXTRA_SELECT_ID = "selectId";

    public void onImageSelected(int position) {
    	this.getIntent().putExtra(EXTRA_SELECT_ID, position);
		// set result code & result data
		Log.v(TAG, "getParent " + getParent());
		if (getParent() == null) {
		    setResult(RESULT_OK, this.getIntent());
		} else {
		    getParent().setResult(RESULT_OK, this.getIntent());
		}
    	Log.v(TAG, "Finish activity.");
    	this.finish();
    }
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_gallery);
		
		// set current item based on extra passed in to this activity
		int reqCode = getIntent().getIntExtra(EXTRA_REQUEST_CODE, -1);
		if (reqCode != -1) {
			Log.v(TAG, "reqCode = " + reqCode);
		}

		if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			GalleryGridFragment ggf = new GalleryGridFragment();
		    Bundle args = new Bundle();
		    args.putInt(EXTRA_REQUEST_CODE, reqCode);
		    ggf.setArguments(args);
//			ft.add(android.R.id.content, new GalleryGridFragment(), TAG);
			ft.add(android.R.id.content, ggf, TAG);
			ft.commit();
		}	
	}
}
