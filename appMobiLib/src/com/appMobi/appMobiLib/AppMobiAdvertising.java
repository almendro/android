package com.appMobi.appMobiLib;

import java.io.File;
import java.net.MalformedURLException;

import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;

public class AppMobiAdvertising extends AppMobiCommand {
	
	final File baseAdDir;
	
	AppMobiAdvertising(AppMobiActivity activity,AppMobiWebView webview){
		super(activity, webview);
		
		baseAdDir = new File(activity.appDir(), AppMobiActivity.adCache);
	}
	
	private boolean getAndExtractAdBundle(AppConfigData config, File adExtractDir, File oldConfigFile, File newConfigFile) {
		boolean success = false;
		
		try {
			//get bundle url
			String bundleURL = config.baseURL + "/" + config.bundleName;
			//append deviceid & platform
			bundleURL = activity.appendDeviceIdAndPlatform(bundleURL);
			//download bundle
			AppMobiCacheHandler.get(bundleURL, activity.getApplicationContext(), AppMobiActivity.bundle, adExtractDir.getParentFile());
			//delete current ad-specific cache
			FileUtils.deleteDirectory(adExtractDir);
			//extract bundle into ad-specific cache
			File adBundle = new File(adExtractDir.getParentFile(), AppMobiActivity.bundle);
			FileUtils.unpackArchive(adBundle, adExtractDir);
			//check if there was a nested subdirectory in the bundle
			FileUtils.checkDirectory(adExtractDir);
			//update config file
			if(oldConfigFile.exists()) oldConfigFile.delete();
			newConfigFile.renameTo(oldConfigFile);
			//cleanup the ad archive
			adBundle.delete();
			//if everything worked, set success to true
			success = true;
		} catch(Exception e) {
			if(Debug.isDebuggerConnected()) Log.d("[appMobi]", e.getMessage(), e);
		}
		
		return success;
	}
	
	//TODO: fix success tracking
	public void getAd(final String adName, final String configUrl, final String callbackId) {
		new Thread("AppMobiAdvertising:getAd") {
			public void run() {
				//keep track of whether process succeeded so we can pass along in js event
				boolean success = false;
				
				File
					currentAdBaseDir = new File(baseAdDir, adName),//the top-level directory for this ad
					currentAdExtractDir = new File(currentAdBaseDir, AppMobiActivity.appMobiCache),//the directory ad content gets extracted into
					oldConfigFile = new File(currentAdBaseDir, AppMobiActivity.appConfig),//the last ad config that was retrieved
					newConfigFile = new File(currentAdBaseDir, AppMobiActivity.newConfig);//the ad config we are about to retrieve

				try {
					
					//check if the ad already exists locally
					boolean hasLocalCopy = oldConfigFile.exists() && new File(currentAdExtractDir, "index.html").exists();

					//retrieve and parse the latest config
					String configURL = activity.appendDeviceIdAndPlatform(configUrl);
					boolean retrievedNewConfig = AppMobiCacheHandler.get(configURL, activity.getApplicationContext(), AppMobiActivity.newConfig, currentAdBaseDir);
					
					if(!retrievedNewConfig) {
						//if unable to retrieve updated config, set success based on whether we have a previous version of the ad
						success = hasLocalCopy;
					} else {
						//if retrieved updated config, parse it and continue
						AppConfigData newConfig = activity.parseConfigWithoutCaching(newConfigFile);
			
						//has a version of this ad been previously retrieved?
						if(hasLocalCopy) {
							AppConfigData oldConfig = activity.parseConfigWithoutCaching(oldConfigFile);
							//if so, check if there is an update for the ad
							if(newConfig.appVersion>oldConfig.appVersion) {
								success = getAndExtractAdBundle(newConfig, currentAdExtractDir, oldConfigFile, newConfigFile);
							} else {
								success = hasLocalCopy;
							}
						} else {
							//otherwise, just get it
							success = getAndExtractAdBundle(newConfig, currentAdExtractDir, oldConfigFile, newConfigFile);
						}
					}
				} catch(Exception e) {
					//handle exception
					if(Debug.isDebuggerConnected()) Log.d("[appMobi]", e.getMessage(), e);
				}
				
				//after retrieving ad as required, inject an event to let js ad framework know the ad is available
				//String adPath = activity.getString(R.string.DeviceProxyBaseUrl) + activity.getString(R.string.DeviceProxyMessagingPort) + "/" + AppMobiActivity.adCache + "/" + adName + "/" + AppMobiActivity.appMobiCache + "/index.html";
				String adPath = null;
				try {
					adPath = new File(currentAdExtractDir, "index.html").toURL().toString();
					String js = "javascript:var e = document.createEvent('Events');e.initEvent('appMobi.advertising.ad.load',true,true);e.identifier='"+callbackId+
					"';e.path='"+adPath+"';e.success="+success+";document.dispatchEvent(e);";
				if(Debug.isDebuggerConnected()) Log.i("[appMobi]", js);
				injectJS(js);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}.start();
	}
	
}
