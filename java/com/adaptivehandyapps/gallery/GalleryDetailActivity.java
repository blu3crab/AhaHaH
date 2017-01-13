// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.gallery;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import com.adaptivehandyapps.ahahah.R;
import com.adaptivehandyapps.bitmapfun.ImageCache;
import com.adaptivehandyapps.activity.AhaHahActivity;
import com.adaptivehandyapps.util.ImageAlbumStorage;

public class GalleryDetailActivity extends FragmentActivity implements OnClickListener {
	private static final String IMAGE_CACHE_DIR = "images";
	public static final String EXTRA_IMAGE = "extra_image";
	
	private ImagePagerAdapter mAdapter;
	private ImageFetcher mImageFetcher;
	private ViewPager mPager;
    
	// use parent instances of mImageAlbumStorage
	private Activity mParentActivity;
	private ImageAlbumStorage mImageAlbumStorage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gallery_detail_pager);
		
		// fetch screen dimensions to use as max size since image load are full screen mode
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		final int height = displayMetrics.heightPixels;
		final int width = displayMetrics.widthPixels;
		
		// use half the longest width to resize the image for resolution that is good enough for portrait or landscape...
		// best quality would not halve size but takes more memory
		final int longest = (height > width? height : width) / 2;
		
		ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
		cacheParams.setMemCacheSizePercent(0.25f); // 25% of app memory
		
		//mImageAlbumStorage = new ImageAlbumStorage(getString(R.string.album_name));
		// use parent activity to access mImageAlbumStorage
		mParentActivity = AhaHahActivity.mParentActivity;
		mImageAlbumStorage = ((AhaHahActivity)mParentActivity).getImageAlbumStorage();

		// load images asynchronously
		mImageFetcher = new ImageFetcher(this, longest);
		mImageFetcher.addImageCache(getSupportFragmentManager(), cacheParams);
		mImageFetcher.setImageFadeIn(false);
		
		// setup ViewPager and backing adapter 
		//mAdapter = new ImagePagerAdapter(getSupportFragmentManager(), Images.imageUrls.length);
		mAdapter = new ImagePagerAdapter(getSupportFragmentManager(), mImageAlbumStorage.getImageFitCount());
		mPager = (ViewPager) findViewById(R.id.view_pager);
		mPager.setAdapter(mAdapter);
		mPager.setPageMargin((int) getResources().getDimension(R.dimen.image_detail_pager_margin));
		mPager.setOffscreenPageLimit(2);
		
	
		// setup activity to go full screen
		getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN);
		
		// supported for Honeycomb (3.x) or later
		final ActionBar actionBar = getActionBar();
		
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		mPager.setOnSystemUiVisibilityChangeListener(
				new View.OnSystemUiVisibilityChangeListener() {					
					@Override
					public void onSystemUiVisibilityChange(int visibility) {
						// Auto-generated method stub
						if ((visibility & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
							actionBar.hide();
						} else {
							actionBar.show();
						}						
					}
				});
		// start low profile mode & hide ActionBar
		mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
		actionBar.hide();
		
		// set current item based on extra passed in to this activity
		final int extraCurrentItem = getIntent().getIntExtra(EXTRA_IMAGE, -1);
		if (extraCurrentItem != -1) {
			mPager.setCurrentItem(extraCurrentItem);
		}
	}

    @Override
    public void onResume() {
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
    }
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.gallery_menu, menu);
		return true;
	}
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.clear_cache:
                mImageFetcher.clearCache();
                Toast.makeText(
                        this, R.string.clear_cache_complete_toast,Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // invoked by ViewPager child fragments to load images
    public ImageFetcher getImageFetcher() {
    	return mImageFetcher;
    }
    
    // main adapter backing ViewPager to create/destroy items on the fly
    private class ImagePagerAdapter extends FragmentStatePagerAdapter {
    	private final int mSize;
    	
    	public ImagePagerAdapter(FragmentManager fm, int size) {
    		super(fm);
    		mSize = size;
    	}
    	
    	@Override
    	public int getCount() {
    		return mSize;
    	}
    	@Override
    	public Fragment getItem(int position) {
    		//return GalleryDetailFragment.newInstance(Images.imageUrls[position]);
    		return GalleryDetailFragment.newInstance(mImageAlbumStorage.getImageFitPath(position));
    	}    	
    }
    
    // setup the ImageView in the ViewPager children fragments to enable/disable low profile mode
    @Override
    public void onClick(View v) {
    	final int vis = mPager.getSystemUiVisibility();
    	if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
    		mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    	} else {
    		mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);    		
    	}
    }
}
