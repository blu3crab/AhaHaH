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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class ImageAlbumStorage {
	private static final String TAG = "ImageAlbumStorage";

	// standard storage location
	private static final String CAMERA_DIR_ROOT = "/dcim/";
	private static final String CAMERA_DIR = "/camera/";
	// image prefix & suffix
	private static final String JPEG_FILE_PREFIX = "AHA_";
	private static final String JPEG_FILE_SUFFIX = ".jpg";
		
	///////////////////////////////////////////////////////////////////////////////
	// add bitmap to media DB
	public static final String addBitmapToMediaDB(Activity activity, Bitmap bitmap, String albumName, String name) {
		String imagePath = null;
		try {
            File f = createImageFile(albumName, name);
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
		}
		return imagePath;
	}
	///////////////////////////////////////////////////////////////////////////////
	// add to media DB
	public static final void addToMediaDB(Activity activity, String imagePath) {
		// establish intent to scan file and add to media DB
		Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
		File f = new File(imagePath);
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		activity.sendBroadcast(mediaScanIntent);
	}
	///////////////////////////////////////////////////////////////////////////////
	// timestamp image file name
	public static final String timestampImageName() {
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
		String imageName = JPEG_FILE_PREFIX + timeStamp + JPEG_FILE_SUFFIX;
		return imageName;
	}
	///////////////////////////////////////////////////////////////////////////////
	// create image file
	public static final File createImageFile(String albumName, String imageName) throws IOException {
		// get or create image album directory
		File albumF = makeAlbumDir(albumName);
		// create image file
        if (albumF != null) {
            File imageF = new File(albumF.getPath() + File.separator + imageName);
            return imageF;
        }
        return null;
	}
    ///////////////////////////////////////////////////////////////////////////////
    // test if media mounted
    public static final boolean isMediaMounted() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
     }
    ///////////////////////////////////////////////////////////////////////////////
	// get album directory, creating subdirs if not present
	public static final File makeAlbumDir(String albumName) {
		File storageDir = null;
		
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			// if FIT, set storage dir to AHA project album
			if (albumName != null) {
				storageDir = getAlbumStorageDir(albumName);
			}
			else {
				// not FIT, set storage dir to DCIM
				storageDir = getAlbumStorageDir(CAMERA_DIR);
			}
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
	public static final List<String> getProjectFolders(String projectsFolder) {
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
	private static final File getAlbumStorageDir(String albumName) {
		Log.v(TAG, "getAlbumStorageDir: " + Environment.getExternalStorageDirectory() + CAMERA_DIR_ROOT + albumName);
		return new File (Environment.getExternalStorageDirectory() + CAMERA_DIR_ROOT + albumName);
	}
	///////////////////////////////////////////////////////////////////////////////
}
