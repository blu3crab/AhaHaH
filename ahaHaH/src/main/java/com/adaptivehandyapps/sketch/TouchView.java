// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.sketch;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

public class TouchView extends View implements
		OnGestureListener, OnDoubleTapListener {

	private static final String TAG = "TouchView"; 

	private SketchActivity mParentActivity;
	private SketchSetting mSketchSetting;
	private ShapeModel mShapeModel;

	// current touch position
	private Paint mPaintCurrent;
	private float mCurrentRadius;
	private float mTouchX = 0.0f;
	private float mTouchY = 0.0f;
	// zoom
	private float mScaleFactor = 1.0f;
	private ScaleGestureDetector mScaleGestureDetector;
//	private OrientationEventListener mOrientationListener;

	// gesture detector
	private boolean mGestureDetected = false;
    private GestureDetectorCompat mDetector; 
    
    // canvas dimensions 
    private int mCanvasWidth = -1;
    private int mCanvasHeight = -1;
    
    // pre-allocated draw/layout objects
    private Bitmap mCanvasBitmap = null;
    private Canvas mCanvas = null;
    private Matrix mIdentityMatrix = null;
    private PointF mBgFocus;
    private RectF mBgRect;
    private int mFocus = -1;
    
	private ShapeObject mShapeObject;		// working draw list object
	
	private Path mPathCross;			// static cross-hair path
	private Path mPathFocus;			// focus cross-hair path
	private Paint mPaintFocus;			// focus paint object

	///////////////////////////////////////////////////////////////////////////
    // constructor
	public TouchView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// obtain sketch settings
		mParentActivity = SketchActivity.getSketchActivity();
		mSketchSetting = mParentActivity.getSketchSettings();
		mShapeModel = mParentActivity.getShapeManager();
		
		// instantiate shape list
//		mShapeList = mShapeModel.getShapeList();

		// pre-allocate draw/layout objects
		mCanvas = new Canvas();
		mIdentityMatrix = new Matrix();
		mBgFocus = new PointF();
		mBgRect = new RectF();

		// TODO: OR color prior to draw?  default gray will not be visible against gray object
		// current touch position
		mPaintCurrent = new Paint();
		mPaintCurrent.setColor(Color.GRAY);
		mPaintCurrent.setStyle(Paint.Style.STROKE);
		mCurrentRadius = 40.0f;
		
		// current focus cross-hair
		mPathFocus = new Path();
		// static cross-hair to offset into mPathFocus during onDraw
		mPathCross = new Path();
		mPathCross.moveTo(-mCurrentRadius, 0.0f);
		mPathCross.lineTo(mCurrentRadius, 0.0f);
		mPathCross.moveTo(0.0f, -mCurrentRadius);
		mPathCross.lineTo(0.0f, mCurrentRadius);
		mPaintFocus = new Paint();
		mPaintFocus.setColor(Color.GRAY);
		mPaintFocus.setStyle(Paint.Style.STROKE);
		mPaintFocus.setAntiAlias(true);
//		mPaintFocus.setStrokeWidth(mSketchSetting.getSize());  
		mPaintFocus.setStrokeWidth(4.0f);  

		// zoom
		mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
		
	    int orientation = getResources().getConfiguration().orientation; 
    	Log.v(TAG, "getResources orientation: " + orientation);

//		mOrientationListener = new OrientationEventListener(context, SensorManager.SENSOR_DELAY_UI) {
//	        public void onOrientationChanged(int orientation) {
//	        	Log.v(TAG, "onOrientationChanged: " + orientation);
//	        }
//	    };

        // Instantiate the gesture detector with the
        // application context and an implementation of
        // GestureDetector.OnGestureListener
        mDetector = new GestureDetectorCompat(context, this);
        // Set the gesture detector as the double tap
        // listener.
        mDetector.setOnDoubleTapListener(this);
        // set long press enabled
        mDetector.setIsLongpressEnabled(true);

	}
	///////////////////////////////////////////////////////////////////////////
    // onMeasure - create & replace canvas bitmap
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	  
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		mCanvasWidth = MeasureSpec.getSize(widthMeasureSpec);
		mCanvasHeight = MeasureSpec.getSize(heightMeasureSpec);
		mParentActivity.setCanvasWidth(mCanvasWidth);
		mParentActivity.setCanvasHeight(mCanvasHeight);
		Log.v(TAG, "canvas W/H: " + mCanvasWidth + "/" + mCanvasHeight);
			  
		mCanvasBitmap = Bitmap.createBitmap(mCanvasWidth, mCanvasHeight, Bitmap.Config.ARGB_8888);
		mCanvas.setBitmap(mCanvasBitmap);
	    
		setMeasuredDimension(mCanvasWidth, mCanvasHeight);

		// use canvas dimensions to set focus & bounds of background rect (0th)
        List<ShapeObject> shapeList = mShapeModel.getShapeList();
		ShapeObject shapeObject = shapeList.get(0);
		mBgFocus.x = getCanvasWidth()/2;
		mBgFocus.y = getCanvasHeight()/2;
		shapeObject.setFocus(mBgFocus);
//		Log.v(TAG, "bg rect focus: " + shapeObject.getFocus());
		mBgRect.left = 0.0f;
		mBgRect.right = getCanvasWidth();
		mBgRect.top = 0.0f;
		mBgRect.bottom = getCanvasHeight();
		shapeObject.setBound(mBgRect);
//		Log.v(TAG, "bg rect bound: " + shapeObject.getBound());

	}
	///////////////////////////////////////////////////////////////////////////
    // onDraw
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// canvas
		canvas.save();

        List<ShapeObject> shapeList = mShapeModel.getShapeList();
        for (int size = shapeList.size(), i = 0; i < size; i++) {
			mShapeObject = shapeList.get(i);
			Log.d(TAG,"OnDraw: shape " + mShapeObject.getName());
			// draw shape
			drawShape(mCanvas, mShapeObject, mShapeObject.getPaint());
			canvas.drawBitmap(mCanvasBitmap, mIdentityMatrix, null);
			// draw bounding rect
			canvas.drawRect(mShapeObject.getBound(), mPaintCurrent);
//			Log.d(TAG,"OnDraw: bound " + mShapeObject.getBound());
		}
		// draw visual indicator of focus
		mFocus = mShapeModel.getShapeListFocus();
		if (mFocus != ShapeModel.NOFOCUS) {
			mShapeObject = shapeList.get(mFocus);
	        Log.d(TAG,"OnDraw: draw focus(" + mFocus + ") at " + mShapeObject.getFocus().x + ", " + mShapeObject.getFocus().y);
			// draw cross-hair at shape focus
			mPathFocus.set(mPathCross);
			mPathFocus.offset(mShapeObject.getFocus().x, mShapeObject.getFocus().y);
			canvas.drawPath(mPathFocus, mPaintFocus);
			// draw current circle at shape focus
			canvas.drawCircle(mShapeObject.getFocus().x, mShapeObject.getFocus().y, mCurrentRadius, mPaintCurrent);
		}
		
		if (mTouchX != 0.0f && mTouchY != 0.0f) {
			// draw circle at current touch
			canvas.drawCircle(mTouchX, mTouchY, mCurrentRadius, mPaintCurrent);
		}
		canvas.restore();
	}
	
	///////////////////////////////////////////////////////////////////////////
    // onDraw helper
	private boolean drawShape(Canvas canvas, ShapeObject shapeObject, Paint paint) {
		// if draw object defined
		if (shapeObject != null) {
			switch (shapeObject.getShapeType())
			{
			case FREE:
				Path path = (Path)shapeObject.getObject();
				canvas.drawPath(path, paint);
				break;
			case LINE:
				PointF[] pts = (PointF[])shapeObject.getObject();
				canvas.drawLine(pts[0].x, pts[0].y, pts[1].x, pts[1].y, paint);
				break;
			case RECT:
				RectF rect = (RectF)shapeObject.getObject();
				canvas.drawRoundRect(rect, 0.0f, 0.0f, paint);
				break;
			case LABEL:
				// LABEL draw
				RectF rrect = (RectF)shapeObject.getObject();
				String label = shapeObject.getName();
				PointF pt = shapeObject.getFocus();
				float textSize = rrect.bottom - rrect.top;
				paint.setTextSize(textSize); 
				float textWidth = (float) (mSketchSetting.getSize()/2.0);
				paint.setStrokeWidth(textWidth);
				canvas.drawText(label, pt.x, pt.y, paint); 
				break;
			case OVAL:
				RectF orect = (RectF)shapeObject.getObject();
				canvas.drawOval(orect, paint);
				break;
			case CIRCLE:
				// x, y, radius
				float[] circle = (float[])shapeObject.getObject();
				canvas.drawCircle(circle[0], circle[1], circle[2], paint);
				break;
			case IMAGE:
				// TODO: set image bounding rect using canvas width/height?
//			    int canvasWidth = canvas.getWidth();
//			    int canvasHeight = canvas.getHeight();
//				Log.v(TAG, "canvas w/h: " + canvasWidth + "/" + canvasHeight);
//				Log.v(TAG, "image bound: " + shapeObject.getBound());

				Bitmap bitmap = (Bitmap)shapeObject.getObject();
				canvas.drawBitmap(bitmap, null, shapeObject.getBound(), null);
				break;
			default:  
				Log.e(TAG, "drawShape invalid shape: " + shapeObject.getShapeType());
				break;
			}
		}
		else {
			return false;
		}
		return true;
	}
	///////////////////////////////////////////////////////////////////////////
    // clear view
	public boolean clearView() {
		mShapeModel.initShapeList();
		invalidate();
		return true;
	}
	///////////////////////////////////////////////////////////////////////////
    // update paint
	public boolean updatePaint() {
		if (mShapeModel.updatePaint()) {
			invalidate();
			return true;
		}
		return false;
	}
	//////////////////////////////////////////////////////////////////////////
	// getters
	public Bitmap getCanvasBitmap() { return mCanvasBitmap;	}
	public int getCanvasWidth() { return mCanvasWidth; }
	public int getCanvasHeight() { return mCanvasHeight; }

	///////////////////////////////////////////////////////////////////////////
	// gesture detectors
	//
    @Override 
    public boolean onTouchEvent(MotionEvent event){ 
        Log.d(TAG,"TouchEvent: ");
//        Log.d(TAG,"TouchEvent: " + event.toString());
        // gesture detector
        this.mDetector.onTouchEvent(event);
		// pinch-zoom gesture detector
		mScaleGestureDetector.onTouchEvent(event);

		// get pointer index from the event object
	    int pointerIndex = event.getActionIndex();
	    // get pointer ID
	    int pointerId = event.getPointerId(pointerIndex);
	    // get masked (not specific to a pointer) action
	    int maskedAction = event.getActionMasked();   
	    Log.v(TAG, "onTouchEvent id: " + pointerId + ", action: " + actionToString(maskedAction) + ", index: " + pointerIndex );

		if (event.getPointerCount() <= 1) {
			// single touch event 
			Log.v(TAG, "onTouchEvent single-touch x, y: " + event.getX(pointerIndex) + ", " + event.getY(pointerIndex));
			
			mTouchX = event.getX();
			mTouchY = event.getY();
			
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
		        // if no gestures detected, start shape or move
				if (!mGestureDetected) {
					if (mShapeModel.getShapeListFocus() == ShapeModel.NOFOCUS) {
						mShapeModel.startShape(mTouchX, mTouchY);
					}
					else {
						mShapeModel.startMove(mTouchX, mTouchY);
					}
				}
				return true;
			case MotionEvent.ACTION_MOVE:
		        // if no gestures detected, refine shape or move
				if (!mGestureDetected) {
					if (mShapeModel.getShapeListFocus() == ShapeModel.NOFOCUS) {
						mShapeModel.refineShape(mTouchX, mTouchY);
					}
					else {
						mShapeModel.refineMove(mTouchX, mTouchY);
					}
				}
				break;
			case MotionEvent.ACTION_UP:
		        // if no gestures detected, complete shape or move
				if (!mGestureDetected) {
					if (mShapeModel.getShapeListFocus() == ShapeModel.NOFOCUS) {
						mShapeModel.completeShape(mTouchX, mTouchY);
					}
					else {
						mShapeModel.refineMove(mTouchX, mTouchY);
					}
				}
				// clear any gesture
				mGestureDetected = false;
				// clear current touch X,Y 
				mTouchX = 0.0f;
				mTouchY = 0.0f;
				break;
			default:
				return false;
			}
		}
		else {
			// multi-touch
			Log.v(TAG, "onTouchEvent multi-touch pointerIndex, x, y: " + pointerIndex + ", " + event.getX(pointerIndex) + ", " + event.getY(pointerIndex));
			for (int size = event.getPointerCount(), i = 0; i < size; i++) {
				Log.v(TAG, "onTouchEvent multi-touch loop (" + i + ") id, x, y: " + event.getPointerId(i) + ", " + event.getX(i) + ", " + event.getY(i));
			}
			// enable pinch-zoom if shape has focus
			if (mShapeModel.getShapeListFocus() != ShapeModel.NOFOCUS) {
				// resize based on scale factor
				mShapeModel.resizeShape(mScaleFactor);
				
				switch(maskedAction) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_POINTER_DOWN:
//					PointF f = new PointF();
//					f.x = event.getX(pointerIndex);
//					f.y = event.getY(pointerIndex);
					break;
				case MotionEvent.ACTION_MOVE:
//					for (int size = event.getPointerCount(), i = 0; i < size; i++) {
//						Log.v(TAG, "onTouchEvent multi-touch (" + i + ") id, x, y: " + event.getPointerId(i) + ", " + event.getX(i) + ", " + event.getY(i));
//					}
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_POINTER_UP:
				case MotionEvent.ACTION_CANCEL:
					break;
				}
			}
		}
		// schedule repaint
		invalidate();

		// Be sure to call the superclass implementation
        //return super.onTouchEvent(event);

		return true; // true = consumed event
	}
	

    @Override
    public boolean onDown(MotionEvent event) { 
        Log.d(TAG,"onDown: "); 
//        Log.d(TAG,"onDown: " + event.toString()); 
//		Toast.makeText(mParentActivity, "onDown", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, 
            float velocityX, float velocityY) {
        Log.d(TAG, "onFling: " + mShapeModel.getShapeListFocus());
//        Log.d(TAG, "onFling: " + event1.toString()+event2.toString());
//		Toast.makeText(mParentActivity, "onFling", Toast.LENGTH_SHORT).show();
        if ( mShapeModel.getShapeListFocus() != ShapeModel.NOFOCUS) {
            Toast.makeText(mParentActivity, "Double tap to clear focus.", Toast.LENGTH_SHORT).show();
        }
//        else {
//            Toast.makeText(mParentActivity, "LongPress on shape to focus or Single tap to step through shape list.", Toast.LENGTH_SHORT).show();
//        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        Log.d(TAG, "onLongPress: "); 
//        Log.d(TAG, "onLongPress: " + event.toString()); 
        // set focus
        int focus = mShapeModel.setShapeListFocus(event.getX(), event.getY());
        if ( focus != ShapeModel.NOFOCUS) {
            List<ShapeObject> shapeList = mShapeModel.getShapeList();
            mShapeObject = shapeList.get(focus);
        	Log.d(TAG, "onLongPress: shape focus " + mShapeObject.getName());
			Toast.makeText(mParentActivity, "LongPress: Focus on " + mShapeObject.getName(), Toast.LENGTH_SHORT).show();
        }
        mGestureDetected = true;
        // focus altered
        invalidate();
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) {
        Log.d(TAG, "onScroll: ");
//        Log.d(TAG, "onScroll: " + e1.toString()+e2.toString());
//		Toast.makeText(mParentActivity, "onScroll", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        Log.d(TAG, "onShowPress: ");
//        Log.d(TAG, "onShowPress: " + event.toString());
//		Toast.makeText(mParentActivity, "onShowPress", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.d(TAG, "onSingleTapUp: ");
//        Log.d(TAG, "onSingleTapUp: " + event.toString());
//		Toast.makeText(mParentActivity, "onSingleTapUp", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        Log.d(TAG, "onSingleTapConfirmed: ");
//        Log.d(TAG, "onSingleTapConfirmed: " + event.toString());
        // set next shape as focus
        int focus = mShapeModel.setNextShapeListFocus();
        if ( focus != ShapeModel.NOFOCUS) {
            List<ShapeObject> shapeList = mShapeModel.getShapeList();
        	mShapeObject = shapeList.get(focus);
        	Log.d(TAG, "onSingleTapConfirmed: shape focus " + mShapeObject.getName());
			Toast.makeText(mParentActivity, "Single Tap Confirmed: Focus on " + mShapeObject.getName(), Toast.LENGTH_SHORT).show();
        }
        mGestureDetected = true;
        // focus altered
        invalidate();
       return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        Log.d(TAG, "onDoubleTap: ");
//        Log.d(TAG, "onDoubleTap: " + event.toString());
        // clear focus
        mShapeModel.clearShapeListFocus();
        Log.d(TAG, "onDoubleTap: draw list focus " + mShapeModel.getShapeListFocus());
		Toast.makeText(mParentActivity, "Double Tap: focus cleared.", Toast.LENGTH_SHORT).show();
        mGestureDetected = true;
        // focus altered
        invalidate();
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        Log.d(TAG, "onDoubleTapEvent: ");
//        Log.d(TAG, "onDoubleTapEvent: " + event.toString());
//		Toast.makeText(mParentActivity, "onDoubleTapEvent", Toast.LENGTH_SHORT).show();
        return true;
    }

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mScaleFactor *= detector.getScaleFactor();
			// prevent scaling too small or too large
			mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 1.9f));
			Log.v(TAG, "ScaleListener : " + mScaleFactor + ", detector: " + detector.getScaleFactor());
	        mGestureDetected = true;
			return true;
		}
	}

	private static String actionToString(int action) {
	    switch (action) {
	                
	        case MotionEvent.ACTION_DOWN: return "ACTION_DOWN";
	        case MotionEvent.ACTION_MOVE: return "ACTION_MOVE";
	        case MotionEvent.ACTION_POINTER_DOWN: return "ACTION_POINTER_DOWN";
	        case MotionEvent.ACTION_UP: return "ACTION_UP";
	        case MotionEvent.ACTION_POINTER_UP: return "ACTION_POINTER_UP";
	        case MotionEvent.ACTION_OUTSIDE: return "ACTION_OUTSIDE";
	        case MotionEvent.ACTION_CANCEL: return "ACTION_CANCEL";
	
	        // onGenericMotionEvent
	        case MotionEvent.ACTION_HOVER_ENTER: return "ACTION_HOVER_ENTER";
	        case MotionEvent.ACTION_HOVER_EXIT: return "ACTION_HOVER_EXIT";
	        case MotionEvent.ACTION_HOVER_MOVE: return "ACTION_HOVER_MOVE";
	
	    }
	    return "other";
	}
}
