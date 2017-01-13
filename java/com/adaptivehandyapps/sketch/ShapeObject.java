// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.sketch;


import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import com.adaptivehandyapps.sketch.SketchSetting.ShapeType;

// shape list object
public class ShapeObject implements java.io.Serializable{
	private static final String TAG = "ShapeObject"; 
	private static final long serialVersionUID = 1L;
	
	///////////////////////////////////////////////
	// accessible via setters/getters
	private ShapeType shapeType;		// shape type
	private String name;				// shape name (image file name)
	private PointF focus;				// center of focus
	private RectF bound;				// bounding rect
	private Paint paint;				// paint
	private Object object;				// coords, bounds, etc
	
	///////////////////////////////////////////////
	public ShapeObject() {
	}
	///////////////////////////////////////////////
	// serialize
	public boolean serialize(ObjectOutputStream oos) {
//		try {
//	    oos.writeObject(shapeType);
//	    oos.writeObject(focus);
//	    oos.writeObject(bound);
//	    oos.writeObject(paint);
//	    oos.writeObject(object);
//		} 

		try {
			oos.writeObject(shapeType);
			Log.v(TAG, "serializing: " + shapeType);
			oos.writeObject(name);
			Log.v(TAG, "serializing: " + name);

			oos.writeFloat(focus.x);
			oos.writeFloat(focus.y);
			oos.writeFloat(bound.bottom);
			oos.writeFloat(bound.left);
			oos.writeFloat(bound.right);
			oos.writeFloat(bound.top);
			oos.writeBoolean(paint.isAntiAlias());
			oos.writeFloat(paint.getStrokeWidth());
			oos.writeInt(paint.getColor());
			oos.writeObject(this.paint.getStyle());
			oos.writeObject(paint.getStrokeJoin());
	
			// save shape object
			switch (shapeType)
			{
			case FREE:  
//				Path path = (Path) this.object; 
//				oos.writeObject(object);
				if (!serializePath(oos)) {
					return false;
				}
				break;
			case LINE:
				PointF[] pts = (PointF[]) object; 
				for (PointF pt : pts) {
					oos.writeFloat(pt.x);
					oos.writeFloat(pt.y);					
				}
				break;
			case RECT:
			case LABEL:
			case OVAL:
				RectF rect = (RectF) object; 
				oos.writeFloat(rect.bottom);
				oos.writeFloat(rect.left);
				oos.writeFloat(rect.right);
				oos.writeFloat(rect.top);
				break;
			case CIRCLE:
				float[] fpt = (float[]) object; 
				oos.writeFloat(fpt[0]);	// center X
				oos.writeFloat(fpt[1]);	// center Y				
				oos.writeFloat(fpt[2]);	// radius	
				break;
			case IMAGE:
				// no output of bitmap - recreate on load
				break;
//			case LABEL:
//				RectF lrect = (RectF) object; 
//				oos.writeFloat(lrect.bottom);
//				oos.writeFloat(lrect.left);
//				oos.writeFloat(lrect.right);
//				oos.writeFloat(lrect.top);
//				break;
			default:  
				Log.e(TAG, "invalid shape: " + shapeType);
				break;
			}
		} catch (Exception e) {
			Log.e(TAG, "serialize exception: " + e.getMessage());	
			return false;
		}
		return true;
	}
	
	public boolean deserialize (ObjectInputStream ois) {
		
//		try {
//            ObjectInputStream ois = new ObjectInputStream(fis);
//			Log.v(TAG, "ois available: " + ois.available());
//			shapeType = (ShapeType) ois.readObject();
//			focus = (PointF) ois.readObject();
//			bound = (RectF) ois.readObject();
//			paint = (Paint) ois.readObject();
//			object = ois.readObject();
//			ois.close();
//		}
		try {
			Log.v(TAG, "ois available: " + ois.available());
			shapeType = (ShapeType) ois.readObject();
    		Log.v(TAG, "deserializing " + shapeType);
			name = (String) ois.readObject();
    		Log.v(TAG, "deserializing " + name);

			focus = new PointF(ois.readFloat(),ois.readFloat());
    		Log.v(TAG, "focus " + getFocus().x + ", " + getFocus().y);
    		Log.v(TAG, "focus " + getFocus());
			bound = new RectF();
			bound.bottom = ois.readFloat();
			bound.left = ois.readFloat();
			bound.right = ois.readFloat();
			bound.top = ois.readFloat();
			paint = new Paint();
			paint.setAntiAlias(ois.readBoolean());
			paint.setStrokeWidth(ois.readFloat());
			paint.setColor(ois.readInt());
			paint.setStyle((Paint.Style)ois.readObject());
			paint.setStrokeJoin((Paint.Join)ois.readObject());
			// load shape object
			switch (shapeType)
			{
			case FREE:  
//				Path path = (Path) this.object; 
//				object = ois.readObject();
				if (!deserializePath(ois)) {
					return false;
				}
				break;
			case LINE:
				PointF[] pts = new PointF[2];
				pts[0] = new PointF(ois.readFloat(), ois.readFloat());
				pts[1] = new PointF(ois.readFloat(), ois.readFloat());
				object = pts;
				break;
			case RECT:
			case LABEL:
			case OVAL:
				RectF rect = new RectF(); 
				rect.bottom = ois.readFloat();
				rect.left = ois.readFloat();
				rect.right = ois.readFloat();
				rect.top = ois.readFloat();
				object = rect;
				break;
			case CIRCLE:
				float[] fpt = new float[3]; 
				fpt[0] = ois.readFloat();	// center X
				fpt[1] = ois.readFloat();	// center Y				
				fpt[2] = ois.readFloat();	// radius	
				object = fpt;
				break;
			case IMAGE:
				try {
					File f = new File(name);
					if (f.exists()) {
						// generate bitmap
						Bitmap bitmap = BitmapFactory.decodeFile(name, null);
						object = bitmap; 
					}
					else {
						Log.e(TAG, "image file missing: " + name);
						return false;
					}
				}
				catch (Exception e) {
					Log.e(TAG, "unable to decode: " + name);
					return false;					
				}
				break;
//			case LABEL:
//				RectF lrect = new RectF(); 
//				lrect.bottom = ois.readFloat();
//				lrect.left = ois.readFloat();
//				lrect.right = ois.readFloat();
//				lrect.top = ois.readFloat();
//				object = lrect;
//				break;
			default:  
				Log.e(TAG, "invalid shape: " + shapeType);
				return false;
			}

		}
		catch (Exception e) {
			Log.e(TAG, "object input stream exception: " + e.getMessage());	
			return false;
		}
		return true;
	}
	///////////////////////////////////////////////
	// serialize/deserialize path
	private boolean serializePath(ObjectOutputStream oos) {
		try {
			Path path = (Path) object;
			PathMeasure pm = new PathMeasure(path, false);
			Log.v(TAG, "path len: " + pm.getLength());
			float len = pm.getLength();
			int nPts = (int)len/5;
			float delta = len/(float)(nPts-1);
			Log.v(TAG, "path len: " + len + ", # npts: " + nPts + ", delta: " + delta);
//			float[] x = new float[nPts];
//			float[] y = new float[nPts];
			float dst = 0;
			float[] pos = new float[2];
			float[] tan = new float[2];
			oos.writeInt(nPts);
			for (int i = 0; i < nPts-1; i++) {
				pm.getPosTan(dst, pos, tan);
//				Log.v(TAG, "path dst: " + dst + ", x: " + pos[0] + ", y: " + pos[1]);
				oos.writeFloat(pos[0]);
				oos.writeFloat(pos[1]);
				dst+=delta;
			}
			pm.getPosTan(len, pos, tan);
			Log.v(TAG, "path dst: " + dst + ", x: " + pos[0] + ", y: " + pos[1]);
			oos.writeFloat(pos[0]);
			oos.writeFloat(pos[1]);
		}
		catch (Exception e) {
			Log.e(TAG, "serializePath exception: " + e.getMessage());	
			return false;
		}
		return true;
	}

	private boolean deserializePath(ObjectInputStream ois) {
		try {
			int nPts = ois.readInt();
			Log.v(TAG, "# npts: " + nPts);
			Path path = new Path();
			float x = ois.readFloat();
			float y = ois.readFloat();
			Log.v(TAG, "x: " + x + ", y: " + y);
			path.moveTo(x, y);
			for (int i = 1; i < nPts; i++) {
				x = ois.readFloat();
				y = ois.readFloat();
//				Log.v(TAG, "x: " + x + ", y: " + y);
				path.lineTo(x, y);
			}
			object = path;
		}
		catch (Exception e) {
			Log.e(TAG, "deserializePath exception: " + e.getMessage());	
			return false;
		}
		return true;
	}

	///////////////////////////////////////////////
	// setters/getters
	public ShapeType getShapeType() {
		return shapeType;
	}
	public void setShapeType(ShapeType shapeType) {
		this.shapeType = shapeType;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public PointF getFocus() {
		return focus;
	}
	public void setFocus(PointF focus) {
		this.focus = focus;
	}
	public RectF getBound() {
		return bound;
	}
	public void setBound(RectF bound) {
		this.bound = bound;
	}
	public Paint getPaint() { return paint;	}
	public void setPaint(Paint paint) {
		this.paint = paint;
	}
	public Object getObject() {
		return object;
	}
	public void setObject(Object object) {
		this.object = object;
	}

}
