/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adaptivehandyapps.gallery;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

public class RecyclingBitmapDrawable extends BitmapDrawable {
	private static final String TAG = "RecyclingBitmapDrawable";
    private static final boolean DEBUG = true;

	private int mCacheRefCount = 0;
	private int mDisplayRefCount = 0;
	private boolean mHasBeenDisplayed;
	
	public RecyclingBitmapDrawable(Resources res, Bitmap bitmap) {
		super(res, bitmap);
	}
	
	// notify drawable of state change while tracking ref count
	public void setIsDisplayed(boolean isDisplayed) {
		synchronized (this) {
			if (isDisplayed) {
				mDisplayRefCount++;
				mHasBeenDisplayed = true;
			} else {
				mDisplayRefCount--;
			}
		}
		checkState();
	}
	
    public void setIsCached(boolean isCached) {
        synchronized (this) {
            if (isCached) {
                mCacheRefCount++;
            } else {
                mCacheRefCount--;
            }
        }
        checkState();
    }

	// check if recycle should be invoked
	private synchronized void checkState() {
		// if ref count 0 & image has been displayed
		if (mCacheRefCount <= 0 && mDisplayRefCount <= 0 && mHasBeenDisplayed && hasValidBitmap()) {
			if (DEBUG) {
				Log.d(TAG, "Recycling " + toString());
			}
			
			getBitmap().recycle();
		}
	}
	
	private synchronized boolean hasValidBitmap() {
		Bitmap bitmap = getBitmap();
		return bitmap != null && !bitmap.isRecycled();
	}
	
	
}
