// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.gallery;

import android.view.View.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.adaptivehandyapps.ahahah.R;

public class GalleryDetailFragment extends Fragment {
	private static final String IMAGE_DATA_EXTRA = "extra_image_data";
	private String mImageUrl;
	private ImageView mImageView;
	private ImageFetcher mImageFetcher;
	
	// factory method to generate new instance of fragment given image number
	public static GalleryDetailFragment newInstance(String imageUrl) {
		final GalleryDetailFragment f = new GalleryDetailFragment();
		
		final Bundle args = new Bundle();
		args.putString(IMAGE_DATA_EXTRA, imageUrl);
		f.setArguments(args);
		
		return f;
	}
	
	// empty constructor per Fragment doc
	public GalleryDetailFragment() {}
	
	// populate image using url from extras
	@Override 
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mImageUrl = getArguments() != null? getArguments().getString(IMAGE_DATA_EXTRA) : null;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.gallery_detail_fragment, container, false);
		mImageView = (ImageView) v.findViewById(R.id.imageView);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		// use parent activity to load image asynchronously into ImageView so single cache can be used
		if (GalleryDetailActivity.class.isInstance(getActivity())) {
			mImageFetcher = ((GalleryDetailActivity) getActivity()).getImageFetcher();
			mImageFetcher.loadImage(mImageUrl, mImageView);
		}
		// pass clicks on to ImageView for parent activity to handle
		if (OnClickListener.class.isInstance(getActivity())) {
			mImageView.setOnClickListener((OnClickListener) getActivity());
		}
	}
	
}
