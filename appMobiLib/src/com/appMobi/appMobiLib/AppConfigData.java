package com.appMobi.appMobiLib;

import java.io.File;

public class AppConfigData {
	public String appName, packageName, releaseName, baseURL, bundleName, jsURL, jsVersion, analyticsID, pushServer, analyticsUrl, installMessage = "";
	public String paymentServer, paymentCallback, paymentMerchant, paymentIcon;
	public String appDirectory, baseDirectory;
	public boolean hasStreaming, hasAnalytics, hasCaching, hasAdvertising, hasPayments, hasPushNotify, hasOAuth, hasLiveUpdate, hasSpeech;
	public int configRevision, appVersion, oauthSequence = 0;
	public File file;
	public int installOption = 1;
	
    public static final class InstallOption {
    	static String[] installOptionDescriptions = {
    			"auto install as soon as available",
    			"auto install on restart/resume",
    			"prompt user",
    			"notify developer"
    		};
    		
    	static final int AUTO_INSTALL_ASAP = 1;
    	static final int AUTO_INSTALL_ON_RESTART = 2;
    	static final int PROMPT_USER = 3;
    	static final int NOTIFY_DEVELOPER = 4;
    }
	
	public AppConfigData(File file) {
		this.file = file;
	}

     @Override
	public String toString(){
         return 
         "app Name: " + appName + "\n" +
         "app Version: " + appVersion  + "\n" +
         "release Name: " + releaseName + "\n" +
         "package Name: " + packageName + "\n" +
         "base URL: " + baseURL + "\n" +
         "bundle Name: " + bundleName + "\n" +
         "javascript url: " + jsURL + "\n" +
         "javascript version: " + jsVersion + "\n" +
         "hasStreaming: " + hasStreaming + "\n" +
         "hasAnalytics: " + hasAnalytics + "\n" +
         "hasCaching: " + hasCaching + "\n" +
         "hasAdvertising: " + hasAdvertising + "\n" +
         "hasPayments: " + hasPayments + "\n" +
         "hasPushNotify: " + hasPushNotify + "\n" +
         "hasOAuth: " + hasOAuth + "\n" +
         "hasSpeech:" + hasSpeech + "\n" +
         "analystics id: " + analyticsID + "\n" +
         "push server: " + pushServer + "\n" +
         "payment server: " + paymentServer + "\n" +
         "oauthSequence: " + oauthSequence + "\n" +
         "installOption: (" + installOption + ") " + InstallOption.installOptionDescriptions[installOption-1] + "\n" +
         "analyticsURl: " + analyticsUrl + "\n" +
         "installMessage: " + installMessage + "\n" +
         "appMobi config revision: " + configRevision;
     }
}

