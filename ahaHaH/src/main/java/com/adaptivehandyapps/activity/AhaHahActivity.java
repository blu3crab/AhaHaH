// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.activity;

import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.adaptivehandyapps.ahahah.R;
import com.adaptivehandyapps.util.AhaDisplayMetrics;
import com.adaptivehandyapps.util.ImageAlbumStorage;
import com.adaptivehandyapps.util.PrefsUtils;

public class AhaHahActivity extends Activity {
//    public class AhaHahActivity extends Activity
//            implements NavigationView.OnNavigationItemSelectedListener {

	private static final String TAG = "AhaHahActivity";
    private static final Boolean TOAST_DISPLAY_METRICS = true;

	// permissions
	public final static int PERMISSIONS_REQUEST = 125;

	private static AhaHahActivity mParentActivity;
	
//	private ImageAlbumStorage mImageAlbumStorage = null;
	private String mProjectFolder = null;

    ///////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////// start activities //////////////////////////
	
    public void startCameraActivity (View view) {
    	// create intent & start activity 
    	Intent intent = new Intent(this, CameraActivity.class);
    	startActivity(intent);
		Toast.makeText(this, R.string.camera_hint, Toast.LENGTH_SHORT).show();
    }
    
    public void startGalleryActivity (View view) {
    	// create intent & start activity 
//    	Intent intent = new Intent(this, GalleryActivity.class);
//    	startActivity(intent);
//		Toast.makeText(this, "Viewing gallery " + mImageAlbumStorage.getImageAlbumName(), Toast.LENGTH_LONG).show();
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setType("image/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
		Toast.makeText(this, "Viewing gallery - touch back when finished.", Toast.LENGTH_LONG).show();
    }
    public void startSketchActivity (View view) {
    	// create intent & start activity 
    	Intent intent = new Intent(this, SketchActivity.class);
    	startActivity(intent);
		Toast.makeText(this, R.string.sketch_hint, Toast.LENGTH_SHORT).show();
    }

    ////////////////////////////////////////////////////////////////////////
    // setters/getters
    public static AhaHahActivity getAhaHahActivity() {
    	return mParentActivity;
    }
//    // Camera & Gallery activities use this single copy of ImageAlbumStorage
//    public ImageAlbumStorage getImageAlbumStorage() {
//    	return mImageAlbumStorage;
//    }

    private String setImageAlbumFolder(String projectFolder) {
    	String albumPath = getString(R.string.album_folder_name)+ "/" + projectFolder;
    	PrefsUtils.setPrefs(this, PrefsUtils.ALBUMNAME_KEY, projectFolder);
//		mImageAlbumStorage = new ImageAlbumStorage(albumPath);
		Log.v(TAG, "setImageAlbumFolder album path: " + albumPath);

        if (!ImageAlbumStorage.isMediaMounted()) {
            Toast.makeText(this, "External storage not mounted R/W. Please insert your SIM card", Toast.LENGTH_LONG).show();
            Toast.makeText(this, "External storage not mounted R/W. Please insert your SIM card", Toast.LENGTH_LONG).show();
            Toast.makeText(this, "External storage not mounted R/W. Please insert your SIM card", Toast.LENGTH_LONG).show();
        }
        return albumPath;
    }
    private String getProjectFolder() {
    	return mProjectFolder;
    }
    private void setProjectFolder(String projectFolder) {
    	mProjectFolder = projectFolder;
		setImageAlbumFolder(getProjectFolder());
		PrefsUtils.setPrefs(this, PrefsUtils.ALBUMNAME_KEY, getProjectFolder());
    }
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
		// seed screen resolution
        String toastText = AhaDisplayMetrics.toString(this);
        if (TOAST_DISPLAY_METRICS) Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();

        setContentView(R.layout.activity_ahahah);

		ImageView v = (ImageView) findViewById(R.id.imageViewSplash);
		v.layout(0, 0, AhaDisplayMetrics.getDisplayWidth(this), AhaDisplayMetrics.getDisplayHeight(this));

		mParentActivity = this;

		// restore project folder setting or set default
        String albumName = PrefsUtils.getPrefs(this, PrefsUtils.ALBUMNAME_KEY);
		if (albumName.equals(PrefsUtils.DEFAULT_STRING_NADA)) {
			// instantiate image album storage class with default project folder
			setProjectFolder(getString(R.string.default_project_name));
			Log.v(TAG, "onCreate default project folder: " + getProjectFolder());
		}
		else {
			// instantiate image album storage class with default project folder
			setProjectFolder(albumName);
			Log.v(TAG, "onCreate restore project folder: " + getProjectFolder());
		}
    	return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.ahahah_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_gallery:
			this.startGalleryActivity(getCurrentFocus());
			return true;
		case R.id.action_camera:
			this.startCameraActivity(getCurrentFocus());
			return true;
		case R.id.action_sketch:
			this.startSketchActivity(getCurrentFocus());
			return true;
		case R.id.action_projects:
			// find view & kickoff popup menu 
			View view = findViewById(R.id.action_projects);
			if (view != null) viewClickListener.onClick(view);
			Toast.makeText(this, R.string.projects_hint, Toast.LENGTH_SHORT).show();
			return true;
//		case R.id.action_settings:
//			Toast.makeText(this, R.string.start_activity_toast, Toast.LENGTH_SHORT).show();
//			return true;

		}
		return super.onOptionsItemSelected(item);
	}

    private View.OnClickListener viewClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showPopupMenu(v);
        }
    };

	private void showPopupMenu(View v) {
		final String NEW_PROJECT = "<new>";
		
		PopupMenu popupMenu = new PopupMenu(this, v);

		// obtain list of existing folders
		List<String> folderList = new ArrayList<String>();
		folderList = ImageAlbumStorage.getProjectFolders(getString(R.string.album_folder_name));

        // dynamically add existing folders to menu
		for (String folder: folderList) {
			popupMenu.getMenu().add(folder);
			Log.v(TAG, "folder: " + folder);			
		}
		// add "new" selection at end of menu
		popupMenu.getMenu().add(NEW_PROJECT);
		// inflate menu & listen for clicks
	    popupMenu.getMenuInflater().inflate(R.menu.projects_menu, popupMenu.getMenu());

		popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
	   
			@Override
			public boolean onMenuItemClick(MenuItem item) {
//				String itemValue = item.toString();
//				Log.v(TAG, "itemValue: " + itemValue);			
	    		String projectFolder = item.toString();
	    		if (projectFolder != null) {
	    			Log.v(TAG, "project folder: " + projectFolder);
	    			if (projectFolder.equals(NEW_PROJECT)) {
	    				// set name of new project
	    				setNewProjectFolder();
	    			}
	    			else {
		    			// instantiate image album storage class with selected project folder
	    				setProjectFolder(projectFolder);
		    			Log.v(TAG, "onMenuItemClick project folder: " + getProjectFolder());
	    			}
	    		}
				return true;
			}
		});
	    
		popupMenu.show();
	}
	
	private void setNewProjectFolder() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("New Project");
		alert.setMessage("Please enter your new project name:");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String projectFolder = input.getText().toString();
				// instantiate image album storage class with selected project folder
				setProjectFolder(projectFolder);
				Log.v(TAG, "setNewProjectFolder project folder: " + getProjectFolder());
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
				Log.v(TAG, "setNewProjectFolder CANCELLED ");
			}
		});

		alert.show();
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
