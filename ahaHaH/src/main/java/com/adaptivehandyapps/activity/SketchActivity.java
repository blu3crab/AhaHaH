// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.adaptivehandyapps.ahahah.R;
import com.adaptivehandyapps.sketch.ShapeManager;
import com.adaptivehandyapps.sketch.SketchSetting;
import com.adaptivehandyapps.sketch.TouchView;
import com.adaptivehandyapps.sketch.SketchSetting.ShapeType;
import com.adaptivehandyapps.util.AhaDisplayMetrics;
import com.adaptivehandyapps.util.ImageAlbumStorage;
import com.adaptivehandyapps.util.PrefsUtils;

public class SketchActivity extends Activity {
	// sketch activity
	private static final String TAG = "SketchActivity";

    public static final int REQUEST_CODE_SELECT_BACKDROP = 2;
    public static final int REQUEST_CODE_SELECT_OVERLAY = 3;

    private Activity mParentActivity;
    private Context mContext;

//	private ImageAlbumStorage mImageAlbumStorage = null;

	private static SketchActivity mSketchActivity;

	private SketchSetting mSketchSettings = null;	// sketch settings
	private ShapeManager mShapeManager = null;		// shape manager
	private TouchView mTouchView = null;			// touch view

    // canvas dimensions 
    private int mCanvasWidth = -1;
    private int mCanvasHeight = -1;

	// popup menu selection
	private int mPopupMenuResId;
    // TODO: save to & restore from Android/app/data
	// temp file name for retaining shape list 
	private String temp = "temp";
	
//	private OrientationEventListener mOrientationListener;

	private String mForecastText = "LaLaLaLaLa";

	private String mImagePath = "";
	private Bitmap mImageBitmap;

//	private ShareActionProvider mShareActionProvider;

	///////////////////////////////////////////////////////////////////////////
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(TAG,"onCreate...");
		super.onCreate(savedInstanceState);
		// set parent activity reference
//        mParentActivity = AhaHahActivity.mParentActivity;
        mParentActivity = AhaHahActivity.getAhaHahActivity();
        mContext = this;

		// set canvas dimensions to display dimensions until touch view canvas created
		mCanvasWidth = AhaDisplayMetrics.getDisplayWidth(this);
		mCanvasHeight = AhaDisplayMetrics.getDisplayHeight(this);
//		// use parent activity to access mImageAlbumStorage
//		mImageAlbumStorage = ((AhaHahActivity)mParentActivity).getImageAlbumStorage();

		// set sketch activity reference
		mSketchActivity = this;
		// instantiate sketch setting
		mSketchSettings = new SketchSetting();
		// set defaults for unsaved items
		mSketchSettings.setDefaultSettings();
		// restore saved sketch settings
		mSketchSettings.restoreSketchSettings();
		// instantiate shape manager
		mShapeManager = new ShapeManager();
		// if project folder has not changed
        String albumName = PrefsUtils.getPrefs(mContext, PrefsUtils.ALBUMNAME_KEY);
		if ( albumName.equals(mSketchSettings.getAlbumName())) {
			// load ShapeManager shape list
			mShapeManager.load(temp);
			Log.v(TAG, "onCreate loading: " + mSketchSettings.getAlbumName());
		}
		else {
			// reset ShapeManager shape list based on obsolete project folder
			mSketchSettings.setAlbumName(albumName);
			// delete ShapeManager shape list
			mShapeManager.delete(temp);			
			Log.v(TAG, "onCreate reset to: " + mSketchSettings.getAlbumName());
		}

		// instantiate touch view after layout (id:the_canvas)
		setContentView(R.layout.activity_sketch);
		mTouchView = (TouchView) findViewById(R.id.the_canvas);

		// fixed landscape orientation
		// get orientation degree - follow scale listener pattern or make it work somehow...
//		mOrientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
//	        public void onOrientationChanged(int orientation) {
//	        	Log.v(TAG, "onOrientationChanged: " + orientation);
//	        }
//	    };
//	    // determine landscape or portrait orientation
//	    int orientation = getResources().getConfiguration().orientation; 
//    	Log.v(TAG, "getResources orientation: " + orientation);
	}
	///////////////////////////////////////////////////////////////////////////
	// activity life-cycle
    protected void onPause() {
		Log.v(TAG, "onPause");     	
    	// perform light weight cleanup, release resources, save draft data
		mSketchSettings.saveSketchSettings();
		// save ShapeManager shape list
		mShapeManager.save(temp);
		
        super.onPause();
    }

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
	///////////////////////////////////////////////////////////////////////////
    // getters:
    public static SketchActivity getSketchActivity() { return mSketchActivity; }
    // provide access to sketch settings
    public SketchSetting getSketchSettings() { return mSketchSettings; }
    // provide access to touch view
	public TouchView getTouchView() { return mTouchView; }
    // provide access to shape manager
    public ShapeManager getShapeManager() { return mShapeManager; }
    // canvas dimensions
    public int getCanvasWidth() { return mCanvasWidth; }
    public void setCanvasWidth(int dim) { mCanvasWidth = dim; }
    public int getCanvasHeight() { return mCanvasHeight; }
    public void setCanvasHeight(int dim) { mCanvasHeight = dim; }

    ///////////////////////////////////////////////////////////////////////////
    public void updatePaint() {
        mShapeManager.updatePaint();
        mTouchView.invalidate();
    }
	///////////////////////////////////////////////////////////////////////////
	// action menu handling
	//
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sketch_menu, menu);
		return true;
	}
	private Intent createShareIntent() {
		if (mImageBitmap == null) {
			// save bitmap
			Bitmap bitmap = mTouchView.getCanvasBitmap();
			saveSketch(bitmap);
			Log.v(TAG, "createShareIntent save sketch.");
		}
		// create share intent
		Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
		shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

		// share image
		String path = MediaStore.Images.Media.insertImage(getContentResolver(), mImageBitmap, "title", null);
		Uri imageUri = Uri.parse(path);

		shareIntent.setType("image/jpeg");
		shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, imageUri);

		return shareIntent;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// anchor popup to view
		View view = findViewById(R.id.action_sketch_shape);
		switch (item.getItemId()) {
		case R.id.action_share:
			// TODO: prevent launching intent if mImageBitmap is null
			// launch the share intent
			startActivity(createShareIntent());
			Toast.makeText(this, "Launching share.", Toast.LENGTH_SHORT).show();
			return true;
		case R.id.action_sketch_file:
			// bring forth context menu!
			mPopupMenuResId = R.menu.sketch_file_menu;
			viewClickListener.onClick(view);
			return true;
		case R.id.action_sketch_shape:
			// bring forth context menu!
			mPopupMenuResId = R.menu.sketch_shape_menu;
			viewClickListener.onClick(view);
			return true;
		case R.id.action_sketch_style:
			// bring forth context menu!
			mPopupMenuResId = R.menu.sketch_style_menu;
			viewClickListener.onClick(view);
			return true;
		case R.id.action_sketch_color:
			// bring forth context menu!
			mPopupMenuResId = R.menu.sketch_color_menu;
			viewClickListener.onClick(view);
			return true;
		case R.id.action_sketch_erase:
			// bring forth context menu!
			mPopupMenuResId = R.menu.sketch_erase_menu;
			viewClickListener.onClick(view);
			return true;
//		case R.id.action_sketch_tool:
//			// bring forth context menu!
//			mPopupMenuResId = R.menu.sketch_tool_menu;
//			viewClickListener.onClick(view);
//			return true;
            default:
                Log.e(TAG, "onOptionsItemSelected sees invalid item id: " + item.getItemId());

        }
		
		return super.onOptionsItemSelected(item);
	}
	
	OnClickListener viewClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			showPopupMenu(v);
		}
	};
	///////////////////////////////////////////////////////////////////////////
	// private
	//
	private void showPopupMenu(View v) {
        // ensure menuResId is valid (e.g. onStop may invalidate)
        if (!mSketchSettings.isValidMenuResId(mPopupMenuResId)) {
            Log.e(TAG, "showPopupMenu sees invalid menuResId.");
            return;
        }

		PopupMenu popupMenu = new PopupMenu(SketchActivity.this, v);
	    popupMenu.getMenuInflater().inflate(mPopupMenuResId, popupMenu.getMenu());

        if (mSketchSettings.isValidCheckMenuResId(mPopupMenuResId)) {
            // check menu selections
            mSketchSettings.checkMenuSelections(mPopupMenuResId, popupMenu.getMenu());
        }

		popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
	   
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				int focus;
				// file menu
				if (mPopupMenuResId == R.menu.sketch_file_menu) {
					switch (item.getItemId()) {
					case R.id.action_sketch_file_new:
						Log.v(TAG, "onMenuItemClick file new.");
						// if current shape list contains more than BG rect
						if (mShapeManager.getShapeList().size() > 1) {
							// prompt to save current sketch
							saveSketchAlert();
						}
						// clear sketch canvas...unfortunately, canvas cleared prior to save
//						mShapeManager.clearShapeList();
						break;
					case R.id.action_sketch_file_loadbackdrop:
						Log.v(TAG, "onMenuItemClick load backdrop.");
						startGalleryActivity(REQUEST_CODE_SELECT_BACKDROP);
						break;
					case R.id.action_sketch_file_loadoverlay:
						Log.v(TAG, "onMenuItemClick load overlay image.");
	        			// ensure rect is focus
	        			focus = mShapeManager.getShapeListFocus();
	        			if (mShapeManager.isShapeType(ShapeType.RECT, focus)) {
	            			// start gallery to select image OVERLAY (focus)
							startGalleryActivity(REQUEST_CODE_SELECT_OVERLAY);
	        			}
	        			else {
	      	        		Toast.makeText(mParentActivity, R.string.sketch_overlay_toast, Toast.LENGTH_LONG).show();
	        			}

						break;
					case R.id.action_sketch_file_savesketch:
						Bitmap bitmap = mTouchView.getCanvasBitmap();
						saveSketch(bitmap);
						Log.v(TAG, "onMenuItemClick save sketch.");
						break;
					}
				}
				// erase menu
				else if (mPopupMenuResId == R.menu.sketch_erase_menu) {
					switch (item.getItemId()) {
					case R.id.action_sketch_erase_backdrop:
						Log.v(TAG, "onMenuItemClick erase backdrop.");
						// if 1st shape is image, assume backdrop & clear
	        			if (mShapeManager.isShapeType(ShapeType.IMAGE, ShapeManager.BACKDROP_IMAGE_INX)) {
	        				mShapeManager.clearShape(ShapeManager.BACKDROP_IMAGE_INX);
	        			}
	        			else {
	     	        		Toast.makeText(mParentActivity, R.string.sketch_no_backdrop_toast, Toast.LENGTH_LONG).show();      				
	        			}
	        			break;
					case R.id.action_sketch_erase_overlay:
						Log.v(TAG, "onMenuItemClick erase overlay.");
						// if focus is image, revert to rect
	        			focus = mShapeManager.getShapeListFocus();
	        			if (mShapeManager.isShapeType(ShapeType.IMAGE, focus)) {
	        				mShapeManager.revertShapeToRect(focus);
	        			}
	        			else {
	     	        		Toast.makeText(mParentActivity, R.string.sketch_no_overlay_toast, Toast.LENGTH_LONG).show();      				
	        			}
						break;
					case R.id.action_sketch_erase_select:
						Log.v(TAG, "onMenuItemClick erase selected shape.");
						// if shape is selected, clear focus shape
	        			focus = mShapeManager.getShapeListFocus();
	        			if (focus != ShapeManager.NOFOCUS) {
	        				mShapeManager.clearShape(focus);
	        			}
	        			else {
	     	        		Toast.makeText(mParentActivity, R.string.sketch_no_selection_toast, Toast.LENGTH_LONG).show();      				
	        			}
						break;
					case R.id.action_sketch_erase_last:
						Log.v(TAG, "onMenuItemClick erase last shape.");
						// clear last shape
						int lastInx = mShapeManager.getShapeList().size()-1;
						if (!mShapeManager.clearShape(lastInx)) {
	     	        		Toast.makeText(mParentActivity, R.string.sketch_empty_list_toast, Toast.LENGTH_LONG).show();      											
						}
						break;
					case R.id.action_sketch_erase_all:
						Log.v(TAG, "onMenuItemClick erase all.");
						// clear sketch canvas
//						mTouchView.clearView();
						mShapeManager.initShapeList();
						break;
					}
				}
				else if (mSketchSettings.setMenuSelection(mPopupMenuResId, item)) {
//					mTouchView.updatePaint();
					mShapeManager.updatePaint();
				}
                else {
                    Log.e(TAG, "showPopupMenu.setOnMenuItemClickListener sees invalid menuResId.");
                }
				mTouchView.invalidate();
				return true;
			}
		});
	    
		popupMenu.show();
	}
	//////////////////////////////////////////////////////////////////////////
    private void startGalleryActivity (int reqCode) {
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
						// set image as backdrop (0th indicates insert BACKDROP)
                        Log.v(TAG, "onActivityResult REQUEST_CODE_SELECT_BACKDROP ");
						mShapeManager.setImageShape(imagePath, 0);
						mTouchView.invalidate();
						break;
					case REQUEST_CODE_SELECT_OVERLAY:
						// if rect selected, set image to selected rect shape
						int focus = mShapeManager.getShapeListFocus();
						Log.v(TAG, "onActivityResult REQUEST_CODE_SELECT_OVERLAY focus shape inx: " + focus);
						if (mShapeManager.isShapeType(ShapeType.RECT, focus)) {
							// set image as OVERLAY (focus)
							mShapeManager.setImageShape(imagePath, focus);
						}
						else {
							Log.e(TAG, "onActivityResult OVERLAY failure - focus (" + focus + ") is not RECT. ");
						}
						mTouchView.invalidate();
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
    private String saveSketch (Bitmap bitmap) {

		String imageName = "nada";
		try {
            String albumName = PrefsUtils.getPrefs(mContext, PrefsUtils.ALBUMNAME_KEY);
			// timestamp an image name & create the file
			imageName = ImageAlbumStorage.timestampImageName();
			Log.v(TAG, "timestampImageName: "+ imageName);
			
			// write (pre-scaled) FIT bitmap to FIT directory 
			String imagePath = ImageAlbumStorage.addBitmapToMediaDB(this, bitmap, ImageAlbumStorage.IMG_DIR_FIT, imageName, albumName);
			Log.v(TAG, "added imagePath: "+ imagePath);
			// retain latest saved image
			mImagePath = imagePath;
			mImageBitmap = bitmap;
			
            Log.v(TAG, "Sketch saved to " + albumName);
            Toast.makeText(mParentActivity, "Sketch saved to " + albumName, Toast.LENGTH_LONG).show();
        }
		catch (Exception e) {
			e.printStackTrace();
		}

	   return imageName;
    }
    ///////////////////////////////////////////////////////////////////////////
    private void saveSketchAlert() {
	    AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("New Sketch Alert");
        alert.setMessage("Clicking 'Yes' will clear your current sketch. To save prior to clearing the canvas, select 'No' and choose 'Save Sketch Image' under 'File'.");

        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int whichButton) {
               // Yes, proceed to clear current sketch
               Log.v(TAG, "saveSketchAlert proceed? YES");
               // clear sketch canvas
               mShapeManager.initShapeList();
               mTouchView.invalidate();
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
