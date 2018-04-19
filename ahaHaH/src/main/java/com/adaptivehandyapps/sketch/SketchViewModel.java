// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.sketch;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.adaptivehandyapps.ahahah.R;
import com.adaptivehandyapps.util.AhaDisplayMetrics;
import com.adaptivehandyapps.util.ImageAlbumStorage;
import com.adaptivehandyapps.util.PrefsUtils;

import java.util.List;

public class SketchViewModel {
	private static final String TAG = "SketchViewModel";

	// activity context
	private Context mContext;

	///////////////////////////////////////////////////////////////////////////
	// action types
    public static final int ACTION_TYPE_UNKNOWN = -1;

    public static final int ACTION_TYPE_START_SHAPE = 0;
    public static final int ACTION_TYPE_START_MOVE = 1;
    public static final int ACTION_TYPE_REFINE_SHAPE = 2;
    public static final int ACTION_TYPE_REFINE_MOVE = 3;
    public static final int ACTION_TYPE_RESIZE_SHAPE = 4;
    public static final int ACTION_TYPE_COMPLETE_SHAPE = 5;

    // selection types
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
	private ShapeModel mShapeModel;		// shape model
	private SketchView mSketchView;		// sketch view

    // focus
    public static final int NOFOCUS = -1;
    private int mShapeListFocus;

    ///////////////////////////////////////////////////////////////////////////
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

	///////////////////////////////////////////////////////////////////////////
	// save/restore settings
	private int MODE_PRIVATE = 0;
	public static final String PREFS_NAME = "SketchSettingsFile";
	private String mKeyShape = "Shape";
	private String mKeyTool = "Tool";
	private String mKeyStyleSize = "StyleSize";
	private String mKeyStyleFill = "StyleFill";
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

		// get references to view and model
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

	    editor.putInt(mKeyShape, mShape.ordinal());
	    editor.putInt(mKeyTool, mTool.ordinal());
	    editor.putInt(mKeyStyleSize, mStyleSize.ordinal());
	    editor.putInt(mKeyStyleFill, mStyleFill.ordinal());
	    editor.putInt(mKeyColor, mColor);
	    editor.putInt(mKeyCustomColor, mCustomColor);
	    editor.putBoolean(mKeyFocusHold, mFocusHold);

	    // commit the edits!
	    editor.commit();

		// save ShapeModel shape list
		mShapeModel.save(TEMP_FILE);

	}
	///////////////////////////////////////////////////////////////////////////
	// restore sketch settings
	public void restoreSketchSettings() {
		SharedPreferences settings = getContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

		String albumName = PrefsUtils.getPrefs(getContext(), PrefsUtils.ALBUMNAME_KEY);
		if (albumName.equals(PrefsUtils.DEFAULT_STRING_NADA)) setAlbumName(getContext().getString(R.string.default_project_name));

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
//				mStyleFocus == Style.NADA ||
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
	private Context getContext() { return mContext; }
	private void setContext(Context context) { this.mContext = context; }

    public Boolean initShapeList() { return mShapeModel.initShapeList(); }
    public List<ShapeObject> getShapeList () { return mShapeModel.getShapeList(); }
    public Boolean updatePaint() { return mShapeModel.updatePaint(); }

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
    public int setColor(int color) { mColor = color; return mColor; }
	public int getCustomColor() { return mCustomColor; }
	public void setCustomColor(int color) {
	    mCustomColor = color;
	    setColor(mCustomColor);
        //Save the value in our preferences.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(mKeyCustomColor, mCustomColor);
        editor.commit();
        // refresh
        mShapeModel.updatePaint();
        mSketchView.invalidate();
    }
	
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
        int focus = getShapeListFocus();
		if (mShapeModel.isShapeType(ShapeType.RECT, focus)) return true;
		return false;
	}
	///////////////////////////////////////////////////////////////////////////////
	// menu handlers
	public Boolean setSelection(String itemname) {
		// map item to action & indicate failure if invalid
		int action = mapItemToSelection(itemname);
		Log.v(TAG,"");
		if (action < 0) return false;

		int preSelectionColor = mColor;
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
		// update paint if color changed
		if (preSelectionColor != mColor) mShapeModel.updatePaint();
		// invalidate to refresh after any selection
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
        int focus = getShapeListFocus();
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
        int focus = getShapeListFocus();
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
        int focus = getShapeListFocus();
        if (focus != NOFOCUS) {
            mShapeModel.clearShape(focus);
            mSketchView.invalidate();
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
			if (mShapeModel.clearShape(lastInx)) {
                mSketchView.invalidate();
            }
            else {
                Toast.makeText(mContext, R.string.sketch_empty_list_toast, Toast.LENGTH_LONG).show();
			}
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////
    public Boolean actionEraseAll() {
        Log.v(TAG, "actionEraseAll...");
        // clear sketch canvas
        mShapeModel.initShapeList();
        mSketchView.invalidate();
        return true;
    }
    ///////////////////////////////////////////////////////////////////////////////
    public Boolean actionViewTouch(int actionType, float touchX, float touchY, float scaleFactor) {
        Log.v(TAG, "actionViewTouch actionType " + actionType + ", X/Y " + touchX + "/" + touchY);
        // action type
        switch (actionType) {
            case ACTION_TYPE_START_SHAPE:
                mShapeModel.startShape(touchX, touchY);
                break;
            case ACTION_TYPE_START_MOVE:
                mShapeModel.startMove(touchX, touchY);
                break;
            case ACTION_TYPE_REFINE_SHAPE:
                mShapeModel.refineShape(touchX, touchY);
                break;
            case ACTION_TYPE_REFINE_MOVE:
                mShapeModel.refineMove(touchX, touchY);
                break;
            case ACTION_TYPE_RESIZE_SHAPE:
                mShapeModel.resizeShape(scaleFactor);
                break;
            case ACTION_TYPE_COMPLETE_SHAPE:
                mShapeModel.completeShape(touchX, touchY);
                break;
            default:
                Log.e(TAG,"Ooops! unknown action type! " + actionType);
        }
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
        else if (itemname.equals(getContext().getString(R.string.action_sketch_color_custom))) {
            return SELECT_TYPE_COLOR_CUSTOM;
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
    ////////////////////////////////////////////////////////////////////////////
    // focus
    public int clearShapeListFocus() {
        // set focus to value
        return setShapeListFocus(NOFOCUS);
    }
    public int setShapeListFocus(int i) {
        mShapeListFocus = NOFOCUS;
        if (i >= 0 && i < mShapeModel.getShapeList().size()) {
            // set draw object
            mShapeModel.setShapeObject(mShapeModel.getShapeList().get(i));
            // set focus to value
            mShapeListFocus = i;
        }
        return mShapeListFocus;
    }
    public int setShapeListFocus(float x, float y) {
        // focus detection - test if x,y is within draw element bounding rect
        int size = mShapeModel.getShapeList().size();
        for (int i = size-1; i > 0; i--) {
            mShapeModel.setShapeObject(mShapeModel.getShapeList().get(i));
            RectF rect = mShapeModel.getShapeObject().getBound();
            if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) {
                return setShapeListFocus(i);
            }
        }
        return setShapeListFocus(mShapeModel.CANVAS_SHAPELIST_INX);
    }
    public int getShapeListFocus() {
        // return focus
        return mShapeListFocus;
    }
    public int setNextShapeListFocus() {
        // test if draw list contains shapes (in addition to background)
        if (mShapeModel.getShapeList().size() < 1) {
            Log.e(TAG, "setNextShapeListFocus empty shape list - no BG_RECT!");
            return NOFOCUS;
        }
        // set focus to next element
        if (mShapeListFocus < mShapeModel.getShapeList().size() - 1) {
            ++mShapeListFocus;
        }
        else {
            mShapeListFocus = 0;
        }
        // set draw object
        mShapeModel.setShapeObject(mShapeModel.getShapeList().get(mShapeListFocus));
        return mShapeListFocus;
    }
    public int setPrevShapeListFocus() {
        if (mShapeModel.getShapeList().size() < 1) {
            Log.e(TAG, "setPrevShapeListFocus empty shape list - no BG_RECT!");
            return NOFOCUS;
        }
        // set focus to previous element
        if (mShapeListFocus > 1) {
            --mShapeListFocus;
        }
        else {
            mShapeListFocus = mShapeModel.getShapeList().size() - 1;
        }
        // set draw object
        mShapeModel.setShapeObject(mShapeModel.getShapeList().get(mShapeListFocus));
        return mShapeListFocus;
    }
    ////////////////////////////////////////////////////////////////////////////
}
