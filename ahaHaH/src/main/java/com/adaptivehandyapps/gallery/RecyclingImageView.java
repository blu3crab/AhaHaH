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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

//
// subclass of ImageView which notifies the drawable when it is being displayed
//
public class RecyclingImageView extends ImageView {

	public RecyclingImageView(Context context) {
		super(context);
	}
	public RecyclingImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	// @see android.widget.ImageView#onDetachedFromWindow()
	@Override
	protected void onDetachedFromWindow() {
		setImageDrawable(null);
		super.onDetachedFromWindow();
	}
	// @see android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable)
	@Override
	public void setImageDrawable(Drawable drawable) {
		// retain previous drawable
		final Drawable prevDrawable = getDrawable();
		// invoke super to set new drawable
		super.setImageDrawable(drawable);
		// notify new drawable it is being displayed
		notifyDrawable(drawable, true);
		// notify old Drawable it is not
		notifyDrawable(prevDrawable, false);
	}
	// notifies drawable it's display state has changed
	private static void notifyDrawable(Drawable drawable, final boolean isDisplayed) {
		if (drawable instanceof RecyclingBitmapDrawable) {
			// notify CountingBitmapDrawable
			((RecyclingBitmapDrawable) drawable).setIsDisplayed(isDisplayed);
		} else if (drawable instanceof LayerDrawable) {
			// recurse each layer of LayerDrawable
			LayerDrawable layerDrawable = (LayerDrawable) drawable;
			for (int i = 0, z = layerDrawable.getNumberOfLayers(); i < z; i++) {
				notifyDrawable(layerDrawable.getDrawable(i), isDisplayed);
			}
		}
	}

}
