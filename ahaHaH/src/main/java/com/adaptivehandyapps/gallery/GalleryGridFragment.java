// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.gallery;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.adaptivehandyapps.ahahah.R;
import com.adaptivehandyapps.bitmapfun.ImageCache.ImageCacheParams;
import com.adaptivehandyapps.activity.GalleryActivity;
import com.adaptivehandyapps.activity.AhaHahActivity;
import com.adaptivehandyapps.util.ImageAlbumStorage;
//
// GridView implementation powering the GalleryGridActivity
//
public class GalleryGridFragment extends Fragment implements AdapterView.OnItemClickListener {
	private static final String TAG = "GalleryGridFragment";
    private static final boolean DEBUG = true;

	private static final String IMAGE_CACHE_DIR = "thumbs";

	private int mImageThumbSize;
	private int mImageThumbSpacing;
	private ImageAdapter mAdapter;
	private ImageFetcher mImageFetcher;
	
	private Bundle args = null;
	private int reqCode = 0;
	/////////////////////////////////////////////////////////////
    private OnImageSelectedListener mCallback;

    // Container Activity must implement this interface
    public interface OnImageSelectedListener {
        public void onImageSelected(int position);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnImageSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

	////////////////////////////////////////////////////////////
	// empty constructor per fragment doc
	public GalleryGridFragment() {}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// determine activation request code
		Bundle args = this.getArguments();
		if (args != null) {
			reqCode = args.getInt(GalleryActivity.EXTRA_REQUEST_CODE);
			Log.v(TAG, "reqCode = " + reqCode);
		}
		
		setHasOptionsMenu(true);
		
		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
		mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
		
		mAdapter = new ImageAdapter(getActivity());
		
		ImageCacheParams cacheParams = new ImageCacheParams(getActivity(), IMAGE_CACHE_DIR);
		cacheParams.setMemCacheSizePercent(0.25f); // cache 25% of app memory
		
		// ImageFecther loads images asynchronously
		mImageFetcher = new ImageFetcher(getActivity(), mImageThumbSize);
		mImageFetcher.setLoadingImage(R.drawable.empty_photo);
		mImageFetcher.addImageCache(getActivity().getSupportFragmentManager(), cacheParams);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceBundle) {
		
		final View v = inflater.inflate(R.layout.gallery_grid_fragment, container, false);
		final GridView mGridView = (GridView) v.findViewById(R.id.gridView);
		
		mGridView.setAdapter(mAdapter);
		mGridView.setOnItemClickListener(this);
		mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView absListView, int scrollState) {
				// Auto-generated method stub
				// pause fetcher to ensure smoother scrolling when flinging
				if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
					mImageFetcher.setPauseWork(true);
				} else {					
					mImageFetcher.setPauseWork(false);
				}				
			}
			
			@Override
			public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				// Auto-generated method stub				
			}
		});
		
		// listener to get final width of GriView, calculate number of column & widths to get square thumbnails
		mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					
					@Override
					public void onGlobalLayout() {
						// Auto-generated method stub
						if (mAdapter.getNumColumns() == 0) {
							final int numColumns = (int) Math.floor(mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
							
							if (numColumns > 0) {
								final int columnWidth = (mGridView.getWidth() / numColumns) - mImageThumbSpacing;
								mAdapter.setNumColumns(numColumns);
								mAdapter.setItemHeight(columnWidth);
								
								if (DEBUG) {
									Log.d(TAG, "onCreateView - numColumns = " + numColumns);
								}
							}
						}
						
					}
				});
		return v;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mImageFetcher.setExitTasksEarly(false);
		mAdapter.notifyDataSetChanged();
	}	@Override
	public void onPause() {
		super.onPause();
		mImageFetcher.setExitTasksEarly(true);
		mImageFetcher.setPauseWork(false);
	}
	public void onDestroy() {
		super.onDestroy();
	}
	//////////////////////////////////////////////////////////////////////
	@TargetApi(16)
	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		if (DEBUG) {
			Log.d(TAG, "onItemClick: position: " + position + ", id: " + id);
		}
		// if sketch activity selection, return id (index into gallery)
		if (reqCode == GalleryActivity.REQUEST_CODE_SELECT_BACKDROP ||
			reqCode == GalleryActivity.REQUEST_CODE_SELECT_OVERLAY	) {
			Log.d(TAG, "onItemClick: callback w/ position: " + (int) id);
	        mCallback.onImageSelected((int) id);
		}
		else {
			final Intent i = new Intent(getActivity(), GalleryDetailActivity.class);
			i.putExtra(GalleryDetailActivity.EXTRA_IMAGE, (int) id);
			startActivity(i);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.gallery_menu, menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (DEBUG) {
			Log.d(TAG, "onOptionsItemSelected...");
		}
		switch (item.getItemId()) {
		case R.id.clear_cache:
			mImageFetcher.clearCache();
			Toast.makeText(getActivity(), R.string.clear_cache_complete_toast, Toast.LENGTH_SHORT).show();
			return true;
//		case android.R.id.home:
//			// app icon in action bar clicked; go home
//			final Intent intent = new Intent(getActivity(), TucAX1Activity.class);
//			//intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			startActivity(intent);
//			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	//
	// main adaptor backing GridView...
	//
	private class ImageAdapter extends BaseAdapter {
		
		private final Context mContext;
		private int mItemHeight = 0;
		private int mNumColumns = 0;
		private int mActionBarHeight = 0;
		private GridView.LayoutParams mImageViewLayoutParams;
	    
		// use parent instances of mImageAlbumStorage
		private Activity mParentActivity;
		private ImageAlbumStorage mImageAlbumStorage;
		
		public ImageAdapter(Context context) {
			super();
			mContext = context;
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
			
			//mImageAlbumStorage = new ImageAlbumStorage(getString(R.string.album_name));
			// use parent activity to access mImageAlbumStorage
			mParentActivity = AhaHahActivity.mParentActivity;
			mImageAlbumStorage = ((AhaHahActivity)mParentActivity).getImageAlbumStorage();

			// calc action bar height
			TypedValue tv = new TypedValue();
			if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
				mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
			}
		}
		
		@Override
		public int getCount() {
			// num images plus num columns for top empty row
			//return Images.imageThumbUrls.length + mNumColumns;
			return mImageAlbumStorage.getImageThumbCount() + mNumColumns;
		}
		@Override
		public Object getItem(int position) {
			//return position < mNumColumns? null : Images.imageThumbUrls[position-mNumColumns];
			return position < mNumColumns? null : mImageAlbumStorage.getImageThumbPath(position-mNumColumns);
		}
		@Override 
		public long getItemId(int position) {
			return position < mNumColumns? 0 : position-mNumColumns;
		}
		@Override
		public int getViewTypeCount() {
			// view types: normal ImageView plus top row of empty views
			return 2;
		}
		@Override 
		public int getItemViewType(int position) {
			return position < mNumColumns? 1 : 0;
		}
		@Override
		public boolean hasStableIds() {
			return true;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup container) {
			// check for top row
			if (position < mNumColumns) {
				if (convertView == null) {
					convertView = new View(mContext);
				}
				// set empty view with ActionBar height
				convertView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,mActionBarHeight));
				return convertView;
			}
			
			// handle main ImageView thumbs
			ImageView imageView;
			if (convertView == null) { // convertView is not recycled, instantiate & initialize
				imageView = new RecyclingImageView(mContext);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setLayoutParams(mImageViewLayoutParams);
			} else {
				imageView = (ImageView) convertView;
			}
			
			// check height matches calculated column width
			if (imageView.getLayoutParams().height != mItemHeight) {
				imageView.setLayoutParams(mImageViewLayoutParams);
			}
			
			// load image asynchronously into imageView while showing placeholder image as background task runs
			//mImageFetcher.loadImage(Images.imageThumbUrls[position-mNumColumns], imageView);
			mImageFetcher.loadImage(mImageAlbumStorage.getImageThumbPath(position-mNumColumns), imageView);
			return imageView;
		}
		
		public void setItemHeight(int height) {
			if (height == mItemHeight) {
				return;
			}
			mItemHeight = height;
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, mItemHeight);
			mImageFetcher.setImageSize(height);
			notifyDataSetChanged();
		}
		
		public void setNumColumns(int numColumns) {
			mNumColumns = numColumns;
		}
		
		public int getNumColumns() {
			return mNumColumns;
		}
	}
}
