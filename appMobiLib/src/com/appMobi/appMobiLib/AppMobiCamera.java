package com.appMobi.appMobiLib;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.appMobi.appMobiLib.util.Debug;


public class AppMobiCamera extends AppMobiCommand {
	private static boolean debug = Debug.isDebuggerConnected();
	private static final String staticCameraPrefsKey = "APPMOBI_CAMERA_PREFS";
	private static final String cameraPrefsFileName = "APPMOBI_CAMERA_PREFS_FILENAME";
	private static final String cameraPrefsQuality = "APPMOBI_CAMERA_PREFS_QUALITY";
	private static final String cameraPrefsIsPNG = "APPMOBI_CAMERA_PREFS_IS_PNG";
	private boolean busy = false;
	//private File outputFile;
	//private boolean isPNG;
	//private int quality;	
	private String cameraPrefsKey;

	public AppMobiCamera(AppMobiActivity activity, AppMobiWebView webview) { 
		super(activity, webview);
		
		cameraPrefsKey = String.format("%s.%s", webview.config.appName, staticCameraPrefsKey);
	}
	
	String getPictureListJS() {
    	StringBuffer js = new StringBuffer("AppMobi.picturelist=[");
    	File dir = new File(pictureDir());
		String[] pics = dir.list();
		
		if(pics!=null) {
			for(String pic:pics) {
				js.append(String.format("'%1$s', ", pic));
			}
		}
		
		js.append("];");
		if (debug)
			System.out.println("initpicturelist: " + js.toString());
		
		return js.toString();
    }

	private String pictureDir() {
		return activity.appDir().toString() + "/_pictures";
	}
	
	private String getNextPictureFile(boolean isPNG) {
		String dir = pictureDir();
		File dirFile = new File(dir);
		if (!dirFile.exists())
			dirFile.mkdirs();
		String filePath = null, baseName = null;
		File outFile;
		int i = 0;
		do {			
			baseName = String.format("picture_%1$03d.%2$s", i++, isPNG?"png":"jpg");
			filePath = String.format("%1$s/%2$s", dir, baseName);
			outFile = new File(filePath);
		} while (outFile.exists());
		return baseName;
	}
	
	public void takePicture(final int quality, final String saveToLibYN, final String picType) {
		//saveToLibYN is not used because the camera activity always saves to gallery

		if (busy) {
			cameraBusy();
			return;
		}
		this.busy = true;

		File outputFile = new File(Environment.getExternalStorageDirectory(), "test."+picType);
		
		//put required info in shared prefs
		SharedPreferences.Editor prefsEditor = activity.getSharedPreferences(cameraPrefsKey, 0).edit();
		prefsEditor.putString(cameraPrefsFileName, outputFile.getAbsolutePath());
		prefsEditor.putBoolean(cameraPrefsIsPNG, "png".equalsIgnoreCase(picType));
		prefsEditor.putInt(cameraPrefsQuality, quality);
		prefsEditor.commit();
		
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(outputFile));
        activity.startActivityForResult(intent, AppMobiActivity.PICTURE_RESULT);
		activity.setLaunchedChildActivity(true);
	}
	
	void cameraActivityResult(int resultCode, Intent intent) {
		if(resultCode == Activity.RESULT_OK) {
			//get info from shared prefs
			SharedPreferences prefs = activity.getSharedPreferences(cameraPrefsKey, 0);
			String outputFile = prefs.getString(cameraPrefsFileName, "");
			boolean isPNG = prefs.getBoolean(cameraPrefsIsPNG, false);
			int quality = prefs.getInt(cameraPrefsQuality, 100);
            savePicture(outputFile, quality, isPNG);
		} else {
			pictureCancelled();
		}
	}    

	private void savePicture(final String outputFile, final int quality, final boolean isPNG) {
		//busy could be false if activity had been stopped
		busy = true;
		
		new Thread() {
			public void run() {
				try{
					
					String dir = pictureDir();
					File dirFile = new File(dir);
					if (!dirFile.exists())
						dirFile.mkdirs();				
					String baseName = getNextPictureFile(isPNG);
					String filePath = String.format("%1$s/%2$s", dir, baseName);
					
					try {
						if (debug) System.out.println("AppMobiCamera.takePicture: file = " + filePath);
	
						OutputStream out = new BufferedOutputStream(
													new FileOutputStream(filePath), 0x8000);
						
						Bitmap bitMap = BitmapFactory.decodeFile(outputFile);
						if (bitMap==null || !bitMap.compress((isPNG?Bitmap.CompressFormat.PNG:Bitmap.CompressFormat.JPEG), quality, out)) {
							throw new IOException("Error converting to PNG");
						}
						bitMap.recycle();
						out.close();
						
						// Add name to JavaScript list, then fire the event
						injectJS(String.format("javascript:AppMobi.picturelist.push('%1$s');var ev = document.createEvent('Events');" +
								"ev.initEvent('appMobi.camera.picture.add',true,true);ev.success=true;" +
								"ev.filename='%1$s';document.dispatchEvent(ev);", baseName));
					} catch (IOException e) {
						if (debug) System.out.println("AppMobiCamera.takePicture err: " + e.getMessage());
						postAddError(baseName);
					}
					finally {
						busy=false;
					}
				} catch(Exception e) {
					//sometimes a NPE occurs after resuming, catch it here but do nothing
					if (Debug.isDebuggerConnected()) Log.e("[appMobi]", "handled camera resume NPE:\n" + e.getMessage(), e);
				}
			}
		}.start();
	}
	
	private void postAddError(String fileName) {
		injectJS(String.format("javascript:var ev = document.createEvent('Events');" +
				"ev.initEvent('appMobi.camera.picture.add',true,true);ev.success=false;" +
				"ev.filename='%1$s';document.dispatchEvent(ev);", fileName));
	}

	private void cameraBusy() {
		injectJS("javascript:var e = document.createEvent('Events');" +
				 "e.initEvent('appMobi.camera.picture.busy',true,true);" +
				 "e.success=false;e.message='busy';document.dispatchEvent(e);");
	}
	private void pictureCancelled() {		
		busy = false;
		injectJS("javascript:var ev = document.createEvent('Events');" +
				 "ev.initEvent('appMobi.camera.picture.cancel',true,true);" +
				 "ev.success=false;document.dispatchEvent(ev);");
	}
	public void importPicture() {
		pickImage();
	}
	/*
	 * Show the photo gallery so the user can select a picture.
	 */
	private void pickImage() {
		if (busy) {
			cameraBusy();
			return;
		}
		busy = true;
		activity.runOnUiThread(new Runnable() {
			public void run() {		
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				activity.setLaunchedChildActivity(true);
				activity.startActivityForResult(Intent.createChooser(intent,
                	"Select Picture"), AppMobiActivity.SELECT_PICTURE);
			}
		});
    }
	/*
	 * 	Called when user has picked an image from the photo gallery.
	 */
	public void imageSelected(Intent intent) {
		Uri uri = intent.getData();
        //OI FILE Manager
        String fileManagerPath = uri.getPath();
        //MEDIA GALLERY
        String selectedImagePath;
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = activity.managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            selectedImagePath = cursor.getString(column_index);
        } else 
        	selectedImagePath = null;
        String filePath = selectedImagePath != null ? selectedImagePath : fileManagerPath;
        System.out.println("AppMobiCamera.imageSelected: " + filePath);
        if (filePath != null) {
//        	try {
       			File f = new File(filePath);
//        		int length = (int)f.length();
//        		byte buf[] = new byte[length];
//        		FileInputStream in = new FileInputStream(filePath);
//        		in.read(buf);
        		savePicture(f.getAbsolutePath(), 100, false);
//        	} catch (IOException e) {
//        		postAddError(filePath);
//        	}
        }
        busy = false;
    }
	public void importCancelled() {
		pictureCancelled();
	}
	private final String baseFromUrl(String url) {
		int ind = url.lastIndexOf('/');
		return (ind < 0 || ind == url.length() - 1) ? url : url.substring(ind + 1);
	}
	private final String fileFromUrl(String url) {
		return pictureDir() + "/" + baseFromUrl(url);
	}
	public void deletePicture(String url) {
		String filePath = fileFromUrl(url);
		String baseName = baseFromUrl(url);
		if (debug) System.out.println("AppMobiCamera.deletePicture: " + url);
		File f = new File(filePath);
		boolean removed = f.delete();
		if (removed) {	/* Update the dictionary. */
			injectJS(String.format("javascript:%1$svar ev = document.createEvent('Events');" +
						"ev.initEvent('appMobi.camera.picture.remove',true,true);ev.success=true;" +
						"ev.filename='%2$s';document.dispatchEvent(ev);", getPictureListJS(), baseName));
		} else {
			injectJS(String.format("javascript:var ev = document.createEvent('Events');" +
			"ev.initEvent('appMobi.camera.picture.remove',true,true);ev.success=false; +" +
			"ev.filename='%1$s';document.dispatchEvent(ev);", baseName));
		}
	}
	public void clearPictures() {
		// Remove all the files.
		String dirName = pictureDir();
		File dir = new File(dirName);
		String[] children = dir.list();
		int cnt = children == null ? 0 : children.length;
        for (int i = 0; i < cnt; i++) {
            new File(dir, children[i]).delete();
        }
        injectJS("javascript:AppMobi.picturelist = [];var ev = document.createEvent('Events');" +
        		"ev.initEvent('appMobi.camera.picture.clear',true,true);" +
        		"ev.success=true;document.dispatchEvent(ev);");
	}    
}
