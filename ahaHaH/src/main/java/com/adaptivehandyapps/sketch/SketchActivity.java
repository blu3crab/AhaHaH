// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.sketch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.adaptivehandyapps.ahahah.R;
import com.adaptivehandyapps.util.DisplayUtils;
import com.adaptivehandyapps.util.PrefsUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;

import afzkl.development.colorpickerview.dialog.ColorPickerDialog;

import static android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

public class SketchActivity extends Activity implements NavigationView.OnNavigationItemSelectedListener {
	// sketch activity
	private static final String TAG = "SketchActivity";

    // toast display metrics at startup
    private static final Boolean TOAST_DISPLAY_METRICS = true;

    // activity request codes
    public static final int REQUEST_CODE_SELECT_BACKDROP = 2;
    public static final int REQUEST_CODE_SELECT_OVERLAY = 3;

	private static SketchActivity mSketchActivity;
	private Context mContext;

	private NavigationView mNavigationView;
	private DrawerLayout mDrawerLayout;
	private NavMenu mNavMenu;
	private Boolean mNavDrawerPinned = false;

	private SketchViewModel mSketchViewModel = null;	// sketch view model
    private Boolean mSketchViewModelSaved = false;      // model saved flag
	private SketchView mSketchView = null;			    // sketch view

    ///////////////////////////////////////////////////////////////////////////
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG,"onCreate...");
		super.onCreate(savedInstanceState);

        String orientationText = DisplayUtils.getOrientationText(this, getRequestedOrientation());

        // seed screen resolution
        String toastText = orientationText + " with " + DisplayUtils.metricsToString(this);
        if (TOAST_DISPLAY_METRICS) Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();

        Log.d(TAG, SketchViewModel.prefsToString(this));

        // set sketch activity reference
		mSketchActivity = this;
        setContext(this);
		// set view layout
		setContentView(R.layout.sketch_drawer_layout);

		// setup drawer
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.openDrawer(GravityCompat.START);
        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                // drawer's position changes...
                Log.d(TAG, "onDrawerSlide offset " + slideOffset);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // drawer has settled in a completely open state & is interactive at this point...
                Log.d(TAG, "onDrawerOpened...buildShapeList...");
                mNavMenu.buildShapeList(mNavigationView, mSketchViewModel);
                // TODO: show pin/unpin FAB, set pinned state
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                // drawer has settled in a completely closed state...
                Log.d(TAG, "onDrawerClosed...");
                // TODO: hide pin/unpin FAB, clear pinned state
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                // drawer motion state changes with new state one of STATE_IDLE, STATE_DRAGGING or STATE_SETTLING.
                String stateText = PrefsUtils.DEFAULT_STRING_NADA;
                if (newState == DrawerLayout.STATE_IDLE) stateText = "STATE_IDLE";
                else if (newState == DrawerLayout.STATE_DRAGGING) stateText = "STATE_DRAGGING";
                else if (newState == DrawerLayout.STATE_SETTLING) stateText = "STATE_SETTLING";
                Log.d(TAG, "onDrawerStateChanged newState " + stateText);
            }
        });
		// setup navigation view
		mNavigationView = (NavigationView) findViewById(R.id.nav_view);
		mNavigationView.setNavigationItemSelectedListener(this);

        // build nav menu
        mNavMenu = new NavMenu(getContext(), mNavigationView);

		// instantiate touch view after layout (id:the_canvas)
		mSketchView = (SketchView) findViewById(R.id.the_canvas);

        // get (instantiate) view model and model
        mSketchViewModel = SketchViewModel.getInstance(getContext(), mSketchView);
        // build shape list
        mNavMenu.buildShapeList(mNavigationView, mSketchViewModel);

        // indicate view model not saved
        setSketchViewModelSaved(false);

    }
    ///////////////////////////////////////////////////////////////////////////////////////////
	@SuppressWarnings("StatementWithEmptyBody")
	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		// Handle navigation view item clicks here.
		String itemname = item.toString();
//		String itemSplit[] = itemname.split(":");
        // for static menu items, extract id & compare to resource
        int id = item.getItemId();
		Log.d(TAG, "onNavigationItemSelected itemname: " + itemname + ", id:" + id);

		// identify selected menu item
		item.setChecked(true);

        if (itemname.equals(getContext().getString(R.string.action_sketch_file_new))) {
            Log.v(TAG, "onNavigationItemSelected file new.");
            // if sketch defined
            if (mSketchViewModel.isSketchDefined()) {
                // prompt to save current sketch
                saveSketchAlert();
            }
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_file_loadbackdrop))) {
            Log.v(TAG, "onNavigationItemSelected load backdrop.");
            launchGalleryActivity(REQUEST_CODE_SELECT_BACKDROP);
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_file_loadoverlay))) {
            Log.v(TAG, "onNavigationItemSelected load overlay image.");
            // start gallery to select image OVERLAY (focus)
            launchGalleryActivity(REQUEST_CODE_SELECT_OVERLAY);
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_file_savesketch))) {
            Log.v(TAG, "onNavigationItemSelected save sketch.");
            mSketchViewModel.actionFileSaveSketch();
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_file_share))) {
            // launch the share activity
            Toast.makeText(this, "Launching share.", Toast.LENGTH_SHORT).show();
            launchShareActivity();
        }
		else if (itemname.equals(getContext().getString(R.string.action_sketch_erase_backdrop))) {
			Log.v(TAG, "onNavigationItemSelected erase backdrop.");
			mSketchViewModel.actionEraseBackdrop();
		}
		else if (itemname.equals(getContext().getString(R.string.action_sketch_erase_overlay))) {
			Log.v(TAG, "onNavigationItemSelected erase overlay.");
            mSketchViewModel.actionEraseOverlay();
		}
		else if (itemname.equals(getContext().getString(R.string.action_sketch_erase_select))) {
			Log.v(TAG, "onMenuItemClick erase selected shape.");
			mSketchViewModel.actionEraseSelection();
		}
		else if (itemname.equals(getContext().getString(R.string.action_sketch_erase_last))) {
			Log.v(TAG, "onNavigationItemSelected erase last shape.");
			mSketchViewModel.actionEraseLastShape();
		}
//        else if (itemname.equals(getContext().getString(R.string.action_sketch_erase_all))) {
//            Log.v(TAG, "onNavigationItemSelected erase all.");
//            mSketchViewModel.actionEraseAll();
//        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_color_custom))) {
            Log.v(TAG, "onNavigationItemSelected custom color.");
            launchColorPickerDialog();
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_focus_clear))) {
            Log.v(TAG, "onNavigationItemSelected clear focus.");
            mSketchViewModel.clearShapeListFocus();
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_focus_next))) {
            Log.v(TAG, "onNavigationItemSelected focus next.");
            mSketchViewModel.setNextShapeListFocus();
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_focus_prev))) {
            Log.v(TAG, "onNavigationItemSelected focus prev.");
            mSketchViewModel.setPrevShapeListFocus();
        }
        else {
            Log.v(TAG, "onNavigationItemSelected setSelection " + itemname);
            // TODO: if custom color launch color dialog
            if (!mSketchViewModel.setSelection(itemname)) {
                Log.e(TAG, "Ooops!  onNavigationItemSelected finds unknown menu item " + itemname);
            }
        }
        // TODO: if unpinned state, close drawer
		// close drawer
		mDrawerLayout.closeDrawer(GravityCompat.START);
        mSketchView.invalidate();
		return true;
	}
	///////////////////////////////////////////////////////////////////////////
	// activity life-cycle
    protected void onStart() {
    	// always called so complements onStop as well as invoked with onCreate
        super.onStart();
		// say hello
        setSketchViewModelSaved(false);
		Log.v(TAG, "onStart");
    }
    protected void onRestart() {
        super.onRestart();
		// say hello
        setSketchViewModelSaved(false);
		Log.v(TAG, "onRestart");
    }
    protected void onResume() {
    	// initialize resources released during OnPause
        super.onResume();
		// say hello
        setSketchViewModelSaved(false);
		Log.v(TAG, "onResume");
    }

    protected void onPause() {
        Log.v(TAG, "onPause");
        // perform light weight cleanup, release resources, save draft data
        if(!isSketchViewModelSaved()) saveSketchViewModel();
        super.onPause();
    }

    protected void onStop() {
        Log.v(TAG, "onStop");
        // perform heavy duty cleanup, DB writes, persist auto-save data
        if(!isSketchViewModelSaved()) saveSketchViewModel();
        super.onStop();  // ...always call super class first
    }
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        // TODO: not invoked!  AhaHahActivity gets signal...
        // remove tempfile
        mSketchViewModel.actionFileNew();
//        if(!isSketchViewModelSaved()) saveSketchViewModel();
        super.onDestroy();
    }
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);

        Log.v(TAG, "onConfigurationChanged to " + DisplayUtils.getOrientationText(this, getRequestedOrientation()));

	    // Checks the orientation of the screen
	    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
	        Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
	    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
	        Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
	    }
	}
    ///////////////////////////////////////////////////////////////////////////
    // getters/setters
    public void setContext(Context context) { mContext = context; }
    public Context getContext() { return mContext; }
    public void setNavDrawerPinned(Boolean pinned) { mNavDrawerPinned = pinned; }
    public Boolean getNavDrawerPinned() { return mNavDrawerPinned; }
    ///////////////////////////////////////////////////////////////////////////
    // save sketch model helpers
    private Boolean isSketchViewModelSaved() { return mSketchViewModelSaved; }
    private Boolean setSketchViewModelSaved(Boolean saved) { mSketchViewModelSaved = saved; return mSketchViewModelSaved;}
    private void saveSketchViewModel() {
        // perform light weight cleanup, release resources, save draft data
        Log.d(TAG, "saveSketchViewModel saving model...");
        mSketchViewModel.saveSketchModel();
	    this.mSketchViewModelSaved = true;
	}
	///////////////////////////////////////////////////////////////////////////
	// gallery activity
    private void launchGalleryActivity(int reqCode) {
    	// create intent & start activity 
//        Intent.ACTION_OPEN_DOCUMENT,
		Intent galleryIntent = new Intent(
				Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		// TODO: export permissions so load of temp file on new session can access photo
        // Permission Denial: opening provider com.google.android.apps.photos.contentprovider.impl.MediaContentProvider from ProcessRecord{d7ea5f8 11065:com.adaptivehandyapps.ahahah/u0a154} (pid=11065, uid=10154) that is not exported from UID 10081
//        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//        galleryIntent.addFlags(FLAG_GRANT_READ_URI_PERMISSION);
//        galleryIntent.addFlags(FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
		startActivityForResult(galleryIntent , reqCode );
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			Log.v(TAG, "onActivityResult result OK: " + " for reqCode: " + requestCode);
//            String imagePath = PrefsUtils.DEFAULT_STRING_NADA;
            Bitmap bitmap = null;
			if (null != data) {
				Uri imageUri = data.getData();
//                final int takeFlags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
//                getContentResolver().takePersistableUriPermission(imageUri, takeFlags);
//                this.getContentResolver().takePersistableUriPermission(imageUri
//                        , data.getFlags()
//                                & ( Intent.FLAG_GRANT_READ_URI_PERMISSION
//                                + Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//                        )
//                );
                bitmap = DisplayUtils.decodeToBitmap(this, imageUri);
				if (bitmap == null) {
				    Toast.makeText(this, R.string.sketch_empty_image_path_toast, Toast.LENGTH_SHORT).show();
				    return;
                }
                switch (requestCode) {
                    case REQUEST_CODE_SELECT_BACKDROP:
                        Log.v(TAG, "onActivityResult REQUEST_CODE_SELECT_BACKDROP ");

//                        // determine target orientation of backdrop
//                        int targetOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
//                        if (bitmap.getHeight() > bitmap.getWidth()) targetOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
//
//                        // get current orientation
//                        int currentOrientation = getRequestedOrientation();
//                        Log.v(TAG, "onActivityResult current vs target orientation " +
//                                getOrientationText(currentOrientation) + " vs " + getOrientationText(targetOrientation));
//
//                        // if target backdrop is not current orientation
//                        if (currentOrientation != targetOrientation) {
//                            // TODO: confirm & clear sketch on orientation change
//                            // set orientation to target
//                            setRequestedOrientation(targetOrientation);
//                            Log.v(TAG, "onActivityResult set target orientation to " + getOrientationText(targetOrientation));
//                        }
////                        mSketchView.invalidate();

                        // set image as backdrop (0th indicates insert BACKDROP)
                        mSketchViewModel.actionFileLoadBackdrop(imageUri.toString(), bitmap);
                        break;
                    case REQUEST_CODE_SELECT_OVERLAY:
                        Log.v(TAG, "onActivityResult REQUEST_CODE_SELECT_OVERLAY ");
                        mSketchViewModel.actionFileLoadOverlay(imageUri.toString(), bitmap);
                        break;
                    default:
                        Log.e(TAG, "GalleryActivity unknown request code: " + requestCode);
                        break;
                }
			}
			else {
				Log.e(TAG, "GalleryActivity NULL data...");
			}
		}
		else {
			Log.e(TAG, "GalleryActivity FAILURE w/ result code: " + resultCode);
		}
	}
    ///////////////////////////////////////////////////////////////////////////
    // share activity
    private void launchShareActivity() {
        // create share intent
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        Bitmap imageBitmap = mSketchView.getCanvasBitmap();
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), imageBitmap, "title", null);
        Uri imageUri = Uri.parse(path);

        shareIntent.setType("image/jpeg");
        shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, imageUri);

        startActivity(shareIntent);
    }
    ///////////////////////////////////////////////////////////////////////////////
    // color picker dialog
    private void launchColorPickerDialog() {

        int initialValue = mSketchViewModel.getCustomColor();
        Log.d(TAG, "launchColorPickerDialog initial value:" + initialValue);

        final ColorPickerDialog colorDialog = new ColorPickerDialog(getContext(), initialValue);

        colorDialog.setAlphaSliderVisible(true);
        colorDialog.setTitle("Pick your Color!");

        colorDialog.setButton(DialogInterface.BUTTON_POSITIVE, getContext().getString(android.R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getContext(), "Selected Color: " + colorToHexString(colorDialog.getColor()), Toast.LENGTH_LONG).show();
                // capture custom color selection
                int color = colorDialog.getColor();
                mSketchViewModel.setCustomColor(color);
            }
        });

        colorDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Nothing to do here.
            }
        });

        colorDialog.show();
    }

    private String colorToHexString(int color) {
        return String.format("#%06X", 0xFFFFFFFF & color);
    }
    ///////////////////////////////////////////////////////////////////////////
    // new sketch alert dialog
    private void saveSketchAlert() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("New Sketch Alert");
        alert.setMessage("Clicking 'Yes' will clear your current sketch. To save prior to clearing the canvas, select 'No' and choose 'Save Sketch Image' under 'File'.");

        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Yes, proceed to clear current sketch
                Log.v(TAG, "saveSketchAlert proceed? YES");
                mSketchViewModel.actionFileNew();
            }
        });

        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // No.
                Log.v(TAG, "saveSketchAlert proceed? NO. ");
            }
        });

        alert.show();
    }
}
///////////////////////////////////////////////////////////////////////////
