package com.appMobi.appMobiLib;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;

import android.content.SharedPreferences;
import com.appMobi.appMobiLib.util.Debug;
import android.util.Log;

import com.appMobi.appMobiLib.AppMobiCacheHandler.DownloadProgressEmitter;

public class AppMobiCache extends AppMobiCommand {
	private final String appMobiCookies;
	private final String appMobiCookiesExpires;
	private static final String DONOTEXPIRE = "never";
	private final String cachedMediaMap;
	private static final SimpleDateFormat sdf;
	private final File appMobiCache, cachedMediaDirectory; 
	private SharedPreferences mediaCache;
	
	static {
		sdf = new java.text.SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
	}
	
	public AppMobiCache(AppMobiActivity activity, AppMobiWebView webview){
		super(activity, webview);

		//make shared prefs keys app specific
		appMobiCookies = String.format("%s.cookies", webview.config.appName); 
		appMobiCookiesExpires = String.format("%s.cookies-expires", webview.config.appName); 
		cachedMediaMap =  String.format("%s.cached-media-map", webview.config.appName);
		
		appMobiCache =  activity.appDir();
		cachedMediaDirectory = new File(appMobiCache, AppMobiActivity.mediaCache);
		if(!cachedMediaDirectory.exists()) {
			cachedMediaDirectory.mkdir();
		}
		//testCookies();
		
		if(activity.configData!=null && !activity.configData.hasCaching) {
			//flush cache if not authorized
			resetPhysicalMediaCache();
		}
		
	}
	
	public String getCookies() {
		Map<String, ?> valueMap = activity.getSharedPreferences(appMobiCookies, 0).getAll();
		SharedPreferences.Editor valueEditor = activity.getSharedPreferences(appMobiCookies, 0).edit();
		Map<String, ?> expiresMap = activity.getSharedPreferences(appMobiCookiesExpires, 0).getAll();
		SharedPreferences.Editor expiresEditor = activity.getSharedPreferences(appMobiCookiesExpires, 0).edit();
		
		StringBuffer cookies = new StringBuffer("{");
		Iterator<String> keys = valueMap.keySet().iterator();
		while(keys.hasNext()){
			String key = keys.next();
			String value = (String) valueMap.get(key);
			String expires = (String) expiresMap.get(key);
			Date expiresDate = null, now = new Date();
			if(!expires.equals(AppMobiCache.DONOTEXPIRE)) {
				try {
					expiresDate = sdf.parse(expires);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			if(expiresDate==null || expiresDate.after(now)){
				value.replaceAll("'", "\\\\'");
				cookies.append(key+":{value:'"+value+"'}, ");
			} else {
				//coookie is expired - remove it from prefs
				valueEditor.remove(key);
				expiresEditor.remove(key);
			}
		}
		valueEditor.commit();
		expiresEditor.commit();
		cookies.append("}");
		if(Debug.isDebuggerConnected()) Log.i("[appMobi]", "AppMobi.cookies: " + cookies.toString());
		return cookies.toString();
	}
	
	public void setCookie(String cookieName, String cookieValue, int expires) {
		setCookie(cookieName, cookieValue, expires, false);
	}
	
	private void setCookie(String cookieName, String cookieValue, int expires, boolean setExpired) {
		//set value
		SharedPreferences settings = activity.getSharedPreferences(appMobiCookies, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(cookieName, cookieValue);
		editor.commit();
		//set expires
		settings = activity.getSharedPreferences(appMobiCookiesExpires, 0);
		editor = settings.edit();
		if(setExpired || expires>=0) {
			GregorianCalendar now = new GregorianCalendar();
			now.add(Calendar.DATE, expires);
			editor.putString(cookieName, sdf.format(now.getTime()));
		} else {
			editor.putString(cookieName, AppMobiCache.DONOTEXPIRE);
		}
		editor.commit();
	}
	
	public void removeCookie(String cookieName){
		SharedPreferences.Editor valueEditor = activity.getSharedPreferences(appMobiCookies, 0).edit();
		SharedPreferences.Editor expiresEditor = activity.getSharedPreferences(appMobiCookiesExpires, 0).edit();
		valueEditor.remove(cookieName);
		expiresEditor.remove(cookieName);
		valueEditor.commit();
		expiresEditor.commit();
	}
	
	public void clearAllCookies() {
		SharedPreferences.Editor valueEditor = activity.getSharedPreferences(appMobiCookies, 0).edit();
		SharedPreferences.Editor expiresEditor = activity.getSharedPreferences(appMobiCookiesExpires, 0).edit();
		valueEditor.clear();
		expiresEditor.clear();		
		valueEditor.commit();
		expiresEditor.commit();		
	}

	/*
	private void testCookies() {
		clearAllCookies();
		getCookies(false);
		setCookie("expired1", "expired1", 0, true);
		setCookie("expired2", "expired2", -1, true);
		setCookie("new1", "new1", 1, true);
		setCookie("new2", "new2", 1, true);
		setCookie("never1", "never", 0, false);
		getCookies(false);
		clearCookie("new2");
		getCookies(false);
		clearAllCookies();
		getCookies(false);
	}
	//*/
	
	private String getFilenameWithURL(String url) {
		String filename = url.substring(url.lastIndexOf('/')+1);
		if(Debug.isDebuggerConnected()) Log.i("[appMobi]", filename);
		return filename;
	}	

	//this gets called at startup time, so init stuff happens in here
	public String getMediaCache() {
		File[] physicalMediaCache = cachedMediaDirectory.listFiles();
		if(physicalMediaCache!=null) if(Debug.isDebuggerConnected()) Log.i("[appMobi]", "****physical media cache****: "+ Arrays.asList(physicalMediaCache));
		
		if(mediaCache==null) {
			mediaCache = activity.getSharedPreferences(cachedMediaMap, 0);
		}
		Map<String, ?> valueMap = mediaCache.getAll();
		
		//inject js
		StringBuffer mediaJS = new StringBuffer("[");
		Iterator<String> keys = valueMap.keySet().iterator();
		while(keys.hasNext()){
			if(mediaJS.length()!=1) mediaJS.append(", ");
			String key = keys.next();
			// String value = (String) valueMap.get(key);
			mediaJS.append("\""+key+"\"");
		}
		mediaJS.append("]");
		//webview.loadUrl("javascript:AppMobi.mediacache = " + mediaJS.toString());
		if(Debug.isDebuggerConnected()) Log.i("[appMobi]", "AppMobi.mediacache: " + mediaJS.toString());
		return mediaJS.toString();
	}

	private void resetPhysicalMediaCache(){
		//delete the media cache directory
		if(cachedMediaDirectory.exists()) FileUtils.deleteDirectory(cachedMediaDirectory);//what if it cant be deleted?
		//create an empty directory
		cachedMediaDirectory.mkdir();
		
		//empty the dictionary
		if(mediaCache==null) {
			mediaCache = activity.getSharedPreferences(cachedMediaMap, 0);
		}
		SharedPreferences.Editor editor = mediaCache.edit();
		editor.clear();
		editor.commit();
	}
	
	public void clearMediaCache() {
		if(webview.config!=null && !webview.config.hasCaching) return;
		resetPhysicalMediaCache();
		//update js object and fire an event
		String js = "javascript:AppMobi.mediacache = new Array();var e = document.createEvent('Events');e.initEvent('appMobi.cache.media.clear',true,true);document.dispatchEvent(e);";
		if(Debug.isDebuggerConnected()) Log.i("[appMobi]", js);
		injectJS(js);
	}

	public void removeFromMediaCache(String url) {
		if(webview.config!=null && !webview.config.hasCaching) return;
		String path = mediaCache.getString(url, "");
		String js;
		boolean success = false;
		
		//try to delete the file
		if(!"".equals(path)) {
			boolean removed = new File(path).delete();
			if(removed) {
				//update the prefs
				SharedPreferences.Editor editor = mediaCache.edit();
				editor.remove(url);
				editor.commit();

				success = true;
			} 
		}

		if(success) {
			js = "javascript:var i = 0; while (i < AppMobi.mediacache.length) { if (AppMobi.mediacache[i] == '"+url+"') { AppMobi.mediacache.splice(i, 1); } else { i++; }};var e = document.createEvent('Events');e.initEvent('appMobi.cache.media.remove',true,true);e.success=true;e.url='"+url+"';document.dispatchEvent(e);";
		} else {
			js = "javascript:var e = document.createEvent('Events');e.initEvent('appMobi.cache.media.remove',true,true);e.success=false;e.url='"+url+"';document.dispatchEvent(e);";
		}
		
		//update js object and fire an event
		if(Debug.isDebuggerConnected()) Log.i("[appMobi]", js);
		injectJS(js);
	}	

	public void addToMediaCache(final String url, final String id){
		if(webview.config!=null && !webview.config.hasCaching) return;
    	new Thread("AppMobiCache:addToMediaCache") {
			@Override
			public void run() {
				downloadToMediaCache(url, id);
			}
    	}.start();
		
	}

	//called by addToMediaCache to run in worker thread
	private void downloadToMediaCache(String url, final String id) {
		
		//create the file to write data into
		File mediaPath = new File(cachedMediaDirectory, getFilenameWithURL(url));
		
		//*
		//download the file: allow for up to 3 retries
		
		int retries = 3;
		boolean success = false;
		while(!success && retries>0) {

			//check if the request succeeded, if so, write out data and increment offset
			success = (id!=null && id.length()>0) ?
				AppMobiCacheHandler.get(url, activity.getApplicationContext(), getFilenameWithURL(url), cachedMediaDirectory, new DownloadProgressEmitter() {

					public void emit(long current, long length) {
						String js = "javascript: var e = document.createEvent('Events');e.initEvent('appMobi.cache.media.update',true,true);e.success=true;e.id='" + id + "';e.current=" + current + ";e.total=" + length + ";document.dispatchEvent(e);";
						if(Debug.isDebuggerConnected()) Log.i("[appMobi]", js);
						injectJS(js);
						
					}
					
				}):
				AppMobiCacheHandler.get(url, activity.getApplicationContext(), getFilenameWithURL(url), cachedMediaDirectory);
			if(success) {
			} else {
				//handle error
				//NSLog(@"error -- code: %d, localizedDescription: %@", [error code], [error localizedDescription]);
				//[self finishedDownloadToMediaCache:url toPath:nil withFlag:NO];
			}
			retries--;
		}
		finishedDownloadToMediaCache(url, mediaPath.getAbsolutePath(), success, id);
	}
	
	//called by downloadToMediaCache after completion
	//update mediaCache, AppMobi.mediacache js object and fire an event
	private void finishedDownloadToMediaCache(String url, String path, boolean didSucceed, String id) {
		String js;
		if(didSucceed) {
			//update the prefs
			SharedPreferences.Editor editor = mediaCache.edit();
			editor.putString(url, path);
			editor.commit();
			
			//update js object and fire an event
			if(id!=null && id.length()>0) {
				js = "javascript:AppMobi.mediacache.push('" + url + "');var e = document.createEvent('Events');e.initEvent('appMobi.cache.media.add',true,true);e.success=true;e.url='" + url + "';e.id='" + id + "';document.dispatchEvent(e);";
			} else {
				js = "javascript:AppMobi.mediacache.push('" + url + "');var e = document.createEvent('Events');e.initEvent('appMobi.cache.media.add',true,true);e.success=true;e.url='" + url + "';document.dispatchEvent(e);";
			}
		} else {
			//fire event
			if(id!=null && id.length()>0) {
				js = "javascript:var e = document.createEvent('Events');e.initEvent('appMobi.cache.media.add',true,true);e.success=false;e.url='" + url + "';e.id='" + id + "';document.dispatchEvent(e);";
			} else {
				js = "javascript:var e = document.createEvent('Events');e.initEvent('appMobi.cache.media.add',true,true);e.success=false;e.url='" + url + "';document.dispatchEvent(e);";
			}
		}
		if(Debug.isDebuggerConnected()) Log.i("[appMobi]", js);
		injectJS(js);
	}
}