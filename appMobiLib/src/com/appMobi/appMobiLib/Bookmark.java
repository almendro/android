package com.appMobi.appMobiLib;


import java.io.File;


/*
<bookmark 
	bookmarkid="FoxNews2GO" 
	appname="FoxNews2GO" 
	relname="3.2.5" 
	name="FoxNews2GO" 
	image="http://am-xdk.s3.amazonaws.com/app.6b1cdbbb-d861-4d34-a41e-eb21a5050e97/d6fadaa3-78d2-4b4b-89f3-c5beb91bcda3/foxnews.png" 
	url="http://localhost:58888/FoxNews2GO/3.2.5/index.html" 
	webroot="" 
	buypage="http://services.appmobi.com/mdemo/amappbutton.aspx?CMD=BUYAPP&amp;APP=FoxNews2GO&amp;REL=3.2.5" 
	paypage="http://services.appmobi.com/mdemo/amappbutton.aspx?CMD=PROCESSPMT" 
	bookmarkpage="http://am-xdk.s3.amazonaws.com/app.6b1cdbbb-d861-4d34-a41e-eb21a5050e97/d6fadaa3-78d2-4b4b-89f3-c5beb91bcda3/addbk.htm" 
	appconfig="https://am-xdk.s3.amazonaws.com/app.6b1cdbbb-d861-4d34-a41e-eb21a5050e97/d6fadaa3-78d2-4b4b-89f3-c5beb91bcda3/QA/appconfig.xml" 
/> */

public class Bookmark {
	public String name, url, image, appName, relName, appConfigUrl, webRoot, buyPage, payPage, bookPage;
	public boolean isPending = false, isAnApp = false, isInstalling = false, isDownloading = false, isInstalled = false, isHidden = false;
	public File imageFile = null;
	AppConfigData config = null;
	int messages;
	
	//placeholder
	public boolean isDeleted = false, isAuthorized = false;

	public Bookmark() {
	}

     @Override
	public String toString(){
         return 
         "name: " + name + "\n" +
         "url: " + url  + "\n" +
         "image: " + image + "\n" +
         "appName: " + appName + "\n" +
         "relName: " + relName + "\n" +
         "appConfigUrl: " + appConfigUrl + "\n" +
         "webRoot: " + webRoot + "\n" +
         "buyPage: " + buyPage + "\n" +
         "payPage: " + payPage + "\n" +
         "bookPage: " + bookPage + "\n" +
         "isPending:" + isPending + "\n" +
         "isAnApp:" + isAnApp + "\n" +
         "isInstalling:" + isInstalling + "\n" +
         "isDownloading:" + isDownloading + "\n" +
         "isInstalled:" + isInstalled
         ;
     }
}

