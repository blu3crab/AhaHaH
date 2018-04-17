// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.sketch;

import afzkl.development.colorpickerview.dialog.ColorPickerDialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.adaptivehandyapps.ahahah.R;
import com.adaptivehandyapps.util.AhaDisplayMetrics;
import com.adaptivehandyapps.util.ImageAlbumStorage;
import com.adaptivehandyapps.util.PrefsUtils;

public class SketchViewModel {
	private static final String TAG = "SketchViewModel";

	// activity context
	private Context mContext;

	///////////////////////////////////////////////////////////////////////////
	// action types
	public static final int ACTION_TYPE_UNKNOWN = -1;

	public static final int ACTION_TYPE_CAMERA = 0;
	public static final int ACTION_TYPE_GALLERY = 1;

	public static final int ACTION_TYPE_FILE_NEW = 2;
	public static final int ACTION_TYPE_FILE_LOADBACKDROP = 3;
	public static final int ACTION_TYPE_FILE_LOADOVERLAY = 4;
	public static final int ACTION_TYPE_FILE_SAVE_SKETCH = 5;

	public static final int SELECT_TYPE_UNKNOWN = -1;

	public static final int SELECT_TYPE_SHAPE_FREE = 6;
	public static final int SELECT_TYPE_SHAPE_LINE = 7;
	public static final int SELECT_TYPE_SHAPE_RECT = 8;
	public static final int SELECT_TYPE_SHAPE_LABEL = 9;
	public static final int SELECT_TYPE_SHAPE_CIRCLE = 10;
    public static final int SELECT_TYPE_SHAPE_OVAL = 11;

    public static final int SELECT_TYPE_STYLE_SMALL = 21;
    public static final int SELECT_TYPE_STYLE_MEDIUM = 22;
    public static final int SELECT_TYPE_STYLE_LARGE = 23;
    public static final int SELECT_TYPE_STYLE_FILL = 24;
    public static final int SELECT_TYPE_STYLE_STROKE = 25;
    public static final int SELECT_TYPE_STYLE_STROKE_FILL = 26;
    public static final int SELECT_TYPE_STYLE_HOLD_FOCUS = 27;

    public static final int SELECT_TYPE_COLOR_BLACK = 31;
    public static final int SELECT_TYPE_COLOR_BLUE = 32;
    public static final int SELECT_TYPE_COLOR_GREEN = 33;
    public static final int SELECT_TYPE_COLOR_YELLOW = 34;
    public static final int SELECT_TYPE_COLOR_ORANGE = 35;
    public static final int SELECT_TYPE_COLOR_RED = 36;
    public static final int SELECT_TYPE_COLOR_VIOLET = 37;
    public static final int SELECT_TYPE_COLOR_WHITE = 38;
    public static final int SELECT_TYPE_COLOR_CUSTOM = 39;

    public static final int SELECT_TYPE_TOOL_PEN = 41;
    public static final int SELECT_TYPE_TOOL_BRUSH = 42;
    public static final int SELECT_TYPE_TOOL_SPRAY = 43;
    public static final int SELECT_TYPE_TOOL_BUCKET = 44;

    // TODO: save to & restore from Android/app/data
	// temp file name for retaining shape list
	private static final String TEMP_FILE = "tempfile";

	// shape model & sketch view references
	private ShapeModel mShapeModel = null;		// shape manager
	private SketchView mSketchView = null;			// touch view

	///////////////////////////////////////////////////////////////////////////
	// settings
	// shapes
	public enum ShapeType {
		NADA(0), FREE(1), LINE(2), RECT(3), LABEL(4), CIRCLE(5), OVAL(6), IMAGE(7);
		public int value;
		ShapeType (int value) {this.value = value;}
	}
	private ShapeType mShape = ShapeType.NADA;

	// tools
	public enum Tool {
		NADA, PEN, BRUSH, SPRAY, BUCKET
	}
	private Tool mTool = Tool.NADA;	

	// styles
	public enum Style {
		NADA, SMALL, MEDIUM, LARGE, FILL, STROKE, STROKEFILL, FOCUS
	}
	private Style mStyleSize = Style.NADA;
	private final float mSizeSmall = 2.0f;
	private final float mSizeMedium = 4.0f;
	private final float mSizeLarge = 6.0f;
	private Style mStyleFill = Style.NADA;
	private Style mStyleFocus = Style.NADA;

	// colors
	public enum Palette {
		NADA, BLACK, BLUE, GREEN, YELLOW, ORANGE, RED, VIOLET, WHITE,  MAX
	}
	private int[] mPalette = {
			0x55555555,
			0xFF000000,
			0xFF0000FF,
			0xFF00FF00,
			0xFFFFFF00,
			0xFFFF9900,
			0xFFFF0000, 
			0xFFFF00FF,
			0xFFFFFFFF,
			0xFF999999 };
	private int mColor = mPalette[Palette.NADA.ordinal()];
	private int mCustomColor = mPalette[Palette.NADA.ordinal()];
	// hold focus switch
	private boolean mFocusHold = false;
	// album name
//	private final static String NADA = "nada";
//	private String mAlbumName = NADA;

	///////////////////////////////////////////////////////////////////////////
	// save/restore settings
	private int MODE_PRIVATE = 0;
	public static final String PREFS_NAME = "SketchSettingsFile";
	private String mKeyShape = "Shape";
	private String mKeyTool = "Tool";
	private String mKeyStyleSize = "StyleSize";
	private String mKeyStyleFill = "StyleFill";
//	private String mKeyAlbum = "Album";
	private String mKeyColor = "Color";
	private String mKeyCustomColor = "CustomColor";
	private String mKeyFocusHold = "FocusHold";

	// canvas dimensions
	private int mCanvasWidth = -1;
	private int mCanvasHeight = -1;

	///////////////////////////////////////////////////////////////////////////
	private static volatile SketchViewModel instance;

	public synchronized static SketchViewModel getInstance(Context c, SketchView sketchView)
	{
		if (instance == null){
			synchronized (SketchViewModel.class) {   // Check for the second time.
				//if there is no instance available... create new one
				if (instance == null){
					instance = new SketchViewModel(c, sketchView);
				}
			}
		}

		return instance;
	}
	///////////////////////////////////////////////////////////////////////////////
    // constructor
	public SketchViewModel(Context context, SketchView sketchView) {
		//Prevent form the reflection api.
		if (instance != null){
			throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
		}

		setContext(context);
		// set canvas dimensions to display dimensions until touch view canvas created
		mCanvasWidth = AhaDisplayMetrics.getDisplayWidth(getContext());
		mCanvasHeight = AhaDisplayMetrics.getDisplayHeight(getContext());

		// get (instantiate) view model and model
//        mSketchView = (SketchView) ((Activity)getContext()).findViewById(R.id.the_canvas);
        mSketchView = sketchView;
		mShapeModel = ShapeModel.getInstance(getContext(), this);

		// restore settings
		restoreSketchSettings();
	}
	///////////////////////////////////////////////////////////////////////////
	// save sketch settings
	public void saveSketchSettings() {
		SharedPreferences settings = getContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
	    SharedPreferences.Editor editor = settings.edit();
//	    editor.remove(PREFS_NAME);

	    editor.putInt(mKeyShape, mShape.ordinal());
	    editor.putInt(mKeyTool, mTool.ordinal());
	    editor.putInt(mKeyStyleSize, mStyleSize.ordinal());
	    editor.putInt(mKeyStyleFill, mStyleFill.ordinal());
	    editor.putInt(mKeyColor, mColor);
	    editor.putInt(mKeyCustomColor, mCustomColor);
	    editor.putBoolean(mKeyFocusHold, mFocusHold);
//	    editor.putString(mKeyAlbum, mAlbumName);

	    // commit the edits!
	    editor.commit();

		// save ShapeModel shape list
		mShapeModel.save(TEMP_FILE);

	}
	///////////////////////////////////////////////////////////////////////////
	// restore sketch settings
	public void restoreSketchSettings() {
		SharedPreferences settings = getContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		//		mAlbumName = settings.getString(mKeyAlbum, NADA);
		String albumName = PrefsUtils.getPrefs(getContext(), PrefsUtils.ALBUMNAME_KEY);
		if (albumName.equals(PrefsUtils.DEFAULT_STRING_NADA)) albumName = getContext().getString(R.string.default_project_name);

		mShape = ShapeType.values()[settings.getInt(mKeyShape, 0)];
		mTool = Tool.values()[settings.getInt(mKeyTool, 0)];
		mStyleSize = Style.values()[settings.getInt(mKeyStyleSize, 0)];
		mStyleFill = Style.values()[settings.getInt(mKeyStyleFill, 0)];
		mColor = settings.getInt(mKeyColor, mPalette[Palette.NADA.ordinal()]);
		mCustomColor = settings.getInt(mKeyCustomColor, mPalette[Palette.NADA.ordinal()]);
		mFocusHold = settings.getBoolean(mKeyFocusHold, false);

		// if no settings, set defaults
		if (mShape == ShapeType.NADA ||
				mTool == Tool.NADA ||
				mStyleSize == Style.NADA ||
				mStyleFill == Style.NADA ||
				mStyleFocus == Style.NADA ||
				mColor == mPalette[Palette.NADA.ordinal()] ) {
			setDefaultSettings();
		}
		// load ShapeModel shape list
		mShapeModel.load(TEMP_FILE);
		Log.v(TAG, "restoreSketchSettings loading mShapeModel " + TEMP_FILE);

	}

	///////////////////////////////////////////////////////////////////////////
	// set default sketch settings
	public void setDefaultSettings() {
		mShape = ShapeType.FREE;
		mTool = Tool.PEN;
		mStyleSize = Style.SMALL;
		mStyleFill = Style.STROKE;
		mStyleFocus = Style.FOCUS;
		mColor = mPalette[Palette.RED.ordinal()];
		mCustomColor = mPalette[Palette.NADA.ordinal()];
		mFocusHold = false;
		setAlbumName(PrefsUtils.DEFAULT_STRING_NADA);

		return;
	}
	///////////////////////////////////////////////////////////////////////////
	// getters/setters
    // TODO: temp reference for direct view access to model
    public ShapeModel getShapeModel() { return mShapeModel; }

	private Context getContext() { return mContext; }
	private void setContext(Context context) { this.mContext = context; }

	// canvas dimensions
	public int getCanvasWidth() { return mCanvasWidth; }
	public void setCanvasWidth(int dim) { mCanvasWidth = dim; }
	public int getCanvasHeight() { return mCanvasHeight; }
	public void setCanvasHeight(int dim) { mCanvasHeight = dim; }

	public String getAlbumName() { return PrefsUtils.getPrefs(getContext(), PrefsUtils.ALBUMNAME_KEY); }
	public Boolean setAlbumName(String albumName)
	{
		if (!getAlbumName().equals(albumName)) {
			mShapeModel.delete(TEMP_FILE);
			Log.v(TAG, "setAlbumName deletes " + TEMP_FILE);
		}
		// TODO: if project folder has not changed
//		String albumName = PrefsUtils.getPrefs(mContext, PrefsUtils.ALBUMNAME_KEY);
//		if ( albumName.equals(mSketchViewModel.getAlbumName())) {
//			// load ShapeModel shape list
//			mShapeModel.load(temp);
//			Log.v(TAG, "onCreate loading: " + mSketchViewModel.getAlbumName());
//		}
//		else {
//			// reset ShapeModel shape list based on obsolete project folder
//			mSketchViewModel.setAlbumName(albumName);
//			// delete ShapeModel shape list
//			mShapeModel.delete(temp);
//			Log.v(TAG, "onCreate reset to: " + mSketchViewModel.getAlbumName());
//		}

		PrefsUtils.setPrefs(getContext(), PrefsUtils.ALBUMNAME_KEY, albumName);
		return true;
	}
	public ShapeType getShape() { return mShape; }
	public Tool getTool() { return mTool; }
	public float getSize() { 
		if (mStyleSize == Style.SMALL) {
			return mSizeSmall;
		}
		else if (mStyleSize == Style.MEDIUM) {
			return mSizeMedium;
		}
		else {
			return mSizeLarge;	
		}
	}
	public Paint.Style getStyle() { 
		if (mStyleFill == SketchViewModel.Style.STROKE) {
			return Paint.Style.STROKE;
		}
		else if (mStyleFill == SketchViewModel.Style.FILL) {
			return Paint.Style.FILL;
		}
		else {
			return Paint.Style.FILL_AND_STROKE;
		}
	}
	public int getColor() { return mColor; }
	public int getCustomColor() { return mCustomColor; }
	public void setCustomColor(int color) { mCustomColor = color; }
	
	public boolean getFocusHold() { return mFocusHold; }
	private void setFocusHold(boolean focusHold) { mFocusHold = focusHold; }

	///////////////////////////////////////////////////////////////////////////
	// helpers
	public Boolean isSketchDefined() {
		// if current shape list contains more than BG rect
		if (mShapeModel.getShapeList().size() > 1) return true;
		return false;
	}
	public Boolean isRectFocus() {
		int focus = mShapeModel.getShapeListFocus();
		if (mShapeModel.isShapeType(ShapeType.RECT, focus)) return true;
		return false;
	}
    ///////////////////////////////////////////////////////////////////////////////
    // ensure menuResId is valid (e.g. onStop may invalidate)
    public boolean isValidCheckMenuResId(int menuResId) {
        if (menuResId != R.menu.sketch_shape_menu &&
                menuResId != R.menu.sketch_style_menu &&
                menuResId != R.menu.sketch_color_menu ) {
            return false;
        }
        return true;
    }
    public boolean isValidMenuResId(int menuResId) {
        if (menuResId != R.menu.sketch_file_menu &&
                menuResId != R.menu.sketch_shape_menu &&
                menuResId != R.menu.sketch_style_menu &&
                menuResId != R.menu.sketch_color_menu &&
                menuResId != R.menu.sketch_erase_menu  ) {
            return false;
        }
        return true;
    }
	///////////////////////////////////////////////////////////////////////////////
	// menu handlers
	public Boolean setSelection(String itemname) {
		// map item to action & indicate failure if invalid
		int action = mapItemToSelection(itemname);
		Log.v(TAG,"");
		if (action < 0) return false;

		// service action
		switch (action) {
			case SELECT_TYPE_SHAPE_FREE:
				mShape = ShapeType.FREE;
				break;
			case SELECT_TYPE_SHAPE_LINE:
				mShape = ShapeType.LINE;
				break;
			case SELECT_TYPE_SHAPE_RECT:
				mShape = ShapeType.RECT;
				break;
			case SELECT_TYPE_SHAPE_LABEL:
				mShape = ShapeType.LABEL;
				break;
			case SELECT_TYPE_SHAPE_CIRCLE:
				mShape = ShapeType.CIRCLE;
				break;
			case SELECT_TYPE_SHAPE_OVAL:
				mShape = ShapeType.OVAL;
				break;
            case SELECT_TYPE_STYLE_SMALL:
                mStyleSize = Style.SMALL;
                return true;
            case SELECT_TYPE_STYLE_MEDIUM:
                mStyleSize = Style.MEDIUM;
                return true;
            case SELECT_TYPE_STYLE_LARGE:
                mStyleSize = Style.LARGE;
                return true;
            case SELECT_TYPE_STYLE_FILL:
                mStyleFill = Style.FILL;
                return true;
            case SELECT_TYPE_STYLE_STROKE:
                mStyleFill = Style.STROKE;
                return true;
            case SELECT_TYPE_STYLE_STROKE_FILL:
                mStyleFill = Style.STROKEFILL;
                return true;
            case SELECT_TYPE_STYLE_HOLD_FOCUS:
                mStyleFocus = Style.FOCUS;
                setFocusHold(!getFocusHold());
                return true;
            case SELECT_TYPE_COLOR_BLACK:
                mColor = mPalette[Palette.BLACK.ordinal()];
                return true;
            case SELECT_TYPE_COLOR_BLUE:
                mColor = mPalette[Palette.BLUE.ordinal()];
                return true;
            case SELECT_TYPE_COLOR_GREEN:
                mColor = mPalette[Palette.GREEN.ordinal()];
                return true;
            case SELECT_TYPE_COLOR_YELLOW:
                mColor = mPalette[Palette.YELLOW.ordinal()];
                return true;
            case SELECT_TYPE_COLOR_ORANGE:
                mColor = mPalette[Palette.ORANGE.ordinal()];
                return true;
            case SELECT_TYPE_COLOR_RED:
                mColor = mPalette[Palette.RED.ordinal()];
                return true;
            case SELECT_TYPE_COLOR_VIOLET:
                mColor = mPalette[Palette.VIOLET.ordinal()];
                return true;
            case SELECT_TYPE_COLOR_WHITE:
                mColor = mPalette[Palette.WHITE.ordinal()];
                return true;
            case SELECT_TYPE_COLOR_CUSTOM:
                // launch ColorPickerDialog
//				mColor = 0xFF888888;
                launchColorPickerDialog();
                return true;
            case SELECT_TYPE_TOOL_PEN:
                mTool = Tool.PEN;
                return true;
            case SELECT_TYPE_TOOL_BRUSH:
                mTool = Tool.BRUSH;
                return true;
            case SELECT_TYPE_TOOL_SPRAY:
                mTool = Tool.SPRAY;
                return true;
            case SELECT_TYPE_TOOL_BUCKET:
                mTool = Tool.BUCKET;
                return true;
			default:
				Log.e(TAG, "Ooops! setSelection finds unknown item " + action);
				return false;
		}
		// TODO: upadte & invalidate on selection?
		mShapeModel.updatePaint();
		mSketchView.invalidate();

		return true;
	}
	///////////////////////////////////////////////////////////////////////////////
	public Boolean actionFileNew() {
        // clear ShapeModel shape list & associated temp file
		mShapeModel.initShapeList();
        mShapeModel.delete(TEMP_FILE);
        // clear sketch canvas
        mSketchView.invalidate();
		return true;
	}
	///////////////////////////////////////////////////////////////////////////////
	public Boolean actionFileLoadBackdrop(String imagePath) {
		// set backdrop image
		mShapeModel.setImageShape(imagePath, 0);
		mSketchView.invalidate();
		return true;
	}
	///////////////////////////////////////////////////////////////////////////////
	public Boolean actionFileLoadOverlay(String imagePath) {
		// if rect selected, set image to selected rect shape
		int focus = mShapeModel.getShapeListFocus();
		Log.v(TAG, "actionFileLoadOverlay focus shape inx: " + focus);
		if (mShapeModel.isShapeType(ShapeType.RECT, focus)) {
			// set image as OVERLAY (focus)
			mShapeModel.setImageShape(imagePath, focus);
		}
		else {
			Log.e(TAG, "actionFileLoadOverlay OVERLAY failure - focus (" + focus + ") is not RECT. ");
		}
		mSketchView.invalidate();
		return true;
	}
    ///////////////////////////////////////////////////////////////////////////////
    public Boolean actionFileSaveSketch() {
        Log.v(TAG, "actionFileSaveSketch...");
        Bitmap bitmap = mSketchView.getCanvasBitmap();
        saveSketch(bitmap);
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////
    public Boolean actionEraseBackdrop() {
        Log.v(TAG, "actionEraseBackdrop...");
        // if 1st shape is image, assume backdrop & clear
        if (mShapeModel.isShapeType(ShapeType.IMAGE, ShapeModel.BACKDROP_IMAGE_INX)) {
            mShapeModel.clearShape(ShapeModel.BACKDROP_IMAGE_INX);
        }
        else {
            Toast.makeText(mContext, R.string.sketch_no_backdrop_toast, Toast.LENGTH_LONG).show();
        }
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////
    public Boolean actionEraseOverlay() {
        Log.v(TAG, "actionEraseOverlay...");
        // if focus is image, revert to rect
        int focus = mShapeModel.getShapeListFocus();
        if (mShapeModel.isShapeType(ShapeType.IMAGE, focus)) {
            mShapeModel.revertShapeToRect(focus);
        }
        else {
            Toast.makeText(mContext, R.string.sketch_no_overlay_toast, Toast.LENGTH_LONG).show();
        }
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////
    public Boolean actionEraseSelection() {
        Log.v(TAG, "actionEraseSelection...");
        // if shape is selected, clear focus shape
        int focus = mShapeModel.getShapeListFocus();
        if (focus != ShapeModel.NOFOCUS) {
            mShapeModel.clearShape(focus);
        }
        else {
            Toast.makeText(mContext, R.string.sketch_no_selection_toast, Toast.LENGTH_LONG).show();
        }
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////
    public Boolean actionEraseLastShape() {
        Log.v(TAG, "actionEraseLastShape...");
			// clear last shape
			int lastInx = mShapeModel.getShapeList().size()-1;
			if (!mShapeModel.clearShape(lastInx)) {
				Toast.makeText(mContext, R.string.sketch_empty_list_toast, Toast.LENGTH_LONG).show();
			}
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////
    public Boolean actionEraseAll() {
        Log.v(TAG, "actionEraseAll...");
        // clear sketch canvas
        mShapeModel.initShapeList();
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
	private String saveSketch (Bitmap bitmap) {

		String imageName = "nada";
		try {
			String albumName = PrefsUtils.getPrefs(mContext, PrefsUtils.ALBUMNAME_KEY);
			// timestamp an image name & create the file
			imageName = ImageAlbumStorage.timestampImageName();
			Log.v(TAG, "saveSketch timestampImageName: "+ imageName);

			// write to project album
			String imagePath = ImageAlbumStorage.addBitmapToMediaDB((Activity)getContext(), bitmap, albumName, imageName);
			Log.v(TAG, "saveSketch added imagePath: "+ imagePath);
			// retain latest saved image
			PrefsUtils.setPrefs(mContext, PrefsUtils.IMAGEPATH_KEY, imagePath);

			Log.v(TAG, "Sketch saved to " + albumName);
			Toast.makeText(mContext, "Sketch saved to " + albumName, Toast.LENGTH_LONG).show();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return imageName;
	}
	///////////////////////////////////////////////////////////////////////////////



//	public boolean setMenuSelection(int menuResId, MenuItem item) {
//
//        if (!isValidMenuResId(menuResId)) {
//            Log.e(TAG, "checkMenuSelections sees invalid menuResId.");
//            return false;
//        }
//
//        item.setChecked(true);
//		Toast.makeText(getContext(), item.toString(), Toast.LENGTH_LONG).show();
//
//		switch (menuResId) {
//		case R.menu.sketch_shape_menu:
//			switch (item.getItemId()) {
//			case R.id.action_sketch_shape_free:
//				mShape = ShapeType.FREE;
//				return true;
//			case R.id.action_sketch_shape_line:
//				mShape = ShapeType.LINE;
//				return true;
//			case R.id.action_sketch_shape_rect:
//				mShape = ShapeType.RECT;
//				return true;
//			case R.id.action_sketch_shape_label:
//				mShape = ShapeType.LABEL;
//				return true;
//			case R.id.action_sketch_shape_circle:
//				mShape = ShapeType.CIRCLE;
//				return true;
//			case R.id.action_sketch_shape_oval:
//				mShape = ShapeType.OVAL;
//				return true;
//			default:
//				Log.e(TAG, "setMenuSelection invalid item " + item.getItemId());
//				return false;
//			}
//		case R.menu.sketch_tool_menu:
//			switch (item.getItemId()) {
//			case R.id.action_sketch_tool_pen:
//				mTool = Tool.PEN;
//				return true;
//			case R.id.action_sketch_tool_brush:
//				mTool = Tool.BRUSH;
//				return true;
//			case R.id.action_sketch_tool_spray:
//				mTool = Tool.SPRAY;
//				return true;
//			case R.id.action_sketch_tool_bucket:
//				mTool = Tool.BUCKET;
//				return true;
//			default:
//				Log.e(TAG, "setMenuSelection invalid item " + item.getItemId());
//				return false;
//			}
//		case R.menu.sketch_style_menu:
//			switch (item.getItemId()) {
//			case R.id.action_sketch_style_small:
//				mStyleSize = Style.SMALL;
//				return true;
//			case R.id.action_sketch_style_medium:
//				mStyleSize = Style.MEDIUM;
//				return true;
//			case R.id.action_sketch_style_large:
//				mStyleSize = Style.LARGE;
//				return true;
//			case R.id.action_sketch_style_fill:
//				mStyleFill = Style.FILL;
//				return true;
//			case R.id.action_sketch_style_stroke:
//				mStyleFill = Style.STROKE;
//				return true;
//			case R.id.action_sketch_style_strokefill:
//				mStyleFill = Style.STROKEFILL;
//				return true;
//			case R.id.action_sketch_style_focus:
//				mStyleFocus = Style.FOCUS;
//				setFocusHold(!getFocusHold());
//				return true;
//			default:
//				Log.e(TAG, "setMenuSelection menu " + menuResId + " invalid item " + item.getItemId());
//				return false;
//			}
//		case R.menu.sketch_color_menu:
//			switch (item.getItemId()) {
//			case R.id.action_sketch_color_black:
//				mColor = mPalette[Palette.BLACK.ordinal()];
//				return true;
//			case R.id.action_sketch_color_blue:
//				mColor = mPalette[Palette.BLUE.ordinal()];
//				return true;
//			case R.id.action_sketch_color_green:
//				mColor = mPalette[Palette.GREEN.ordinal()];
//				return true;
//			case R.id.action_sketch_color_yellow:
//				mColor = mPalette[Palette.YELLOW.ordinal()];
//				return true;
//			case R.id.action_sketch_color_orange:
//				mColor = mPalette[Palette.ORANGE.ordinal()];
//				return true;
//			case R.id.action_sketch_color_red:
//				mColor = mPalette[Palette.RED.ordinal()];
//				return true;
//			case R.id.action_sketch_color_violet:
//				mColor = mPalette[Palette.VIOLET.ordinal()];
//				return true;
//			case R.id.action_sketch_color_white:
//				mColor = mPalette[Palette.WHITE.ordinal()];
//				return true;
//			case R.id.action_sketch_color_custom:
//				// launch ColorPickerDialog
////				mColor = 0xFF888888;
//				launchColorPickerDialog();
//				return true;
//			default:
//				Log.e(TAG, "setMenuSelection menu " + menuResId + " invalid item " + item.getItemId());
//				return false;
//			}
//		default:
//			Log.e(TAG, "setMenuSelection invalid menu " + menuResId);
//			return false;
//		}
//	}

    ///////////////////////////////////////////////////////////////////////////////
	public void checkMenuSelections(int menuResId, Menu menu) {
        // ensure mPopupMenuResId defined (e.g. onStop may invalidate)
        if (!isValidMenuResId(menuResId)) {
            Log.e(TAG, "checkMenuSelections sees invalid menuResId.");
            return;
        }
		// determine size of menu
		int itemMax = menu.size();
		Log.d(TAG, "checkMenuSelections: menu size " + itemMax);
		// switch on menu
		switch (menuResId) {
		// shape menu
		case R.menu.sketch_shape_menu: 
			// for each menu item, set checked if setting matches
			for (int index = 0; index < itemMax; index++) {
				MenuItem item = menu.getItem(index);
				switch (item.getItemId()) {
				case R.id.action_sketch_shape_free:
					if (mShape == ShapeType.FREE) item.setChecked(true);
					break;
				case R.id.action_sketch_shape_line:
					if (mShape == ShapeType.LINE) item.setChecked(true);
					break;
				case R.id.action_sketch_shape_rect:
					if (mShape == ShapeType.RECT) item.setChecked(true);
					break;
				case R.id.action_sketch_shape_label:
					if (mShape == ShapeType.LABEL) item.setChecked(true);
					break;
				case R.id.action_sketch_shape_circle:
					if (mShape == ShapeType.CIRCLE) item.setChecked(true);
					break;
				case R.id.action_sketch_shape_oval:
					if (mShape == ShapeType.OVAL) item.setChecked(true);
					break;
				default:
					Log.e(TAG, "checkMenuSelections menu " + menuResId + " invalid item " + item.getItemId());
					break;
				}
			}
			break;
		// tool menu
		case R.menu.sketch_tool_menu: 
			// for each menu item, set checked if setting matches
			for (int index = 0; index < itemMax; index++) {
				MenuItem item = menu.getItem(index);
				switch (item.getItemId()) {
				case R.id.action_sketch_tool_pen:
					if (mTool == Tool.PEN) item.setChecked(true);
					break;
				case R.id.action_sketch_tool_brush:
					if (mTool == Tool.BRUSH) item.setChecked(true);
					break;
				case R.id.action_sketch_tool_spray:
					if (mTool == Tool.SPRAY) item.setChecked(true);
					break;
				case R.id.action_sketch_tool_bucket:
					if (mTool == Tool.BUCKET) item.setChecked(true);
					break;
				default:
					Log.e(TAG, "checkMenuSelections menu " + menuResId + " invalid item " + item.getItemId());
					break;
				}
			}
			break;
		// style menu
		case R.menu.sketch_style_menu: 
			// for each menu item, set checked if setting matches
			for (int index = 0; index < itemMax; index++) {
				MenuItem item = menu.getItem(index);
				item.setChecked(false);
				switch (item.getItemId()) {
				case R.id.action_sketch_style_small:
					if (mStyleSize == Style.SMALL) item.setChecked(true);
					break;
				case R.id.action_sketch_style_medium:
					if (mStyleSize == Style.MEDIUM) item.setChecked(true);
					break;
				case R.id.action_sketch_style_large:
					if (mStyleSize == Style.LARGE) item.setChecked(true);
					break;
				case R.id.action_sketch_style_fill:
					if (mStyleFill == Style.FILL) item.setChecked(true);
					break;
				case R.id.action_sketch_style_stroke:
					if (mStyleFill == Style.STROKE) item.setChecked(true);
					break;
				case R.id.action_sketch_style_strokefill:
					if (mStyleFill == Style.STROKEFILL) item.setChecked(true);
					break;
				case R.id.action_sketch_style_focus:
					if (mStyleFocus == Style.FOCUS) item.setChecked(getFocusHold());
					break;
				default:
					Log.e(TAG, "checkMenuSelections menu " + menuResId + " invalid item " + item.getItemId());
					break;
				}
			}
			break;
		case R.menu.sketch_color_menu: 
			Log.d(TAG, "checkMenuSelections: mColor " + mColor);
			// for each menu item, set checked if setting matches
			for (int index = 0; index < itemMax; index++) {
				MenuItem item = menu.getItem(index);
				Log.d(TAG, "checkMenuSelections: menu item " + item.toString());
				item.setChecked(false);
				switch (item.getItemId()) {
				case R.id.action_sketch_color_black:
					Log.d(TAG, "checkMenuSelections: BLACK checked w/ mColor " + mColor);
					if (mColor == mPalette[Palette.BLACK.ordinal()]) item.setChecked(true);
					break;
				case R.id.action_sketch_color_blue:
					if (mColor == mPalette[Palette.BLUE.ordinal()]) item.setChecked(true);
					break;
				case R.id.action_sketch_color_green:
					if (mColor == mPalette[Palette.GREEN.ordinal()]) item.setChecked(true);
					break;
				case R.id.action_sketch_color_yellow:
					if (mColor == mPalette[Palette.YELLOW.ordinal()]) item.setChecked(true);
					break;
				case R.id.action_sketch_color_orange:
					if (mColor == mPalette[Palette.ORANGE.ordinal()]) item.setChecked(true);
					break;
				case R.id.action_sketch_color_red:
					Log.d(TAG, "checkMenuSelections: mColor " + mColor + " Palette " + mPalette[Palette.RED.ordinal()]);
					if (mColor == mPalette[Palette.RED.ordinal()]) item.setChecked(true);
					break;
				case R.id.action_sketch_color_violet:
					if (mColor == mPalette[Palette.VIOLET.ordinal()]) item.setChecked(true);
					break;
				case R.id.action_sketch_color_white:
					if (mColor == mPalette[Palette.WHITE.ordinal()]) item.setChecked(true);
					break;
				case R.id.action_sketch_color_custom:
					boolean checked = false;
					int i = Palette.NADA.ordinal()+1;
					while (!checked && i < Palette.MAX.ordinal()) {
						if (mColor == mPalette[i]) checked = true;
						++i;
					}
					if (!checked) {
						item.setChecked(true);
					}
					break;
				default:
					Log.e(TAG, "checkMenuSelections menu " + menuResId + " invalid item " + item.getItemId());
					break;
				}
			}
			break;
		default:
			Log.e(TAG, "checkMenuSelections invalid menu " + menuResId);
			break;
		}
	}
	///////////////////////////////////////////////////////////////////////////////
	private void launchColorPickerDialog() {
//		int color = 0xFF008800;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        int initialValue = prefs.getInt(mKeyCustomColor, 0xFF000000);
        
        Log.d("mColorPicker", "initial value:" + initialValue);
                        
        final ColorPickerDialog colorDialog = new ColorPickerDialog(getContext(), initialValue);
        
        colorDialog.setAlphaSliderVisible(true);
        colorDialog.setTitle("Pick your Color!");
        
        colorDialog.setButton(DialogInterface.BUTTON_POSITIVE, getContext().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getContext(), "Selected Color: " + colorToHexString(colorDialog.getColor()), Toast.LENGTH_LONG).show();
                // capture custom color selection
                mColor = colorDialog.getColor();
                setCustomColor(mColor);
                        
                //Save the value in our preferences.
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(mKeyCustomColor, colorDialog.getColor());
                editor.commit();
                // invoke parent to update the color of a shape with focus
				SketchActivity sketchActivity = (SketchActivity)getContext();
				sketchActivity.updatePaint();
            }
        });
        
        colorDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                        //Nothing to do here.
                }
        });
        
        colorDialog.show();
        
//		return color;

	}
		
	private String colorToHexString(int color) {
	        return String.format("#%06X", 0xFFFFFFFF & color);
	}
	///////////////////////////////////////////////////////////////////////////////
	// map menu items to action code
	public int mapItemToAction(String itemname) {

		if (itemname.equals(getContext().getString(R.string.action_camera))) {
			return ACTION_TYPE_CAMERA;
		}
		else if (itemname.equals(getContext().getString(R.string.action_gallery))) {
			return ACTION_TYPE_GALLERY;
		}
		else if (itemname.equals(getContext().getString(R.string.action_sketch_file_new))) {
			return ACTION_TYPE_FILE_NEW;
		}
		else if (itemname.equals(getContext().getString(R.string.action_sketch_file_loadbackdrop))) {
			return ACTION_TYPE_FILE_LOADBACKDROP;
		}
		else if (itemname.equals(getContext().getString(R.string.action_sketch_file_loadoverlay))) {
			return ACTION_TYPE_FILE_LOADOVERLAY;
		}
		else if (itemname.equals(getContext().getString(R.string.action_sketch_file_savesketch))) {
			return ACTION_TYPE_FILE_SAVE_SKETCH;
		}
        Log.e(TAG, "Ooops!  mapItemToAction finds unknown item " + itemname);
        return ACTION_TYPE_UNKNOWN;
	}
    ///////////////////////////////////////////////////////////////////////////////
    // map menu items to action code
    public int mapItemToSelection(String itemname) {

        if (itemname.equals(getContext().getString(R.string.action_sketch_shape_free))) {
            return SELECT_TYPE_SHAPE_FREE;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_shape_line))) {
            return SELECT_TYPE_SHAPE_LINE;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_shape_rect))) {
            return SELECT_TYPE_SHAPE_RECT;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_shape_label))) {
            return SELECT_TYPE_SHAPE_LABEL;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_shape_circle))) {
            return SELECT_TYPE_SHAPE_CIRCLE;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_shape_oval))) {
            return SELECT_TYPE_SHAPE_OVAL;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_style_small))) {
            return SELECT_TYPE_STYLE_SMALL;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_style_medium))) {
            return SELECT_TYPE_STYLE_MEDIUM;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_style_large))) {
            return SELECT_TYPE_STYLE_LARGE;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_style_fill))) {
            return SELECT_TYPE_STYLE_FILL;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_style_stroke))) {
            return SELECT_TYPE_STYLE_STROKE;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_style_strokefill))) {
            return SELECT_TYPE_STYLE_STROKE_FILL;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_style_focus))) {
            return SELECT_TYPE_STYLE_HOLD_FOCUS;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_color_black))) {
            return SELECT_TYPE_COLOR_BLACK;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_color_blue))) {
            return SELECT_TYPE_COLOR_BLUE;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_color_green))) {
            return SELECT_TYPE_COLOR_GREEN;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_color_yellow))) {
            return SELECT_TYPE_COLOR_YELLOW;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_color_orange))) {
            return SELECT_TYPE_COLOR_ORANGE;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_color_red))) {
            return SELECT_TYPE_COLOR_RED;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_color_violet))) {
            return SELECT_TYPE_COLOR_VIOLET;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_color_white))) {
            return SELECT_TYPE_COLOR_WHITE;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_tool_pen))) {
            return SELECT_TYPE_TOOL_PEN;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_tool_brush))) {
            return SELECT_TYPE_TOOL_BRUSH;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_tool_spray))) {
            return SELECT_TYPE_TOOL_SPRAY;
        }
        else if (itemname.equals(getContext().getString(R.string.action_sketch_tool_bucket))) {
            return SELECT_TYPE_TOOL_BUCKET;
        }
        Log.e(TAG, "Ooops!  mapItemToAction finds unknown item " + itemname);
        return SELECT_TYPE_UNKNOWN;

    }
}
