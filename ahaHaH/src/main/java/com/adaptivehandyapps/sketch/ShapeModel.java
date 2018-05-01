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
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.adaptivehandyapps.ahahah.R;
import com.adaptivehandyapps.sketch.SketchViewModel.ShapeType;

public class ShapeModel {

	private static final String TAG = "ShapeModel";

    // context
    private Context mContext;
    // view model
	private SketchViewModel mSketchViewModel;

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
	
	public static final int CANVAS_SHAPELIST_INX = 0;
	private List<ShapeObject> mShapeList = new ArrayList<ShapeObject>();
    String mBgRectName = "Canvas";

    //////////////////////////////////////////////////////////////////////////////////////////
    private static volatile ShapeModel instance;

    public synchronized static ShapeModel getInstance(Context c, SketchViewModel sketchViewModel)
    {
        if (instance == null){
            synchronized (ShapeModel.class) {   // Check for the second time.
                //if there is no instance available... create new one
                if (instance == null){
                    instance = new ShapeModel(c,sketchViewModel );
                }
            }
        }
        return instance;
    }
	////////////////////////////////////////////////////////////////////////////
	// constructor
	public ShapeModel(Context context, SketchViewModel sketchViewModel) {
        setContext(context);
		// obtain sketch view model
        mSketchViewModel = sketchViewModel;
		initShapeList();
	}
    ////////////////////////////////////////////////////////////////////////////
	// getters/setters
    private Context getContext() { return mContext; }
    private void setContext(Context context) { this.mContext = context; }

	////////////////////////////////////////////////////////////////////////////
	public Boolean initShapeList() {
		// add canvas paint, background full-screen rect as 0th draw shape element
		mShapeList = new ArrayList<ShapeObject>();
		ShapeObject shapeObject = new ShapeObject();
		shapeObject.setShapeType(SketchViewModel.ShapeType.RECT);
		shapeObject.setName(mBgRectName);
		Log.v(TAG, "shape name: " + mBgRectName);
		shapeObject.setFocus(new PointF(
				mSketchViewModel.getCanvasWidth()/2.0f,
                mSketchViewModel.getCanvasHeight()/2.0f));
		shapeObject.setBound(new RectF (
				0.0f, 0.0f,
                mSketchViewModel.getCanvasWidth(), mSketchViewModel.getCanvasHeight()));
		Paint paintCanvas = new Paint();
		paintCanvas.setColor(Color.CYAN);
		shapeObject.setPaint(paintCanvas);
		shapeObject.setObject(shapeObject.getBound()); 
		mShapeList.add(shapeObject);

		// set draw list index to unselected
		mSketchViewModel.clearShapeListFocus();
		return true;
    }
	
	////////////////////////////////////////////////////////////////////////////
	// save shape list
	public boolean save (String filename) {
		Log.v(TAG, "save shape list to " + filename);
		// snap start time
	    long startTime = System.currentTimeMillis();

		try {
			FileOutputStream fos = getContext().openFileOutput(filename, Context.MODE_PRIVATE);
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
//			Log.e(TAG, "serialize exception: " + e.getCause().metricsToString() + " - " + e.getMessage());
			Log.e(TAG, "serialize exception: " + e.getMessage());
			return false;
		}
        // snap completion time
	    long doneTime = System.currentTimeMillis();
        long responseTime = doneTime - startTime;
        Log.v(TAG, "elapsed serialization time: " + responseTime + " ms");
		return true;
	}
	////////////////////////////////////////////////////////////////////////////
	// load shape list
	public boolean load (Context context, String filename) {
		Log.v(TAG, "load shape list from " + filename);
		// snap start time
	    long startTime = System.currentTimeMillis();
		FileInputStream fis = null;
        try {
        	fis = getContext().openFileInput(filename);
        	Log.v(TAG, "fis available: " + fis.available());
            ObjectInputStream ois = new ObjectInputStream(fis);
			Log.v(TAG, "ois available: " + ois.available());
        	try {
                // clear shape list
                mShapeList = new ArrayList<ShapeObject>();
        		boolean more = true;
        		while (more) {
        			ShapeObject mShapeObject = new ShapeObject();
        			more = mShapeObject.deserialize(context, ois);
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
        	boolean success = getContext().deleteFile(filename);
        	Log.v(TAG, "delete file returns: " + success);
        }
        catch(Exception e) {
            Log.e(TAG, "delete file exception:" + e.getMessage());
        }
		return true;
	}
	////////////////////////////////////////////////////////////////////////////
	// getters/setters, utility
	public List<ShapeObject> getShapeList () { return mShapeList; }
	public ShapeObject getShapeObject() { return mShapeObject; }
	public void setShapeObject(ShapeObject shapeObject) { this.mShapeObject = shapeObject; }

	public boolean clearShape(int shapeInx) {
		Log.v(TAG, "clearShape shapeInx: " + shapeInx);
		if (shapeInx > 0 && shapeInx < mShapeList.size()) { 
			mShapeList.remove(shapeInx);
		}
		else {
			Log.e(TAG, "clearShape OutOfBounds...");
			return false;
		}
		mSketchViewModel.clearShapeListFocus();
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
		if (shapeInx > SketchViewModel.NOFOCUS && shapeInx < mShapeList.size()) {
			ShapeObject shapeObject = mShapeList.get(shapeInx);
			if (shapeObject != null &&
					shapeObject.getShapeType() == shapeType) {
				return true;
			}
		}
		return false;
	}
    ////////////////////////////////////////////////////////////////////////////
    // set image type shape: BACKDROP
    public ShapeObject setImageBackdrop(String imagePath, Bitmap bitmap) {
        // BACKDROP - create shape & insert as 1st element
        Log.v(TAG, "setImageBackdrop BACKDROP image path " + imagePath);
        // set image type & path
        ShapeObject shapeObject = new ShapeObject();
        shapeObject.setShapeType(SketchViewModel.ShapeType.IMAGE);
        shapeObject.setName(imagePath);
        Paint paint = new Paint();
        shapeObject.setPaint(paint);

        // TODO: get device dimensions or canvas dimensions?
//			DisplayMetrics displayMetrics = DisplayUtils.getDisplayMetrics(getContext());
//			int targetDeviceW = displayMetrics.widthPixels;
//			int targetDeviceH = displayMetrics.heightPixels;
        int targetDeviceW = mSketchViewModel.getCanvasWidth();
        int targetDeviceH = mSketchViewModel.getCanvasHeight();
        Log.v(TAG, "setImageBackdrop target device W/H: " + targetDeviceW + "/" + targetDeviceH);

//        // get size of photo
//        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
//        bmOptions.inJustDecodeBounds = true;
//        BitmapFactory.decodeFile(imagePath, bmOptions);
//        int imageW = bmOptions.outWidth;
//        int imageH = bmOptions.outHeight;
//        Log.v(TAG, "setImageBackdrop photo W/H: " + imageW + "/" + imageH);
//        // round up since int math truncates remainder resulting scale factor 1 (FIT = FULL)
//        int scaleFactor = Math.min(imageW/targetDeviceW, imageH/targetDeviceH)+1;
//        Log.v(TAG, "setImageBackdrop scale factor: " + scaleFactor);
//        // set bitmap options to scale the image decode target
//        bmOptions.inJustDecodeBounds = false;
//        bmOptions.inSampleSize = scaleFactor;
//        bmOptions.inPurgeable = true;
//
//        // set bitmap
//        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, bmOptions);
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
        if (mShapeList.size() > 1) {
            testShapeObject = mShapeList.get(1);
            if (testShapeObject.getShapeType() == ShapeType.IMAGE) {
                // remove current backdrop
                mShapeList.remove(1);
            }
        }
        // insert into shape list
        mShapeList.add(1, shapeObject);
        return shapeObject;
    }
    ////////////////////////////////////////////////////////////////////////////
    // set image type shape: OVERLAY
    public ShapeObject setImageOverlay(String imagePath, Bitmap bitmap) {
        // OVERLAY - if focus on rect then get object from list else create default rect preserving aspect ratio
        Log.v(TAG, "setImageOverlay OVERLAY image path " + imagePath);
        ShapeObject shapeObject;
        int focus = mSketchViewModel.getShapeListFocus();
        // if no focus or focus is not rectangle
        if (focus <= 0 ||
               (mShapeList.get(focus) != null && mShapeList.get(focus).getShapeType() != SketchViewModel.ShapeType.RECT)) {
            // create rect with overlay image preserving aspect ratio
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setStrokeWidth(mSketchViewModel.getSize());
            mPaint.setColor(mSketchViewModel.getColor());
            mPaint.setStyle(mSketchViewModel.getStyle());
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            // get device center
            int targetDeviceW = mSketchViewModel.getCanvasWidth();
            int targetDeviceH = mSketchViewModel.getCanvasHeight();
            Log.v(TAG, "setImageOverlay target device W/H: " + targetDeviceW + "/" + targetDeviceH);
//            // get size of photo
//            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
//            bmOptions.inJustDecodeBounds = true;
//            BitmapFactory.decodeFile(imagePath, bmOptions);
//            int imageW = bmOptions.outWidth;
//            int imageH = bmOptions.outHeight;
            int imageW = bitmap.getWidth();
            int imageH = bitmap.getHeight();
            Log.v(TAG, "setImageOverlay photo W/H: " + imageW + "/" + imageH);
            // center on device
            mCenter = new PointF(
                    (float)targetDeviceW/2,
                    (float)targetDeviceH/2);
            // set rect around center
            mRect = new RectF();
//            float ratio = (float) imageH / (float) imageW;
            float ratio = (float) imageW / (float) imageH;
            float sizeH = (float)(targetDeviceH/4);
            float sizeW = sizeH * ratio;
            float left = mCenter.x - sizeW;
            float top = mCenter.y - sizeH;
            float right = mCenter.x + sizeW;
            float bottom = mCenter.y + sizeH;
            mRect.set(left, top, right, bottom);
            Log.v(TAG,"setImageOverlay rect " + mRect.toString());
//            if (imageW > imageH) {
//                // if landscape
//                float ratio = (float) imageH / (float) imageW;
//                float left = mCenter.x - (targetDeviceW/2);
//                float top = mCenter.y - ((float) imageH * ratio);
//                float right = mCenter.x + (targetDeviceW/2);
//                float bottom = mCenter.y + ((float) imageH * ratio);
//                mRect.set(left, top, right, bottom);
//            }
//            else {
//                // portrait
//                float ratio = (float) imageW / (float) imageH;
//                float left = mCenter.x - ((float) imageW * ratio);
//                float top = mCenter.y - (targetDeviceH/2);
//                float right = mCenter.x + ((float) imageW * ratio);
//                float bottom = mCenter.y + (targetDeviceH/2);
//                mRect.set(left, top, right, bottom);
//            }
            shapeObject = new ShapeObject();
            shapeObject.setFocus(mCenter);
            shapeObject.setBound(mRect);
            shapeObject.setPaint(mPaint);
            shapeObject.setObject(mRect);

            shapeObject.setShapeType(SketchViewModel.ShapeType.IMAGE);
            shapeObject.setName(imagePath);
            // set bitmap
//            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, null);
            shapeObject.setObject(bitmap);
            // insert into shape list
            mShapeList.add(shapeObject);
        }
        else {
            shapeObject = mShapeList.get(focus);
            if (shapeObject != null &&
                    shapeObject.getShapeType() == SketchViewModel.ShapeType.RECT) {
                Log.v(TAG, "setImageOverlay assign overlay to RECT shape inx " + focus);
                shapeObject.setShapeType(SketchViewModel.ShapeType.IMAGE);
                shapeObject.setName(imagePath);
                // set bitmap
//                Bitmap bitmap = BitmapFactory.decodeFile(imagePath, null);
                shapeObject.setObject(bitmap);
            }

        }
        // set focus to overlay to allow easy re-positioning
        mSketchViewModel.setShapeListFocus(shapeObject.getName());
        return shapeObject;
    }
	////////////////////////////////////////////////////////////////////////////
	// start shape - create draw object capturing initial touch, paint settings
	public boolean startShape(float x, float y) {
		// working shape
		mShapeType = mSketchViewModel.getShape();
		mShapeObject = new ShapeObject();
		mShapeObject.setShapeType(mShapeType);
		mShapeObject.setName(mShapeType.toString()+mShapeList.size());
		Log.v(TAG, "startShape name:" + mShapeObject.getName());

		// working paint
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setStrokeWidth(mSketchViewModel.getSize());
		mPaint.setColor(mSketchViewModel.getColor());
		mPaint.setStyle(mSketchViewModel.getStyle());
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
			float left = x - mSketchViewModel.getSize();
			float top = y - mSketchViewModel.getSize();
			float right = x + mSketchViewModel.getSize();
			float bottom = y + mSketchViewModel.getSize();
			mRect.set(left, top, right, bottom);
			mShapeObject.setFocus(mCenter);
			mShapeObject.setBound(mRect);
			mShapeObject.setPaint(mPaint);
			mShapeObject.setObject(mRect); 
			break;
		case CIRCLE:
			// touch is center
			mCenter = new PointF (x, y);
			mRadius = mSketchViewModel.getSize();
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
		mShapeType = mSketchViewModel.getShape();
		Log.v(TAG, "refineShape name:" + mShapeObject.getName());

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
        case IMAGE:
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
						mCenter.x+radius < mSketchViewModel.getCanvasWidth() &&
						mCenter.y+radius < mSketchViewModel.getCanvasHeight()) {
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
		Log.v(TAG, "completeShape name:" + mShapeObject.getName());

        if ((mShapeObject.getBound().right - mShapeObject.getBound().left) < SIZE_TINY &&
            (mShapeObject.getBound().bottom - mShapeObject.getBound().top) < SIZE_TINY ) {
            Log.v(TAG, "completeShape reject tiny shape...");
            clearShape(mShapeList.size()-1);
            return false;
        }

//        // if focus hold set
//		if (mSketchViewModel.getFocusHold()) {
			// set focus to new shape
			mSketchViewModel.setShapeListFocus (mShapeList.size()-1);
//		}
//		else {
//			// clear focus
//			mSketchViewModel.clearShapeListFocus();
//        }
		Log.v(TAG, "completeShape focus: " + mSketchViewModel.getShapeListFocus());
		
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
		AlertDialog.Builder alert = new AlertDialog.Builder(getContext());

		alert.setTitle("New Label");
		alert.setMessage("Please enter your label:");

		// Set an EditText view to get user input 
		final EditText input = new EditText(getContext());
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
//		mPaint.setStrokeWidth(mSketchViewModel.getSize());
//		mPaint.setColor(Color.WHITE);
//		mPaint.setStyle(mSketchViewModel.getStyle());
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
				mRect.right + deltaX > mSketchViewModel.getCanvasWidth() ||
				mRect.top + deltaY < CANVAS_MIN || 
				mRect.bottom + deltaY > mSketchViewModel.getCanvasHeight()) {
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
		case IMAGE:
            mRect = (RectF)mShapeObject.getBound();
            mRect.left += deltaX;
            mRect.top += deltaY;
            mRect.right += deltaX;
            mRect.bottom += deltaY;
            mShapeObject.setBound(mRect);
            mShapeObject.getFocus().x = mRect.left + ((mRect.right-mRect.left)/2);
            mShapeObject.getFocus().y = mRect.top + ((mRect.bottom-mRect.top)/2);
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
	public Boolean updatePaint() {
		if (mSketchViewModel.getShapeListFocus() != SketchViewModel.NOFOCUS) {
			mShapeObject = mShapeList.get(mSketchViewModel.getShapeListFocus());
			mShapeObject.getPaint().setColor(mSketchViewModel.getColor());
            if (mSketchViewModel.getShapeListFocus() != CANVAS_SHAPELIST_INX) {
                mShapeObject.getPaint().setStyle(mSketchViewModel.getStyle());
                mShapeObject.getPaint().setStrokeWidth(mSketchViewModel.getSize());
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
		if (Math.abs(center.x - x) > mSketchViewModel.getSize() || Math.abs(center.y - y) > mSketchViewModel.getSize()) {
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
				if (center.x + (center.x - x) < mSketchViewModel.getCanvasWidth()) {
					rect.left = x;
					rect.right = center.x + (center.x - x);
				}
			}
			if (y < center.y) {
				// touch above center
				if (center.y + (center.y - y) < mSketchViewModel.getCanvasHeight()) {
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
			(rect.right + mDX) > mSketchViewModel.getCanvasWidth() ||
			(rect.left - mDX) > (rect.right + mDX) ) {
			Log.v(TAG, "resizeRect fails: X too large or small...");
			return false;
		}
		mDY = (rect.bottom - rect.top) * scaleFactor / 2;
		Log.v(TAG, "mDY " + mDY);
		if ((rect.top - mDY) < CANVAS_MIN || 
			(rect.bottom + mDY) > mSketchViewModel.getCanvasHeight() ||
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
