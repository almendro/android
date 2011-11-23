package com.appMobi.appMobiLib;

import java.io.File;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.appMobi.appMobiLib.AppMobiActivity.AppInfo;

public class AppConfigHandler extends DefaultHandler {

	/* 
	<XML>
	<CONFIG appname="flyrs.local" type="APP" pkgname="QA" release="3.2.5" revision="4">
	<BUNDLE base="https://am-xdk.s3.amazonaws.com/app.c7025190-7b76-4faa-ab5e-93a3d4df6b89/c010f456-1681-4ed9-8812-81a239aad6d7/QA" file="bundle.zip" version="12" />
	<JAVASCRIPT platform="iphone" version="3.2.5" url="http://assets.edge.flycast.fm/appmobi/javascript/iPhone/3.2.5/appmobi_iphone.js" />
	<JAVASCRIPT platform="android" version="3.2.5" url="http://assets.edge.flycast.fm/appmobi/javascript/Android/3.2.5/appmobi_android.js" />
	<SECURITY hasCaching="1" hasStreaming="1" hasAnalytics="1" hasPushNotify="1" hasInAppPay="1" hasAdvertising="1" hasOAuth="1" />
	<ANALYTICS id="" server="https://queue.amazonaws.com/668107645782/appmobitest" />
	<NOTIFICATIONS server="amsdev.broadp3.com" />
	<PAYMENTS server="" callback="" merchant="" icon="" />
	<ADVERTISEMENTS server="" />
	<OAUTH sequence="0" />
	</CONFIG>
	</XML>
	 */
	
	public AppConfigData config;
	private AppMobiActivity activity;
	private File file;
	
	public AppConfigHandler(File file, AppMobiActivity activity) {
		this.file = file;
		this.activity = activity;
	}

	public AppConfigData getParsedData() {
		//hack to fix bad config case
		if(config==null && file.exists()) {
			file.delete();
		}
		return config;
	}

	@Override
	public void startDocument() throws SAXException {
	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		if (localName.equals("CONFIG")) {
			config = new AppConfigData(file);
			if(atts.getValue("appname")!=null) config.appName = atts.getValue("appname");
			if(atts.getValue("pkgname")!=null) config.packageName = atts.getValue("pkgname");
			if(atts.getValue("release")!=null) config.releaseName = atts.getValue("release");
			try {
				if(atts.getValue("revision")!=null) config.configRevision = Integer.parseInt(atts.getValue("revision"));
			} catch(NumberFormatException e) {
				System.out.println("CONFIG/revision must be an integer");
			}
		} else if (localName.equals("BUNDLE")) {
			if(atts.getValue("base")!=null) {
				config.baseURL = atts.getValue("base");
				config.baseDirectory = activity.getApplicationContext().getFilesDir() + "/" + config.appName + "/" + config.releaseName;
				config.appDirectory = activity.getApplicationContext().getFilesDir() + "/" + AppMobiActivity.appMobiCache + "/" + config.appName + "/" + config.releaseName;
			}
			if(atts.getValue("file")!=null) config.bundleName = atts.getValue("file");
			try {
				if(atts.getValue("version")!=null) config.appVersion = Integer.parseInt(atts.getValue("version"));
			} catch(NumberFormatException e) {
				System.out.println("BUNDLE/version must be an integer");
			}
		} else if (localName.equals("ANALYTICS")) {
			if(atts.getValue("id")!=null) config.analyticsID = atts.getValue("id");
			if(atts.getValue("server")!=null) config.analyticsUrl = atts.getValue("server");
		} else if (localName.equals("JAVASCRIPT")) {
			//check platform
			if(atts.getValue("platform")!=null && atts.getValue("platform").equals("android")) {
				if(atts.getValue("version")!=null) config.jsVersion = atts.getValue("version");
				if(atts.getValue("url")!=null) config.jsURL = atts.getValue("url");
			}
		} else if (localName.equals("SECURITY")) {
				if(atts.getValue("hasStreaming")!=null) config.hasStreaming = atts.getValue("hasStreaming").equals("1");
				if(atts.getValue("hasAnalytics")!=null) config.hasAnalytics = atts.getValue("hasAnalytics").equals("1");
				if(atts.getValue("hasCaching")!=null) config.hasCaching = atts.getValue("hasCaching").equals("1");
				if(atts.getValue("hasAdvertising")!=null) config.hasAdvertising = atts.getValue("hasAdvertising").equals("1");
				if(atts.getValue("hasPayments")!=null) config.hasPayments = atts.getValue("hasPayments").equals("1");
				if(atts.getValue("hasPushNotify")!=null) config.hasPushNotify = atts.getValue("hasPushNotify").equals("1");
				if(atts.getValue("hasOAuth")!=null) config.hasOAuth = atts.getValue("hasOAuth").equals("1");
				if(atts.getValue("hasLiveUpdate")!=null) config.hasLiveUpdate = atts.getValue("hasLiveUpdate").equals("1");
				if(atts.getValue("hasSpeech")!=null) config.hasSpeech = atts.getValue("hasSpeech").equals("1");
		} else if(localName.equals("NOTIFICATIONS")) {
			if(atts.getValue("server")!=null) config.pushServer = atts.getValue("server");
		} else if(localName.equals("UPDATE")) {
			if(atts.getValue("type")!=null) config.installOption = Integer.parseInt(atts.getValue("type"));
			if(atts.getValue("message")!=null) config.installMessage = atts.getValue("message");
		} else if(localName.equals("PAYMENTS")) {
			if(atts.getValue("server")!=null) config.paymentServer = atts.getValue("server");
			if(atts.getValue("callback")!=null) config.paymentCallback = atts.getValue("callback");
			if(atts.getValue("merchant")!=null) config.paymentMerchant = atts.getValue("merchant");
			if(atts.getValue("icon")!=null) config.paymentIcon = atts.getValue("icon");
		} else if(localName.equals("OAUTH")) {
			if(atts.getValue("sequence")!=null) config.oauthSequence = Integer.parseInt(atts.getValue("sequence"));
		} else if(localName.equals("OTAU")) {
			if(atts.getValue("installOption")!=null) config.installOption = Integer.parseInt(atts.getValue("installOption"));
		}

	}	
	
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		if (localName.equals("CONFIG")) {
		} else if (localName.equals("BUNDLE")) {
		}
	}

	@Override
	public void characters(char ch[], int start, int length) {
	}
}
