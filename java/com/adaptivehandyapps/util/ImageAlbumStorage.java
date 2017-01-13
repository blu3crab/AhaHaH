// Project: AHA Handyman Helper
// Contributor(s): M.A.Tucker
// Origination: MAR 2013
// Copyright © 2015 Adaptive Handy Apps, LLC.  All Rights Reserved.
package com.adaptivehandyapps.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;


public final class ImageAlbumStorage {

	///////////////////////////////////////////////////////////////////////////////
	// class data
	private final String TAG = "ImageAlbumStorage";

	// sub-directories for full, fit & thumb image versions
	public static final String IMG_DIR_FULL = "/full/";  	// full res image
	public static final String IMG_DIR_FIT = "/fit/";		// fit to device display
	public static final String IMG_DIR_THUMB = "/thumb/";	// 100x100 thumb
//	public static final String IMG_DIR_SKETCH = "/sketch/";	// sketch (fit)
	
	// standard storage location
	private static final String CAMERA_DIR = "/dcim/";
	// image prefix & suffix
	private static final String JPEG_FILE_PREFIX = "IMG_";
	private static final String JPEG_FILE_SUFFIX = ".jpg";
		
    private String mAlbumName;
	private String[] mFitImages;
	private String[] mThumbImages;
	private boolean mRefreshFitLists;
	private boolean mRefreshThumbLists;
	
	///////////////////////////////////////////////////////////////////////////////
    // constructor
    public ImageAlbumStorage(String albumName) {
    	mAlbumName = albumName;  // R.string.album_name
    	refreshImageLists(true); // indicate image lists need refresh
        // ensure thumb & fit images are sync: identical count & names
    	boolean repaired = syncFitThumbImages();
    	if (repaired) {
        	refreshImageLists(true); // indicate image lists need refresh
	        Log.e(TAG, albumName + " repaired. ");
    	}
    }
	///////////////////////////////////////////////////////////////////////////////
    // ensure thumb & fit images are sync: identical count & names
    private boolean syncFitThumbImages() {
    	boolean damaged = false;
		String[] fitNames;
		String[] thumbNames;

		int fitCount = getImageFitCount();
    	int thumbCount = getImageThumbCount();
    	// damaged if thumb & fit counts differ
    	if (fitCount != thumbCount) {
	        Log.e(TAG, "Fit-Thumb mismatch: " + fitCount + " != " + thumbCount); 
	        damaged = true;
	        // get list of images names
    		fitNames = getImagesList(IMG_DIR_FIT, false);
    		thumbNames = getImagesList(IMG_DIR_THUMB, false);
    	}
    	else { 
	        // get list of images names
    		fitNames = getImagesList(IMG_DIR_FIT, false);
    		thumbNames = getImagesList(IMG_DIR_THUMB, false);

	    	int i = 0;
	    	while (i < fitCount && !damaged) {
	    		// damaged if names differ
	    		if (!fitNames[i].equals(thumbNames[i])) {
	    			damaged = true;
					Log.v(TAG, "damage detected (" + i + ") fit: " + fitNames[i] + ", thumb: " + thumbNames[i]); 
	    		}
	    		++i;
	    	}
    	}
    	if (damaged) {
    		// repair
			List<String> fitList = Arrays.asList(fitNames);
			List<String> thumbList = Arrays.asList(thumbNames);
			// if fit not in thumb list, delete fit
			deleteMismatchedFiles(fitList, thumbList, mFitImages);
			// if thumb not in fit list, delete thumb
			deleteMismatchedFiles(thumbList, fitList, mThumbImages);
    	}
    	return damaged;
    }
	///////////////////////////////////////////////////////////////////////////////
	// delete mismatched files from test list against reference list
    private boolean deleteMismatchedFiles(
    		List<String> testList, List<String> refList,
    		String[] testPaths) {
    	boolean deleted = false;
		// if test image not in reference list, delete test image
		for (String f : testList) {
			if (!refList.contains(f)) {
				int i = testList.indexOf(f);
				// delete 
				Log.v(TAG, "deleting " + f + " with path: " + testPaths[i]);
				File file = new File(testPaths[i]);
				boolean success = file.delete();
				if (success) {
					deleted = true;
					Log.v(TAG, "success deleting " + f + " with path: " + mThumbImages[i]);
				}
			}
		}   	
    	return deleted;
    }
	///////////////////////////////////////////////////////////////////////////////
	// add bitmap to media DB
	public String addBitmapToMediaDB(Activity activity, Bitmap bitmap, String dir, String name) {
		File f = null;
		String imagePath = null;
		try {
			f = createImageFile(dir, name);
			imagePath = f.getAbsolutePath();
			
		    try {
		        FileOutputStream fos = new FileOutputStream(f);
		        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
		        fos.close();
		    } catch (FileNotFoundException e) {
		        Log.d(TAG, "File not found: " + e.getMessage());
		    } catch (IOException e) {
		        Log.d(TAG, "Error accessing file: " + e.getMessage());
		    }  
		    
		    addToMediaDB(activity, imagePath);

		} catch (IOException e) {
	        Log.d(TAG, "Exception: " + e.getMessage());
			e.printStackTrace();
			f = null;
		}
		return imagePath;
	}
	///////////////////////////////////////////////////////////////////////////////
	// add to media DB
	public void addToMediaDB(Activity activity, String imagePath) {
		// establish intent to scan file and add to media DB
		Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
		File f = new File(imagePath);
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		activity.sendBroadcast(mediaScanIntent);
	}
	///////////////////////////////////////////////////////////////////////////////
	// timestamp image file name
	public final static String timestampImageName() {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
		String imageName = JPEG_FILE_PREFIX + timeStamp + JPEG_FILE_SUFFIX;
		return imageName;
	}
	///////////////////////////////////////////////////////////////////////////////
	// create image file
	public File createImageFile(String imageDir, String imageName) throws IOException {
		// get or create image album directory
		File albumF = makeAlbumDir(imageDir);
		// create image file
//		File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        if (albumF != null) {
            File imageF = new File(albumF.getPath() + File.separator + imageName);
            return imageF;
        }
        return null;
	}
    ///////////////////////////////////////////////////////////////////////////////
    // test if media mounted
    public boolean isMediaMounted() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
     }
    ///////////////////////////////////////////////////////////////////////////////
	// get album directory, creating subdirs if not present
	public File makeAlbumDir(String imgdir) {
		File storageDir = null;
		
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			storageDir = getAlbumStorageDir(mAlbumName + imgdir);
			Log.d(TAG, "makeAlbumDir: storageDir " + storageDir);
			
			if (storageDir != null) {
				if (!storageDir.mkdirs()) {
					if (!storageDir.exists()) {
						Log.e(TAG, "makeAlbumDir unable to make directory.");
						return null;
					}
				}
			}
		} else {
			Log.v(TAG, "makeAlbumDir: External storage not mounted R/W.");
		}
		return storageDir;
	}
	///////////////////////////////////////////////////////////////////////////////
	// public getters/setters
    public String getImageAlbumName() { return mAlbumName; }

	public List<String> getProjectFolders(String projectsFolder) {
		List<String> folderList = new ArrayList<String>();
		File folder = getAlbumStorageDir(projectsFolder);
		if (folder != null && folder.isDirectory()) {

			File[] fileList = folder.listFiles();
			for (int j = 0; j < fileList.length; j++) {
				if (fileList[j].isDirectory()) {
					folderList.add(fileList[j].getName());
					Log.v(TAG, "Project folder(" + j + ") " + folderList.get(j));
				}
			}
		}
		else {
			// not a project folder!
			Log.e(TAG, "Invalid project folder: " + projectsFolder);
		}
		return folderList;
	}

	///////////////////////////////////////////////////////////////////////////////
	// private: get or create parent album directory
	private File getAlbumStorageDir(String albumName) {
		Log.v(TAG, "getAlbumStorageDir: " + Environment.getExternalStorageDirectory() + CAMERA_DIR + albumName);
		return new File (Environment.getExternalStorageDirectory() + CAMERA_DIR + albumName);
	}
	
	private String[] getImagesList(String imgdir, boolean fullpath) {
    	// get list of images in specified dir
		String[] imageList = null;
		File folder;
    	folder = makeAlbumDir(imgdir);
		if (folder != null && folder.isDirectory()) {

			File[] fileList = folder.listFiles();
			// sort order not guarenteed - sort on last modified date
			Arrays.sort(fileList, new Comparator<File>(){
			    public int compare(File f1, File f2)
			    {
			        return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
			    } });
			imageList = new String[fileList.length];
			for (int j = 0; j < fileList.length; j++) {
				if (fileList[j].isFile()) {
					if (fullpath) {
						imageList[j] = fileList[j].getPath();
					}
					else {
						imageList[j] = fileList[j].getName();						
					}
					Log.v(TAG, "Image(" + j + ") " + imageList[j]);
				}
			}

		} else {
			// not an album!
			Log.e(TAG, "Invalid album name: " + mAlbumName + imgdir);
		}
		return imageList;
	}
	
	///////////////////////////////////////////////////////////////////////////////
    // gallery accessor methods
	//
	// control image lists refresh
	public final void refreshImageLists(boolean refresh)
	{
		mRefreshFitLists = refresh;
		mRefreshThumbLists = refresh;
	}
    public final String getImageFitPath (int position) {
    	if (mRefreshFitLists) {
    		mFitImages = getImagesList(IMG_DIR_FIT, true);
    		mRefreshFitLists = false;
    	}
        if (mFitImages != null) {
            Log.v(TAG, "getImagePath: " + mFitImages[position]);
            return mFitImages[position];
        }
        return "nada";
    }
    public final String getImageThumbPath (int position) {
    	if (mRefreshThumbLists) {
    		mThumbImages = getImagesList(IMG_DIR_THUMB, true);
    		mRefreshThumbLists = false;
    	}
        if (mFitImages != null) {
            Log.v(TAG, "getImageThumbPath: " + mThumbImages[position]);
            return mThumbImages[position];
        }
        return "nada";
    }
    public final int getImageFitCount() {
    	if (mRefreshFitLists) {
    		mFitImages = getImagesList(IMG_DIR_FIT, true);
    		mRefreshFitLists = false;
    	}
    	if (mFitImages != null) {
    		return mFitImages.length;
    	}
		return 0;
//		return getImageCount(IMG_DIR_FIT);	
    }
    public final int getImageThumbCount() {
    	if (mRefreshThumbLists) {
    		mThumbImages = getImagesList(IMG_DIR_THUMB, true);
    		mRefreshThumbLists = false;
    	}
    	if (mThumbImages != null) {
    		return mThumbImages.length;
    	}
		return 0;
//		return getImageCount(IMG_DIR_THUMB);	
    }
//    public final int getImageCount(String dir) {
//	  	if (mRefreshImageLists) {
//	  		mImages = getImagesList(dir);
//	  		mRefreshImageLists = false;
//	  	}
//	  	if (mImages != null) {
//	  		return mImages.length;
//	  	}
//		return 0;	
//    }
	///////////////////////////////////////////////////////////////////////////////
}
