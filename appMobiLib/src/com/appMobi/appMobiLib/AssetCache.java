package com.appMobi.appMobiLib;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;

public class AssetCache {
	Activity activity;
	int checkpoint;
	HashMap<Integer, ArrayList<AssetCacheFile>> checkpoint2files;
	public File assetCacheDirectory;
	
	public AssetCache(Activity activity) {
		this.activity = activity;
		checkpoint2files = new HashMap<Integer, ArrayList<AssetCacheFile>>();
		File appMobiCache = activity.getApplicationContext().getFileStreamPath("appmobicache");
		assetCacheDirectory = new File(appMobiCache, "_cache");
		if(!assetCacheDirectory.exists()) {
			assetCacheDirectory.mkdir();
		}
	}
	
	void addFile(AssetCacheFile file) {
		ArrayList<AssetCacheFile> checkpointFiles = checkpoint2files.get(file.checkpoint);
		if(checkpointFiles == null) {
			checkpointFiles = new ArrayList<AssetCacheFile>();
			checkpoint2files.put(file.checkpoint, checkpointFiles);		
		}
		checkpointFiles.add(file);
	}
	
	boolean downloadToAssetCache(AssetCacheFile file) {
		return AppMobiCacheHandler.get(file.url, activity.getApplicationContext(), file.getFilename(), assetCacheDirectory);
	}
	
	void updatePhysicalCache(ArrayList<ArrayList<AssetCacheFile>> filesToDownloadAndDelete) {
		//download all the files in the list to the physical cache
		ArrayList<AssetCacheFile> filesToDownload = filesToDownloadAndDelete.get(0);
		for(AssetCacheFile fileToDownload : filesToDownload) {
			downloadToAssetCache(fileToDownload);
		}

		//delete files in list from physical cache
		ArrayList<AssetCacheFile> filesToDelete = filesToDownloadAndDelete.get(1);
		for(AssetCacheFile fileToDelete : filesToDelete) {
			File file = new File(assetCacheDirectory, fileToDelete.getFilename());
			file.delete();
		}
		
	}
	
	void validateCache() {
		//add all files from all checkpoints
		ArrayList<AssetCacheFile> allFiles = new ArrayList<AssetCacheFile>();
		for(ArrayList<AssetCacheFile> checkpointFiles: checkpoint2files.values()) {
			allFiles.addAll(checkpointFiles);
		}
		
		//compare virtual cache to physical cache
		for(AssetCacheFile file : allFiles) {
			File assetPath = new File(assetCacheDirectory, file.getFilename());
			if (!assetPath.exists() || (assetPath.length()!=file.length)) {
				//isValid = NO;
				//break;
				downloadToAssetCache(file);
			}
		}
	}
	
	public void updateCache(AssetCache newCache) {
		//identify new files to be downloaded, old files to be removed, then update the physical cache by downloading/removing
		if(newCache.checkpoint > checkpoint) {
			ArrayList<AssetCacheFile> filesToDownload = new ArrayList<AssetCacheFile>();
			ArrayList<AssetCacheFile> filesToDelete = new ArrayList<AssetCacheFile>();
			for(ArrayList<AssetCacheFile> checkpointFiles: checkpoint2files.values()) {
				filesToDelete.addAll(checkpointFiles);
			}
			int checkpointIndex = newCache.checkpoint;
			while (checkpointIndex>0) {
				ArrayList<AssetCacheFile> checkpointFiles = newCache.checkpoint2files.get(checkpointIndex);
				if(checkpointFiles!=null) {
					if(checkpointIndex > checkpoint) filesToDownload.addAll(checkpointFiles);
					filesToDelete.removeAll(checkpointFiles);
				}
				checkpointIndex--;
			}
			final ArrayList<ArrayList<AssetCacheFile>> filesToDownloadAndDelete = new ArrayList<ArrayList<AssetCacheFile>>();
			filesToDownloadAndDelete.add(filesToDownload);
			filesToDownloadAndDelete.add(filesToDelete);
	    	new Thread("AssetCache:updateCache") {
				@Override
				public void run() {
					updatePhysicalCache(filesToDownloadAndDelete);
				}
	    	}.start();
		}
	}
}