// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.activity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.adaptivehandyapps.ahahah.BuildConfig;
import com.adaptivehandyapps.ahahah.R;
import com.adaptivehandyapps.sketch.SketchActivity;
import com.adaptivehandyapps.sketch.SketchViewModel;
import com.adaptivehandyapps.util.AhaDisplayMetrics;
import com.adaptivehandyapps.util.ImageAlbumStorage;
import com.adaptivehandyapps.util.PrefsUtils;

public class AhaHahActivity extends Activity {

	private static final String TAG = "AhaHahActivity";
//	// toast display metrics at startup
//    private static final Boolean TOAST_DISPLAY_METRICS = true;

	// permission request codes
	public final static int PERMISSIONS_REQUEST = 125;

    // capture photo path by launching camera activity for result
    private static final Boolean CAPTURE_PHOTO_PATH = true;
	// activity request codes
	private static final int ACTION_TAKE_PHOTO = 2;

    ////////////////////////////////////////////////////////////////////////
    // setters/getters
    String mPhotoCapturePath;
    public String getPhotoCapturePath() { return mPhotoCapturePath; }
    public void setPhotoCapturePath(String photoCapturePath) { this.mPhotoCapturePath = photoCapturePath; }

    //////////////// activity lifecycle methods ////////////////
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate...");

		if (requestForPermission(this)) {
			Log.d(TAG, "onCreate permissions granted...");
			init();
		}
	}

	private Boolean init() {
//		// seed screen resolution
//        String toastText = AhaDisplayMetrics.toString(this);
//        if (TOAST_DISPLAY_METRICS) Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();

        setContentView(R.layout.activity_ahahah);

        Log.d(TAG, SketchViewModel.prefsToString(this));

        // on initial installation, remnant settings may persist...if so reset project dir & last sketch
		String albumNameDefault = this.getString(R.string.default_project_name);
		if (PrefsUtils.getPrefs(this, PrefsUtils.ALBUMNAME_KEY, albumNameDefault).equals(PrefsUtils.DEFAULT_STRING_NADA)) {
			PrefsUtils.setPrefs(this, PrefsUtils.ALBUMNAME_KEY, albumNameDefault);
			PrefsUtils.setPrefs(this, PrefsUtils.IMAGEPATH_KEY, PrefsUtils.DEFAULT_STRING_NADA);
			Log.d(TAG, SketchViewModel.prefsToString(this));
		}

//		// launch sketch activity
//        this.startSketchActivity(getCurrentFocus());

        return true;
	}
	///////////////////////////////////////////////////////////////////////////////
	private Boolean setupSplashView() {
		ImageView splashView = (ImageView) findViewById(R.id.imageViewSplash);
		ImageView thumbView = (ImageView) findViewById(R.id.imageViewThumb);
		String imagePath = PrefsUtils.getPrefs(this, PrefsUtils.IMAGEPATH_KEY, PrefsUtils.DEFAULT_STRING_NADA);
		// if image path & target views defined
		if (!imagePath.equals(PrefsUtils.DEFAULT_STRING_NADA) && splashView != null && thumbView != null) {
			// ensure file still exists
			File file = new File(imagePath);
			if (!file.exists()) return false;

			// get device dimensions
			DisplayMetrics displayMetrics = AhaDisplayMetrics.getDisplayMetrics(this);
			int targetDeviceW = displayMetrics.widthPixels;
			int targetDeviceH = displayMetrics.heightPixels;
			Log.v(TAG, "target device W/H: " + targetDeviceW + "/" + targetDeviceH);

			int scaleFactor = 1;
			// get size of photo
			BitmapFactory.Options bmOptions = new BitmapFactory.Options();
			bmOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(imagePath, bmOptions);
			//////////////////////////FIT////////////////////////
			int photoW = bmOptions.outWidth;
			int photoH = bmOptions.outHeight;
			Log.v(TAG, "setupView: photo W/H: " + photoW + "/" + photoH);
			// round up since int math truncates remainder resulting scale factor 1 (FIT = FULL)
			scaleFactor = Math.min(photoW / targetDeviceW, photoH / targetDeviceH) + 1;
			Log.v(TAG, "setupView: FIT scale factor: " + scaleFactor);
			// set bitmap options to scale the image decode target
			bmOptions.inJustDecodeBounds = false;
			bmOptions.inSampleSize = scaleFactor;
			bmOptions.inPurgeable = true;

			// decode the JPEG into the bitmap
			Bitmap bitmap = BitmapFactory.decodeFile(imagePath, bmOptions);
			// associate bitmap with view
			splashView.setImageBitmap(bitmap);
			splashView.setVisibility(View.VISIBLE);

			//////////////////////////THUMB///////////////
			// set bitmap options to scale the image decode target
			scaleFactor = Math.min(photoW / (photoW / 10), photoH / (photoH / 10));
			Log.v(TAG, "setupView: THUMB scale factor: " + scaleFactor);

			bmOptions.inJustDecodeBounds = false;
			bmOptions.inSampleSize = scaleFactor;
			bmOptions.inPurgeable = true;

			// decode the JPEG into the bitmap
			bitmap = BitmapFactory.decodeFile(imagePath, bmOptions);

			// associate bitmap with view
			thumbView.setImageBitmap(bitmap);
			thumbView.setVisibility(View.VISIBLE);
		}
		return true;
	}

	// orientation fixed to LANDSCAPE in AndroidManifest.xml
//	public void onConfigurationChanged(Configuration newConfig) {
//	    super.onConfigurationChanged(newConfig);
//
//	    Log.v(TAG, "onConfigurationChanged");
//	    
//	    // Checks the orientation of the screen
//	    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//	        Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
//	    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
//	        Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
//	    }
//	}
	
    protected void onStart() {
    	// always called so complements onStop as well as invoked with onCreate
        super.onStart();
		// say hello
		Log.v(TAG, "onStart");     	
    }
    protected void onRestart() {
        super.onRestart();
		// say hello
		Log.v(TAG, "onRestart");     	
    }
    protected void onResume() {
    	// initialize resources released during OnPause
        super.onResume();
		// say hello
		Log.v(TAG, "onResume");
		// setup splash view
        setupSplashView();
    }

    protected void onPause() {
    	// perform light weight cleanup, release resources, save draft data
        super.onPause();
		// say hello
		Log.v(TAG, "onPause");     	
    }
    protected void onStop() {
    	// perform heavy duty cleanup, DB writes, persist auto-save data
        super.onStop();  // ...always call super class first
		// say hello
		Log.v(TAG, "onStop");     
    }
    protected void onDestroy() {
        super.onDestroy();
		// say hello
		Log.v(TAG, "onDestroy");     	
    }

//	///////////////////////////////////////////////////////////////////////////////
//	// camera activity
//	public void startCameraActivity (View view) {
//		// create intent & start activity
//		if (isIntentAvailable(this, MediaStore.ACTION_IMAGE_CAPTURE)) {
//            // camera is available, launch camera intent
//		    // NO GO! allow repeated photo snaps
//            Log.v(TAG, "camera available...");
//            if (!CAPTURE_PHOTO_PATH) {
//                // no album targeted, launch vanilla camera intent
//                Intent intentTakePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                startActivity(intentTakePhoto);
//            }
//            else {
//                // dispatch camera intent after specifying path for capture
//                String photoCapturePath = dispatchTakePhotoIntent();
//                setPhotoCapturePath(photoCapturePath);
//            }
//			Toast.makeText(this, R.string.camera_hint, Toast.LENGTH_SHORT).show();
//		} else {
//			Log.v(TAG, "camera NOT available...");
//			Toast.makeText(this, "camera NOT available...", Toast.LENGTH_SHORT).show();
//		}
//
//		Toast.makeText(this, R.string.camera_hint, Toast.LENGTH_SHORT).show();
//	}
//
//	// establish if camera intent is available
//	public static boolean isIntentAvailable (Context context, String action) {
//		final PackageManager packageManager = context.getPackageManager();
//		final Intent intent = new Intent(action);
//		List<ResolveInfo> list =
//				packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
//		return list.size() > 0;
//	}
//
//	private String dispatchTakePhotoIntent () {
//		Intent intentTakePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//		try {
//			// timestamp an image name & create the file
//			String imageName = ImageAlbumStorage.timestampImageName();
//			Log.v(TAG, "timestampImageName: "+ imageName);
//			// create file in camera dir
//			File photo = ImageAlbumStorage.createImageFile(null, imageName);
//			if (photo == null) return PrefsUtils.DEFAULT_STRING_NADA;
//			String currentPhotoPath = photo.getAbsolutePath();
//			Log.v(TAG, "createImageFile: "+ currentPhotoPath);
//			// put file handle in take photo intent extras
//			Uri imageUri = FileProvider.getUriForFile(this,
//					BuildConfig.APPLICATION_ID + ".provider", photo);
//			intentTakePhoto.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
//
//			// start camera for result of photo
//			startActivityForResult(intentTakePhoto, ACTION_TAKE_PHOTO);
//			return currentPhotoPath;
//
//		} catch (IOException e) {
//			Log.e(TAG, e.getMessage());
//            return PrefsUtils.DEFAULT_STRING_NADA;
//		}
//	}
//	// take photo result - capture resulting image
//	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
//		Log.v(TAG, "onActivityResult: request code " + requestCode + ", result code " + resultCode);
//		if (resultCode == RESULT_OK ) {
//			if (requestCode == ACTION_TAKE_PHOTO) {
//				// TODO: unless orientation fixed, photo orientation on return to parent activity incorrect in 3 of 4 orientation
//				String path = getPhotoCapturePath();
//				Log.v(TAG, "onActivityResult getPhotoCapturePath " + path);
//			}
//		} else {
//			Log.e(TAG, "onActivityResult NOT OK! request code " + requestCode + ", result code " + resultCode);
//		}
//	}
//	///////////////////////////////////////////////////////////////////////////////
//	// gallery activity (view not select)
//	public void startGalleryActivity (View view) {
//		// create intent & start activity
//		Intent intent = new Intent();
//		intent.setAction(android.content.Intent.ACTION_VIEW);
//		intent.setType("image/*");
//		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		startActivity(intent);
//		Toast.makeText(this, "Viewing gallery - touch back when finished.", Toast.LENGTH_LONG).show();
//	}
	///////////////////////////////////////////////////////////////////////////////
	// sketch activity
	public void startSketchActivity (View view) {
		// create intent & start activity
		Intent intent = new Intent(this, SketchActivity.class);
		startActivity(intent);
		Toast.makeText(this, R.string.sketch_hint, Toast.LENGTH_SHORT).show();
	}

	///////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////
	// request write permission (read permission implicit)
	//		requestForPermission();
	public static boolean requestForPermission(Context context) {

		boolean isPermissionGranted = true;
		final int version = Build.VERSION.SDK_INT;
		if (version >= 23) {
			List<String> permList = new ArrayList<>();
			if (!hasWritePermission(context)) {
				permList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//				permList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
			}
			if (!hasCameraPermission(context)) {
				permList.add(Manifest.permission.CAMERA);
			}
			if (!hasInternetPermission(context)) {
				permList.add(Manifest.permission.INTERNET);
			}
			if (permList.size() > 0) {
				isPermissionGranted = false;
				Log.d("onRequestPermissions", permList.toString());
				ActivityCompat.requestPermissions((Activity)context, permList.toArray(new String[permList.size()]), PERMISSIONS_REQUEST);
			}
		}
		return isPermissionGranted;
	}

	public static boolean hasWritePermission(Context context) {
		return (hasPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE));
	}

	public static boolean hasCameraPermission(Context context) {
		return (hasPermission(context, android.Manifest.permission.CAMERA));
	}

	public static boolean hasInternetPermission(Context context) {
		return (hasPermission(context, android.Manifest.permission.INTERNET));
	}
	private static boolean hasPermission(Context context, String perm) {
		return (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, perm));
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,  @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult code = " + requestCode + " for " + permissions.length + " permissions.");
//        for (int i = 0; i < permissions.length; i++) {
//            Log.d(TAG, "onRequestPermissionsResult permissions " + permissions[i] + " grant result " + grantResults[i]);
//        }

		if (requestCode == PERMISSIONS_REQUEST) {
            boolean permissionGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                Log.d(TAG, "onRequestPermissionsResult permissions " + permissions[i] + " grant result " + grantResults[i]);
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) permissionGranted = false;
            }

            if (permissionGranted) {
                    Log.d(TAG, "onRequestPermissionsResult granted - init view.");
                    init();
			}
			else {
				// denied
				Log.e(TAG, "onRequestPermissionsResult permissions not granted - finish.");
				finishAndRemoveTask();
			}
		}
	}
	///////////////////////////////////////////////////////////////////////////////
}
