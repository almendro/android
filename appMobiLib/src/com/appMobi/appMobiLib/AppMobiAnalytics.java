package com.appMobi.appMobiLib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.SimpleTimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import com.appMobi.appMobiLib.util.Debug;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.appMobi.appMobiLib.AppMobiActivity.AppInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


public class AppMobiAnalytics extends AppMobiCommand {
	private static String deviceKey = null;
	private static final String OFFLINE = "OFFLINE";
	Collection<PageEvent> events;
	final Object lock;
	
	static {
		initStoredDeviceKey();
	}
	
	public AppMobiAnalytics(AppMobiActivity activity, AppMobiWebView webview){
		super(activity, webview);

		lock = this;
		
		//load events from disk
		loadEvents();

		new Thread("AppMobiAnalytics:constructor") {
			public void run() {
				//if there were any saved events, submit them now
				if( events != null && events.size() > 0 ) {
					if(lock==null) return;
					synchronized(lock) {
						submitEvents();
					}
				}
			}
		}.start();
		
	}
	
	private void saveEvents() {
		File eventsFile = new File(activity.baseDir(), "analytics.dat");
		String json = null;
		json = PageEvent.toJSON(events);
		try {
			FileWriter writer = new FileWriter(eventsFile);
			writer.write(json);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
		}
	}
	
	private void loadEvents() {
		File eventsFile = new File(activity.baseDir(), "analytics.dat");
		if(eventsFile.exists()) {
			try {
				FileInputStream fis = new FileInputStream(eventsFile);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				FileUtils.copyInputStream(fis, baos);
				events = PageEvent.fromJSON(baos.toString());
				if (events == null)
					events = new LinkedList<PageEvent>();
				//remove events that are older than 30 days
				Date now = new Date();
				ArrayList<PageEvent> toBeRemoved = new ArrayList<PageEvent>(0);
				for(PageEvent event:events) {
					long diff = now.getTime() - event.date.getTime();
					if(diff/(1000*60*60*24*30) > 30) {
						toBeRemoved.add(event);
					}
					if(OFFLINE.equals(event.ip)) {
						event.ip = getIpFromServer();
					}
				}
				events.removeAll(toBeRemoved);
			} catch (Exception e) {
				if(Debug.isDebuggerConnected()) {
					Log.d("[appMobi]", e.getMessage(), e);
				}
			}
		} else {
			events = new LinkedList<PageEvent>();
		}
	}
	
	public void logPageEvent(String page, String query, String status, String method, String bytes, String userReferer) {
		
		if( !webview.config.hasAnalytics ) return;
		
		final PageEvent event = new PageEvent(page, query, status, method, bytes, userReferer);
		
		new Thread("AppMobiAnalytics:logPageEvent") {
			public void run() {
				synchronized(lock) {
					events.add(event);
					saveEvents();
					submitEvents();
				}
			}
		}.start();
	}
	
	private void submitEvents() {
		if(webview!=null && webview.config != null && webview.config.analyticsUrl!=null && webview.config.analyticsUrl.length()>0) {
			ArrayList<PageEvent> toBeRemoved = new ArrayList<PageEvent>(0);
			for(PageEvent event:events) {
				String url = webview.config.analyticsUrl + "?Action=SendMessage&MessageBody=" + event.getPageString() + "&Version=2009-02-01";
				DefaultHttpClient httpclient = new DefaultHttpClient();
				HttpResponse response = null;
				try {
					response = httpclient.execute(new HttpGet(url));
				} catch (Exception e) {
					if(Debug.isDebuggerConnected()) {
						Log.d("[appMobi]", e.getMessage(), e);
					}
				}
				if(response!=null && response.getEntity()!=null) {
					int responseLength = (int)response.getEntity().getContentLength();
					ByteArrayOutputStream baos = new ByteArrayOutputStream(responseLength>0?responseLength:1024);
					try {
						FileUtils.copyInputStream(response.getEntity().getContent(), baos);
					} catch (Exception e) {
					}
					if(baos.toString().indexOf("<SendMessageResponse")!=-1) {
						toBeRemoved.add(event);
					}
				}
			}
			events.removeAll(toBeRemoved);
			saveEvents();
		}
	}
	
	private static String getIpFromServer() {
		String ip = null;
		String url = "http://services.appmobi.com/external/echoip.aspx";
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpResponse response = null;
		try {
			response = httpclient.execute(new HttpGet(url));
		} catch (Exception e) {
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]", e.getMessage(), e);
			}
		}
		// make sure we get a response and that it is only an IP -- hotel wifi accepting screen bypass
		if(response!=null && response.getEntity()!=null && response.getEntity().getContentLength()<16) {
			int responseLength = (int)response.getEntity().getContentLength();
			ByteArrayOutputStream baos = new ByteArrayOutputStream(responseLength>0?responseLength:1024);
			try {
				FileUtils.copyInputStream(response.getEntity().getContent(), baos);
			} catch (Exception e) {
			}
			String temp = baos.toString();
			if(response.getStatusLine().getStatusCode()==200 && temp.length()>0) ip = temp;
		}
		return ip==null?OFFLINE:ip;
	}
	
	static String getDeviceKey() {
		if(deviceKey==null) {
			initStoredDeviceKey();
		}
		return deviceKey;
	}
	
	private static void initStoredDeviceKey() {
		final String USER_DATA = "analytics_user_data";
		SharedPreferences prefs = AppMobiActivity.sharedActivity.getSharedPreferences(AppInfo.APP_PREFS, Context.MODE_PRIVATE);
		deviceKey = prefs.getString(USER_DATA, null);
		
		if(deviceKey==null) {
			String deviceid = AppMobiActivity.sharedActivity.getDeviceID();
			long timestamp = System.currentTimeMillis();
			deviceKey = (deviceid.hashCode() + "." + timestamp).replace(' ', '+');
			
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(USER_DATA, deviceKey);
			editor.commit();
		}
	}

	final static class PageEvent {
		private String page;
		private String query;
		private String status;
		private String method;
		private String bytes;
		private Date date;
		private String dateTime;
		private String ip;
		private String userAgent;
		private String referer;
		
		public PageEvent() {
			super();
		}

		public PageEvent(String page, String query, String status, String method, String bytes, String userReferer) {
			super();
			
			this.page = page.replace(' ', '+');
			this.query = query.replace(' ', '+');
			this.status = status.replace(' ', '+');
			this.method = method.replace(' ', '+');
			this.bytes = bytes.replace(' ', '+');
			
			//format the date for the log
			SimpleDateFormat format = new  SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));
			format.setCalendar(cal);
			this.date = new Date();
			this.dateTime = format.format(this.date);

			this.ip = getIpFromServer();
			
			this.userAgent = 
				("Mozilla/5.0+(Linux;+U;+Android+" + Build.VERSION.RELEASE + //osVersion
				";+en-us;+" + Build.MODEL + //device name
				"+Build/FRF91)+AppleWebKit/533.1+(KHTML,+like+Gecko)+Version/4.0+Mobile+Safari/533.1+(" + AppMobiAnalytics.deviceKey + ")").replace(' ', '+');
			
			this.referer = ("http://" + AppMobiActivity.sharedActivity.configData.appName + "-" + AppMobiActivity.sharedActivity.configData.releaseName + "-" + userReferer).replace(' ', '+');

		}

		//check if the ip is valid, if not try to get it again
		//return the 
		private String getPageString() {
			if(this.ip == OFFLINE) {
				this.ip = getIpFromServer();
			}
			String pageStr = URLEncoder.encode(
					(dateTime + " " + //date/time
							ip + " " + deviceKey + " " + //ip, userData
							method + " " + //method
							page + " " + query + " " + status + " " + //page, query, status
							bytes + " " + //bytes
							userAgent + " " + referer)
						);
			
			if(Debug.isDebuggerConnected()) {
				Log.d("[appMobi]","logPageEvent ~~ " + pageStr);
			}

			return pageStr;
		}

		//write to json
		public static String toJSON(Collection<PageEvent> pageEvents) {
			String json = new Gson().toJson(pageEvents, new TypeToken<Collection<PageEvent>>(){}.getType());
			return json;
		}
		
		//read from JSON
		public static Collection<PageEvent> fromJSON(String json) {
			Collection<PageEvent> pageEvents = null;
			pageEvents = new Gson().fromJson(json, new TypeToken<Collection<PageEvent>>(){}.getType());
			return pageEvents;
		}
	}
	
}