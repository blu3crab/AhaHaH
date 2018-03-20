// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
//
// CameraActivity class
//
// Purpose: 
//	- initiate camera intent
//	- capture resulting image
//  - write right sized image and thumb to gallery
///////////////////////////////////////////////////////////////////////////////
package com.adaptivehandyapps.activity;

import java.io.File;
import java.io.IOException;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.adaptivehandyapps.ahahah.R;
import com.adaptivehandyapps.util.ImageAlbumStorage;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class CameraActivity extends Activity {
	// class members
	private final String TAG = "CameraActivity";
	
	private Activity mParentActivity;

	private static final int ACTION_TAKE_PHOTO = 2;
	
	private Intent mIntentTakePhoto;

	private int mTargetDeviceW = 0;   
	private int mTargetDeviceH = 0;

	private ImageView mPhotoView;
	private ImageView mThumbView;
	
	// use parent instance of mImageAlbumStorage
	private ImageAlbumStorage mImageAlbumStorage = null;
	private String mImageName;
	private String mCurrentPhotoPath;
	
	///////////////////////////////////////////////////////////////////////////////
	// initiate camera activity
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mParentActivity = AhaHahActivity.mParentActivity;
		
		// get device dimensions
		DisplayMetrics displayMetrics = getDisplayMetrics();
		mTargetDeviceW = displayMetrics.widthPixels;   
		mTargetDeviceH = displayMetrics.heightPixels;
		Log.v(TAG, "target device W/H: " + mTargetDeviceW + "/" + mTargetDeviceH);

		mPhotoView = (ImageView) mParentActivity.findViewById(R.id.imageViewSplash);
		int photoViewW = mPhotoView.getWidth();   
		int photoViewH = mPhotoView.getHeight();
		Log.v(TAG, "imageViewSplash W/H: " + photoViewW + "/" + photoViewH);

		mThumbView = (ImageView) mParentActivity.findViewById(R.id.imageViewThumb);
		int thumbViewW = mThumbView.getWidth();   
		int thumbViewH = mThumbView.getHeight();
		Log.v(TAG, "imageViewThumb W/H: " + thumbViewW + "/" + thumbViewH);
		
		// use parent activity to access mImageAlbumStorage
		mImageAlbumStorage = ((AhaHahActivity)mParentActivity).getImageAlbumStorage();

		if (isIntentAvailable(this, MediaStore.ACTION_IMAGE_CAPTURE)) {
			// camera is available, launch camera intent
			dispatchTakePhotoIntent();
			Log.v(TAG, "camera available...");
		} else {
			Button btnCamera = (Button) mParentActivity.findViewById(R.id.buttonCamera);
			btnCamera.setText(getText(R.string.no).toString() + btnCamera.getText());
			btnCamera.setEnabled(false);
			Log.v(TAG, "camera NOT available...");
		}

	}
	// establish if camera intent is available
	public static boolean isIntentAvailable (Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list =
				packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}
	///////////////////////////////////////////////////////////////////////////////
	private int getTargetWidth() { return mTargetDeviceW; }
	private int getTargetHeight() { return mTargetDeviceH; }
	///////////////////////////////////////////////////////////////////////////////
	// dispatch take photo intent
	private void dispatchTakePhotoIntent () {
		mIntentTakePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		
		File f = null;
		
		try {
			// timestamp an image name & create the file
			mImageName = ImageAlbumStorage.timestampImageName();
			Log.v(TAG, "timestampImageName: "+ mImageName);
			f = mImageAlbumStorage.createImageFile(ImageAlbumStorage.IMG_DIR_FULL, mImageName);
            if (f == null) return;
			mCurrentPhotoPath = f.getAbsolutePath();
			Log.v(TAG, "createImageFile: "+ mCurrentPhotoPath);
			// put file handle in take photo intent extras
			mIntentTakePhoto.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
			
		} catch (IOException e) {
			e.printStackTrace();
			f = null;
			mCurrentPhotoPath = null;
		}
		// start camera for result of photo
		startActivityForResult(mIntentTakePhoto, ACTION_TAKE_PHOTO);
	}
	///////////////////////////////////////////////////////////////////////////////
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	// take photo result - capture resulting image
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK ) {
			// unless orientation fixed, photo orientation on return to parent activity incorrect in 3 of 4 orientation
			addPhotoToAlbum();
			// indicate album has been updated 
			mImageAlbumStorage.refreshImageLists(true);
		} else {
			Log.v(TAG, "onActivityResult: intent " + intent + ", result code " + resultCode);
		}
		try {
			// finish actual camera activity & this camera activity
			this.finishActivity(requestCode);
			this.finish();
		} catch (Throwable e) {
			Log.v(TAG, "onActivityResult: finalize exception! " + e.getMessage());
			e.printStackTrace();
		}
	}
	// add FULL photo, setup views, add FIT & THUMB bitmaps to media DBs
	private void addPhotoToAlbum() {
		Log.v(TAG,"addPhotoToAlbum...");
		if (mCurrentPhotoPath != null) {
			// add FULL image
			mImageAlbumStorage.addToMediaDB(this, mCurrentPhotoPath);
			// setup view & add FIT & THUMB bitmaps to media DB
			setupView();
			// clear path indicating completion
			mCurrentPhotoPath = null;
		}
	}
	// setup view & write FIT & THUMB bitmaps 
	private void setupView() {
		int scaleFactor = 1;
		// get size of photo
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		//////////////////////////FIT////////////////////////
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;
		Log.v(TAG, "setupView: photo W/H: " + photoW + "/" + photoH);
		// round up since int math truncates remainder resulting scale factor 1 (FIT = FULL)
		scaleFactor = Math.min(photoW/getTargetWidth (), photoH/getTargetHeight())+1;
		Log.v(TAG, "setupView: FIT scale factor: " + scaleFactor);
		// set bitmap options to scale the image decode target
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;
		
		// decode the JPEG into the bitmap
		Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		// associate bitmap with view
		mPhotoView.setImageBitmap(bitmap);
		mPhotoView.setVisibility(View.VISIBLE);
		
		// write scaled FIT bitmap to FIT directory 
		mCurrentPhotoPath = mImageAlbumStorage.addBitmapToMediaDB(this, bitmap, ImageAlbumStorage.IMG_DIR_FIT, mImageName);

		//////////////////////////THUMB///////////////
		// set bitmap options to scale the image decode target
		scaleFactor = Math.min(photoW/(photoW/10), photoH/(photoH/10));
		Log.v(TAG, "setupView: THUMB scale factor: " + scaleFactor);
		
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		// decode the JPEG into the bitmap
		bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
		
		// associate bitmap with view
		mThumbView.setImageBitmap(bitmap);
		mThumbView.setVisibility(View.VISIBLE);
		
		// write scaled THUMB bitmap to THUMB directory 
		mCurrentPhotoPath = mImageAlbumStorage.addBitmapToMediaDB(this, bitmap, ImageAlbumStorage.IMG_DIR_THUMB, mImageName);
	}
	///////////////////////////////////////////////////////////////////////////////
	public DisplayMetrics getDisplayMetrics() {
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		Log.v(TAG, "display metrics W/H: " + displayMetrics.widthPixels + "/" + displayMetrics.heightPixels);
		return displayMetrics;
	}
	///////////////////////////////////////////////////////////////////////////////


}
    