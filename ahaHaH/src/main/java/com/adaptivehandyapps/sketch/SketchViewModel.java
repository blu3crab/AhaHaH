// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.sketch;

import afzkl.development.colorpickerview.dialog.ColorPickerDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.adaptivehandyapps.ahahah.R;
import com.adaptivehandyapps.util.AhaDisplayMetrics;
import com.adaptivehandyapps.util.PrefsUtils;

public class SketchViewModel {
	private static final String TAG = "SketchViewModel";

	// activity context
	private Context mContext;

	//////////////settings//////////////////////////
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

	/////////////save/restore settings////////////////
	private int MODE_PRIVATE = 0;
	public static final String PREFS_NAME = "SketchSettingsFile";
	private String mKeyShape = "Shape";
	private String mKeyTool = "Tool";
	private String mKeyStyleSize = "StyleSize";
	private String mKeyStyleFill = "StyleFill";
	private String mKeyAlbum = "Album";
	private String mKeyColor = "Color";
	private String mKeyCustomColor = "CustomColor";
	private String mKeyFocusHold = "FocusHold";

	// canvas dimensions
	private int mCanvasWidth = -1;
	private int mCanvasHeight = -1;

	//////////////////////////////////////////////////////////////////////////////////////////
	private static volatile SketchViewModel instance;

	public synchronized static SketchViewModel getInstance(Context c)
	{
		if (instance == null){
			synchronized (SketchViewModel.class) {   // Check for the second time.
				//if there is no instance available... create new one
				if (instance == null){
					instance = new SketchViewModel(c);
				}
			}
		}

		return instance;
	}
	///////////////////////////////////////////////////////////////////////////////
    // constructor
	public SketchViewModel(Context context) {
		setContext(context);
		// set canvas dimensions to display dimensions until touch view canvas created
		mCanvasWidth = AhaDisplayMetrics.getDisplayWidth(getContext());
		mCanvasHeight = AhaDisplayMetrics.getDisplayHeight(getContext());
		// restore settings
		restoreSketchSettings();
	}
	//
	/////////////save, restore, menu methods////////////////
	//
//	// save sketch settings
//	public void saveSketchSettings() {
//		SharedPreferences settings = getContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
//	    SharedPreferences.Editor editor = settings.edit();
////	    editor.remove(PREFS_NAME);
//
//	    editor.putInt(mKeyShape, mShape.ordinal());
//	    editor.putInt(mKeyTool, mTool.ordinal());
//	    editor.putInt(mKeyStyleSize, mStyleSize.ordinal());
//	    editor.putInt(mKeyStyleFill, mStyleFill.ordinal());
//	    editor.putInt(mKeyColor, mColor);
//	    editor.putInt(mKeyCustomColor, mCustomColor);
//	    editor.putBoolean(mKeyFocusHold, mFocusHold);
//	    editor.putString(mKeyAlbum, mAlbumName);
//
//	    // commit the edits!
//	    editor.commit();
//
//	}
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
	}

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
	/////////////getters////////////////
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
	public boolean setMenuSelection(int menuResId, MenuItem item) {

        if (!isValidMenuResId(menuResId)) {
            Log.e(TAG, "checkMenuSelections sees invalid menuResId.");
            return false;
        }

        item.setChecked(true);
		Toast.makeText(getContext(), item.toString(), Toast.LENGTH_LONG).show();

		switch (menuResId) {
		case R.menu.sketch_shape_menu: 
			switch (item.getItemId()) {
			case R.id.action_sketch_shape_free:
				mShape = ShapeType.FREE;
				return true;
			case R.id.action_sketch_shape_line:
				mShape = ShapeType.LINE;
				return true;
			case R.id.action_sketch_shape_rect:
				mShape = ShapeType.RECT;
				return true;
			case R.id.action_sketch_shape_label:
				mShape = ShapeType.LABEL;
				return true;
			case R.id.action_sketch_shape_circle:
				mShape = ShapeType.CIRCLE;
				return true;
			case R.id.action_sketch_shape_oval:
				mShape = ShapeType.OVAL;
				return true;
			default:
				Log.e(TAG, "setMenuSelection invalid item " + item.getItemId());
				return false;
			}
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
		case R.menu.sketch_style_menu: 
			switch (item.getItemId()) {
			case R.id.action_sketch_style_small:
				mStyleSize = Style.SMALL;
				return true;
			case R.id.action_sketch_style_medium:
				mStyleSize = Style.MEDIUM;
				return true;
			case R.id.action_sketch_style_large:
				mStyleSize = Style.LARGE;
				return true;
			case R.id.action_sketch_style_fill:
				mStyleFill = Style.FILL;
				return true;
			case R.id.action_sketch_style_stroke:
				mStyleFill = Style.STROKE;
				return true;
			case R.id.action_sketch_style_strokefill:
				mStyleFill = Style.STROKEFILL;
				return true;
			case R.id.action_sketch_style_focus:
				mStyleFocus = Style.FOCUS;
				setFocusHold(!getFocusHold());
				return true;
			default:
				Log.e(TAG, "setMenuSelection menu " + menuResId + " invalid item " + item.getItemId());
				return false;
			}
		case R.menu.sketch_color_menu: 
			switch (item.getItemId()) {
			case R.id.action_sketch_color_black:
				mColor = mPalette[Palette.BLACK.ordinal()];
				return true;
			case R.id.action_sketch_color_blue:
				mColor = mPalette[Palette.BLUE.ordinal()];
				return true;
			case R.id.action_sketch_color_green:
				mColor = mPalette[Palette.GREEN.ordinal()];
				return true;
			case R.id.action_sketch_color_yellow:
				mColor = mPalette[Palette.YELLOW.ordinal()];
				return true;
			case R.id.action_sketch_color_orange:
				mColor = mPalette[Palette.ORANGE.ordinal()];
				return true;
			case R.id.action_sketch_color_red:
				mColor = mPalette[Palette.RED.ordinal()];
				return true;
			case R.id.action_sketch_color_violet:
				mColor = mPalette[Palette.VIOLET.ordinal()];
				return true;
			case R.id.action_sketch_color_white:
				mColor = mPalette[Palette.WHITE.ordinal()];
				return true;
			case R.id.action_sketch_color_custom:
				// launch ColorPickerDialog
//				mColor = 0xFF888888;
				launchColorPickerDialog();
				return true;
			default:
				Log.e(TAG, "setMenuSelection menu " + menuResId + " invalid item " + item.getItemId());
				return false;
			}
		default:
			Log.e(TAG, "setMenuSelection invalid menu " + menuResId);
			return false;
		}
	}

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
}
