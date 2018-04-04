// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.sketch;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.EditText;

import com.adaptivehandyapps.activity.SketchActivity;
import com.adaptivehandyapps.sketch.SketchSetting.ShapeType;
import com.adaptivehandyapps.util.AhaDisplayMetrics;

public class ShapeManager {

	private static final String TAG = "ShapeManager"; 

	// parent activity for sketch setting 
	private SketchActivity mParentActivity;
	private SketchSetting mSketchSetting;
	// working
	private ShapeObject mShapeObject;	// working shape list object
	private ShapeType mShapeType;		// working sketch shape type
	private Paint mPaint;				// working paint
	private PointF[] mPts;				// line, circle
	private RectF mRect;				// rect, round rect, oval
	private PointF mCenter;				// rect, circle center
	private float mRadius;				// circle, radius
	float[] mCircle;					// circle center x,y, radius
	private Path mPath;					// free path
	private PointF[] mMovePts;			// move points

	public static final int BACKDROP_IMAGE_INX = 1;
	// replaced with canvas dimensions
	public static final float CANVAS_MIN = 0.0f;
//	public static final float SCREEN_MAX_X = 1280.0f; // samsung tablet display & canvas width
//	public static final float SCREEN_MAX_Y = 719.0f;  // samsung tablet canvas height
//	public static final float SCREEN_MAX_Y = 800.0f;  // samsung tablet display height

	public static final float REFACTOR = 0.05f;
	public static final float SIZE_TINY = 16.0f;
	
	private float mDX;
	private float mDY;
	
	public static final int NOFOCUS = -1;
	public static final int CANVAS_SHAPELIST_INX = 0;
	private int mShapeListFocus;
	private List<ShapeObject> mShapeList = new ArrayList<ShapeObject>();
//    String mBgRectName = "BG_RECT";
    String mBgRectName = "Canvas";
    private ShapeObject shapeObject;

//	private BigInteger startTime;
//	private BigInteger doneTime;

	////////////////////////////////////////////////////////////////////////////
	// constructor
	public ShapeManager() {
		// obtain sketch settings
		mParentActivity = SketchActivity.getSketchActivity();
		mSketchSetting = mParentActivity.getSketchSettings();
		initShapeList();
	}
	////////////////////////////////////////////////////////////////////////////
	public void initShapeList() {
		// add canvas paint, background full-screen rect as 0th draw shape element
		mShapeList = new ArrayList<ShapeObject>();
		ShapeObject shapeObject = new ShapeObject();
		shapeObject.setShapeType(SketchSetting.ShapeType.RECT);
		shapeObject.setName(mBgRectName);
		Log.v(TAG, "shape name: " + mBgRectName);
		shapeObject.setFocus(new PointF(
				mParentActivity.getCanvasWidth()/2.0f, 
				mParentActivity.getCanvasHeight()/2.0f));
		shapeObject.setBound(new RectF (
				0.0f, 0.0f, 
				mParentActivity.getCanvasWidth(), mParentActivity.getCanvasHeight())); 
		Paint paintCanvas = new Paint();
		paintCanvas.setColor(Color.CYAN);
		shapeObject.setPaint(paintCanvas);
		shapeObject.setObject(shapeObject.getBound()); 
		mShapeList.add(shapeObject);

		// set draw list index to unselected
        mShapeListFocus = clearShapeListFocus();
//        mShapeListFocus = setShapeListFocus(0);
    }
	
	////////////////////////////////////////////////////////////////////////////
	// save shape list
	public boolean save (String filename) {
		Log.v(TAG, "save shape list to " + filename);     	
		// snap start time
	    long startTime = System.currentTimeMillis();

		try {	
			FileOutputStream fos = mParentActivity.openFileOutput(filename, Context.MODE_PRIVATE);
		    ObjectOutputStream oos = new ObjectOutputStream(fos);
			for (int size = mShapeList.size(), i = 0; i < size; i++) {
				mShapeObject = mShapeList.get(i);				
				mShapeObject.serialize(oos);

        		Log.v(TAG, "serialized " + mShapeObject.getShapeType());
			}
		    oos.flush();
		    oos.close();
			fos.close();
		} catch (Exception e) {
//			Log.e(TAG, "serialize exception: " + e.getCause().toString() + " - " + e.getMessage());
			Log.e(TAG, "serialize exception: " + e.getMessage());
		}
        // snap completion time
	    long doneTime = System.currentTimeMillis();
        long responseTime = doneTime - startTime; 
        Log.v(TAG, "elapsed serialization time: " + responseTime + " ms");
		return true;
	}
	////////////////////////////////////////////////////////////////////////////
	// load shape list
	public boolean load (String filename) {
		Log.v(TAG, "load shape list from " + filename); 
		// snap start time
	    long startTime = System.currentTimeMillis();
       // clear shape list
		mShapeList = new ArrayList<ShapeObject>();
		FileInputStream fis = null;
        try {
        	fis = mParentActivity.openFileInput(filename);
        	Log.v(TAG, "fis available: " + fis.available());
            ObjectInputStream ois = new ObjectInputStream(fis);
			Log.v(TAG, "ois available: " + ois.available());
        	try {
        		boolean more = true;
        		while (more) {
        			ShapeObject mShapeObject = new ShapeObject();
        			more = mShapeObject.deserialize(ois);
        			if (more) {
		    			Log.v(TAG, "deserialized " + mShapeObject.getShapeType());
		        		mShapeList.add(mShapeObject);
        			}
        		}
	        }
	        catch (Exception e) {
	            Log.e(TAG, e.getMessage());    	
	        }
			Log.v(TAG, "deserialized " + mShapeList.size() + " shape objects.");
			// invalid file format or corrupted file
			if (mShapeList.size() == 0) {
				initShapeList();
			}
			ois.close();
        } 
        catch(IOException e) {
            Log.e(TAG, "open file exception:" + e.getMessage());
        } 
        finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Log.e(TAG, "close exception: " + e.getMessage());
                }
            }
        }
        // snap completion time
	    long doneTime = System.currentTimeMillis();
	    long responseTime = doneTime - startTime; 
        Log.v(TAG, "elapsed deserialization time: " + responseTime + " ms");
		return true;
	}
	////////////////////////////////////////////////////////////////////////////
	// delete shape list
	public boolean delete (String filename) {
		Log.v(TAG, "delete shape list in " + filename); 
        try {
        	boolean success = mParentActivity.deleteFile(filename);
        	Log.v(TAG, "delete file returns: " + success);
        } 
        catch(Exception e) {
            Log.e(TAG, "delete file exception:" + e.getMessage());
        } 
		return true;
	}
	////////////////////////////////////////////////////////////////////////////
	// getters/setters, utility
	public List<ShapeObject> getShapeList () {
		// return draw list
		return mShapeList;
	}
	public boolean clearShape(int shapeInx) {
		Log.v(TAG, "clearShape shapeInx: " + shapeInx);
		if (shapeInx > 0 && shapeInx < mShapeList.size()) { 
			mShapeList.remove(shapeInx);
		}
		else {
			Log.e(TAG, "clearShape OutOfBounds...");
			return false;
		}
		mShapeListFocus = clearShapeListFocus();
		return true;
	}
	public boolean revertShapeToRect(int shapeInx) {
		Log.v(TAG, "revertShapeToRect shapeInx: " + shapeInx);
		if (shapeInx > 0 && shapeInx < mShapeList.size()) { 
			ShapeObject shapeObject = mShapeList.get(shapeInx);
			shapeObject.setShapeType(ShapeType.RECT);
			shapeObject.setObject(shapeObject.getBound());
		}
		else {
			Log.e(TAG, "revertShapeToRect OutOfBounds...");
			return false;
		}
		return true;
	}
	public boolean isShapeType(ShapeType shapeType, int shapeInx) {
		if (shapeInx > NOFOCUS && shapeInx < mShapeList.size()) {
			ShapeObject shapeObject = mShapeList.get(shapeInx);
			if (shapeObject != null &&
					shapeObject.getShapeType() == shapeType) {
				return true;
			}
		}
		return false;
	}
	////////////////////////////////////////////////////////////////////////////
	// focus
	public int clearShapeListFocus() {
		// set focus to value
		return mShapeListFocus = NOFOCUS;
	}
	public int setShapeListFocus(int i) {
		mShapeListFocus = NOFOCUS;
		if (i >= 0 && i < mShapeList.size()) {
			// set draw object
			mShapeObject = mShapeList.get(i);
			// set focus to value
			mShapeListFocus = i;
		}
		return mShapeListFocus;
	}
	public int getShapeListFocus() {
		// return focus
		return mShapeListFocus;
	}
	public int setNextShapeListFocus() {
		// test if draw list contains shapes (in addition to background)
		if (mShapeList.size() < 1) {
            Log.e(TAG, "setNextShapeListFocus empty shape list - no BG_RECT!");
			return NOFOCUS;
		}
		// set focus to next element
		if (mShapeListFocus < mShapeList.size() - 1) {
			++mShapeListFocus;
		}
		else {
//            mShapeListFocus = 1;
            mShapeListFocus = 0;
		}
		// set draw object
		mShapeObject = mShapeList.get(mShapeListFocus);
		return mShapeListFocus;
	}
	public int setPrevShapeListFocus() {
		if (mShapeList.size() < 1) {
            Log.e(TAG, "setPrevShapeListFocus empty shape list - no BG_RECT!");
			return NOFOCUS;
		}
		// set focus to previous element
		if (mShapeListFocus > 1) {
			--mShapeListFocus;
		}
		else {
			mShapeListFocus = mShapeList.size() - 1;
		}
		// set draw object
		mShapeObject = mShapeList.get(mShapeListFocus);
		return mShapeListFocus;
	}
	public int setShapeListFocus(float x, float y) {
		// focus detection - test if x,y is within draw element bounding rect
		for (int size = mShapeList.size(), i = size-1; i > 0; i--) {
			mShapeObject = mShapeList.get(i);
			mRect = mShapeObject.getBound();
			if (x >= mRect.left && x <= mRect.right && y >= mRect.top && y <= mRect.bottom) {
				return setShapeListFocus(i);
			}
		}
		return setShapeListFocus(CANVAS_SHAPELIST_INX);
	}
	////////////////////////////////////////////////////////////////////////////
	// set image type shape: BACKDROP or OVERLAY
	public ShapeObject setImageShape(String imagePath, int shapeListInx) {
		ShapeObject shapeObject = null;
		Paint paint = new Paint();
		Log.v(TAG, "setImageShape setting shape inx: " + shapeListInx + " to image path: " + imagePath);
		// set image type & path
		if (shapeListInx <= 0) {
			// BACKDROP - if position zero then create shape & insert as 1st element
			shapeObject = new ShapeObject();
			shapeObject.setShapeType(SketchSetting.ShapeType.IMAGE);
			shapeObject.setName(imagePath);
			shapeObject.setPaint(paint);
			Log.v(TAG, "BACKDROP image path: " + imagePath);

			// get device dimensions
			DisplayMetrics displayMetrics = AhaDisplayMetrics.getDisplayMetrics(mParentActivity);
			int targetDeviceW = displayMetrics.widthPixels;
			int targetDeviceH = displayMetrics.heightPixels;
			Log.v(TAG, "target device W/H: " + targetDeviceW + "/" + targetDeviceH);

			// get size of photo
			BitmapFactory.Options bmOptions = new BitmapFactory.Options();
			bmOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(imagePath, bmOptions);
			int imageW = bmOptions.outWidth;
			int imageH = bmOptions.outHeight;
			Log.v(TAG, "photo W/H: " + imageW + "/" + imageH);
			// round up since int math truncates remainder resulting scale factor 1 (FIT = FULL)
			int scaleFactor = Math.min(imageW/targetDeviceW, imageH/targetDeviceH)+1;
			Log.v(TAG, "setupView: FIT scale factor: " + scaleFactor);
			// set bitmap options to scale the image decode target
			bmOptions.inJustDecodeBounds = false;
			bmOptions.inSampleSize = scaleFactor;
			bmOptions.inPurgeable = true;

			// set bitmap
			Bitmap bitmap = BitmapFactory.decodeFile(imagePath, bmOptions);
			shapeObject.setObject(bitmap);

			// set focus to screen center
			PointF focus = new PointF(
					(float)targetDeviceW/2,
					(float)targetDeviceH/2);
			shapeObject.setFocus(focus);
			Log.v(TAG, "focus: " + shapeObject.getFocus());
			// set bounds based on canvas size around focus
			RectF rect = new RectF();
			rect.bottom = focus.y + (float)targetDeviceH/2;
			rect.left = focus.x - (float)targetDeviceW/2;
			rect.top = focus.y - (float)targetDeviceH/2;
			rect.right = focus.x + (float)targetDeviceW/2;
			shapeObject.setBound(rect);
			Log.v(TAG, "bound: " + shapeObject.getBound());

			// check if backdrop image already present
			ShapeObject testShapeObject = null;
			if (mShapeList.size()>1) {
				testShapeObject = mShapeList.get(1);
				if (testShapeObject.getShapeType() == ShapeType.IMAGE) {
					// remove current backdrop
					mShapeList.remove(1);
				}
			}
			// insert into shape list
			mShapeList.add(1, shapeObject);
		}
		else {
			// OVERLAY - if position non-zero then get object from list
			shapeObject = mShapeList.get(shapeListInx);
			if (shapeObject != null &&
					shapeObject.getShapeType() == SketchSetting.ShapeType.RECT) {
				shapeObject.setShapeType(SketchSetting.ShapeType.IMAGE);
				shapeObject.setName(imagePath);
				Log.v(TAG, "OVERLAY image path: " + imagePath);
				// set bitmap
				Bitmap bitmap = BitmapFactory.decodeFile(imagePath, null);
				shapeObject.setObject(bitmap); 
			}
			else {
				Log.e(TAG, "setImageShape OVERLAY failure: invalid shape or shape type at inx: " + shapeListInx);
				return null;
			}
		}
		return shapeObject;
	}
	////////////////////////////////////////////////////////////////////////////
	// start shape - create draw object capturing initial touch, paint settings
	public boolean startShape(float x, float y) {
		// working shape
		mShapeType = mSketchSetting.getShape();
		mShapeObject = new ShapeObject();
		mShapeObject.setShapeType(mShapeType);
		mShapeObject.setName(mShapeType.toString()+mShapeList.size());
		Log.v(TAG, "startShape name:" + mShapeObject.getName());

		// working paint
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setStrokeWidth(mSketchSetting.getSize());  
		mPaint.setColor(mSketchSetting.getColor());
		mPaint.setStyle(mSketchSetting.getStyle());
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		// 
		switch (mShapeType)
		{
		case FREE:  
			mPath = new Path();
			mPath.moveTo(x, y);
			mShapeObject.setFocus(new PointF(x,y));
			mShapeObject.setBound(new RectF (x, y, x, y));
			mShapeObject.setPaint(mPaint);
			mShapeObject.setObject(mPath); 
			break;
		case LINE:
			mPts = new PointF[2];
			mPts[0] = new PointF(x, y);
			mPts[1] = new PointF(x, y);
			mCenter = new PointF(mPts[0].x,mPts[0].y);
			mRect = new RectF (mPts[0].x, mPts[0].y, mPts[1].x, mPts[1].y);
			mShapeObject.setFocus(mCenter);
			mShapeObject.setBound(mRect);  
			mShapeObject.setPaint(mPaint);
			mShapeObject.setObject(mPts); 
			break;
		case RECT:
		case LABEL:
		case OVAL:
			// touch is rect center
			mCenter = new PointF (x, y);
			// set rect around center
			mRect = new RectF();
			float left = x - mSketchSetting.getSize();
			float top = y - mSketchSetting.getSize();
			float right = x + mSketchSetting.getSize();
			float bottom = y + mSketchSetting.getSize();
			mRect.set(left, top, right, bottom);
			mShapeObject.setFocus(mCenter);
			mShapeObject.setBound(mRect);
			mShapeObject.setPaint(mPaint);
			mShapeObject.setObject(mRect); 
			break;
		case CIRCLE:
			// touch is center
			mCenter = new PointF (x, y);
			mRadius = mSketchSetting.getSize();
			// set rect around center
			mCircle = new float[3];
			mCircle[0] = mCenter.x;
			mCircle[1] = mCenter.y;
			mCircle[2] = mRadius;
			Log.v(TAG, "startShape circle x,y, radius: " + mCenter.x + ", " + mCenter.x + ", " + mRadius);
			mShapeObject.setFocus(mCenter);
			mShapeObject.setBound(new RectF ( mCenter.x-mRadius, mCenter.y-mRadius, mCenter.x+mRadius, mCenter.y+mRadius ));
			mShapeObject.setPaint(mPaint);
			mShapeObject.setObject(mCircle); 
			break;
		default:  
			mShapeObject = null;
			Log.e(TAG, "startShape invalid shape: " + mShapeType);
			return false;
		}
		mShapeList.add(mShapeObject);
		return true;
	}
	////////////////////////////////////////////////////////////////////////////
	// refine shape - adding touches to resize or refine
	public boolean refineShape(float x, float y) {
		// working shape
		mShapeType = mSketchSetting.getShape();

		switch (mShapeType)
		{
		case FREE:
			mPath.lineTo(x, y);
			mRect = mShapeObject.getBound();
			mRect.left = Math.min(mRect.left, x);
			mRect.top = Math.min(mRect.top, y);
			mRect.right = Math.max(mRect.right, x);
			mRect.bottom = Math.max(mRect.bottom, y);
			mShapeObject.getFocus().x = mRect.left + ((mRect.right-mRect.left)/2);
			mShapeObject.getFocus().y = mRect.top + ((mRect.bottom-mRect.top)/2);
			break;
		case LINE:
			// set 2nd point in line to reflect movement
			mPts[1].x = x;
			mPts[1].y = y;
			mRect.left = Math.min(mPts[0].x, mPts[1].x);
			mRect.top = Math.min(mPts[0].y, mPts[1].y);
			mRect.right = Math.max(mPts[0].x, mPts[1].x);
			mRect.bottom = Math.max(mPts[0].y, mPts[1].y);
			mShapeObject.getFocus().x = mRect.left + ((mRect.right-mRect.left)/2);
			mShapeObject.getFocus().y = mRect.top + ((mRect.bottom-mRect.top)/2);
			break;
		case RECT:
		case LABEL:
		case OVAL:
			// size rect around center based on x,y
			sizeRect(mRect, mCenter, x, y);
			Log.v(TAG, "refineShape rect LTRB: " + mRect.left + ", " + mRect.top + ", " + mRect.right + ", " + mRect.bottom);
			break;
		case CIRCLE:
			if (isDelta(mCenter, x, y)) {
				// prevent exceeding screen bounds
				float radius = setRadius(mCenter, x, y);
				if ( mCenter.x-radius > CANVAS_MIN &&
						mCenter.y-radius > CANVAS_MIN &&
						mCenter.x+radius < mParentActivity.getCanvasWidth() &&
						mCenter.y+radius < mParentActivity.getCanvasHeight()) {
					// update radius reflecting touch - radius derived from c**2 =a**2 + b**2 
					mRadius = setRadius(mCenter, x, y);
					mCircle[2] = mRadius;
					mShapeObject.getBound().set ( mCenter.x-mRadius, mCenter.y-mRadius, mCenter.x+mRadius, mCenter.y+mRadius );
					Log.v(TAG, "refineShape circle x,y, radius: " + mCenter.x + ", " + mCenter.x + ", " + mRadius);
				}
			}
			break;
		default:  
			Log.e(TAG, "refineShape invalid shape: " + mShapeType);
			break;
		}
		return true;
	}
	////////////////////////////////////////////////////////////////////////////
	// complete shape - adding final touches to resize or refine
	public boolean completeShape(float x, float y) {
		// refine shape to reflect final point
		refineShape(x, y);

        if ((mShapeObject.getBound().right - mShapeObject.getBound().left) < SIZE_TINY &&
            (mShapeObject.getBound().bottom - mShapeObject.getBound().top) < SIZE_TINY ) {
            Log.v(TAG, "completeShape reject tiny shape...");
            clearShape(mShapeList.size()-1);
            return false;
        }

        // if focus hold set
		if (mSketchSetting.getFocusHold()) {
			// set focus to new shape
			mShapeListFocus = setShapeListFocus (mShapeList.size()-1);
		}
		else {
			// clear focus
			mShapeListFocus = NOFOCUS;
		}
		Log.v(TAG, "completeShape focus: " + mShapeListFocus);
		
		if (mShapeType == ShapeType.LABEL) {
			// present label text entry dialog
			enterLabelText(mShapeObject);
		}
//		if (mShapeType == ShapeType.FREE) {
//			getPathPoints(mPath);
//		}
		return true;
	}
	private void enterLabelText(final ShapeObject shapeObject) {
		AlertDialog.Builder alert = new AlertDialog.Builder(mParentActivity);

		alert.setTitle("New Label");
		alert.setMessage("Please enter your label:");

		// Set an EditText view to get user input 
		final EditText input = new EditText(mParentActivity);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String labelText = input.getText().toString();
				// set shape object name to label text
				shapeObject.setName(labelText);
				Log.v(TAG, "enterLabelText: " + shapeObject.getName());
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
				Log.v(TAG, "enterLabelText CANCELLED ");
			}
		});

		alert.show();
	}
		
//	private boolean getPathPoints(Path path) {
//		PathMeasure pm = new PathMeasure(path, false);
//		Log.v(TAG, "path len: " + pm.getLength());
//		float len = pm.getLength();
//		int nPts = (int)len/5;
//		float delta = len/(float)(nPts-1);
//		Log.v(TAG, "path len: " + len + ", # segs: " + nPts + ", delta: " + delta);
//		float[] x = new float[nPts];
//		float[] y = new float[nPts];
//		float dst = 0;
//		float[] pos = new float[2];
//		float[] tan = new float[2];
//		for (int i = 0; i < nPts-1; i++) {
//			pm.getPosTan(dst, pos, tan);
//			Log.v(TAG, "path dst: " + dst + ", x: " + pos[0] + ", y: " + pos[1]);
//			x[i] = pos[0];
//			y[i] = pos[1];
//			dst+=delta;
//		}
//		pm.getPosTan(len, pos, tan);
//		Log.v(TAG, "path dst: " + dst + ", x: " + pos[0] + ", y: " + pos[1]);
//		x[nPts-1] = pos[0];
//		y[nPts-1] = pos[1];
//		addPathApprox( nPts, x, y );
//		return true;
//	}
//	private boolean addPathApprox(int nPts, float[] x, float[] y) {
//		// working shape
//		mShapeType = ShapeType.FREE;
//		// working paint
//		mPaint = new Paint();
//		mPaint.setAntiAlias(true);
//		mPaint.setStrokeWidth(mSketchSetting.getSize());  
//		mPaint.setColor(Color.WHITE);
//		mPaint.setStyle(mSketchSetting.getStyle());
//		mPaint.setStrokeJoin(Paint.Join.ROUND);
//		// 
//		mPath = new Path();
//		mPath.moveTo(x[0], y[0]);
//		mShapeObject = new ShapeObject();
//		mShapeObject.setShapeType(mShapeType);
//		mShapeObject.setFocus(new PointF(x[0], y[0]));
//		mShapeObject.setBound(new RectF (x[0], y[0], x[0], y[0]));
//		mShapeObject.setPaint(mPaint);
//		mShapeObject.setObject(mPath); 
//		mShapeList.add(mShapeObject);
//		
//		for (int i = 1; i < nPts; i++) {
//			mPath.lineTo(x[i], y[i]);
//			mRect = mShapeObject.getBound();
//			mRect.left = Math.min(mRect.left, x[i]);
//			mRect.top = Math.min(mRect.top, y[i]);
//			mRect.right = Math.max(mRect.right, x[i]);
//			mRect.bottom = Math.max(mRect.bottom, y[i]);
//			mShapeObject.getFocus().x = mRect.left + ((mRect.right-mRect.left)/2);
//			mShapeObject.getFocus().y = mRect.top + ((mRect.bottom-mRect.top)/2);			
//		}
//		return true;
//	}
	////////////////////////////////////////////////////////////////////////////
	// move shape
	public boolean startMove(float x, float y) {
		mMovePts = new PointF[2];
		mMovePts[0] = new PointF(x, y);
		mMovePts[1] = new PointF(x, y);

		return true;
	}
	////////////////////////////////////////////////////////////////////////////
	// move shape 
	public boolean refineMove(float x, float y) {
		mMovePts[1].x = x;
		mMovePts[1].y = y;
		float deltaX = mMovePts[1].x - mMovePts[0].x;
		float deltaY = mMovePts[1].y - mMovePts[0].y;
		mMovePts[0].x = mMovePts[1].x;
		mMovePts[0].y = mMovePts[1].y;
		// disallow shape moving off screen
		mRect = mShapeObject.getBound();
		if (mRect.left + deltaX < CANVAS_MIN || 
				mRect.right + deltaX > mParentActivity.getCanvasWidth() || 
				mRect.top + deltaY < CANVAS_MIN || 
				mRect.bottom + deltaY > mParentActivity.getCanvasHeight()) {
			return false;
		}

		switch (mShapeObject.getShapeType())
		{
		case FREE:
			mPath = (Path)mShapeObject.getObject();
			mPath.offset(deltaX, deltaY);
			mRect = mShapeObject.getBound();
			mRect.left += deltaX;
			mRect.top += deltaY;
			mRect.right += deltaX;
			mRect.bottom += deltaY;
			mShapeObject.getFocus().x = mRect.left + ((mRect.right-mRect.left)/2);
			mShapeObject.getFocus().y = mRect.top + ((mRect.bottom-mRect.top)/2);
			break;
		case LINE:
			mPts = (PointF[])mShapeObject.getObject();
			mPts[0].x += deltaX;
			mPts[0].y += deltaY;
			mPts[1].x += deltaX;
			mPts[1].y += deltaY;
			mRect = mShapeObject.getBound();
			mRect.left = Math.min(mPts[0].x, mPts[1].x);
			mRect.top = Math.min(mPts[0].y, mPts[1].y);
			mRect.right = Math.max(mPts[0].x, mPts[1].x);
			mRect.bottom = Math.max(mPts[0].y, mPts[1].y);
			mShapeObject.getFocus().x = mRect.left + ((mRect.right-mRect.left)/2);
			mShapeObject.getFocus().y = mRect.top + ((mRect.bottom-mRect.top)/2);
			break;
		case RECT:
		case LABEL:
		case OVAL:
			mRect = (RectF)mShapeObject.getObject();
			mRect.left += deltaX;
			mRect.top += deltaY;
			mRect.right += deltaX;
			mRect.bottom += deltaY;
			mShapeObject.setBound(mRect);
			mShapeObject.getFocus().x = mRect.left + ((mRect.right-mRect.left)/2);
			mShapeObject.getFocus().y = mRect.top + ((mRect.bottom-mRect.top)/2);
			break;
		case CIRCLE:
			// x, y, radius
			float[] circle = (float[])mShapeObject.getObject();
			circle[0] += deltaX;
			circle[1] += deltaY;
			mShapeObject.getBound().set ( circle[0]-circle[2], circle[1]-circle[2], circle[0]+circle[2], circle[1]+circle[2] );
			mShapeObject.getFocus().x = circle[0];
			mShapeObject.getFocus().y = circle[1];
			break;
		default:
			Log.e(TAG, "refineMove invalid shape: " + mShapeObject.getShapeType());
			break;
		}
		return true;
	}
	////////////////////////////////////////////////////////////////////////////
	public boolean completeMove(float x, float y) {
		return true;
	}
	////////////////////////////////////////////////////////////////////////////
	// resize shape
	public boolean resizeShape(float scaleFactor) {
		if (scaleFactor == 1) {
			// if no scaling, return
			return false;
		}
		else {
			// preserve original scale factor
//			mScaleFactor = scaleFactor;
			// tranform scale factor from 
			//		origin around 1 where alittle (.1) through alot (1.9) to 
			//  	origin around 0 decrease alot (-.9) through increase alot (.9)
			if (scaleFactor < 1) {
				scaleFactor = -(1 - scaleFactor);
			}
			else {
				scaleFactor = scaleFactor - 1;			
			}
			scaleFactor *= REFACTOR;
		}
		Log.v(TAG, "resizeShape scale factor: " + scaleFactor);
			
		switch (mShapeObject.getShapeType())
		{
		case FREE:
			if (scaleFactor < 0) {
				scaleFactor = -0.01f;
			}
			else {
				scaleFactor = 0.01f;
			}
			// resize bounding rect
			mRect = mShapeObject.getBound();
			if (resizeRect (mRect, scaleFactor)) {
				Matrix matrix = new Matrix();
				if (scaleFactor < 0) {
					scaleFactor = 0.99f;
				}
				else {
					scaleFactor = 1.01f;
				}
				matrix.setScale(scaleFactor, scaleFactor, mShapeObject.getFocus().x, mShapeObject.getFocus().y);
				mPath = (Path)mShapeObject.getObject();
				mPath.transform(matrix);
			}
			break;
		case LINE:
			mPts = (PointF[])mShapeObject.getObject();
			Log.v(TAG, "resizeShape o-line: " + mPts[0].x + ", " + mPts[0].y + " to " + mPts[1].x + ", " + mPts[1].y);
			mRect = mShapeObject.getBound();
			mRect.left = Math.min(mPts[0].x, mPts[1].x);
			mRect.top = Math.min(mPts[0].y, mPts[1].y);
			mRect.right = Math.max(mPts[0].x, mPts[1].x);
			mRect.bottom = Math.max(mPts[0].y, mPts[1].y);
			
			// resize bounding rect
			if ( resizeRect (mRect, scaleFactor) ) {
			
				// maintain pt order
				if (mPts[0].x < mPts[1].x) {
					mPts[0].x = mRect.left;
					mPts[1].x = mRect.right;
				}
				else {
					mPts[0].x = mRect.right;
					mPts[1].x = mRect.left;
				}
				if (mPts[0].y < mPts[1].y) {
					mPts[0].y = mRect.top;
					mPts[1].y = mRect.bottom;
				}
				else {
					mPts[0].y = mRect.bottom;				
					mPts[1].y = mRect.top;
				}
			}
			Log.v(TAG, "resizeShape r-line: " + mPts[0].x + ", " + mPts[0].y + " to " + mPts[1].x + ", " + mPts[1].y);
			break;
		case RECT:
		case LABEL:
		case OVAL:
		case IMAGE:
			// resize bounding rect
			mRect = mShapeObject.getBound();
			resizeRect (mRect, scaleFactor);
			break;
		case CIRCLE:
			// resize bounding rect
			mRect = mShapeObject.getBound();
			if (resizeRect (mRect, scaleFactor)) {
				// x, y, radius
				float[] circle = (float[])mShapeObject.getObject();
				// resize radius
				circle[2] = circle[2] + (circle[2] * scaleFactor);
			}
			break;
		default:  
			Log.e(TAG, "resizeShape invalid shape: " + mShapeObject.getShapeType());
			break;
		}
		return true;
	}
	////////////////////////////////////////////////////////////////////////////
	// update paint color, style, size
	public boolean updatePaint() {
		if (getShapeListFocus() != NOFOCUS) {
			mShapeObject = mShapeList.get(getShapeListFocus());
			mShapeObject.getPaint().setColor(mSketchSetting.getColor());
            if (getShapeListFocus() != CANVAS_SHAPELIST_INX) {
                mShapeObject.getPaint().setStyle(mSketchSetting.getStyle());
                mShapeObject.getPaint().setStrokeWidth(mSketchSetting.getSize());
            }
			return true;
		}
		return false;
	}

	////////////////////////////////////////////////////
	// private helpers
	////////////////////////////////////////////////////
	
	private boolean isDelta (PointF center, float x, float y) {
		// true if delta between center & point is detectable (greater than line size) 
		if (Math.abs(center.x - x) > mSketchSetting.getSize() || Math.abs(center.y - y) > mSketchSetting.getSize()) {
			return true;
		}
		return false;
	}
	// size rect around center based on x,y
	private boolean sizeRect(RectF rect, PointF center, float x, float y) {
		// prevent size < settings size
		if (isDelta(center, x, y)) {
			if (x > center.x) {
				// touch right of center
				if (center.x - (x - center.x) > CANVAS_MIN) {
					rect.right = x;
					rect.left = center.x - (x - center.x);
				}
			}
			else {
				// touch left of center
				if (center.x + (center.x - x) < mParentActivity.getCanvasWidth()) {
					rect.left = x;
					rect.right = center.x + (center.x - x);
				}
			}
			if (y < center.y) {
				// touch above center
				if (center.y + (center.y - y) < mParentActivity.getCanvasHeight()) {
					rect.top = y;
					rect.bottom = center.y + (center.y - y);
				}
			}
			else {
				// touch below center
				if (center.y - (y - center.y) > CANVAS_MIN) {
					rect.bottom = y;
					rect.top = center.y - (y - center.y);
				}
			}
			// no bounding violations
			return true;
		}
		// no sizing - too small change between center & x,y
		return false;
	}
	private float setRadius(PointF center, float x, float y) {
		// update radius reflecting touch - radius derived from c**2 =a**2 + b**2 
		double xDelta = (double) (center.x - x);
		double yDelta = (double) (center.y - y);
		float radius = (float) Math.sqrt((xDelta*xDelta) + (yDelta*yDelta));
		return radius;
	}
	
	private boolean resizeRect (RectF rect, float scaleFactor) {
		Log.v(TAG, "rect: " + rect + ", scale: " + scaleFactor);
		// disallow resizing if outside screen edges or if left/right, top/bottom invert 
		mDX = (rect.right - rect.left) * scaleFactor / 2.0f;
		Log.v(TAG, "mDX " + mDX);
		if ((rect.left - mDX) < CANVAS_MIN || 
			(rect.right + mDX) > mParentActivity.getCanvasWidth() ||
			(rect.left - mDX) > (rect.right + mDX) ) {
			Log.v(TAG, "resizeRect fails: X too large or small...");
			return false;
		}
		mDY = (rect.bottom - rect.top) * scaleFactor / 2;
		Log.v(TAG, "mDY " + mDY);
		if ((rect.top - mDY) < CANVAS_MIN || 
			(rect.bottom + mDY) > mParentActivity.getCanvasHeight() ||
			(rect.top - mDY) > (mRect.bottom + mDY) ) {
			Log.v(TAG, "resizeRect fails: Y too large or small...");
			return false;
		}
		// disallow rect getting very tiny
		if (((rect.right + mDX) - (rect.left - mDX)) < SIZE_TINY ||
			((rect.bottom + mDY) - (rect.top - mDY)) < SIZE_TINY ) {
			Log.v(TAG, "resizeRect fails: too tiny...");
			return false;
		}
		// resize bounding rect
		rect.left = rect.left - mDX;
		rect.top = rect.top - mDY;
		rect.right = rect.right + mDX;
		rect.bottom = rect.bottom + mDY;

		return true;
	}
	////////////////////////////////////////////////////////////////////////////
}
