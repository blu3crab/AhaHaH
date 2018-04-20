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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.adaptivehandyapps.ahahah.R;
import com.adaptivehandyapps.util.AhaDisplayMetrics;
import com.adaptivehandyapps.util.PrefsUtils;

import afzkl.development.colorpickerview.dialog.ColorPickerDialog;

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

	private SketchViewModel mSketchViewModel = null;	// sketch view model
    private Boolean mSketchViewModelSaved = false;      // model saved flag
	private SketchView mSketchView = null;			    // sketch view

	///////////////////////////////////////////////////////////////////////////
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG,"onCreate...");
		super.onCreate(savedInstanceState);
        // seed screen resolution
        String toastText = AhaDisplayMetrics.toString(this);
        if (TOAST_DISPLAY_METRICS) Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();

        Log.d(TAG, PrefsUtils.toString(this));

        // set sketch activity reference
		mSketchActivity = this;
        setContext(this);
		// set view layout
		setContentView(R.layout.sketch_drawer_layout);

		// setup drawer
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.openDrawer(GravityCompat.START);

		// setup navigation view
		mNavigationView = (NavigationView) findViewById(R.id.nav_view);
		mNavigationView.setNavigationItemSelectedListener(this);

        // build nav menu
        mNavMenu = new NavMenu(getContext(), mNavigationView);

		// instantiate touch view after layout (id:the_canvas)
		mSketchView = (SketchView) findViewById(R.id.the_canvas);

        // get (instantiate) view model and model
        mSketchViewModel = SketchViewModel.getInstance(getContext(), mSketchView);
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
            // ensure rect is focus
            if (mSketchViewModel.isRectFocus()) {
                // start gallery to select image OVERLAY (focus)
                launchGalleryActivity(REQUEST_CODE_SELECT_OVERLAY);
            } else {
                Toast.makeText(mContext, R.string.sketch_overlay_toast, Toast.LENGTH_LONG).show();
            }
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
        else if (itemname.equals(getContext().getString(R.string.action_sketch_erase_all))) {
            Log.v(TAG, "onNavigationItemSelected erase all.");
            mSketchViewModel.actionEraseAll();
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_color_custom))) {
            Log.v(TAG, "onNavigationItemSelected custom color.");
            launchColorPickerDialog();
        }
        else {
            Log.v(TAG, "onNavigationItemSelected setSelection " + itemname);
            // TODO: if custom color launch color dialog
            if (!mSketchViewModel.setSelection(itemname)) {
                Log.e(TAG, "Ooops!  onNavigationItemSelected finds unknown menu item " + itemname);
            }
        }
        // TODO: close drawer flag?  if not close, invalidate
		// close drawer
		mDrawerLayout.closeDrawer(GravityCompat.START);
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
        if(!isSketchViewModelSaved()) saveSketchViewModel();
        super.onDestroy();
    }
    ///////////////////////////////////////////////////////////////////////////
    // getters/setters
    public void setContext(Context context) { mContext = context; }
    public Context getContext() { return mContext; }
    ///////////////////////////////////////////////////////////////////////////
    // save sketch model helpers
    private Boolean isSketchViewModelSaved() { return mSketchViewModelSaved; }
    private Boolean setSketchViewModelSaved(Boolean saved) { mSketchViewModelSaved = saved; return mSketchViewModelSaved;}
    private void saveSketchViewModel() {
        // perform light weight cleanup, release resources, save draft data
        mSketchViewModel.saveSketchModel();
	    this.mSketchViewModelSaved = true;
	}
	///////////////////////////////////////////////////////////////////////////
	// gallery activity
    private void launchGalleryActivity(int reqCode) {
    	// create intent & start activity 
		Intent galleryIntent = new Intent(
				Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(galleryIntent , reqCode );
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			Log.v(TAG, "onActivityResult result OK: " + " for reqCode: " + requestCode);
			if (null != data) {
				Uri imageUri = data.getData();
				// get image path
				String imagePath = getRealPathFromURI(this, imageUri);
				Log.v(TAG, "onActivityResult image path: " + imagePath);
				switch (requestCode) {
					case REQUEST_CODE_SELECT_BACKDROP:
                        Log.v(TAG, "onActivityResult REQUEST_CODE_SELECT_BACKDROP ");
						// set image as backdrop (0th indicates insert BACKDROP)
                        mSketchViewModel.actionFileLoadBackdrop(imagePath);
						break;
					case REQUEST_CODE_SELECT_OVERLAY:
                        Log.v(TAG, "onActivityResult REQUEST_CODE_SELECT_OVERLAY ");
                        mSketchViewModel.actionFileLoadOverlay(imagePath);
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
	public String getRealPathFromURI(Context context, Uri contentUri) {
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
